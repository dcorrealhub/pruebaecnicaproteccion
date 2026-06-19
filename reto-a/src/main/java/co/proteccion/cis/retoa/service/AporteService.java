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

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensual;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevision;

    /**
     * Registra un nuevo aporte para un afiliado, actualiza su saldo acumulado
     * del mes y genera el evento de auditoría correspondiente.
     *
     * @throws IllegalArgumentException si el monto es inválido, el afiliado
     *         no existe, o el acumulado mensual supera el tope permitido.
     */
    @Transactional
    public Aporte registrar(AporteRequest req) {
        BigDecimal monto = req.getMonto();

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        Saldo s = saldoRepo.findByAfiliadoIdForUpdate(req.getAfiliadoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Afiliado no encontrado: " + req.getAfiliadoId()));

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

        Aporte guardado = aporteRepo.save(aporte);

        eventoRepo.save(new EventoAporte(guardado));

        log.info("Aporte registrado: monto={} afiliado={}", monto, req.getAfiliadoId());

        return guardado;
    }

    public List<Aporte> consolidado(String afiliadoId, String periodo) {
        return aporteRepo.findByAfiliadoIdAndPeriodo(afiliadoId, periodo);
    }
}