package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.ConflictoIdempotenciaException;
import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.exception.SolicitudInvalidaException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.domain.model.ParametrosAfiliado;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.model.TipoEventoAporte;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.ResultadoRegistro;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.ParametrosAfiliadoRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.domain.service.PoliticaAportes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del caso de uso con puertos en memoria: validan la orquestación
 * (idempotencia, tope, revisión, periodo) sin Spring ni base de datos.
 */
class RegistrarAporteUseCaseImplTest {

    // 2026-10-01T03:00Z es todavía 30 de septiembre en Bogotá (UTC-5)
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-01T03:00:00Z"), ZoneId.of("America/Bogota"));

    private AportesEnMemoria aportes;
    private SaldosEnMemoria saldos;
    private EventosEnMemoria eventos;
    private Map<String, ParametrosAfiliado> parametrosPorAfiliado;
    private RegistrarAporteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        aportes = new AportesEnMemoria();
        saldos = new SaldosEnMemoria();
        eventos = new EventosEnMemoria();
        parametrosPorAfiliado = new HashMap<>();
        ParametrosAfiliadoRepositoryPort parametros = afiliadoId -> Optional.ofNullable(parametrosPorAfiliado.get(afiliadoId));
        var politica = new PoliticaAportes(new BigDecimal("10000000"), new BigDecimal("5000000"),
                Map.of(Canal.SUCURSAL, new BigDecimal("3000000")));
        useCase = new RegistrarAporteUseCaseImpl(aportes, saldos, eventos, parametros, politica, RELOJ);
    }

    @Test
    void registraAporteConFechaYPeriodoDelServidorEnZonaColombia() {
        ResultadoRegistro r = useCase.registrar(comando("AF-001", "100000", "WEB", "key-00000001"));

        assertThat(r.creado()).isTrue();
        assertThat(r.aporte().getFecha()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(r.aporte().getPeriodo()).isEqualTo("2026-09");
        assertThat(r.aporte().getMonto()).isEqualByComparingTo("100000");
        assertThat(r.aporte().getMonto().scale()).isEqualTo(2);
        assertThat(saldos.total("AF-001", "2026-09")).isEqualByComparingTo("100000");
        assertThat(eventos.registrados).containsExactly(Map.entry(r.aporte().getId(), TipoEventoAporte.APORTE_REGISTRADO));
    }

    @Test
    void reintentoConMismaClaveYMismoContenidoDevuelveElOriginalSinDuplicar() {
        ResultadoRegistro primero = useCase.registrar(comando("AF-001", "100000", "WEB", "key-00000001"));
        ResultadoRegistro reintento = useCase.registrar(comando("AF-001", "100000.00", "WEB", "key-00000001"));

        assertThat(reintento.creado()).isFalse();
        assertThat(reintento.aporte().getId()).isEqualTo(primero.aporte().getId());
        assertThat(aportes.porId).hasSize(1);
        assertThat(saldos.total("AF-001", "2026-09")).isEqualByComparingTo("100000");
        assertThat(eventos.registrados).hasSize(1);
    }

    @Test
    void mismaClaveConContenidoDistintoSeRechaza() {
        useCase.registrar(comando("AF-001", "100000", "WEB", "key-00000001"));

        assertThatThrownBy(() -> useCase.registrar(comando("AF-001", "200000", "WEB", "key-00000001")))
                .isInstanceOf(ConflictoIdempotenciaException.class);
        assertThatThrownBy(() -> useCase.registrar(comando("AF-002", "100000", "WEB", "key-00000001")))
                .isInstanceOf(ConflictoIdempotenciaException.class);
        assertThat(aportes.porId).hasSize(1);
    }

    @Test
    void elTopeSeEvaluaSobreElAcumuladoDelMes() {
        useCase.registrar(comando("AF-001", "6000000", "WEB", "key-00000001"));
        useCase.registrar(comando("AF-001", "4000000", "WEB", "key-00000002")); // llega exacto al tope

        assertThatThrownBy(() -> useCase.registrar(comando("AF-001", "0.01", "WEB", "key-00000003")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tope mensual");
        assertThat(saldos.total("AF-001", "2026-09")).isEqualByComparingTo("10000000");
        assertThat(aportes.porId).hasSize(2);
    }

    @Test
    void elTopeEsPorAfiliado() {
        useCase.registrar(comando("AF-001", "10000000", "WEB", "key-00000001"));
        ResultadoRegistro otro = useCase.registrar(comando("AF-002", "10000000", "WEB", "key-00000002"));

        assertThat(otro.creado()).isTrue();
    }

    @Test
    void aplicaElTopePropioDelAfiliado() {
        parametrosPorAfiliado.put("AF-001", new ParametrosAfiliado(new BigDecimal("2000000"), null));

        useCase.registrar(comando("AF-001", "2000000", "WEB", "key-00000001")); // exacto a su tope

        assertThatThrownBy(() -> useCase.registrar(comando("AF-001", "0.01", "WEB", "key-00000002")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("$2.000.000,00");
        // otro afiliado sin tope propio sigue con el tope por defecto
        assertThat(useCase.registrar(comando("AF-002", "9000000", "WEB", "key-00000003")).creado()).isTrue();
    }

    @Test
    void elTopePropioPuedeSerMayorQueElPorDefecto() {
        parametrosPorAfiliado.put("AF-001", new ParametrosAfiliado(new BigDecimal("20000000"), null));

        assertThat(useCase.registrar(comando("AF-001", "15000000", "WEB", "key-00000001")).creado()).isTrue();
    }

    @Test
    void marcaParaRevisionSoloSiSuperaElUmbral() {
        Aporte igual = useCase.registrar(comando("AF-001", "5000000", "WEB", "key-00000001")).aporte();
        Aporte mayor = useCase.registrar(comando("AF-002", "5000000.01", "WEB", "key-00000002")).aporte();

        assertThat(igual.isMarcadaRevision()).isFalse();
        assertThat(mayor.isMarcadaRevision()).isTrue();
    }

    @Test
    void aplicaElUmbralPropioDelAfiliado() {
        parametrosPorAfiliado.put("AF-001", new ParametrosAfiliado(null, new BigDecimal("300000")));

        Aporte enSuUmbral = useCase.registrar(comando("AF-001", "300000", "WEB", "key-00000001")).aporte();
        Aporte sobreSuUmbral = useCase.registrar(comando("AF-001", "300000.01", "WEB", "key-00000002")).aporte();
        // el mismo monto para un afiliado sin umbral propio no se marca (umbral por defecto 5.000.000)
        Aporte otroAfiliado = useCase.registrar(comando("AF-002", "300000.01", "WEB", "key-00000003")).aporte();

        assertThat(enSuUmbral.isMarcadaRevision()).isFalse();
        assertThat(sobreSuUmbral.isMarcadaRevision()).isTrue();
        assertThat(otroAfiliado.isMarcadaRevision()).isFalse();
    }

    @Test
    void aportePorSucursalSeMarcaSobreTresMillones() {
        Aporte sucursalEnUmbral = useCase.registrar(comando("AF-001", "3000000", "SUCURSAL", "key-00000001")).aporte();
        Aporte sucursalSobreUmbral = useCase.registrar(comando("AF-002", "3000000.01", "SUCURSAL", "key-00000002")).aporte();
        Aporte webMismoMonto = useCase.registrar(comando("AF-003", "3000000.01", "WEB", "key-00000003")).aporte();

        assertThat(sucursalEnUmbral.isMarcadaRevision()).isFalse();
        assertThat(sucursalSobreUmbral.isMarcadaRevision()).isTrue();
        assertThat(webMismoMonto.isMarcadaRevision()).isFalse();
    }

    @Test
    void enSucursalElUmbralDelCanalPrevaleceSobreElDelAfiliado() {
        parametrosPorAfiliado.put("AF-001", new ParametrosAfiliado(null, new BigDecimal("8000000")));

        Aporte sucursal = useCase.registrar(comando("AF-001", "4000000", "SUCURSAL", "key-00000001")).aporte();
        Aporte web = useCase.registrar(comando("AF-001", "4000000", "WEB", "key-00000002")).aporte();

        assertThat(sucursal.isMarcadaRevision()).isTrue();   // 4M > 3M del canal, aunque el afiliado tenga 8M
        assertThat(web.isMarcadaRevision()).isFalse();       // 4M < 8M del afiliado
    }

    @Test
    void rechazaMontoNoPositivoSinTocarElSaldo() {
        assertThatThrownBy(() -> useCase.registrar(comando("AF-001", "-5", "WEB", "key-00000001")))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(saldos.porClave).isEmpty();
        assertThat(aportes.porId).isEmpty();
    }

    @Test
    void rechazaCanalDesconocido() {
        assertThatThrownBy(() -> useCase.registrar(comando("AF-001", "100", "FAX", "key-00000001")))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    private static RegistrarAporteCommand comando(String afiliado, String monto, String canal, String key) {
        return new RegistrarAporteCommand(afiliado, new BigDecimal(monto), canal, key);
    }

    // ---------------------------------------------------------------- fakes

    static class AportesEnMemoria implements AporteRepositoryPort {
        final Map<Long, Aporte> porId = new LinkedHashMap<>();
        private final AtomicLong secuencia = new AtomicLong();

        @Override
        public Aporte guardar(Aporte a) {
            Aporte persistido = new Aporte(secuencia.incrementAndGet(), a.getAfiliadoId(), a.getMonto(), a.getFecha(),
                    a.getCanal(), a.getPeriodo(), a.isMarcadaRevision(), a.getIdempotenciaKey());
            porId.put(persistido.getId(), persistido);
            return persistido;
        }

        @Override
        public Optional<Aporte> findByIdempotenciaKey(String key) {
            return porId.values().stream().filter(a -> a.getIdempotenciaKey().equals(key)).findFirst();
        }

        @Override
        public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId, String desde, String hasta) {
            return porId.values().stream()
                    .filter(a -> a.getAfiliadoId().equals(afiliadoId))
                    .filter(a -> a.getPeriodo().compareTo(desde) >= 0 && a.getPeriodo().compareTo(hasta) <= 0)
                    .toList();
        }
    }

    static class SaldosEnMemoria implements SaldoRepositoryPort {
        final Map<String, SaldoMensual> porClave = new HashMap<>();
        private final AtomicLong secuencia = new AtomicLong();

        BigDecimal total(String afiliadoId, String mes) {
            return porClave.get(afiliadoId + "|" + mes).getTotal();
        }

        @Override
        public Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes) {
            return Optional.ofNullable(porClave.get(afiliadoId + "|" + mes));
        }

        @Override
        public SaldoMensual guardar(SaldoMensual s) {
            SaldoMensual actual = porClave.get(s.getAfiliadoId() + "|" + s.getMes());
            if (!actual.getVersion().equals(s.getVersion())) {
                throw new IllegalStateException("versión desactualizada");
            }
            SaldoMensual nuevo = new SaldoMensual(s.getId(), s.getAfiliadoId(), s.getMes(), s.getTotal(), s.getVersion() + 1);
            porClave.put(s.getAfiliadoId() + "|" + s.getMes(), nuevo);
            return nuevo;
        }

        @Override
        public SaldoMensual inicializar(String afiliadoId, String mes) {
            SaldoMensual nuevo = new SaldoMensual(secuencia.incrementAndGet(), afiliadoId, mes, BigDecimal.ZERO, 0);
            porClave.put(afiliadoId + "|" + mes, nuevo);
            return nuevo;
        }
    }

    static class EventosEnMemoria implements EventoAporteRepositoryPort {
        final List<Map.Entry<Long, TipoEventoAporte>> registrados = new ArrayList<>();

        @Override
        public void registrar(Long aporteId, TipoEventoAporte tipo) {
            registrados.add(Map.entry(aporteId, tipo));
        }
    }
}
