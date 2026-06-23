package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Implementación del caso de uso de registro de aportes voluntarios.
 *
 * <p>Flujo principal:
 * <ol>
 *   <li>Idempotencia: si ya existe un aporte con la misma {@code idempotenciaKey},
 *       retornarlo sin persistir duplicados.</li>
 *   <li>Validar que el monto sea positivo.</li>
 *   <li>Obtener o inicializar el saldo mensual del afiliado.</li>
 *   <li>Validar que el monto no supere el tope mensual disponible.</li>
 *   <li>Determinar si el aporte debe marcarse para revisión.</li>
 *   <li>Actualizar el saldo mensual.</li>
 *   <li>Persistir el aporte y retornarlo.</li>
 * </ol>
 *
 * <p>Todo el flujo corre dentro de una única transacción. Si cualquier paso
 * falla, la transacción se revierte completamente.
 */
@Service
@RequiredArgsConstructor
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort  saldoRepository;

    /** Tope máximo de aportes acumulados en un mes por afiliado. */
    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensual;

    /** Monto a partir del cual el aporte se marca para revisión manual. */
    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevision;

    // Formato del campo periodo: YYYY-MM
    private static final DateTimeFormatter PERIODO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM");

    // -------------------------------------------------------------------------
    // Caso de uso
    // -------------------------------------------------------------------------

    /**
     * Registra un aporte voluntario aplicando todas las reglas de negocio.
     *
     * @param command datos del aporte (afiliadoId, monto, canal, idempotenciaKey)
     * @return el aporte persistido, con id, periodo, fecha y marcadaRevision asignados
     * @throws IllegalArgumentException si el monto no es positivo o supera el tope mensual
     */
    @Override
    @Transactional
    public Aporte registrar(RegistrarAporteCommand command) {

        // ------------------------------------------------------------------
        // 1. IDEMPOTENCIA
        //    Si ya existe un aporte con la misma clave, lo retornamos tal
        //    cual sin persistir un duplicado. Garantiza que reintentos del
        //    cliente (por timeout, red inestable, etc.) sean seguros.
        // ------------------------------------------------------------------
        return aporteRepository
                .findByIdempotenciaKey(command.idempotenciaKey())
                .orElseGet(() -> registrarNuevoAporte(command));
    }

    // -------------------------------------------------------------------------
    // Flujo interno — solo se ejecuta cuando no hay idempotencia previa
    // -------------------------------------------------------------------------

    private Aporte registrarNuevoAporte(RegistrarAporteCommand command) {

        // ------------------------------------------------------------------
        // 2. VALIDACIÓN DE MONTO POSITIVO
        //    BigDecimal.signum() retorna -1, 0 o 1. Solo signum == 1 indica
        //    un valor estrictamente positivo. La validación @DecimalMin del DTO
        //    ya filtra esto en el controlador, pero la regla de negocio debe
        //    estar también en el caso de uso (defensa en profundidad).
        // ------------------------------------------------------------------
        if (command.monto().signum() <= 0) {
            throw new IllegalArgumentException(
                    "El monto del aporte debe ser mayor a cero. Recibido: " + command.monto());
        }

        // ------------------------------------------------------------------
        // 3. PERIODO Y FECHA
        //    El periodo se deriva de la fecha actual: mismo mes en que se
        //    registra el aporte. Formato YYYY-MM, consistente con la BD y
        //    el modelo de dominio.
        // ------------------------------------------------------------------
        LocalDate hoy     = LocalDate.now();
        String    periodo = hoy.format(PERIODO_FORMATTER);

        // ------------------------------------------------------------------
        // 4. OBTENER O INICIALIZAR SALDO MENSUAL
        //    Si el afiliado no tiene saldo para este mes, se crea con total=0.
        //    El adaptador garantiza que la inicialización sea atómica.
        // ------------------------------------------------------------------
        SaldoMensual saldo = saldoRepository
                .findByAfiliadoIdAndMes(command.afiliadoId(), periodo)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodo));

        // ------------------------------------------------------------------
        // 5. VALIDACIÓN DE TOPE MENSUAL
        //    El acumulado actual más el nuevo monto no puede superar el tope.
        //    Se usa compareTo para comparación exacta de BigDecimal (evita
        //    problemas con equals y distintas escalas decimales).
        // ------------------------------------------------------------------
        BigDecimal nuevoTotal = saldo.calcularNuevoTotal(command.monto());
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            BigDecimal disponible = topeMensual.subtract(saldo.getTotal());
            throw new IllegalArgumentException(String.format(
                    "El aporte de %s supera el tope mensual disponible de %s para el afiliado %s en %s.",
                    command.monto(), disponible, command.afiliadoId(), periodo));
        }

        // ------------------------------------------------------------------
        // 6. MARCADO PARA REVISIÓN
        //    Un aporte se marca si su monto individual supera el umbral de
        //    revisión. Es una decisión sobre el aporte individual, no sobre
        //    el acumulado mensual.
        // ------------------------------------------------------------------
        boolean marcadaRevision = command.monto().compareTo(umbralRevision) > 0;

        // ------------------------------------------------------------------
        // 7. ACTUALIZAR SALDO MENSUAL
        //    Se usa el método de dominio conTotal() que produce un nuevo
        //    SaldoMensual inmutable con el total actualizado, preservando
        //    el version para la concurrencia optimista.
        //    Si hay conflicto de concurrencia, Hibernate lanza
        //    OptimisticLockException antes de retornar — la transacción
        //    se revierte automáticamente.
        // ------------------------------------------------------------------
        saldoRepository.guardar(saldo.conTotal(nuevoTotal));

        // ------------------------------------------------------------------
        // 8. CONSTRUIR Y PERSISTIR EL APORTE
        //    id=null porque la BD asigna el identificador (IDENTITY).
        //    fecha=hoy, periodo=mes actual.
        // ------------------------------------------------------------------
        Aporte nuevoAporte = new Aporte(
                null,
                command.afiliadoId(),
                command.monto(),
                hoy,
                command.canal(),
                periodo,
                marcadaRevision,
                command.idempotenciaKey()
        );

        return aporteRepository.guardar(nuevoAporte);
    }
}
