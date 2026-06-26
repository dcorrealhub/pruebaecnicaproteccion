package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SpringDataSaldoRepositoryTest {

    @Autowired
    private SpringDataSaldoRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByAfiliadoIdAndMes_cuandoExiste_retornaEntity() {
        var entity = crearEntity("AF-001", "2026-06", new BigDecimal("100000"));
        repository.save(entity);

        var result = repository.findByAfiliadoIdAndMes("AF-001", "2026-06");

        assertTrue(result.isPresent());
        assertEquals("AF-001", result.get().getAfiliadoId());
        assertEquals("2026-06", result.get().getMes());
        assertEquals(0, new BigDecimal("100000").compareTo(result.get().getTotal()));
    }

    @Test
    void findByAfiliadoIdAndMes_cuandoNoExiste_retornaVacio() {
        var result = repository.findByAfiliadoIdAndMes("AF-999", "2026-06");

        assertTrue(result.isEmpty());
    }

    @Test
    void guardar_incrementaVersion() {
        var entity = crearEntity("AF-001", "2026-06", new BigDecimal("500000"));
        repository.save(entity);
        entityManager.flush();

        var loaded = repository.findByAfiliadoIdAndMes("AF-001", "2026-06").orElseThrow();
        assertEquals(0, loaded.getVersion());

        loaded.setTotal(new BigDecimal("600000"));
        repository.save(loaded);
        entityManager.flush();

        var updated = repository.findByAfiliadoIdAndMes("AF-001", "2026-06").orElseThrow();
        assertEquals(1, updated.getVersion());
        assertEquals(0, new BigDecimal("600000").compareTo(updated.getTotal()));
    }

    @Test
    void guardar_concurrencia_versionSeIncrementaAlActualizar() {
        var entity = crearEntity("AF-001", "2026-06", new BigDecimal("500000"));
        repository.save(entity);
        entityManager.flush();

        var loaded = repository.findByAfiliadoIdAndMes("AF-001", "2026-06").orElseThrow();
        assertEquals(0, loaded.getVersion());

        loaded.setTotal(new BigDecimal("999999"));
        repository.save(loaded);
        entityManager.flush();

        var finalVersion = repository.findByAfiliadoIdAndMes("AF-001", "2026-06")
                .orElseThrow().getVersion();
        assertEquals(1, finalVersion);
    }

    @Test
    void uniqueConstraint_afiliadoIdMes_evitaDuplicados() {
        repository.save(crearEntity("AF-001", "2026-06", new BigDecimal("100000")));
        entityManager.flush();

        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.save(crearEntity("AF-001", "2026-06", new BigDecimal("200000")));
            entityManager.flush();
        });
    }

    private SaldoMensualEntity crearEntity(String afiliadoId, String mes, BigDecimal total) {
        var entity = new SaldoMensualEntity();
        entity.setAfiliadoId(afiliadoId);
        entity.setMes(mes);
        entity.setTotal(total);
        return entity;
    }
}