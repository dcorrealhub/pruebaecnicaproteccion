package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private static final DateTimeFormatter PERIODO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensual;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevision;

    @Transactional
    @Override
    public Aporte registrar(RegistrarAporteCommand command) {
        Optional<Aporte> existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            log.info("Solicitud idempotente — clave ya procesada: {}", command.idempotenciaKey());
            return existente.get();
        }

        if (command.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("MONTO_INVALIDO", "El monto debe ser mayor a cero");
        }

        String periodo = command.fecha().format(PERIODO_FORMATTER);

        SaldoMensual saldo = saldoRepository
                .findByAfiliadoIdAndMes(command.afiliadoId(), periodo)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodo));

        BigDecimal nuevoTotal = saldo.getTotal().add(command.monto());
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            throw new ReglaNegocioException("TOPE_MENSUAL_EXCEDIDO",
                    String.format("El aporte superaría el tope mensual de %s para el periodo %s", topeMensual, periodo));
        }

        boolean marcadaRevision = command.monto().compareTo(umbralRevision) > 0;

        Aporte aporte = new Aporte(null, command.afiliadoId(), command.monto(), command.fecha(),
                command.canal(), periodo, marcadaRevision, command.idempotenciaKey());

        Aporte aportePersistido = aporteRepository.guardar(aporte);

        saldoRepository.guardar(saldo.conTotal(nuevoTotal));

        log.info("Aporte registrado — afiliadoId={}, periodo={}, monto={}, marcadaRevision={}",
                command.afiliadoId(), periodo, command.monto(), marcadaRevision);

        return aportePersistido;
    }
}
