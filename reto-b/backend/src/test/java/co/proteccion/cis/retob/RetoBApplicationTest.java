package co.proteccion.cis.retob;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "API_USERNAME=test-user",
        "API_PASSWORD=test-pass",
        "JWT_SECRET=test-secret-key-for-ci-only-not-production"
})
class RetoBApplicationTest {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring se levanta correctamente
    }
}
