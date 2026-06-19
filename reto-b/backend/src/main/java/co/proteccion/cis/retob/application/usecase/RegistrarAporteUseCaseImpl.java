package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.application.AporteLimites;
import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private static final DateTimeFormatter PERIODO_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;
    private final AporteLimites limites;
    private final Clock clock;

    @Override
    @Transactional
    public Aporte registrar(RegistrarAporteCommand command) {
        // 1. Idempotencia: si ya se registró con esta clave, retornar el aporte existente
        Optional<Aporte> existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            return existente.get();
        }

        // La fecha del aporte la provee el cliente; no se admiten aportes con fecha futura
        LocalDate fecha = command.fecha();
        if (fecha.isAfter(LocalDate.now(clock))) {
            throw new ReglaNegocioException("La fecha del aporte no puede ser futura.");
        }
        // El periodo del tope mensual se deriva de la fecha del aporte (no de la fecha actual)
        String periodo = fecha.format(PERIODO_FMT);

        // 2. Verificar tope mensual (get-or-init del saldo del mes)
        SaldoMensual saldo = saldoRepository.findByAfiliadoIdAndMes(command.afiliadoId(), periodo)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodo));

        BigDecimal nuevoTotal = saldo.calcularNuevoTotal(command.monto());
        if (nuevoTotal.compareTo(limites.topeMensual()) > 0) {
            BigDecimal disponible = limites.topeMensual().subtract(saldo.getTotal());
            throw new ReglaNegocioException(
                    String.format("El aporte supera el tope mensual. Disponible este mes: $%s", disponible.toPlainString())
            );
        }

        // 3. Marcar para revisión si supera el umbral
        boolean marcadaRevision = command.monto().compareTo(limites.umbralRevision()) > 0;

        // 4. Persistir el aporte
        Aporte nuevo = new Aporte(null, command.afiliadoId(), command.monto(), fecha,
                command.canal(), periodo, marcadaRevision, command.idempotenciaKey());
        Aporte persistido = aporteRepository.guardar(nuevo);

        // 5. Actualizar el saldo mensual (concurrencia optimista via @Version en la entity)
        saldoRepository.guardar(saldo.conTotal(nuevoTotal));

        return persistido;
    }
}
