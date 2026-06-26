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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AporteService {

    private final AporteJpaRepository aporteRepo;
    private final SaldoJpaRepository saldoRepo;
    private final EventoAporteJpaRepository eventoRepo;

    // @Value no soporta BigDecimal directamente; se usa String y se convierte al usarlo
    @Value("${aporte.tope-mensual:10000000}")
    private String topeMensualStr;

    @Value("${aporte.umbral-revision:5000000}")
    private String umbralRevisionStr;

    public Aporte registrar(AporteRequest req) {
        BigDecimal topeMensual    = new BigDecimal(topeMensualStr);
        BigDecimal umbralRevision = new BigDecimal(umbralRevisionStr);
        BigDecimal monto          = req.getMonto();

        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        Saldo s = saldoRepo.findByAfiliadoId(req.getAfiliadoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Afiliado no encontrado: " + req.getAfiliadoId()));

        // Hallazgo N° 4 corregido: compareTo() > 0 en lugar de == topeMensual
        BigDecimal nuevo = s.getTotalMes().add(monto);
        if (nuevo.compareTo(topeMensual) > 0) {
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
        aporte.setMarcadaRevision(monto.compareTo(umbralRevision) > 0);

        eventoRepo.save(new EventoAporte(aporte));

        Aporte saved = aporteRepo.save(aporte);

        // Hallazgo N° 10 parcial: INFO no expone el monto exacto ni el afiliadoId
        log.info("Aporte registrado. aporteId={} periodo={} marcadaRevision={}",
                saved.getId(), saved.getPeriodo(), saved.isMarcadaRevision());

        return saved;
    }

    public List<Aporte> consolidado(String afiliadoId, String periodo) {
        return aporteRepo.findByAfiliadoIdAndPeriodo(afiliadoId, periodo);
    }
}
