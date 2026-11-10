package org.mgroko.backend.admin.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.ConfiguracionSchedulerLimpiezaImagenesResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerLimpiezaImagenesRequest;
import org.mgroko.backend.admin.dto.EjecutarLimpiezaImagenesResponse;
import org.mgroko.backend.storage.servicio.LimpiezaImagenesScheduler;
import org.mgroko.backend.storage.servicio.LimpiezaImagenesService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminLimpiezaImagenesServiceTest {

    @Mock
    private ConfiguracionSistemaService configuracionSistemaService;

    @Mock
    private LimpiezaImagenesService limpiezaImagenesService;

    @Mock
    private LimpiezaImagenesScheduler limpiezaImagenesScheduler;

    @Mock
    private AdminLimpiezaImagenesAuditoriaService auditoriaService;

    private AdminLimpiezaImagenesService servicio;

    @BeforeEach
    void setUp() {
        servicio = new AdminLimpiezaImagenesService(
                configuracionSistemaService,
                limpiezaImagenesService,
                limpiezaImagenesScheduler,
                auditoriaService);
    }

    @Test
    void obtenerConfiguracion_mapeaIntervaloGraciaYFechas() {
        LocalDateTime proxima = LocalDateTime.of(2026, 11, 11, 12, 0);
        LocalDateTime ultima = LocalDateTime.of(2026, 11, 10, 12, 0);
        when(configuracionSistemaService.obtenerIntervaloLimpiezaImagenesHoras()).thenReturn(24);
        when(configuracionSistemaService.obtenerGraciaLimpiezaImagenesMinutos()).thenReturn(60);
        when(limpiezaImagenesScheduler.proximaEjecucion()).thenReturn(proxima);
        when(limpiezaImagenesScheduler.ultimaEjecucion()).thenReturn(ultima);

        ConfiguracionSchedulerLimpiezaImagenesResponse response = servicio.obtenerConfiguracion();

        assertEquals(24, response.intervaloHoras());
        assertEquals(60, response.graciaMinutos());
        assertEquals(proxima, response.proximaEjecucion());
        assertEquals(ultima, response.ultimaEjecucion());
    }

    @Test
    void actualizarConfiguracion_persisteElNuevoIntervalo() {
        when(configuracionSistemaService.obtenerGraciaLimpiezaImagenesMinutos()).thenReturn(60);
        when(limpiezaImagenesScheduler.proximaEjecucion())
                .thenReturn(LocalDateTime.of(2026, 11, 10, 18, 0));
        when(limpiezaImagenesScheduler.ultimaEjecucion()).thenReturn(null);

        ConfiguracionSchedulerLimpiezaImagenesResponse response =
                servicio.actualizarConfiguracion(new ConfigurarSchedulerLimpiezaImagenesRequest(6));

        assertEquals(6, response.intervaloHoras());
        assertEquals(60, response.graciaMinutos());
        verify(configuracionSistemaService).actualizarIntervaloLimpiezaImagenes(6);
    }

    @Test
    void ejecutarAhora_ejecutaLimpiezaMarcaFechaYAudita() {
        LimpiezaImagenesService.Resultado resultado =
                new LimpiezaImagenesService.Resultado(2, 1, 1, 0);
        when(limpiezaImagenesService.ejecutar()).thenReturn(resultado);

        EjecutarLimpiezaImagenesResponse response = servicio.ejecutarAhora();

        assertEquals(4, response.registrosAfectados());
        assertEquals(2, response.archivosHuerfanos());
        assertEquals(1, response.imagenesHuerfanas());
        assertEquals(1, response.imagenesPerfilesBaja());
        assertEquals(0, response.errores());
        assertTrue(response.mensaje().contains("2 archivo(s) huérfano(s)"));
        verify(limpiezaImagenesService).ejecutar();
        verify(limpiezaImagenesScheduler).marcarEjecucionManual();
        verify(auditoriaService).registrarEjecucion(resultado);
    }

    @Test
    void ejecutarAhora_sinElementos_devuelveMensajeInformativo() {
        LimpiezaImagenesService.Resultado resultado =
                new LimpiezaImagenesService.Resultado(0, 0, 0, 0);
        when(limpiezaImagenesService.ejecutar()).thenReturn(resultado);

        EjecutarLimpiezaImagenesResponse response = servicio.ejecutarAhora();

        assertEquals(0, response.registrosAfectados());
        assertTrue(response.mensaje().contains("No se encontraron"));
    }

    @Test
    void ejecutarAhora_conErrores_informaLaCantidad() {
        LimpiezaImagenesService.Resultado resultado =
                new LimpiezaImagenesService.Resultado(1, 0, 0, 2);
        when(limpiezaImagenesService.ejecutar()).thenReturn(resultado);

        EjecutarLimpiezaImagenesResponse response = servicio.ejecutarAhora();

        assertTrue(response.mensaje().contains("2 elemento(s) no se pudieron eliminar"));
    }
}
