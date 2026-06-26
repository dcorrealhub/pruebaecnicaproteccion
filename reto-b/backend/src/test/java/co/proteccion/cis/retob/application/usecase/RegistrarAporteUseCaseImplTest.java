package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AfiliadoNotFoundException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;
import co.proteccion.cis.retob.domain.model.*;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.*;
import co.proteccion.cis.retob.infrastructure.persistence.adapter.SaldoInicializadorAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrarAporteUseCaseImpl")
class RegistrarAporteUseCaseImplTest {

    @Mock AporteRepositoryPort         aporteRepository;
    @Mock SaldoRepositoryPort           saldoRepository;
    @Mock AfiliadoRepository            afiliadoRepository;
    @Mock ParametroRepository           parametroRepository;
    @Mock SaldoInicializadorAdapter     saldoInicializador;

    @InjectMocks
    RegistrarAporteUseCaseImpl useCase;

    private static final BigDecimal TOPE      = new BigDecimal("10000000");
    private static final BigDecimal UMBRAL    = new BigDecimal("5000000");
    private static final String     AFILIADO  = "AF-001";
    private static final String     IDEM_KEY  = "uuid-test-001";

    @BeforeEach
    void inyectarDefaults() {
        ReflectionTestUtils.setField(useCase, "topeMensualDefault", TOPE);
        ReflectionTestUtils.setField(useCase, "umbralRevisionDefault", UMBRAL);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Afiliado afiliadoActivo() {
        return new Afiliado(1L, AFILIADO, "Juan Sintético", EstadoAfiliado.ACTIVO, OffsetDateTime.now());
    }

    private SaldoMensual saldoVacio() {
        return new SaldoMensual(1L, AFILIADO, periodoActual(), BigDecimal.ZERO, 0);
    }

    private Aporte aporteGuardado(BigDecimal monto, EstadoAporte estado) {
        return new Aporte(10L, AFILIADO, monto, LocalDate.now(),
                CanalOrigen.APP_MOVIL, periodoActual(), estado, IDEM_KEY);
    }

    private String periodoActual() {
        return LocalDate.now().getYear() + "-" +
               String.format("%02d", LocalDate.now().getMonthValue());
    }

    private RegistrarAporteCommand comando(BigDecimal monto) {
        return new RegistrarAporteCommand(AFILIADO, monto, CanalOrigen.APP_MOVIL, IDEM_KEY);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("reenvío de la misma idempotenciaKey retorna el aporte original sin persistir de nuevo")
        void mismo_key_retorna_existente() {
            Aporte existente = aporteGuardado(new BigDecimal("1000000"), EstadoAporte.PENDIENTE);
            when(aporteRepository.findByIdempotenciaKey(IDEM_KEY)).thenReturn(Optional.of(existente));

            Aporte resultado = useCase.registrar(comando(new BigDecimal("1000000")));

            assertThat(resultado.getId()).isEqualTo(10L);
            // No debe validar afiliado ni tocar el saldo
            verifyNoInteractions(afiliadoRepository, saldoRepository, saldoInicializador);
        }
    }

    @Nested
    @DisplayName("Validaciones de negocio")
    class Validaciones {

        @Test
        @DisplayName("lanza AfiliadoNotFoundException si el afiliado no existe")
        void afiliado_no_encontrado() {
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.registrar(comando(new BigDecimal("100000"))))
                    .isInstanceOf(AfiliadoNotFoundException.class)
                    .hasMessageContaining(AFILIADO);
        }

        @Test
        @DisplayName("lanza TopeMensualExcedidoException cuando el nuevo total supera el tope")
        void tope_mensual_excedido() {
            // Saldo acumulado: 9.5 M — nuevo aporte: 1 M — total: 10.5 M > 10 M
            SaldoMensual saldoAlto = new SaldoMensual(1L, AFILIADO, periodoActual(),
                    new BigDecimal("9500000"), 0);

            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString()))
                    .thenReturn(saldoAlto);

