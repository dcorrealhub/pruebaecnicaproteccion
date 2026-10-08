package co.proteccion.cis.retoa.service;

import co.proteccion.cis.retoa.domain.Aporte;
import co.proteccion.cis.retoa.domain.EventoAporte;
import co.proteccion.cis.retoa.domain.Saldo;
import co.proteccion.cis.retoa.dto.AporteRequest;
import co.proteccion.cis.retoa.repository.AporteJpaRepository;
import co.proteccion.cis.retoa.repository.EventoAporteJpaRepository;
import co.proteccion.cis.retoa.repository.SaldoJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AporteService {

    private final AporteJpaRepository aporteRepo;
    private final SaldoJpaRepository saldoRepo;
    private final EventoAporteJpaRepository eventoRepo;

    // H-005: tope y umbral monetarios como double.
    @Value("${aporte.tope-mensual:10000000}")
    private double topeMensual;

    @Value("${aporte.umbral-revision:5000000}")
    private double umbralRevision;

    // H-004: sin @Transactional; saldo, evento y aporte se confirman en tres commits independientes.
    public Aporte registrar(AporteRequest req) {
        // H-005: monto en double; 10 x 0.1 = 0.9999999999999999 y 1e400 llega como Infinity.
        double monto = req.getMonto();

        // H-005: Infinity pasa esta validación y se persiste.
        if (monto <= 0) {
            // H-013: IllegalArgumentException genérica; sin @RestControllerAdvice termina en HTTP 500.
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        // H-006: busca el saldo ignorando el mes; acumula en la fila de cualquier mes y falla si hay más de una.
        // H-003: lectura sin bloqueo (read-modify-write); aportes concurrentes se pisan.
        Saldo s = saldoRepo.findByAfiliadoId(req.getAfiliadoId())
                .orElseThrow(() -> new IllegalArgumentException("Afiliado no encontrado: " + req.getAfiliadoId()));

        double nuevo = s.getTotalMes() + monto;

        // H-002: compara con == ; cualquier acumulado mayor al tope se acepta y el exacto se rechaza.
        if (nuevo == topeMensual) {
            throw new IllegalArgumentException("El monto supera el tope mensual permitido");
        }

        // H-003: escritura last-write-wins sin @Version ni lock; 50 aportes concurrentes de 1.000 dejaron saldo 9.000.
        s.setTotalMes(nuevo);
        saldoRepo.save(s);

        // H-010: periodo calculado con la zona de la JVM y con un now() distinto al de la fecha.
        String periodo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Aporte aporte = new Aporte();
        aporte.setAfiliadoId(req.getAfiliadoId());
        aporte.setMonto(monto);
        // H-010: segunda llamada a now(); al cruzar medianoche de fin de mes fecha y periodo quedan inconsistentes.
        aporte.setFecha(LocalDate.now());
        // H-012: canal de texto libre sin validar.
        aporte.setCanal(req.getCanal());
        aporte.setPeriodo(periodo);
        aporte.setMarcadaRevision(monto > umbralRevision);

        // H-016: evento guardado antes que el aporte; queda sin aporteId, actor ni correlación.
        eventoRepo.save(new EventoAporte(aporte));

        // H-016: log de "registrado" antes de persistir el aporte y sin id de aporte.
        log.info("Aporte registrado: monto={} afiliado={}", monto, req.getAfiliadoId());

        return aporteRepo.save(aporte);
    }
}
