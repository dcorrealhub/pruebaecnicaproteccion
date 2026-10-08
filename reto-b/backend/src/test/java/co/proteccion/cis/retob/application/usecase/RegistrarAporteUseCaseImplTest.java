package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AporteRechazadoException;
import co.proteccion.cis.retob.domain.exception.IdempotenciaConflictoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.EventoAporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.model.enums.Canal;
import co.proteccion.cis.retob.domain.model.enums.TipoEventoAporte;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.Creado;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistroAporte;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.Repetido;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarAporteUseCaseImplTest {

    private static final String AFILIADO = "AF-001";
    private static final String CLAVE = "clave-1";
    private static final LocalDate HOY = LocalDate.of(2026, 10, 8);
    private static final String PERIODO = "2026-10";
    private static final BigDecimal TOPE_MENSUAL = new BigDecimal("10000000");
    private static final BigDecimal UMBRAL_DEFAULT = new BigDecimal("5000000");
    private static final BigDecimal UMBRAL_SUCURSAL = new BigDecimal("3000000");

    @Mock
    private AporteRepositoryPort aporteRepository;
    @Mock
    private SaldoRepositoryPort saldoRepository;
    @Mock
    private EventoAporteRepositoryPort eventoAporteRepository;

    private RegistrarAporteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        ZoneId bogota = ZoneId.of("America/Bogota");
        Clock clock = Clock.fixed(HOY.atStartOfDay(bogota).plusHours(12).toInstant(), bogota);
        useCase = new RegistrarAporteUseCaseImpl(
                aporteRepository, saldoRepository, eventoAporteRepository, clock,
                TOPE_MENSUAL, UMBRAL_DEFAULT, Map.of(Canal.SUCURSAL, UMBRAL_SUCURSAL));
    }

    @Test
    void registrar_aporteValido_persisteAporteSaldoYEventoYRetornaCreado() {
        // Arrange
        var command = comando(new BigDecimal("1000000"), Canal.WEB);
        when(aporteRepository.findByIdempotenciaKey(CLAVE)).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(AFILIADO, PERIODO))
                .thenReturn(Optional.of(saldo(new BigDecimal("2000000"))));
        when(aporteRepository.guardar(any())).thenAnswer(inv -> conId(inv.getArgument(0), 10L));

        // Act
        RegistroAporte resultado = useCase.registrar(command);

        // Assert
        assertThat(resultado).isInstanceOf(Creado.class);
        Aporte aporte = resultado.aporte();
        assertThat(aporte.getId()).isEqualTo(10L);
        assertThat(aporte.getPeriodo()).isEqualTo(PERIODO);
        assertThat(aporte.isMarcadaRevision()).isFalse();

        var saldoCaptor = ArgumentCaptor.forClass(SaldoMensual.class);
        verify(saldoRepository).guardar(saldoCaptor.capture());
        assertThat(saldoCaptor.getValue().getTotal()).isEqualByComparingTo("3000000");

        var eventoCaptor = ArgumentCaptor.forClass(EventoAporte.class);
        verify(eventoAporteRepository).guardar(eventoCaptor.capture());
        assertThat(eventoCaptor.getValue().getAporteId()).isEqualTo(10L);
        assertThat(eventoCaptor.getValue().getTipo()).isEqualTo(TipoEventoAporte.APORTE_REGISTRADO);
    }

    @Test
    void registrar_montoCero_lanzaAporteRechazadoYNoPersiste() {
        // Arrange
        var command = comando(BigDecimal.ZERO, Canal.WEB);
        when(aporteRepository.findByIdempotenciaKey(CLAVE)).thenReturn(Optional.empty());

        // Act
        Throwable lanzado = catchThrowable(() -> useCase.registrar(command));

        // Assert
        assertThat(lanzado).isInstanceOfSatisfying(AporteRechazadoException.class,
                e -> assertThat(e.getCodigo()).isEqualTo("MONTO_NO_POSITIVO"));
        verify(aporteRepository, never()).guardar(any());
        verifyNoInteractions(saldoRepository, eventoAporteRepository);
    }

    @Test
    void registrar_superaTopeMensual_lanzaAporteRechazadoYNoPersiste() {
        // Arrange
        var command = comando(new BigDecimal("1500000"), Canal.WEB);
        when(aporteRepository.findByIdempotenciaKey(CLAVE)).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(AFILIADO, PERIODO))
                .thenReturn(Optional.of(saldo(new BigDecimal("9000000"))));

        // Act
        Throwable lanzado = catchThrowable(() -> useCase.registrar(command));

        // Assert
        assertThat(lanzado).isInstanceOfSatisfying(AporteRechazadoException.class, e -> {
            assertThat(e.getCodigo()).isEqualTo("TOPE_MENSUAL_EXCEDIDO");
            assertThat(e.getMessage()).contains("Disponible: 1000000");
        });
        verify(saldoRepository, never()).guardar(any());
        verify(aporteRepository, never()).guardar(any());
        verifyNoInteractions(eventoAporteRepository);
    }

    @ParameterizedTest(name = "{0} con monto {1} → marcado={2}")
    @CsvSource({
            "SUCURSAL, 3000000, false",
            "SUCURSAL, 3000001, true",
            "WEB,      3000001, false",
            "WEB,      5000001, true"
    })
    void registrar_montoSegunCanal_marcaRevisionConUmbralDelCanal(Canal canal, BigDecimal monto, boolean esperado) {
        // Arrange
        var command = comando(monto, canal);
        when(aporteRepository.findByIdempotenciaKey(CLAVE)).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(AFILIADO, PERIODO))
                .thenReturn(Optional.of(saldo(BigDecimal.ZERO)));
        when(aporteRepository.guardar(any())).thenAnswer(inv -> conId(inv.getArgument(0), 1L));

        // Act
        RegistroAporte resultado = useCase.registrar(command);

        // Assert
        assertThat(resultado.aporte().isMarcadaRevision()).isEqualTo(esperado);
    }

    @Test
    void registrar_claveExistenteMismoContenido_retornaRepetidoSinPersistir() {
        // Arrange
        var command = comando(new BigDecimal("1000000"), Canal.WEB);
        var existente = new Aporte(7L, AFILIADO, new BigDecimal("1000000.00"), HOY, Canal.WEB, PERIODO, false, CLAVE);
        when(aporteRepository.findByIdempotenciaKey(CLAVE)).thenReturn(Optional.of(existente));

        // Act
        RegistroAporte resultado = useCase.registrar(command);

        // Assert
        assertThat(resultado).isInstanceOf(Repetido.class);
        assertThat(resultado.aporte()).isSameAs(existente);
        verify(aporteRepository, never()).guardar(any());
        verifyNoInteractions(saldoRepository, eventoAporteRepository);
    }

    @Test
    void registrar_claveExistenteContenidoDistinto_lanzaIdempotenciaConflicto() {
        // Arrange
        var command = comando(new BigDecimal("2000000"), Canal.WEB);
        var existente = new Aporte(7L, AFILIADO, new BigDecimal("1000000"), HOY, Canal.WEB, PERIODO, false, CLAVE);
        when(aporteRepository.findByIdempotenciaKey(CLAVE)).thenReturn(Optional.of(existente));

        // Act
        Throwable lanzado = catchThrowable(() -> useCase.registrar(command));

        // Assert
        assertThat(lanzado).isInstanceOf(IdempotenciaConflictoException.class);
        verify(aporteRepository, never()).guardar(any());
        verifyNoInteractions(saldoRepository, eventoAporteRepository);
    }

    private RegistrarAporteCommand comando(BigDecimal monto, Canal canal) {
        return new RegistrarAporteCommand(AFILIADO, monto, HOY, canal, CLAVE);
    }

    private SaldoMensual saldo(BigDecimal total) {
        return new SaldoMensual(1L, AFILIADO, PERIODO, total, 0);
    }

    private Aporte conId(Aporte aporte, Long id) {
        return new Aporte(id, aporte.getAfiliadoId(), aporte.getMonto(), aporte.getFecha(),
                aporte.getCanal(), aporte.getPeriodo(), aporte.isMarcadaRevision(), aporte.getIdempotenciaKey());
    }
}
