package co.proteccion.cis.retob.domain.usecase;

import co.proteccion.cis.retob.domain.exception.DomainValidationException;
import co.proteccion.cis.retob.domain.model.parametro.ParametrosAporte;
import co.proteccion.cis.retob.domain.model.parametro.gateway.ParametroOutputPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParametroUseCaseTest {

    @Mock ParametroOutputPort parametroOutputPort;

    ParametroUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ParametroUseCase(parametroOutputPort);
    }

    @Test
    void actualizar_valido_persiste() {
        ParametrosAporte p = new ParametrosAporte(new BigDecimal("20000000"), new BigDecimal("8000000"));
        when(parametroOutputPort.actualizarGlobal(p)).thenReturn(p);

        assertThatCode(() -> useCase.actualizarGlobal(p)).doesNotThrowAnyException();
        verify(parametroOutputPort).actualizarGlobal(p);
    }

    @Test
    void actualizar_umbralMayorQueTope_lanzaValidacion() {
        ParametrosAporte p = new ParametrosAporte(new BigDecimal("1000"), new BigDecimal("2000"));

        assertThatThrownBy(() -> useCase.actualizarGlobal(p))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("umbral");
        verify(parametroOutputPort, never()).actualizarGlobal(any());
    }

    @Test
    void actualizar_valorNoPositivo_lanzaValidacion() {
        assertThatThrownBy(() -> useCase.actualizarGlobal(new ParametrosAporte(BigDecimal.ZERO, BigDecimal.ONE)))
                .isInstanceOf(DomainValidationException.class);
    }
}
