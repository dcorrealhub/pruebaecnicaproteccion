package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Adaptador JPA para {@link SaldoRepositoryPort}.
 *
 * Responsabilidad única: traducir entre el modelo de dominio {@link SaldoMensual}
 * y la entidad JPA {@link SaldoMensualEntity}, delegando la persistencia en
 * {@link SpringDataSaldoRepository}. No contiene lógica de negocio.
 *
 * La concurrencia optimista es gestionada automáticamente por Hibernate
 * gracias a {@code @Version} en {@link SaldoMensualEntity}: si la versión
 * no coincide al guardar, Hibernate lanza {@code OptimisticLockException}
 * antes de retornar. El adaptador no intercepta esa excepción — es
 * responsabilidad del caso de uso decidir cómo manejarla.
 */
@Repository
@RequiredArgsConstructor
public class JpaSaldoRepositoryAdapter implements SaldoRepositoryPort {

    private final SpringDataSaldoRepository springDataRepo;

    // -------------------------------------------------------------------------
    // Puerto de salida — implementación
    // -------------------------------------------------------------------------

    /**
     * Busca el saldo acumulado de un afiliado en un mes concreto.
     *
     * Retorna {@link Optional#empty()} cuando el afiliado no tiene saldo
     * registrado para ese mes todavía. El caso de uso usa ese vacío como
     * señal para llamar a {@link #inicializar}.
     */
    @Override
    public Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes) {
        return springDataRepo
                .findByAfiliadoIdAndMes(afiliadoId, mes)
                .map(this::toDomain);
    }

    /**
     * Persiste un saldo existente con un nuevo total.
     *
     * El objeto {@code saldo} siempre llega con {@code id} y {@code version}
     * conocidos (fue leído previamente por {@link #findByAfiliadoIdAndMes}).
     * JPA ejecuta un UPDATE, no un INSERT.
     *
     * Hibernate incluye automáticamente {@code WHERE version = ?} en el UPDATE
     * gracias a {@code @Version}. Si otro hilo modificó el registro entre la
     * lectura y este guardado, Hibernate lanza {@code OptimisticLockException}
     * antes de retornar — el adaptador no la captura.
     *
     * El objeto retornado contiene la {@code version} ya incrementada por
     * Hibernate, que es el valor correcto para cualquier operación posterior.
     */
    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        SaldoMensualEntity entidad = toEntity(saldo);
        SaldoMensualEntity guardada = springDataRepo.save(entidad);
        return toDomain(guardada);
    }

    /**
     * Crea un saldo mensual nuevo para un afiliado con total cero.
     *
     * Se llama únicamente cuando {@link #findByAfiliadoIdAndMes} devuelve
     * {@link Optional#empty()}. El {@code id} se pasa como {@code null}
     * para que JPA ejecute un INSERT y la secuencia IDENTITY asigne el
     * identificador. La {@code version} arranca en {@code 0}, coherente
     * con el {@code DEFAULT 0} definido en {@code V1__init.sql}.
     *
     * El objeto retornado incluye el {@code id} generado por la base de datos.
     */
    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        SaldoMensualEntity nueva = SaldoMensualEntity.builder()
                .id(null)                    // INSERT: la BD asigna el id
                .afiliadoId(afiliadoId)
                .mes(mes)
                .total(BigDecimal.ZERO)      // acumulado inicial
                .version(0)                  // versión inicial, coherente con DEFAULT 0 en SQL
                .build();
        SaldoMensualEntity persistida = springDataRepo.save(nueva);
        return toDomain(persistida);
    }

    // -------------------------------------------------------------------------
    // Métodos privados de mapeo — sin lógica de negocio
    // -------------------------------------------------------------------------

    /**
     * Convierte la entidad JPA al objeto de dominio.
     *
     * Todos los campos se copian directamente, incluido {@code version}, que
     * el caso de uso necesita para detectar conflictos de concurrencia en
     * escrituras posteriores.
     */
    private SaldoMensual toDomain(SaldoMensualEntity entidad) {
        return new SaldoMensual(
                entidad.getId(),
                entidad.getAfiliadoId(),
                entidad.getMes(),
                entidad.getTotal(),
                entidad.getVersion()
        );
    }

    /**
     * Convierte el objeto de dominio a la entidad JPA.
     *
     * El campo {@code version} se pasa tal cual desde el dominio. Pasarlo como
     * {@code null} en un UPDATE haría que Hibernate omita la cláusula
     * {@code WHERE version = ?}, anulando la protección contra escrituras
     * concurrentes.
     */
    private SaldoMensualEntity toEntity(SaldoMensual saldo) {
        return SaldoMensualEntity.builder()
                .id(saldo.getId())
                .afiliadoId(saldo.getAfiliadoId())
                .mes(saldo.getMes())
                .total(saldo.getTotal())
                .version(saldo.getVersion())  // NUNCA null en un guardar: protege la concurrencia
                .build();
    }
}
