package co.proteccion.cis.retob.application.usecase;

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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    void setUp() throws Exception {
        setField("topeMensual", new BigDecimal("10000000"));
        setField("umbralRevision", new BigDecimal("5000000"));
    }

    private void setField(String name, Object value) throws Exception {
        Field f = RegistrarAporteUseCaseImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(useCase, value);
    }

    private final RegistrarAporteCommand cmd = new RegistrarAporteCommand(
            "AF-001", new BigDecimal("500000"), "APP_MOVIL", "key-unica"
    );

    // 3.1 Idempotencia
    @Test
    void registrar_cuandoIdempotenciaKeyYaExiste_retornaExistenteSinDuplicar() {
        var existente = crearAporte(1L, "AF-001", "500000", "key-unica", false);
        when(aporteRepository.findByIdempotenciaKey("key-unica")).thenReturn(Optional.of(existente));

        var result = useCase.registrar(cmd);

        assertSame(existente, result);
        verify(aporteRepository, never()).guardar(any());
        verify(saldoRepository, never()).guardar(any());
    }

    // 3.2 Monto inválido
    @Test
    void registrar_cuandoMontoEsNulo_lanzaExcepcion() {
        var cmd = new RegistrarAporteCommand("AF-001", null, "WEB", "key-err");

        var ex = assertThrows(IllegalArgumentException.class, () -> useCase.registrar(cmd));
        assertEquals("El monto debe ser positivo", ex.getMessage());
    }

    @Test
    void registrar_cuandoMontoEsCero_lanzaExcepcion() {
        var cmd = new RegistrarAporteCommand("AF-001", BigDecimal.ZERO, "WEB", "key-err");

        var ex = assertThrows(IllegalArgumentException.class, () -> useCase.registrar(cmd));
        assertEquals("El monto debe ser positivo", ex.getMessage());
    }

    @Test
    void registrar_cuandoMontoEsNegativo_lanzaExcepcion() {
        var cmd = new RegistrarAporteCommand("AF-001", new BigDecimal("-100"), "WEB", "key-err");

        var ex = assertThrows(IllegalArgumentException.class, () -> useCase.registrar(cmd));
        assertEquals("El monto debe ser positivo", ex.getMessage());
    }

    // 3.3 Tope mensual
    @Test
    void registrar_cuandoMontoSuperaTopeMensual_lanzaExcepcion() {
        when(aporteRepository.findByIdempotenciaKey("key-tope")).thenReturn(Optional.empty());
        var saldo = new SaldoMensual(1L, "AF-001", "2026-06", new BigDecimal("9500000"), 0);
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2026-06")).thenReturn(Optional.of(saldo));

        var cmd = new RegistrarAporteCommand("AF-001", new BigDecimal("600000"), "WEB", "key-tope");

        var ex = assertThrows(IllegalArgumentException.class, () -> useCase.registrar(cmd));
        assertEquals("El monto supera el tope mensual permitido", ex.getMessage());
    }

    // 3.4 Umbral de revisión
    @Test
    void registrar_cuandoMontoSuperaUmbralRevision_marcaRevision() {
        when(aporteRepository.findByIdempotenciaKey("key-revision")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2026-06")).thenReturn(Optional.empty());
        when(saldoRepository.inicializar("AF-001", "2026-06"))
                .thenReturn(new SaldoMensual(1L, "AF-001", "2026-06", BigDecimal.ZERO, 0));
        var aporteGuardado = crearAporte(1L, "AF-001", "6000000", "key-revision", true);
        when(aporteRepository.guardar(argThat(a -> a.isMarcadaRevision()))).thenReturn(aporteGuardado);

        var cmd = new RegistrarAporteCommand("AF-001", new BigDecimal("6000000"), "WEB", "key-revision");

        var result = useCase.registrar(cmd);

        assertTrue(result.isMarcadaRevision());
    }

    @Test
    void registrar_cuandoMontoNoSuperaUmbralRevision_noMarcaRevision() {
        when(aporteRepository.findByIdempotenciaKey("key-normal")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2026-06")).thenReturn(Optional.empty());
        when(saldoRepository.inicializar("AF-001", "2026-06"))
                .thenReturn(new SaldoMensual(1L, "AF-001", "2026-06", BigDecimal.ZERO, 0));
        var aporteGuardado = crearAporte(1L, "AF-001", "300000", "key-normal", false);
        when(aporteRepository.guardar(any())).thenReturn(aporteGuardado);

        var cmd = new RegistrarAporteCommand("AF-001", new BigDecimal("300000"), "WEB", "key-normal");

        var result = useCase.registrar(cmd);

        assertFalse(result.isMarcadaRevision());
    }

    // 3.5 Flujo feliz
    @Test
    void registrar_cuandoFlujoFeliz_guardaAporteYActualizaSaldo() {
        when(aporteRepository.findByIdempotenciaKey("key-feliz")).thenReturn(Optional.empty());
        var saldo = new SaldoMensual(1L, "AF-001", "2026-06", new BigDecimal("200000"), 0);
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2026-06")).thenReturn(Optional.of(saldo));
        var aporteGuardado = crearAporte(2L, "AF-001", "500000", "key-feliz", false);
        when(aporteRepository.guardar(any())).thenReturn(aporteGuardado);

        var cmd = new RegistrarAporteCommand("AF-001", new BigDecimal("500000"), "APP_MOVIL", "key-feliz");

        var result = useCase.registrar(cmd);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        verify(saldoRepository).guardar(argThat(s ->
                s.getTotal().compareTo(new BigDecimal("700000")) == 0));
        verify(aporteRepository).guardar(argThat(a ->
                a.getId() == null &&
                a.getAfiliadoId().equals("AF-001") &&
                a.getMonto().compareTo(new BigDecimal("500000")) == 0 &&
                a.getCanal().equals("APP_MOVIL")));
    }

    @Test
    void registrar_cuandoNoExisteSaldo_inicializaYNuevoSaldo() {
        when(aporteRepository.findByIdempotenciaKey("key-nuevo")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2026-06")).thenReturn(Optional.empty());
        when(saldoRepository.inicializar("AF-001", "2026-06"))
                .thenReturn(new SaldoMensual(1L, "AF-001", "2026-06", BigDecimal.ZERO, 0));
        var aporteGuardado = crearAporte(3L, "AF-001", "500000", "key-nuevo", false);
        when(aporteRepository.guardar(any())).thenReturn(aporteGuardado);

        var cmd = new RegistrarAporteCommand("AF-001", new BigDecimal("500000"), "WEB", "key-nuevo");

        var result = useCase.registrar(cmd);

        assertNotNull(result);
        verify(saldoRepository).inicializar("AF-001", "2026-06");
        verify(saldoRepository).guardar(argThat(s ->
                s.getTotal().compareTo(new BigDecimal("500000")) == 0));
    }

    private Aporte crearAporte(Long id, String afiliadoId, String monto, String idempotenciaKey, boolean marcadaRevision) {
        return new Aporte(
                id,
                afiliadoId,
                new BigDecimal(monto),
                LocalDate.now(),
                "APP_MOVIL",
                "2026-06",
                marcadaRevision,
                idempotenciaKey
        );
    }
}