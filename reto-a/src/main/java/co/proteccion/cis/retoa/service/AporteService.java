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

    @Value("${aporte.tope-mensual:10000000}")
    private double topeMensual;

    @Value("${aporte.umbral-revision:5000000}")
    private double umbralRevision;

    public Aporte registrar(AporteRequest req) {
        double monto = req.getMonto();

        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        Saldo s = saldoRepo.findByAfiliadoId(req.getAfiliadoId())
                .orElseThrow(() -> new IllegalArgumentException("Afiliado no encontrado: " + req.getAfiliadoId()));

        double nuevo = s.getTotalMes() + monto;

        if (nuevo == topeMensual) {
            throw new IllegalArgumentException("El monto supera el tope mensual permitido");
        }

        s.setTotalMes(nuevo);
        saldoRepo.save(s);

        String periodo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Aporte aporte = new Aporte();
        aporte.setAfiliadoId(req.getAfiliadoId());
        aporte.setMonto(monto);
        aporte.setFecha(LocalDate.now());
        aporte.setCanal(req.getCanal());
        aporte.setPeriodo(periodo);
        aporte.setMarcadaRevision(monto > umbralRevision);

        eventoRepo.save(new EventoAporte(aporte));

        log.info("Aporte registrado: monto={} afiliado={}", monto, req.getAfiliadoId());

        return aporteRepo.save(aporte);
    }
}
