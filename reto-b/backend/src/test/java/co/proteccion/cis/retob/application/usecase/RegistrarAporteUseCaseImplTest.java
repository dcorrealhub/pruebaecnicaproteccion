package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AfiliadoBloqueadoException;
import co.proteccion.cis.retob.domain.exception.AfiliadoNotFoundException;
import co.proteccion.cis.retob.domain.exception.MontoMinimoNoAlcanzadoException;
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

    @InjectMocks RegistrarAporteUseCaseImpl useCase;

    static final BigDecimal MINIMO  = new BigDecimal("10000");
    static final BigDecimal TOPE    = new BigDecimal("10000000");
    static final BigDecimal UMBRAL  = new BigDecimal("5000000");
    static final String AFILIADO    = "AF-001";
    static final String IDEM_KEY    = "uuid-test-001";
    static final String APORTE_ID   = "550e8400-e29b-41d4-a716-446655440002";

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(useCase, "montoMinimoDefault", MINIMO);
        ReflectionTestUtils.setField(useCase, "topeMensualDefault", TOPE);
        ReflectionTestUtils.setField(useCase, "umbralRevisionDefault", UMBRAL);
    }

    private Afiliado afiliadoActivo() {
        return new Afiliado("af-uuid-001", AFILIADO, "Juan", EstadoAfiliado.ACTIVO, OffsetDateTime.now());
    }

    private Afiliado afiliadoBloqueado() {
        return new Afiliado("af-uuid-001", AFILIADO, "Juan", EstadoAfiliado.BLOQUEADO, OffsetDateTime.now());
    }

    private SaldoMensual saldoVacio() {
        String periodo = LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue());
        return new SaldoMensual(1L, AFILIADO, periodo, BigDecimal.ZERO, 0);
    }

    private Aporte aporteGuardado(BigDecimal monto, EstadoAporte estado) {
        return new Aporte(APORTE_ID, AFILIADO, monto, LocalDate.now(), CanalOrigen.APP_MOVIL,
                LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue()),
                estado, IDEM_KEY);
    }

    private RegistrarAporteCommand comando(BigDecimal monto) {
        return new RegistrarAporteCommand(AFILIADO, monto, CanalOrigen.APP_MOVIL, IDEM_KEY);
    }

    @Nested @DisplayName("Idempotencia")
    class Idempotencia {
        @Test @DisplayName("mismo idempotenciaKey retorna aporte existente sin persistir de nuevo")
        void mismo_key_retorna_existente() {
            Aporte existente = aporteGuardado(new BigDecimal("1000000"), EstadoAporte.PENDIENTE);
            when(aporteRepository.findByIdempotenciaKey(IDEM_KEY)).thenReturn(Optional.of(existente));

            Aporte resultado = useCase.registrar(comando(new BigDecimal("1000000")));

            assertThat(resultado.getId()).isEqualTo(APORTE_ID);
            verifyNoInteractions(afiliadoRepository, saldoRepository, saldoInicializador);
        }
    }

    @Nested @DisplayName("Validaciones")
    class Validaciones {
        @Test @DisplayName("afiliado no encontrado → AfiliadoNotFoundException")
        void afiliado_no_encontrado() {
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.registrar(comando(new BigDecimal("100000"))))
                    .isInstanceOf(AfiliadoNotFoundException.class).hasMessageContaining(AFILIADO);
        }

        @Test @DisplayName("afiliado BLOQUEADO → AfiliadoBloqueadoException")
        void afiliado_bloqueado() {
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoBloqueado()));

            assertThatThrownBy(() -> useCase.registrar(comando(new BigDecimal("100000"))))
                    .isInstanceOf(AfiliadoBloqueadoException.class)
                    .hasMessageContaining(AFILIADO)
                    .hasMessageContaining("bloqueado");

            verifyNoInteractions(saldoInicializador, saldoRepository);
        }

        @Test @DisplayName("monto menor al mínimo → MontoMinimoNoAlcanzadoException")
        void monto_bajo_minimo() {
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.registrar(comando(new BigDecimal("9999"))))
                    .isInstanceOf(MontoMinimoNoAlcanzadoException.class)
                    .hasMessageContaining("9999")
                    .hasMessageContaining("10000");

            verifyNoInteractions(saldoInicializador, saldoRepository);
        }

        @Test @DisplayName("monto igual al mínimo pasa la validación")
        void monto_igual_al_minimo_pasa() {
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString())).thenReturn(saldoVacio());
            when(aporteRepository.guardar(any())).thenReturn(aporteGuardado(MINIMO, EstadoAporte.PENDIENTE));
            when(saldoRepository.guardar(any())).thenReturn(saldoVacio());

            assertThatCode(() -> useCase.registrar(comando(MINIMO))).doesNotThrowAnyException();
        }

        @Test @DisplayName("tope mensual excedido → TopeMensualExcedidoException")
        void tope_excedido() {
            SaldoMensual saldoAlto = new SaldoMensual(1L, AFILIADO,
                    LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue()),
                    new BigDecimal("9500000"), 0);
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString())).thenReturn(saldoAlto);

            assertThatThrownBy(() -> useCase.registrar(comando(new BigDecimal("1000000"))))
                    .isInstanceOf(TopeMensualExcedidoException.class);
        }
    }

    @Nested @DisplayName("Estado automático")
    class EstadoAutomatico {
        @Test @DisplayName("monto < umbral → PENDIENTE")
        void monto_bajo_umbral() {
            BigDecimal monto = new BigDecimal("1000000");
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString())).thenReturn(saldoVacio());
            when(aporteRepository.guardar(argThat(a -> a.getEstado() == EstadoAporte.PENDIENTE)))
                    .thenReturn(aporteGuardado(monto, EstadoAporte.PENDIENTE));
            when(saldoRepository.guardar(any())).thenReturn(saldoVacio());

            assertThat(useCase.registrar(comando(monto)).getEstado()).isEqualTo(EstadoAporte.PENDIENTE);
        }

        @Test @DisplayName("monto > umbral → EN_REVISION")
        void monto_sobre_umbral() {
            BigDecimal monto = new BigDecimal("6000000");
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString())).thenReturn(saldoVacio());
            when(aporteRepository.guardar(argThat(a -> a.getEstado() == EstadoAporte.EN_REVISION)))
                    .thenReturn(aporteGuardado(monto, EstadoAporte.EN_REVISION));
            when(saldoRepository.guardar(any())).thenReturn(saldoVacio());

            assertThat(useCase.registrar(comando(monto)).getEstado()).isEqualTo(EstadoAporte.EN_REVISION);
        }

        @Test @DisplayName("monto == umbral → PENDIENTE (no supera)")
        void monto_igual_umbral() {
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString())).thenReturn(saldoVacio());
            when(aporteRepository.guardar(argThat(a -> a.getEstado() == EstadoAporte.PENDIENTE)))
                    .thenReturn(aporteGuardado(UMBRAL, EstadoAporte.PENDIENTE));
            when(saldoRepository.guardar(any())).thenReturn(saldoVacio());

            assertThat(useCase.registrar(comando(UMBRAL)).getEstado()).isEqualTo(EstadoAporte.PENDIENTE);
        }
    }

    @Nested @DisplayName("Parámetros desde DB vs defaults")
    class Parametros {
        @Test @DisplayName("tabla vacía → usa defaults de @Value")
        void defaults_cuando_vacia() {
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString())).thenReturn(saldoVacio());
            when(aporteRepository.guardar(any())).thenReturn(aporteGuardado(new BigDecimal("1000000"), EstadoAporte.PENDIENTE));
            when(saldoRepository.guardar(any())).thenReturn(saldoVacio());

            assertThatCode(() -> useCase.registrar(comando(new BigDecimal("1000000")))).doesNotThrowAnyException();
        }

        @Test @DisplayName("parámetros desde DB tienen precedencia sobre defaults")
        void parametros_db_tienen_precedencia() {
            ParametrosFondo params = new ParametrosFondo(null,
                    new BigDecimal("50000"),    // montoMinimo más alto
                    new BigDecimal("20000000"), // topeMensual más alto
                    new BigDecimal("10000000"), // umbralRevision más alto
                    "ADMIN", null, null);
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.of(params));

            // Con montoMinimo=50.000 en DB, un aporte de 10.000 (que pasaría con defaults) debe fallar
            assertThatThrownBy(() -> useCase.registrar(comando(new BigDecimal("10000"))))
                    .isInstanceOf(MontoMinimoNoAlcanzadoException.class);
        }
    }

    @Nested @DisplayName("Interacciones")
    class Interacciones {
        @Test @DisplayName("camino feliz persiste aporte y saldo")
        void persiste_aporte_y_saldo() {
            BigDecimal monto = new BigDecimal("1000000");
            when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
            when(afiliadoRepository.findByAfiliadoId(AFILIADO)).thenReturn(Optional.of(afiliadoActivo()));
            when(parametroRepository.findLatest()).thenReturn(Optional.empty());
            when(saldoInicializador.obtenerOInicializar(eq(AFILIADO), anyString())).thenReturn(saldoVacio());
            when(aporteRepository.guardar(any())).thenReturn(aporteGuardado(monto, EstadoAporte.PENDIENTE));
            when(saldoRepository.guardar(any())).thenReturn(saldoVacio());

            useCase.registrar(comando(monto));

            verify(aporteRepository).guardar(any(Aporte.class));
            verify(saldoRepository).guardar(any(SaldoMensual.class));
        }
    }
}
