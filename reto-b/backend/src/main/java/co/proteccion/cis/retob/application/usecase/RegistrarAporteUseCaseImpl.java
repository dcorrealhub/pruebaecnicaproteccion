package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AfiliadoNotFoundException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.domain.model.ParametrosFondo;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AfiliadoRepository;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.ParametroRepository;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;
    private final AfiliadoRepository afiliadoRepository;
    private final ParametroRepository parametroRepository;

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensualDefault;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevisionDefault;

    @Override
    @Transactional
    public Aporte registrar(RegistrarAporteCommand command) {
        // 1. Idempotencia: si ya existe con esta key, retornar sin duplicar
        var existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            return existente.get();
        }

        // 2. Validar que el afiliado exista
        afiliadoRepository.findByAfiliadoId(command.afiliadoId())
                .orElseThrow(() -> new AfiliadoNotFoundException(command.afiliadoId()));

        // 3. Obtener parámetros activos (fallback a env vars si la tabla está vacía)
        ParametrosFondo params = parametroRepository.findLatest().orElse(null);
        BigDecimal topeMensual    = params != null ? params.getTopeMensual()    : topeMensualDefault;
        BigDecimal umbralRevision = params != null ? params.getUmbralRevision() : umbralRevisionDefault;

        // 4. Calcular periodo (YYYY-MM) y obtener o crear saldo mensual
        LocalDate hoy = LocalDate.now();
        String periodo = hoy.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        SaldoMensual saldo = saldoRepository
                .findByAfiliadoIdAndMes(command.afiliadoId(), periodo)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodo));

        // 5. Validar tope mensual
        BigDecimal nuevoTotal = saldo.calcularNuevoTotal(command.monto());
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            throw new TopeMensualExcedidoException(topeMensual, saldo.getTotal(), command.monto());
        }

        // 6. Determinar estado según umbral de revisión
        EstadoAporte estado = command.monto().compareTo(umbralRevision) > 0
                ? EstadoAporte.EN_REVISION
                : EstadoAporte.PENDIENTE;

        // 7. Persistir aporte
        Aporte aporte = new Aporte(
                null,
                command.afiliadoId(),
                command.monto(),
                hoy,
                command.canal(),
                periodo,
                estado,
                command.idempotenciaKey()
        );
        Aporte guardado = aporteRepository.guardar(aporte);

        // 8. Actualizar saldo mensual (lock optimista vía @Version)
        saldoRepository.guardar(saldo.conTotal(nuevoTotal));

        return guardado;
    }
}
