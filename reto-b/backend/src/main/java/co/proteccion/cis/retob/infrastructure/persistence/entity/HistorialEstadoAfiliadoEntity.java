package co.proteccion.cis.retob.infrastructure.persistence.entity;

import co.proteccion.cis.retob.domain.model.EstadoAfiliado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "historial_estado_afiliado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEstadoAfiliadoEntity {

    @Id
    @Column(columnDefinition = "UUID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "afiliado_id", nullable = false, length = 50)
    private String afiliadoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 20)
    private EstadoAfiliado estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoAfiliado estadoNuevo;

    @Column(name = "cambiado_en", nullable = false)
    private OffsetDateTime cambiadoEn;
}
