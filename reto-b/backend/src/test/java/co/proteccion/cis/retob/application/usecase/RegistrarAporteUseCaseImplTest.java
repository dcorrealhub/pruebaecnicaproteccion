package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.support.AporteMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegistrarAporteUseCaseImplTest {

    @Mock
    AporteRepositoryPort aporteRepo;

    @Mock
    SaldoRepositoryPort saldoRepo;

    @InjectMocks
    RegistrarAporteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "topeMensual", new BigDecimal("10000000"));
        ReflectionTestUtils.setField(useCase, "umbralRevision", new BigDecimal("5000000"));
    }

    private void configurarHappyPath() {
        given(aporteRepo.findByIdempotenciaKey(anyString())).willReturn(Optional.empty());
        given(saldoRepo.findByAfiliadoIdAndMes(anyString(), anyString()))
                .willReturn(Optional.of(AporteMother.saldoEnCero("AF-001", "2026-06")));
        given(aporteRepo.guardar(any())).willReturn(AporteMother.normal());
        given(saldoRepo.guardar(any())).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registrar_idempotenciaKeyExistente_retornaAporteExistenteSinPersistir() {
        given(aporteRepo.findByIdempotenciaKey("idem-normal-001"))
                .willReturn(Optional.of(AporteMother.normal()));

        RegistrarAporteCommand command = AporteMother.command("AF-001", "100000", "WEB", "idem-normal-001");
        Aporte resultado = useCase.registrar(command);

        assertThat(resultado.getIdempotenciaKey()).isEqualTo("idem-normal-001");
        verify(aporteRepo, never()).guardar(any());
        verify(saldoRepo, never()).guardar(any());
    }

    @Test
    void registrar_montoPositivo_persisteAporteYActualizaSaldo() {
        configurarHappyPath();
        RegistrarAporteCommand command = AporteMother.command("AF-001", "100000", "WEB", "idem-001");

        Aporte resultado = useCase.registrar(command);

        assertThat(resultado.getAfiliadoId()).isEqualTo("AF-001");
        verify(aporteRepo).guardar(any());
        verify(saldoRepo).guardar(any());
    }

    @Test
    void registrar_montoIgualACero_lanzaIllegalArgumentException() {
        given(aporteRepo.findByIdempotenciaKey(anyString())).willReturn(Optional.empty());
        RegistrarAporteCommand command = AporteMother.command("AF-001", "0", "WEB", "idem-001");

        assertThatThrownBy(() -> useCase.registrar(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El monto debe ser mayor a cero");
    }

    @Test
    void registrar_montoNegativo_lanzaIllegalArgumentException() {
        given(aporteRepo.findByIdempotenciaKey(anyString())).willReturn(Optional.empty());
        RegistrarAporteCommand command = AporteMother.command("AF-001", "-1", "WEB", "idem-001");

        assertThatThrownBy(() -> useCase.registrar(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El monto debe ser mayor a cero");
    }

    @Test
    void registrar_saldoNoExiste_llamaInicializarYProcede() {
        given(aporteRepo.findByIdempotenciaKey(anyString())).willReturn(Optional.empty());
        given(saldoRepo.findByAfiliadoIdAndMes(anyString(), anyString())).willReturn(Optional.empty());
        given(saldoRepo.inicializar(anyString(), anyString()))
                .willReturn(AporteMother.saldoEnCero("AF-001", "2026-06"));
        given(aporteRepo.guardar(any())).willReturn(AporteMother.normal());
        given(saldoRepo.guardar(any())).willAnswer(inv -> inv.getArgument(0));

        RegistrarAporteCommand command = AporteMother.command("AF-001", "100000", "WEB", "idem-001");
        useCase.registrar(command);

        verify(saldoRepo).inicializar(eq("AF-001"), anyString());
    }

    @Test
    void registrar_aporteSuperaTopeMensual_lanzaIllegalArgumentException() {
        given(aporteRepo.findByIdempotenciaKey(anyString())).willReturn(Optional.empty());
        given(saldoRepo.findByAfiliadoIdAndMes(anyString(), anyString()))
                .willReturn(Optional.of(AporteMother.saldoCon("AF-001", "2026-06", "9500000")));

        RegistrarAporteCommand command = AporteMother.command("AF-001", "1000000", "WEB", "idem-001");

        assertThatThrownBy(() -> useCase.registrar(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tope mensual");
    }

    @Test
    void registrar_aporteLlevaJustoAlTope_nuncaLanzaExcepcion() {
        given(aporteRepo.findByIdempotenciaKey(anyString())).willReturn(Optional.empty());
        given(saldoRepo.findByAfiliadoIdAndMes(anyString(), anyString()))
                .willReturn(Optional.of(AporteMother.saldoCon("AF-001", "2026-06", "9000000")));
        given(aporteRepo.guardar(any())).willReturn(AporteMother.normal());
        given(saldoRepo.guardar(any())).willAnswer(inv -> inv.getArgument(0));

        RegistrarAporteCommand command = AporteMother.command("AF-001", "1000000", "WEB", "idem-001");
        useCase.registrar(command);

        verify(aporteRepo).guardar(any());
    }

    @Test
    void registrar_montoSuperaUmbral_marcadaRevisionTrue() {
        configurarHappyPath();
        ArgumentCaptor<Aporte> captor = ArgumentCaptor.forClass(Aporte.class);
        given(aporteRepo.guardar(captor.capture())).willReturn(AporteMother.normal());

        RegistrarAporteCommand command = AporteMother.command("AF-001", "5000001", "WEB", "idem-001");
        useCase.registrar(command);

        assertThat(captor.getValue().isMarcadaRevision()).isTrue();
    }

    @Test
    void registrar_montoIgualAlUmbral_marcadaRevisionFalse() {
        configurarHappyPath();
        ArgumentCaptor<Aporte> captor = ArgumentCaptor.forClass(Aporte.class);
        given(aporteRepo.guardar(captor.capture())).willReturn(AporteMother.normal());

        RegistrarAporteCommand command = AporteMother.command("AF-001", "5000000", "WEB", "idem-001");
        useCase.registrar(command);

        assertThat(captor.getValue().isMarcadaRevision()).isFalse();
    }

    @Test
    void registrar_montoDebajoDelUmbral_marcadaRevisionFalse() {
        configurarHappyPath();
        ArgumentCaptor<Aporte> captor = ArgumentCaptor.forClass(Aporte.class);
        given(aporteRepo.guardar(captor.capture())).willReturn(AporteMother.normal());

        RegistrarAporteCommand command = AporteMother.command("AF-001", "4999999", "WEB", "idem-001");
        useCase.registrar(command);

        assertThat(captor.getValue().isMarcadaRevision()).isFalse();
    }
}
