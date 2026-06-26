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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AporteService {

    private final AporteJpaRepository aporteRepo;
    private final SaldoJpaRepository saldoRepo;
    private final EventoAporteJpaRepository eventoRepo;

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensual;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevision;

    @Transactional
    public Aporte registrar(AporteRequest req) {
        BigDecimal monto = req.getMonto();

        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        String periodo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Saldo s = saldoRepo.findByAfiliadoIdAndMes(req.getAfiliadoId(), periodo)
                .orElseGet(() -> {
                    Saldo nuevoSaldo = new Saldo();
                    nuevoSaldo.setAfiliadoId(req.getAfiliadoId());
                    nuevoSaldo.setMes(periodo);
                    nuevoSaldo.setTotalMes(BigDecimal.ZERO);
                    return saldoRepo.save(nuevoSaldo);
                });

        BigDecimal nuevo = s.getTotalMes().add(monto);

        if (nuevo.compareTo(topeMensual) > 0) {
            throw new IllegalArgumentException("El monto supera el tope mensual permitido");
        }

        s.setTotalMes(nuevo);
        saldoRepo.save(s);

        Aporte aporte = new Aporte();
        aporte.setAfiliadoId(req.getAfiliadoId());
        aporte.setMonto(monto);
        aporte.setFecha(LocalDate.now());
        aporte.setCanal(req.getCanal());
        aporte.setPeriodo(periodo);
        aporte.setMarcadaRevision(monto.compareTo(umbralRevision) > 0);

        EventoAporte evento = new EventoAporte();
        evento.setAfiliadoId(aporte.getAfiliadoId());
        evento.setMonto(aporte.getMonto());
        evento.setTipo("APORTE_REGISTRADO");
        evento.setFechaEvento(LocalDateTime.now());
        eventoRepo.save(evento);

        log.info("Aporte registrado: monto={} afiliado={}", monto, req.getAfiliadoId());

        return aporteRepo.save(aporte);
    }

    public List<Aporte> obtenerConsolidado(String afiliadoId, String periodo) {
        return aporteRepo.findByAfiliadoIdAndPeriodo(afiliadoId, periodo);
    }
}
