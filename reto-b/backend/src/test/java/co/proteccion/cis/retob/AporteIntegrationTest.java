package co.proteccion.cis.retob;

import co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException;
import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.entity.ParametroAfiliadoEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataEventoAporteRepository;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataParametroAfiliadoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de extremo a extremo (HTTP → caso de uso → PostgreSQL real con Flyway).
 * Cada prueba usa su propio afiliado para no depender del orden de ejecución.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AporteIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired MockMvc mvc;
    @Autowired RegistrarAporteUseCase registrarAporteUseCase;
    @Autowired SpringDataAporteRepository aporteRepo;
    @Autowired SpringDataSaldoRepository saldoRepo;
    @Autowired SpringDataEventoAporteRepository eventoRepo;
    @Autowired SpringDataParametroAfiliadoRepository parametroRepo;
    @Autowired Clock clock;

    // ------------------------------------------------------------------ HTTP

    @Test
    void registraAporteYReintentoIdempotenteDevuelve200ConElMismoId() throws Exception {
        String afiliado = nuevoAfiliado();
        String body = json(afiliado, "250000.50", "APP_MOVIL", UUID.randomUUID().toString());

        String id = mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monto").value(250000.50))
                .andExpect(jsonPath("$.periodo").value(periodoActual()))
                .andExpect(jsonPath("$.marcadaRevision").value(false))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.parseLong(id)));

        assertThat(aporteRepo.findByAfiliadoId(afiliado)).hasSize(1);
        assertThat(eventoRepo.findByAporteId(Long.parseLong(id))).hasSize(1);
    }

    @Test
    void mismaClaveConOtroMontoDevuelve409() throws Exception {
        String afiliado = nuevoAfiliado();
        String key = UUID.randomUUID().toString();
        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content(json(afiliado, "1000", "WEB", key)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content(json(afiliado, "2000", "WEB", key)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("IDEMPOTENCIA_CONFLICTO"));
    }

    @Test
    void superarElTopeDevuelve422ConMensajeClaro() throws Exception {
        String afiliado = nuevoAfiliado();
        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(afiliado, "9500000", "WEB", UUID.randomUUID().toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marcadaRevision").value(true));

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(afiliado, "600000", "WEB", UUID.randomUUID().toString())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("TOPE_MENSUAL_EXCEDIDO"))
                .andExpect(jsonPath("$.detail", containsString("Disponible")));
    }

    @Test
    void respetaElTopeConfiguradoEnBaseDeDatosParaElAfiliado() throws Exception {
        String afiliado = nuevoAfiliado();
        parametroRepo.save(ParametroAfiliadoEntity.builder().afiliadoId(afiliado).topeMensual(new BigDecimal("1500000")).build());

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(afiliado, "1000000", "WEB", UUID.randomUUID().toString())))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(afiliado, "500000.01", "WEB", UUID.randomUUID().toString())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("TOPE_MENSUAL_EXCEDIDO"))
                .andExpect(jsonPath("$.detail", containsString("$1.500.000,00")));
    }

    @Test
    void respetaElUmbralConfiguradoEnBaseDeDatosParaElAfiliado() throws Exception {
        String afiliado = nuevoAfiliado();
        parametroRepo.save(ParametroAfiliadoEntity.builder().afiliadoId(afiliado).umbralRevision(new BigDecimal("100000")).build());

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(afiliado, "100000.01", "WEB", UUID.randomUUID().toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marcadaRevision").value(true));
    }

    @Test
    void aportePorSucursalSobreTresMillonesQuedaMarcado() throws Exception {
        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(nuevoAfiliado(), "3000000.01", "SUCURSAL", UUID.randomUUID().toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marcadaRevision").value(true));

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(nuevoAfiliado(), "3000000.01", "APP_MOVIL", UUID.randomUUID().toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marcadaRevision").value(false));
    }

    @Test
    void datosInvalidosDevuelven400SinDetallesInternos() throws Exception {
        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(json("AF-1", "-10", "WEB", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores", hasSize(2)));

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(json("AF-1", "10", "FAX", UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CANAL_INVALIDO"));

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void consolidadoRetornaTotalYDetalleDelRango() throws Exception {
        String afiliado = nuevoAfiliado();
        for (String monto : List.of("100000.10", "200000.20", "300000.30")) {
            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                            .content(json(afiliado, monto, "SUCURSAL", UUID.randomUUID().toString())))
                    .andExpect(status().isCreated());
        }
        String periodo = periodoActual();
        String anterior = YearMonth.parse(periodo).minusMonths(1).toString();

        mvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", afiliado).param("periodoDesde", anterior).param("periodoHasta", periodo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAportado").value(600000.60))
                .andExpect(jsonPath("$.detalle", hasSize(3)))
                .andExpect(jsonPath("$.detalle[0].canal").value("SUCURSAL"));

        mvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", afiliado).param("periodoDesde", anterior).param("periodoHasta", anterior))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAportado").value(0))
                .andExpect(jsonPath("$.detalle", hasSize(0)));
    }

    @Test
    void consolidadoValidaLosPeriodos() throws Exception {
        mvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", "AF-1").param("periodoDesde", "2026-13").param("periodoHasta", "2026-01"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", "AF-1").param("periodoDesde", "2026-06").param("periodoHasta", "2026-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PERIODO_INVALIDO"));
    }

    // ----------------------------------------------------------- concurrencia

    /**
     * 8 aportes simultáneos de 2.000.000 para el mismo afiliado con tope de 10.000.000:
     * exactamente 5 deben entrar. Sin control de concurrencia, varios leerían el mismo
     * saldo y el tope se superaría.
     */
    @Test
    void aportesConcurrentesNoSuperanElTopeMensual() throws Exception {
        String afiliado = nuevoAfiliado();
        int hilos = 8;
        List<Callable<String>> tareas = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            String key = UUID.randomUUID().toString();
            tareas.add(() -> registrarConReintentos(new RegistrarAporteCommand(afiliado, new BigDecimal("2000000"), "WEB", key)));
        }

        List<String> resultados = ejecutarEnParalelo(tareas);

        assertThat(resultados).filteredOn("OK"::equals).hasSize(5);
        assertThat(resultados).filteredOn("TOPE"::equals).hasSize(3);

        List<AporteEntity> persistidos = aporteRepo.findByAfiliadoId(afiliado);
        BigDecimal suma = persistidos.stream().map(AporteEntity::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(persistidos).hasSize(5);
        assertThat(suma).isEqualByComparingTo("10000000");
        assertThat(saldoRepo.findByAfiliadoIdAndMes(afiliado, periodoActual()).orElseThrow().getTotal())
                .isEqualByComparingTo(suma);
    }

    /** El mismo aporte enviado varias veces en paralelo (doble clic, reintentos de red) se registra una sola vez. */
    @Test
    void mismaClaveEnParaleloRegistraUnSoloAporte() throws Exception {
        String afiliado = nuevoAfiliado();
        String key = UUID.randomUUID().toString();
        List<Callable<String>> tareas = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tareas.add(() -> registrarConReintentos(new RegistrarAporteCommand(afiliado, new BigDecimal("1000"), "WEB", key)));
        }

        List<String> resultados = ejecutarEnParalelo(tareas);

        assertThat(resultados).containsOnly("OK");
        assertThat(aporteRepo.findByAfiliadoId(afiliado)).hasSize(1);
        assertThat(saldoRepo.findByAfiliadoIdAndMes(afiliado, periodoActual()).orElseThrow().getTotal())
                .isEqualByComparingTo("1000");
    }

    /** Simula al cliente: ante un conflicto de concurrencia reintenta con la MISMA clave. */
    private String registrarConReintentos(RegistrarAporteCommand command) throws InterruptedException {
        for (int intento = 0; intento < 20; intento++) {
            try {
                registrarAporteUseCase.registrar(command);
                return "OK";
            } catch (ConflictoConcurrenciaException e) {
                Thread.sleep(ThreadLocalRandom.current().nextInt(5, 30));
            } catch (ReglaNegocioException e) {
                return "TOPE";
            }
        }
        return "AGOTADO";
    }

    private static List<String> ejecutarEnParalelo(List<Callable<String>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<String>> futuros = new ArrayList<>();
            for (Callable<String> tarea : tareas) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return tarea.call();
                }));
            }
            salida.countDown();
            List<String> resultados = new ArrayList<>();
            for (Future<String> f : futuros) {
                resultados.add(f.get(60, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- helpers

    private String periodoActual() {
        return YearMonth.now(clock).toString();
    }

    private static String nuevoAfiliado() {
        return "AF-TEST-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String json(String afiliado, String monto, String canal, String key) {
        return """
                {"afiliadoId":"%s","monto":%s,"canal":"%s","idempotenciaKey":"%s"}
                """.formatted(afiliado, monto, canal, key);
    }
}
