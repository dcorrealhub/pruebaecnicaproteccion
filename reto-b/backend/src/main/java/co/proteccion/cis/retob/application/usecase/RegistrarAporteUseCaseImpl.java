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
import co.proteccion.cis.retob.infrastructure.persistence.adapter.SaldoInicializadorAdapter;
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
    private final SaldoInicializadorAdapter saldoInicializador;

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensualDefault;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevisionDefault;

    @Override
    @Transactional
    public Aporte registrar(RegistrarAporteCommand command) {
        // 1. Idempotencia — reenvío de la misma key retorna el aporte original
        var existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            return existente.get();
        }

        // 2. Validar existencia del afiliado
        afiliadoRepository.findByAfiliadoId(command.afiliadoId())
                .orElseThrow(() -> new AfiliadoNotFoundException(command.afiliadoId()));

        // 3. Parámetros activos (fallback a env vars si historico_parametros está vacío)
        ParametrosFondo params = parametroRepository.findLatest().orElse(null);
        BigDecimal topeMensual    = params != null ? params.getTopeMensual()    : topeMensualDefault;
        BigDecimal umbralRevision = params != null ? params.getUmbralRevision() : umbralRevisionDefault;

        // 4. Saldo mensual — REQUIRES_NEW aísla la race condition de inicialización
        LocalDate hoy = LocalDate.now();
        String periodo = hoy.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        SaldoMensual saldo = saldoInicializador.obtenerOInicializar(command.afiliadoId(), periodo);

        // 5. Tope mensual
        BigDecimal nuevoTotal = saldo.calcularNuevoTotal(command.monto());
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            throw new TopeMensualExcedidoException(topeMensual, saldo.getTotal(), command.monto());
        }

        // 6. Estado automático por umbral
        EstadoAporte estado = command.monto().compareTo(umbralRevision) > 0
                ? EstadoAporte.EN_REVISION
                : EstadoAporte.PENDIENTE;

        // 7. Persistir aporte
        Aporte aporte = new Aporte(null, command.afiliadoId(), command.monto(), hoy,
                command.canal(), periodo, estado, command.idempotenciaKey());
        Aporte guardado = aporteRepository.guardar(aporte);

        // 8. Actualizar saldo — @Version garantiza atomicidad bajo concurrencia
        saldoRepository.guardar(saldo.conTotal(nuevoTotal));

        return guardado;
    }
}
