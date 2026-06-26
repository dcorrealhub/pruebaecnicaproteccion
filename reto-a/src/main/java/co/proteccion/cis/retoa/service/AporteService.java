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

    @Transactional(rollbackFor = Exception.class)
    public Aporte registrar(AporteRequest req) {
        // Hallazgo N° 5: rechazar antes de cualquier operacion si la llave ya fue usada
        if (aporteRepo.existsByIdempotencyKey(req.getIdempotencyKey())) {
            throw new IllegalStateException(
                "Aporte duplicado: llave de idempotencia ya utilizada");
        }

        BigDecimal topeMensual    = new BigDecimal(topeMensualStr);
        BigDecimal umbralRevision = new BigDecimal(umbralRevisionStr);
        BigDecimal monto          = req.getMonto();

        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        // Hallazgo N° 3: locking pesimista serializa el acceso al saldo bajo concurrencia
        // Hallazgo N° 9: mensaje genérico — no exponer afiliadoId al cliente
        Saldo s = saldoRepo.findByAfiliadoIdForUpdate(req.getAfiliadoId())
                .orElseThrow(() -> new IllegalArgumentException("Afiliado no encontrado"));

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
        aporte.setIdempotencyKey(req.getIdempotencyKey());

        // Hallazgo N° 14: persistir aporte primero para obtener el ID antes de crear el evento
        Aporte saved = aporteRepo.save(aporte);
        eventoRepo.save(new EventoAporte(saved));

        // Hallazgo N° 10: INFO solo publica metadatos no financieros; monto en DEBUG
        log.info("Aporte registrado. aporteId={} periodo={} marcadaRevision={}",
                saved.getId(), saved.getPeriodo(), saved.isMarcadaRevision());
        log.debug("Aporte detalle. aporteId={} monto={} afiliadoId={}",
                saved.getId(), saved.getMonto(), saved.getAfiliadoId());

        return saved;
    }

    public List<Aporte> consolidado(String afiliadoId, String periodo) {
        return aporteRepo.findByAfiliadoIdAndPeriodo(afiliadoId, periodo);
    }
}
