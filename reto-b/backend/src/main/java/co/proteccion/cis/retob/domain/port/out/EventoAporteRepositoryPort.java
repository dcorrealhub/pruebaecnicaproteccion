package co.proteccion.cis.retob.domain.port.out;

public interface EventoAporteRepositoryPort {

    void registrarEvento(Long aporteId, String tipo);
}