            assertThatThrownBy(() -> useCase.registrar(comando(new BigDecimal("1000000"))))
                    .isInstanceOf(TopeMensualExcedidoException.class)
                    .hasMessageContaining("10000000");
        }
    }

    @Nested
    @DisplayName("Estado automático según umbral")
    class EstadoAutomatico {

        @Test
        @DisplayName("monto <= umbral → estado PENDIENTE")
        void monto_bajo_umbral_queda_pendiente() {
            BigDecimal monto = new BigDecimal("1000000"); // < 5 M
            Aporte esperado = aporteGuardado(monto, EstadoAporte.PENDIENTE);

            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString()))
                    .thenReturn(saldoVacio());
            when(aporteRepository.guardar(argThat(a -> a.getEstado() == EstadoAporte.PENDIENTE)))
                    .thenReturn(esperado);
            when(saldoRepository.guardar(any())).thenReturn(saldoVacio());

            Aporte resultado = useCase.registrar(comando(monto));

            assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.PENDIENTE);
        }

        @Test
        @DisplayName("monto > umbral → estado EN_REVISION")
        void monto_sobre_umbral_queda_en_revision() {
            BigDecimal monto = new BigDecimal("6000000"); // > 5 M
            Aporte esperado = aporteGuardado(monto, EstadoAporte.EN_REVISION);

            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString()))
                    .thenReturn(saldoVacio());
            when(aporteRepository.guardar(argThat(a -> a.getEstado() == EstadoAporte.EN_REVISION)))
                    .thenReturn(esperado);
            when(saldoRepository.guardar(any())).thenReturn(saldoVacio());

            Aporte resultado = useCase.registrar(comando(monto));

            assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.EN_REVISION);
        }

        @Test
        @DisplayName("monto exactamente igual al umbral → estado PENDIENTE (no supera)")
        void monto_igual_umbral_queda_pendiente() {
            BigDecimal monto = UMBRAL; // == 5 M, no supera
            Aporte esperado = aporteGuardado(monto, EstadoAporte.PENDIENTE);

            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString()))
                    .thenReturn(saldoVacio());
            when(aporteRepository.guardar(argThat(a -> a.getEstado() == EstadoAporte.PENDIENTE)))
                    .thenReturn(esperado);
            when(saldoRepository.guardar(any())).thenReturn(saldoVacio());

            Aporte resultado = useCase.registrar(comando(monto));

            assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.PENDIENTE);
        }
    }

    @Nested
    @DisplayName("Parámetros desde DB vs defaults de entorno")
    class Parametros {

        @Test
        @DisplayName("si historico_parametros está vacío, usa los defaults de @Value")
        void usa_defaults_cuando_tabla_vacia() {
            BigDecimal monto = new BigDecimal("1000000");

            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty()); // tabla vacía
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString()))
                    .thenReturn(saldoVacio());
            when(aporteRepository.guardar(any())).thenReturn(aporteGuardado(monto, EstadoAporte.PENDIENTE));
            when(saldoRepository.guardar(any())).thenReturn(saldoVacio());

            // No debe lanzar excepción — los defaults de 10M / 5M aplican
            assertThatCode(() -> useCase.registrar(comando(monto))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("si hay parámetros en DB, usa esos topes en lugar de los defaults")
        void usa_parametros_de_db() {
            BigDecimal topeDB    = new BigDecimal("3000000");
            BigDecimal umbralDB  = new BigDecimal("1500000");
            BigDecimal monto     = new BigDecimal("2000000"); // entre umbralDB y topeDB

            ParametrosFondo params = new ParametrosFondo(1L, topeDB, umbralDB,
                    "ADMIN", OffsetDateTime.now(), "test");
            SaldoMensual saldoConAcumulado = new SaldoMensual(1L, AFILIADO, periodoActual(),
                    new BigDecimal("2000000"), 0); // 2M + 2M = 4M > 3M tope DB

            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.of(params));
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString()))
                    .thenReturn(saldoConAcumulado);

            // 2M acumulado + 2M nuevo = 4M > 3M tope de DB → debe lanzar excepción
            assertThatThrownBy(() -> useCase.registrar(comando(monto)))
                    .isInstanceOf(TopeMensualExcedidoException.class)
                    .hasMessageContaining("3000000");
        }
    }

    @Nested
    @DisplayName("Interacciones con repositorios")
    class Interacciones {

        @Test
        @DisplayName("en el camino feliz se persisten el aporte y el saldo en la misma operación")
        void persiste_aporte_y_saldo() {
            BigDecimal monto = new BigDecimal("1000000");
            Aporte guardado  = aporteGuardado(monto, EstadoAporte.PENDIENTE);

            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString()))
                    .thenReturn(saldoVacio());
            when(aporteRepository.guardar(any())).thenReturn(guardado);
            when(saldoRepository.guardar(any())).thenReturn(saldoVacio());

            useCase.registrar(comando(monto));

            verify(aporteRepository).guardar(any(Aporte.class));
            verify(saldoRepository).guardar(any(SaldoMensual.class));
        }
    }
}
