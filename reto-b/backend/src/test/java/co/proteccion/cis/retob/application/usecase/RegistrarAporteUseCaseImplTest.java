package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarAporteUseCaseImplTest {

    @Mock
    private AporteRepositoryPort aporteRepository;

    @Mock
    private SaldoRepositoryPort saldoRepository;

    @InjectMocks
    private RegistrarAporteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "topeMensual", new BigDecimal("1000000"));
        ReflectionTestUtils.setField(useCase, "umbralRevision", new BigDecimal("500000"));
    }

    @Test
    void registrar_idempotencia_retornaAporteExistentesinReprocesar() {
        Aporte aporteExistente = aporte(1L, "500000", false);
        when(aporteRepository.findByIdempotenciaKey("clave-1")).thenReturn(Optional.of(aporteExistente));

        Aporte resultado = useCase.registrar(comando("AF-001", "500000", "clave-1"));

        assertThat(resultado).isSameAs(aporteExistente);
        verify(saldoRepository, never()).findByAfiliadoIdAndMes(any(), any());
        verify(aporteRepository, never()).guardar(any());
    }

    @Test
    void registrar_montoInvalido_lanzaReglaNegocioException() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.registrar(comando("AF-001", "0.00", "clave-2")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("mayor a cero")
                .extracting("codigo").isEqualTo("MONTO_INVALIDO");
    }

    @Test
    void registrar_topeMensualExcedido_lanzaReglaNegocioException() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(eq("AF-001"), any()))
                .thenReturn(Optional.of(saldo("900000")));

        assertThatThrownBy(() -> useCase.registrar(comando("AF-001", "200000", "clave-3")))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("TOPE_MENSUAL_EXCEDIDO");
    }

    @Test
    void registrar_montoSuperaUmbral_marcaParaRevision() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.of(saldo("0")));
        when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saldoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Aporte resultado = useCase.registrar(comando("AF-001", "600000", "clave-4"));

        assertThat(resultado.isMarcadaRevision()).isTrue();
    }

    @Test
    void registrar_montoMenorUmbral_noMarcaRevision() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.of(saldo("0")));
        when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saldoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Aporte resultado = useCase.registrar(comando("AF-001", "100000", "clave-5"));

        assertThat(resultado.isMarcadaRevision()).isFalse();
    }

    @Test
    void registrar_saldoNoExiste_inicializaYProcesa() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.empty());
        when(saldoRepository.inicializar(any(), any())).thenReturn(saldo("0"));
        when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saldoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Aporte resultado = useCase.registrar(comando("AF-002", "100000", "clave-6"));

        assertThat(resultado).isNotNull();
        verify(saldoRepository).inicializar(eq("AF-002"), any());
    }

    private RegistrarAporteCommand comando(String afiliadoId, String monto, String clave) {
        return new RegistrarAporteCommand(
                afiliadoId,
                new BigDecimal(monto),
                LocalDate.of(2026, 6, 1),
                "APP_MOVIL",
                clave
        );
    }

    private Aporte aporte(Long id, String monto, boolean revision) {
        return new Aporte(id, "AF-001", new BigDecimal(monto), LocalDate.of(2026, 6, 1),
                "APP_MOVIL", "2026-06", revision, "clave-test");
    }

    private SaldoMensual saldo(String total) {
        return new SaldoMensual(1L, "AF-001", "2026-06", new BigDecimal(total), 0);
    }
}
