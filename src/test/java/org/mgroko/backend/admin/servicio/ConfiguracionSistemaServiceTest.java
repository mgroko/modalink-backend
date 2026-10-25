package org.mgroko.backend.admin.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.ConfiguracionSchedulerBajaResponse;
import org.mgroko.backend.admin.dto.ConfiguracionSchedulerResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerBajaRequest;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerRequest;
import org.mgroko.backend.admin.dto.EjecutarSchedulerResponse;
import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.mgroko.backend.perfiles.servicio.ExpirarPerfilService;
import org.mgroko.backend.repositorio.ConfiguracionSistemaRepository;
import org.mgroko.backend.usuario.servicio.ExpirarCuentaService;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfiguracionSistemaServiceTest {

    @Mock
    private ConfiguracionSistemaRepository configuracionSistemaRepository;

    @Mock
    private ExpirarDeshabilitacionService expirarDeshabilitacionService;

    @Mock
    private ExpirarCuentaService expirarCuentaService;

    @Mock
    private ExpirarPerfilService expirarPerfilService;

    @InjectMocks
    private ConfiguracionSistemaService configuracionSistemaService;

    @Test
    @DisplayName("obtenerConfiguracionDeshabilitacion - Retorna valores por defecto cuando no hay registros en BD")
    void obtenerConfiguracionDeshabilitacion_valoresPorDefecto() {
        when(configuracionSistemaRepository.findByClave(any())).thenReturn(Optional.empty());

        ConfiguracionSchedulerResponse response = configuracionSistemaService.obtenerConfiguracionDeshabilitacion();

        assertEquals(2, response.hora());
        assertEquals(0, response.minuto());
        assertEquals("0 0 2 * * *", response.cron());
        assertNotNull(response.proximaEjecucion());
    }

    @Test
    @DisplayName("actualizarConfiguracionDeshabilitacion - Genera cron correcto y persiste valores")
    void actualizarConfiguracionDeshabilitacion_guardaValores() {
        ConfigurarSchedulerRequest request = new ConfigurarSchedulerRequest(4, 30);
        when(configuracionSistemaRepository.findByClave(any())).thenReturn(Optional.empty());

        ConfiguracionSchedulerResponse response = configuracionSistemaService.actualizarConfiguracionDeshabilitacion(request);

        assertEquals(4, response.hora());
        assertEquals(30, response.minuto());
        assertEquals("0 30 4 * * *", response.cron());
        assertNotNull(response.proximaEjecucion());

        ArgumentCaptor<ConfiguracionSistema> captor = ArgumentCaptor.forClass(ConfiguracionSistema.class);
        verify(configuracionSistemaRepository, org.mockito.Mockito.atLeast(3)).save(captor.capture());

        boolean contieneCron = captor.getAllValues().stream()
                .anyMatch(c -> ConfiguracionSistemaService.CLAVE_DESHABILITACION_CRON.equals(c.getClave()) && "0 30 4 * * *".equals(c.getValor()));
        assertTrue(contieneCron);
    }

    @Test
    @DisplayName("ejecutarDeshabilitacionManual - Invoca expirarVencidos y retorna resumen")
    void ejecutarDeshabilitacionManual_retornaResumen() {
        when(expirarDeshabilitacionService.reactivarVencidos()).thenReturn(5);

        EjecutarSchedulerResponse response = configuracionSistemaService.ejecutarDeshabilitacionManual();

        assertEquals(5, response.registrosAfectados());
        assertNotNull(response.ejecutadoEn());
        assertTrue(response.mensaje().contains("5 usuario(s)"));
        verify(expirarDeshabilitacionService).reactivarVencidos();
    }

    @Test
    @DisplayName("obtenerMargenActividadDefecto - Retorna el valor configurado")
    void obtenerMargenActividadDefecto_valorConfigurado() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_MARGEN_ACTIVIDAD_MIN))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_MARGEN_ACTIVIDAD_MIN)
                        .valor("45").build()));

        assertEquals(45, configuracionSistemaService.obtenerMargenActividadDefecto());
    }

    @Test
    @DisplayName("obtenerMargenActividadDefecto - Retorna 30 si no hay fila configurada")
    void obtenerMargenActividadDefecto_sinFila_retorna30() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_MARGEN_ACTIVIDAD_MIN))
                .thenReturn(Optional.empty());

        assertEquals(30, configuracionSistemaService.obtenerMargenActividadDefecto());
    }

    @Test
    @DisplayName("obtenerMargenActividadDefecto - Retorna 30 si el valor no es numerico")
    void obtenerMargenActividadDefecto_valorInvalido_retorna30() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_MARGEN_ACTIVIDAD_MIN))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_MARGEN_ACTIVIDAD_MIN)
                        .valor("no-numerico").build()));

        assertEquals(30, configuracionSistemaService.obtenerMargenActividadDefecto());
    }

    @Test
    @DisplayName("obtenerMargenActividadDefecto - Retorna 30 si el valor es negativo")
    void obtenerMargenActividadDefecto_valorNegativo_retorna30() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_MARGEN_ACTIVIDAD_MIN))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_MARGEN_ACTIVIDAD_MIN)
                        .valor("-5").build()));

        assertEquals(30, configuracionSistemaService.obtenerMargenActividadDefecto());
    }

    // ---------------------------------------------------------------
    // Scheduler de bajas
    // ---------------------------------------------------------------

    @Test
    @DisplayName("obtenerDiasBaja - Retorna 30 por defecto y el valor configurado cuando existe")
    void obtenerDiasBaja_valoresPorDefectoYConfigurado() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_DIAS_BAJA))
                .thenReturn(Optional.empty());
        assertEquals(30, configuracionSistemaService.obtenerDiasBaja());

        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_DIAS_BAJA))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_DIAS_BAJA)
                        .valor("45").build()));
        assertEquals(45, configuracionSistemaService.obtenerDiasBaja());
    }

    @Test
    @DisplayName("obtenerConfiguracionBaja - Retorna valores por defecto cuando no hay registros en BD")
    void obtenerConfiguracionBaja_valoresPorDefecto() {
        when(configuracionSistemaRepository.findByClave(any())).thenReturn(Optional.empty());

        ConfiguracionSchedulerBajaResponse response = configuracionSistemaService.obtenerConfiguracionBaja();

        assertEquals(2, response.hora());
        assertEquals(0, response.minuto());
        assertEquals("0 0 2 * * *", response.cron());
        assertEquals(30, response.diasBaja());
        assertNotNull(response.proximaEjecucion());
    }

    @Test
    @DisplayName("actualizarConfiguracionBaja - Genera cron correcto y persiste horario y días")
    void actualizarConfiguracionBaja_guardaValores() {
        ConfigurarSchedulerBajaRequest request = new ConfigurarSchedulerBajaRequest(4, 30, 45);
        when(configuracionSistemaRepository.findByClave(any())).thenReturn(Optional.empty());

        ConfiguracionSchedulerBajaResponse response =
                configuracionSistemaService.actualizarConfiguracionBaja(request);

        assertEquals(4, response.hora());
        assertEquals(30, response.minuto());
        assertEquals("0 30 4 * * *", response.cron());
        assertEquals(45, response.diasBaja());
        assertNotNull(response.proximaEjecucion());

        ArgumentCaptor<ConfiguracionSistema> captor = ArgumentCaptor.forClass(ConfiguracionSistema.class);
        verify(configuracionSistemaRepository, org.mockito.Mockito.atLeast(4)).save(captor.capture());

        boolean contieneCron = captor.getAllValues().stream()
                .anyMatch(c -> ConfiguracionSistemaService.CLAVE_BAJA_CRON.equals(c.getClave())
                        && "0 30 4 * * *".equals(c.getValor()));
        boolean contieneDias = captor.getAllValues().stream()
                .anyMatch(c -> ConfiguracionSistemaService.CLAVE_DIAS_BAJA.equals(c.getClave())
                        && "45".equals(c.getValor()));
        assertTrue(contieneCron);
        assertTrue(contieneDias);
    }

    @Test
    @DisplayName("ejecutarBajaManual - Expira cuentas y perfiles con el plazo configurado")
    void ejecutarBajaManual_retornaResumen() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_DIAS_BAJA))
                .thenReturn(Optional.empty());
        when(expirarCuentaService.expirarVencidos(30)).thenReturn(1);
        when(expirarPerfilService.expirarVencidos(30)).thenReturn(2);

        EjecutarSchedulerResponse response = configuracionSistemaService.ejecutarBajaManual();

        assertEquals(3, response.registrosAfectados());
        assertNotNull(response.ejecutadoEn());
        assertTrue(response.mensaje().contains("1 cuenta(s) y 2 perfil(es)"));
        verify(expirarCuentaService).expirarVencidos(30);
        verify(expirarPerfilService).expirarVencidos(30);
    }

    @Test
    @DisplayName("ejecutarBajaManual - Sin vencidos retorna 0 y mensaje informativo")
    void ejecutarBajaManual_sinVencidos_retornaCero() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_DIAS_BAJA))
                .thenReturn(Optional.empty());
        when(expirarCuentaService.expirarVencidos(30)).thenReturn(0);
        when(expirarPerfilService.expirarVencidos(30)).thenReturn(0);

        EjecutarSchedulerResponse response = configuracionSistemaService.ejecutarBajaManual();

        assertEquals(0, response.registrosAfectados());
        assertTrue(response.mensaje().contains("No se encontraron"));
    }

    // ---------------------------------------------------------------
    // Limpieza de imagenes (Fase 5)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("obtenerIntervaloLimpiezaImagenesHoras - Retorna el valor configurado")
    void obtenerIntervaloLimpiezaImagenesHoras_valorConfigurado() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS)
                        .valor("6").build()));

        assertEquals(6, configuracionSistemaService.obtenerIntervaloLimpiezaImagenesHoras());
    }

    @Test
    @DisplayName("obtenerIntervaloLimpiezaImagenesHoras - Sin fila configurada lanza IllegalStateException")
    void obtenerIntervaloLimpiezaImagenesHoras_sinFila_lanzaIllegalState() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> configuracionSistemaService.obtenerIntervaloLimpiezaImagenesHoras());
    }

    @Test
    @DisplayName("obtenerIntervaloLimpiezaImagenesHoras - Valor no numerico lanza IllegalStateException")
    void obtenerIntervaloLimpiezaImagenesHoras_valorNoNumerico_lanzaIllegalState() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS)
                        .valor("cada-cuanto").build()));

        assertThrows(IllegalStateException.class,
                () -> configuracionSistemaService.obtenerIntervaloLimpiezaImagenesHoras());
    }

    @Test
    @DisplayName("obtenerIntervaloLimpiezaImagenesHoras - Valor en cero lanza IllegalStateException")
    void obtenerIntervaloLimpiezaImagenesHoras_valorEnCero_lanzaIllegalState() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS)
                        .valor("0").build()));

        assertThrows(IllegalStateException.class,
                () -> configuracionSistemaService.obtenerIntervaloLimpiezaImagenesHoras());
    }

    @Test
    @DisplayName("obtenerGraciaLimpiezaImagenesMinutos - Retorna el valor configurado")
    void obtenerGraciaLimpiezaImagenesMinutos_valorConfigurado() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_GRACIA_MINUTOS))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_GRACIA_MINUTOS)
                        .valor("90").build()));

        assertEquals(90, configuracionSistemaService.obtenerGraciaLimpiezaImagenesMinutos());
    }

    @Test
    @DisplayName("obtenerGraciaLimpiezaImagenesMinutos - Sin fila configurada lanza IllegalStateException")
    void obtenerGraciaLimpiezaImagenesMinutos_sinFila_lanzaIllegalState() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_GRACIA_MINUTOS))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> configuracionSistemaService.obtenerGraciaLimpiezaImagenesMinutos());
    }

    @Test
    @DisplayName("obtenerGraciaLimpiezaImagenesMinutos - Valor negativo lanza IllegalStateException")
    void obtenerGraciaLimpiezaImagenesMinutos_valorNegativo_lanzaIllegalState() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_GRACIA_MINUTOS))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_GRACIA_MINUTOS)
                        .valor("-1").build()));

        assertThrows(IllegalStateException.class,
                () -> configuracionSistemaService.obtenerGraciaLimpiezaImagenesMinutos());
    }
}
