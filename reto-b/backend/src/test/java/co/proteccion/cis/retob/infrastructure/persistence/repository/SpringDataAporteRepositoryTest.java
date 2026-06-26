package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SpringDataAporteRepositoryTest {

    @Autowired
    private SpringDataAporteRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByIdempotenciaKey_cuandoExiste_retornaEntity() {
        var entity = crearEntity("key-existente", "AF-001", "2026-01");
        repository.save(entity);

        var result = repository.findByIdempotenciaKey("key-existente");

        assertTrue(result.isPresent());
        assertEquals("AF-001", result.get().getAfiliadoId());
        assertEquals("key-existente", result.get().getIdempotenciaKey());
    }

    @Test
    void findByIdempotenciaKey_cuandoNoExiste_retornaVacio() {
        var result = repository.findByIdempotenciaKey("key-inexistente");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByAfiliadoIdAndPeriodoBetween_cuandoExistenDatos_retornaLista() {
        repository.save(crearEntity("k1", "AF-001", "2026-01"));
        repository.save(crearEntity("k2", "AF-001", "2026-02"));
        repository.save(crearEntity("k3", "AF-002", "2026-01"));

        var result = repository.findByAfiliadoIdAndPeriodoBetween("AF-001", "2026-01", "2026-02");

        assertEquals(2, result.size());
    }

    @Test
    void findByAfiliadoIdAndPeriodoBetween_cuandoNoHayDatos_retornaListaVacia() {
        repository.save(crearEntity("k1", "AF-001", "2025-01"));

        var result = repository.findByAfiliadoIdAndPeriodoBetween("AF-002", "2026-01", "2026-06");

        assertTrue(result.isEmpty());
    }

    @Test
    void guardar_persisteConIdGenerado() {
        var entity = crearEntity("key-nueva", "AF-001", "2026-06");

        var saved = repository.save(entity);

        assertNotNull(saved.getId());
        assertEquals("AF-001", saved.getAfiliadoId());
        assertNotNull(saved.getCreadoEn());
    }

    private AporteEntity crearEntity(String idempotenciaKey, String afiliadoId, String periodo) {
        var entity = new AporteEntity();
        entity.setAfiliadoId(afiliadoId);
        entity.setMonto(new BigDecimal("500000"));
        entity.setFecha(LocalDate.now());
        entity.setCanal("APP_MOVIL");
        entity.setPeriodo(periodo);
        entity.setMarcadaRevision(false);
        entity.setIdempotenciaKey(idempotenciaKey);
        return entity;
    }
}