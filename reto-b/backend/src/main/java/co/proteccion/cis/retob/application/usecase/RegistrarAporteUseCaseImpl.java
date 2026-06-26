package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.config.AporteProperties;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort  saldoRepository;
    private final AporteProperties     properties;

    @Override
    @Transactional
    public Aporte registrar(RegistrarAporteCommand command) {

        // 1. Idempotencia — reintento seguro
        return aporteRepository.findByIdempotenciaKey(command.idempotenciaKey())
                .orElseGet(() -> crearNuevo(command));
    }

    private Aporte crearNuevo(RegistrarAporteCommand command) {

        // 2. Validar monto positivo
        if (command.monto() == null || command.monto().signum() <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor a cero");
        }

        LocalDate hoy = LocalDate.now();
        String mes = hoy.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // 3. Obtener o inicializar saldo mensual
        SaldoMensual saldo = saldoRepository
                .findByAfiliadoIdAndMes(command.afiliadoId(), mes)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), mes));

        // 4. Validar tope mensual
        var nuevoTotal = saldo.calcularNuevoTotal(command.monto());
        if (nuevoTotal.compareTo(properties.topeMensual()) > 0) {
            throw new ReglaNegocioException(
                    "Supera el tope mensual permitido de " + properties.topeMensual()
            );
        }

        // 5. Marcar para revisión si supera umbral
        boolean requiereRevision = command.monto()
                .compareTo(properties.umbralRevision()) > 0;

        // 6. Persistir aporte
        Aporte nuevo = Aporte.nuevo(
                command.afiliadoId(),
                command.monto(),
                hoy,
                command.canal(),
                requiereRevision,
                command.idempotenciaKey()
        );
        Aporte persistido = aporteRepository.guardar(nuevo);

        // 7. Actualizar saldo mensual
        saldoRepository.guardar(saldo.conTotal(nuevoTotal));

        return persistido;
    }
}
