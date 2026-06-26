package co.proteccion.cis.retob;

import co.proteccion.cis.retob.config.AporteProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AporteProperties.class)
public class RetoBApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetoBApplication.class, args);
    }
}
