package co.proteccion.cis.retob.application.service;

import co.proteccion.cis.retob.domain.exception.IdempotenciaEnProcesoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.IdempotenciaRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.IdempotenciaRepositoryPort.Estado;
import co.proteccion.cis.retob.domain.port.out.IdempotenciaRepositoryPort.Registro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotenciaAporteService {

    private final IdempotenciaRepositoryPort idempotenciaRepository;
    private final AporteRepositoryPort aporteRepository;

    public Optional<Aporte> resolverAporteExistente(String idempotenciaKey) {
        return idempotenciaRepository.findByKey(idempotenciaKey)
                .filter(registro -> registro.estado() == Estado.COMPLETADO && registro.aporteId() != null)
                .flatMap(registro -> aporteRepository.findById(registro.aporteId()));
    }

    public boolean intentarClaim(String idempotenciaKey) {
        return idempotenciaRepository.intentarClaim(idempotenciaKey);
    }

    public Aporte resolverTrasConflicto(String idempotenciaKey) {
        return idempotenciaRepository.findByKey(idempotenciaKey)
                .flatMap(this::mapearRegistro)
                .orElseThrow(() -> new IllegalStateException(
                        "Clave de idempotencia duplicada sin registro recuperable: " + idempotenciaKey));
    }

    public void completar(String idempotenciaKey, Long aporteId) {
        idempotenciaRepository.completar(idempotenciaKey, aporteId);
    }

    public void liberarClaim(String idempotenciaKey) {
        idempotenciaRepository.liberarClaim(idempotenciaKey);
    }

    private Optional<Aporte> mapearRegistro(Registro registro) {
        if (registro.estado() == Estado.COMPLETADO && registro.aporteId() != null) {
            return aporteRepository.findById(registro.aporteId());
        }
        if (registro.estado() == Estado.EN_PROCESO) {
            throw new IdempotenciaEnProcesoException(registro.idempotenciaKey());
        }
        return Optional.empty();
    }
}
