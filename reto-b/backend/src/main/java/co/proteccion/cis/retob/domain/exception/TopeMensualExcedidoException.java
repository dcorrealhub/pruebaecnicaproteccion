package co.proteccion.cis.retob.domain.exception;

import java.math.BigDecimal;

public class TopeMensualExcedidoException extends DomainException {

    public TopeMensualExcedidoException(String afiliadoId, String periodo,
                                        BigDecimal totalProyectado, BigDecimal tope) {
        super("TOPE_MENSUAL_EXCEDIDO",
                "El aporte excede el tope mensual para afiliado %s en periodo %s: total proyectado %s, tope %s"
                        .formatted(afiliadoId, periodo, totalProyectado, tope));
    }
}
