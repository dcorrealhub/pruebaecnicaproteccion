package co.proteccion.cis.retob.application;

import co.proteccion.cis.retob.application.usecase.RegistrarAporteTransaccion;
import co.proteccion.cis.retob.application.usecase.RegistrarAporteUseCaseImpl;
import co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException;
import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarAporteUseCaseImplTest {

    @Mock RegistrarAporteTransaccion transaccion;
    @Mock AporteRepositoryPort aporteRepository;

    @InjectMocks RegistrarAporteUseCaseImpl useCase;

    private RegistrarAporteCommand comando(LocalDate fecha) {
        return new RegistrarAporteCommand(
                "AF-001", new BigDecimal("1000"), fecha, Canal.WEB, "key-1");
    }

    private Aporte aporteDummy() {
        return Aporte.nuevo("AF-001", new BigDecimal("1000"),
                LocalDate.of(2025, 3, 10), Canal.WEB, false, "key-1");
    }

    @Test
    void rechaza_fecha_futura() {
        assertThatThrownBy(() -> useCase.registrar(comando(LocalDate.now().plusDays(1))))
                .isInstanceOf(ReglaNegocioException.class);
        verify(transaccion, times(0)).ejecutar(any());
    }

    @Test
    void rechaza_fecha_nula() {
        assertThatThrownBy(() -> useCase.registrar(comando(null)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void feliz_delega_en_transaccion() {
        Aporte esperado = aporteDummy();
        when(transaccion.ejecutar(any())).thenReturn(esperado);

        Aporte resultado = useCase.registrar(comando(LocalDate.of(2025, 3, 10)));

        assertThat(resultado).isSameAs(esperado);
        verify(transaccion, times(1)).ejecutar(any());
    }

    @Test
    void carrera_idempotente_recupera_original_sin_duplicar() {
        Aporte original = aporteDummy();
        // La inserción concurrente viola el UNIQUE de idempotencia.
        when(transaccion.ejecutar(any()))
                .thenThrow(new DataIntegrityViolationException("uq_aporte_idempotencia"));
        when(aporteRepository.findByIdempotenciaKey("key-1")).thenReturn(Optional.of(original));

        Aporte resultado = useCase.registrar(comando(LocalDate.of(2025, 3, 10)));

        assertThat(resultado).isSameAs(original);
        verify(transaccion, times(1)).ejecutar(any());
    }

    @Test
    void reintenta_ante_conflicto_optimista_y_luego_tiene_exito() {
        Aporte esperado = aporteDummy();
        when(transaccion.ejecutar(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("saldo", 1L))
                .thenReturn(esperado);

        Aporte resultado = useCase.registrar(comando(LocalDate.of(2025, 3, 10)));

        assertThat(resultado).isSameAs(esperado);
        verify(transaccion, times(2)).ejecutar(any());
    }

    @Test
    void conflicto_persistente_tras_reintentos_lanza_409() {
        when(transaccion.ejecutar(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("saldo", 1L));

        assertThatThrownBy(() -> useCase.registrar(comando(LocalDate.of(2025, 3, 10))))
                .isInstanceOf(ConflictoConcurrenciaException.class);
        verify(transaccion, times(3)).ejecutar(any());
    }
}
