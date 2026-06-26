package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarAporteUseCaseImplTest {

    @Mock AporteRepositoryPort aporteRepository;
    @Mock SaldoRepositoryPort saldoRepository;

    RegistrarAporteUseCaseImpl useCase;

    static final BigDecimal TOPE = new BigDecimal("10000000");
    static final BigDecimal UMBRAL = new BigDecimal("5000000");
    static final String PERIODO = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

    @BeforeEach
    void setUp() {
        useCase = new RegistrarAporteUseCaseImpl(aporteRepository, saldoRepository);
        // ReflectionTestUtils inyecta los @Value sin levantar el contexto de Spring.
        ReflectionTestUtils.setField(useCase, "topeMensual", TOPE);
        ReflectionTestUtils.setField(useCase, "umbralRevision", UMBRAL);
    }

    // Un reintento con la misma idempotenciaKey debe devolver el aporte original sin duplicarlo.
    @Test
    void registrar_claveDuplicada_retornaAporteExistentesinPersistir() {
        var existente = aporte("KEY-1", new BigDecimal("100"), false);
        when(aporteRepository.findByIdempotenciaKey("KEY-1")).thenReturn(Optional.of(existente));

        var result = useCase.registrar(new RegistrarAporteCommand("AF-001", new BigDecimal("100"), "WEB", "KEY-1"));

        assertThat(result).isSameAs(existente);
        verify(aporteRepository, never()).guardar(any());
        verify(saldoRepository, never()).guardar(any());
    }

    // El acumulado resultante (9M + 2M = 11M) supera el tope de 10M; debe rechazarse.
    @Test
    void registrar_montoSuperaTopeMensual_lanzaExcepcion() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        var saldo = saldo(new BigDecimal("9000000"));
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", PERIODO)).thenReturn(Optional.of(saldo));

        assertThatThrownBy(() ->
                useCase.registrar(new RegistrarAporteCommand("AF-001", new BigDecimal("2000000"), "WEB", "KEY-2"))
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tope mensual");
    }

    // Un aporte de 6M supera el umbral de revisión de 5M; debe quedar marcado para revisión manual.
    @Test
    void registrar_montoSuperaUmbral_marcaAporteParaRevision() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.of(saldo(BigDecimal.ZERO)));
        when(saldoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));
        var montoAlto = new BigDecimal("6000000");
        when(aporteRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

        var result = useCase.registrar(new RegistrarAporteCommand("AF-001", montoAlto, "WEB", "KEY-3"));

        assertThat(result.isMarcadaRevision()).isTrue();
    }

    // Un aporte por debajo del umbral no debe generar alerta de revisión.
    @Test
    void registrar_montoMenorUmbral_noMarcaRevision() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.of(saldo(BigDecimal.ZERO)));
        when(saldoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(aporteRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

        var result = useCase.registrar(new RegistrarAporteCommand("AF-001", new BigDecimal("100"), "WEB", "KEY-4"));

        assertThat(result.isMarcadaRevision()).isFalse();
    }

    // El primer aporte del mes para un afiliado debe crear el saldo en cero antes de acumular.
    @Test
    void registrar_sinSaldoPrevio_inicializaYActualiza() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.empty());
        when(saldoRepository.inicializar(eq("AF-001"), eq(PERIODO))).thenReturn(saldo(BigDecimal.ZERO));
        when(saldoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(aporteRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

        useCase.registrar(new RegistrarAporteCommand("AF-001", new BigDecimal("500"), "APP_MOVIL", "KEY-5"));

        verify(saldoRepository).inicializar("AF-001", PERIODO);
        verify(saldoRepository).guardar(any());
    }

    // Verifica que el periodo generado coincide con el mes actual y los datos del comando se preservan.
    @Test
    void registrar_aporteValido_persisteConPeriodoCorrecto() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.of(saldo(BigDecimal.ZERO)));
        when(saldoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(aporteRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

        var result = useCase.registrar(new RegistrarAporteCommand("AF-001", new BigDecimal("1000"), "SUCURSAL", "KEY-6"));

        assertThat(result.getPeriodo()).isEqualTo(PERIODO);
        assertThat(result.getAfiliadoId()).isEqualTo("AF-001");
        assertThat(result.getMonto()).isEqualByComparingTo(new BigDecimal("1000"));
    }

    // Un afiliadoId en minúsculas debe normalizarse a mayúsculas antes de persistir y consultar saldo.
    @Test
    void registrar_afiliadoIdEnMinusculas_normalizaAMayusculas() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(eq("AF-001"), eq(PERIODO))).thenReturn(Optional.of(saldo(BigDecimal.ZERO)));
        when(saldoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(aporteRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

        var result = useCase.registrar(new RegistrarAporteCommand("af-001", new BigDecimal("1000"), "WEB", "KEY-N1"));

        assertThat(result.getAfiliadoId()).isEqualTo("AF-001");
        verify(saldoRepository).findByAfiliadoIdAndMes("AF-001", PERIODO);
    }

    // --- helpers ---

    private SaldoMensual saldo(BigDecimal total) {
        return new SaldoMensual(1L, "AF-001", PERIODO, total, 0);
    }

    private Aporte aporte(String key, BigDecimal monto, boolean revision) {
        return new Aporte(1L, "AF-001", monto, LocalDate.now(), "WEB", PERIODO, revision, key);
    }
}
