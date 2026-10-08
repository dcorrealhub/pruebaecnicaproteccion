package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de las reglas de negocio del registro de aportes.
 * Los puertos de salida se mockean: no hay Spring ni base de datos.
 */
class RegistrarAporteUseCaseImplTest {

    private static final String AFILIADO = "AF-001";

    private AporteRepositoryPort aporteRepository;
    private SaldoRepositoryPort saldoRepository;
    private RegistrarAporteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        aporteRepository = mock(AporteRepositoryPort.class);
        saldoRepository = mock(SaldoRepositoryPort.class);
        // Mock del transaction manager: TransactionTemplate ejecuta la lambda sin transacción real
        useCase = new RegistrarAporteUseCaseImpl(aporteRepository, saldoRepository,
                mock(PlatformTransactionManager.class));
        ReflectionTestUtils.setField(useCase, "topeMensual", new BigDecimal("10000000"));
        ReflectionTestUtils.setField(useCase, "umbralRevision", new BigDecimal("5000000"));
        ReflectionTestUtils.setField(useCase, "umbralRevisionSucursal", new BigDecimal("3000000"));

        when(aporteRepository.findByIdempotenciaKey(anyString())).thenReturn(Optional.empty());
        when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void saldoActual(String total) {
        when(saldoRepository.findByAfiliadoIdAndMes(eq(AFILIADO), anyString()))
                .thenReturn(Optional.of(new SaldoMensual(1L, AFILIADO, "2026-10", new BigDecimal(total), 0)));
    }

    @Test
    void rechazaAporteQueSuperaElTopeMensual() {
        saldoActual("9500000");
        var command = new RegistrarAporteCommand(AFILIADO, new BigDecimal("600000"), "WEB", "k-tope");

        assertThatThrownBy(() -> useCase.registrar(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tope mensual");

        verify(saldoRepository, never()).guardar(any());
        verify(aporteRepository, never()).guardar(any());
    }

    @Test
    void permiteAporteQueIgualaExactamenteElTope() {
        saldoActual("9500000");
        var command = new RegistrarAporteCommand(AFILIADO, new BigDecimal("500000"), "WEB", "k-limite");

        Aporte aporte = useCase.registrar(command);

        assertThat(aporte.getMonto()).isEqualByComparingTo("500000");
        verify(saldoRepository).guardar(argThat(s -> s.getTotal().compareTo(new BigDecimal("10000000")) == 0));
    }

    @Test
    void marcaParaRevisionAporteSobreElUmbral() {
        saldoActual("0");
        var command = new RegistrarAporteCommand(AFILIADO, new BigDecimal("6000000"), "WEB", "k-umbral");

        Aporte aporte = useCase.registrar(command);

        assertThat(aporte.isMarcadaRevision()).isTrue();
    }

    @Test
    void noMarcaParaRevisionAporteBajoElUmbral() {
        saldoActual("0");
        var command = new RegistrarAporteCommand(AFILIADO, new BigDecimal("4000000"), "WEB", "k-bajo");

        assertThat(useCase.registrar(command).isMarcadaRevision()).isFalse();
    }

    @Test
    void canalSucursalUsaUmbralDeRevisionPropio() {
        saldoActual("0");
        var command = new RegistrarAporteCommand(AFILIADO, new BigDecimal("3500000"), "SUCURSAL", "k-suc");

        assertThat(useCase.registrar(command).isMarcadaRevision()).isTrue();
    }

    @Test
    void idempotenciaDevuelveAporteExistenteSinDuplicar() {
        var existente = new Aporte(42L, AFILIADO, new BigDecimal("1000000"), LocalDate.of(2026, 10, 1),
                "WEB", "2026-10", false, "k-dup", false);
        when(aporteRepository.findByIdempotenciaKey("k-dup")).thenReturn(Optional.of(existente));
        var command = new RegistrarAporteCommand(AFILIADO, new BigDecimal("1000000"), "WEB", "k-dup");

        Aporte resultado = useCase.registrar(command);

        assertThat(resultado.getId()).isEqualTo(42L);
        assertThat(resultado.isYaExistia()).isTrue();
        verify(aporteRepository, never()).guardar(any());
        verifyNoInteractions(saldoRepository);
    }
}
