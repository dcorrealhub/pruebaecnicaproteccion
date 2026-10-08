package co.proteccion.cis.retob.infrastructure.persistence;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.infrastructure.persistence.adapter.JpaAporteRepositoryAdapter;
import co.proteccion.cis.retob.infrastructure.persistence.adapter.JpaSaldoRepositoryAdapter;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({JpaAporteRepositoryAdapter.class, JpaSaldoRepositoryAdapter.class})
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:persistdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class PersistenciaIntegracionTest {

    @Autowired JpaAporteRepositoryAdapter aporteAdapter;
    @Autowired JpaSaldoRepositoryAdapter saldoAdapter;
    @Autowired TestEntityManager em;

    private Aporte aporte(String idempotenciaKey, LocalDate fecha, BigDecimal monto) {
        return Aporte.nuevo("AF-001", monto, fecha, Canal.WEB, false, idempotenciaKey);
    }

    @Test
    void idempotencia_unique_impide_duplicar_misma_clave() {
        aporteAdapter.guardar(aporte("key-dup", LocalDate.of(2025, 3, 10), new BigDecimal("1000")));

        assertThatThrownBy(() ->
                aporteAdapter.guardar(aporte("key-dup", LocalDate.of(2025, 3, 11), new BigDecimal("2000"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void consolidado_filtra_por_rango_de_periodo() {
        aporteAdapter.guardar(aporte("k-ene", LocalDate.of(2025, 1, 15), new BigDecimal("1000")));
        aporteAdapter.guardar(aporte("k-mar", LocalDate.of(2025, 3, 15), new BigDecimal("2000")));
        aporteAdapter.guardar(aporte("k-jun", LocalDate.of(2025, 6, 15), new BigDecimal("3000")));

        List<Aporte> enRango = aporteAdapter
                .findByAfiliadoIdAndPeriodoBetween("AF-001", "2025-02", "2025-05");

        assertThat(enRango).hasSize(1);
        assertThat(enRango.get(0).getPeriodo()).isEqualTo("2025-03");
    }

    @Test
    void monto_persiste_con_escala_2() {
        aporteAdapter.guardar(aporte("k-escala", LocalDate.of(2025, 3, 10), new BigDecimal("1234.5")));

        Aporte recuperado = aporteAdapter.findByIdempotenciaKey("k-escala").orElseThrow();
        assertThat(recuperado.getMonto()).isEqualByComparingTo("1234.50");
        assertThat(recuperado.getMonto().scale()).isEqualTo(2);
    }

    @Test
    void saldo_bloqueo_optimista_detecta_version_desactualizada() {
        // Persistir un saldo inicial (version 0) y vaciar el contexto de persistencia.
        SaldoMensualEntity inicial = em.persistFlushFind(
                new SaldoMensualEntity(null, "AF-001", "2025-03", new BigDecimal("0.00"), null));
        Integer versionVieja = inicial.getVersion();
        em.clear();

        // Un escritor actualiza el saldo: la versión en BD avanza a 1.
        SaldoMensual actual = saldoAdapter.findByAfiliadoIdAndMes("AF-001", "2025-03").orElseThrow();
        saldoAdapter.guardar(actual.conTotal(new BigDecimal("1000.00")));
        em.flush();
        em.clear();

        // Un segundo escritor intenta guardar partiendo de la versión vieja (detached) -> conflicto.
        SaldoMensual rezagado = new SaldoMensual(
                inicial.getId(), "AF-001", "2025-03", new BigDecimal("2000.00"), versionVieja);
        assertThatThrownBy(() -> {
            saldoAdapter.guardar(rezagado);
            em.flush();
        }).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
