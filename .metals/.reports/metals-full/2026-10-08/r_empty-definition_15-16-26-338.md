error id: file:///C:/Users/Mariana.DESKTOP-GV675UP/Desktop/pruebaecnicaproteccion/reto-b/backend/src/main/java/co/proteccion/cis/retob/application/usecase/RegistrarAporteUseCaseImpl.java:_empty_/RegistrarAporteCommand#monto#
file:///C:/Users/Mariana.DESKTOP-GV675UP/Desktop/pruebaecnicaproteccion/reto-b/backend/src/main/java/co/proteccion/cis/retob/application/usecase/RegistrarAporteUseCaseImpl.java
empty definition using pc, found symbol in pc: _empty_/RegistrarAporteCommand#monto#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 4937
uri: file:///C:/Users/Mariana.DESKTOP-GV675UP/Desktop/pruebaecnicaproteccion/reto-b/backend/src/main/java/co/proteccion/cis/retob/application/usecase/RegistrarAporteUseCaseImpl.java
text:
```scala
package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Implementación del caso de uso de registro de aportes.
 *
 * TODO (candidato): implementar la lógica de negocio:
 *   1. Verificar idempotencia: si ya existe un aporte con la misma idempotenciaKey, retornarlo.
 *   2. Validar monto positivo y que no supere el tope mensual del afiliado.
 *   3. Marcar para revisión si el monto supera {@code umbralRevision}.
 *   4. Actualizar el saldo mensual del afiliado de forma concurrentemente segura.
 *   5. Persistir el aporte y publicar el evento correspondiente.
 *   6. Envolver todo en una transacción (@Transactional).
 */
@Service
@RequiredArgsConstructor
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {
  
    private static final int MAX_REINTENTOS = 3;


    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensual;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevision;

    @Value("${aporte.umbral-revision-sucursal:3000000}")
    private BigDecimal umbralRevisionSucursal;

    public RegistrarAporteUseCaseImpl(AporteRepositoryPort aporteRepository,
                                       SaldoRepositoryPort saldoRepository,
                                       PlatformTransactionManager transactionManager) {
        this.aporteRepository = aporteRepository;
        this.saldoRepository = saldoRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Aporte registrar(RegistrarAporteCommand command) {

        // 1. Idempotencia: si ya existe un aporte con esta clave, se devuelve tal cual (marcado yaExistia=true)
        var existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            Aporte a = existente.get();
            return new Aporte(a.getId(), a.getAfiliadoId(), a.getMonto(), a.getFecha(),
                    a.getCanal(), a.getPeriodo(), a.isMarcadaRevision(), a.getIdempotenciaKey(), true);
        }

        // 2. Validacion basica del monto
        if (command.monto() == null || command.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        // 3. Logica critica (saldo + aporte) con reintentos por conflicto de concurrencia
        int intentos = 0;
        while (true) {
            try {
                return transactionTemplate.execute(status -> registrarEnTransaccion(command));
            } catch (OptimisticLockingFailureException e) {
                intentos++;
                if (intentos >= MAX_REINTENTOS) {
                    throw new IllegalStateException(
                            "No se pudo registrar el aporte tras " + MAX_REINTENTOS +
                                    " intentos por alta concurrencia. Intente de nuevo.", e);
                }
                // el ciclo vuelve a intentar: la siguiente vuelta lee el saldo ya actualizado
            }
        }
    }

    private Aporte registrarEnTransaccion(RegistrarAporteCommand command) {
        String periodo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        SaldoMensual saldo = saldoRepository.findByAfiliadoIdAndMes(command.afiliadoId(), periodo)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodo));

        BigDecimal nuevoTotal = saldo.calcularNuevoTotal(command.monto());

        if (nuevoTotal.compareTo(topeMensual) > 0) {
            throw new IllegalArgumentException("El monto supera el tope mensual permitido");
        }

        saldoRepository.guardar(saldo.conTotal(nuevoTotal));

       BigDecimal umbralAplicable = "SUCURSAL".equals(command.canal())
                ? umbralRevisionSucursal
                : umbralRevision;
        boolean marcarRevision = command.m@@onto().compareTo(umbralAplicable) > 0;

        Aporte nuevoAporte = new Aporte(
                null,
                command.afiliadoId(),
                command.monto(),
                LocalDate.now(),
                command.canal(),
                periodo,
                marcarRevision,
                command.idempotenciaKey(),
                false
        );

        return aporteRepository.guardar(nuevoAporte);
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/RegistrarAporteCommand#monto#