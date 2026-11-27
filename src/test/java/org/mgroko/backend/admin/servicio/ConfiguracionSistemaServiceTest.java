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
import org.mgroko.backend.admin.dto.EjecutarSchedulerBajaResponse;
import org.mgroko.backend.admin.dto.EjecutarSchedulerResponse;
import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.mgroko.backend.perfiles.servicio.ExpirarPerfilService;
import org.mgroko.backend.repositorio.ConfiguracionSistemaRepository;
import org.mgroko.backend.usuario.servicio.ExpirarCuentaService;
import org.mgroko.backend.usuario.servicio.ResumenBajaDefinitiva;
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

    @Mock
    private org.mgroko.backend.auditoria.servicio.AuditoriaEventoService auditoriaEventoService;

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
    @DisplayName("actualizarIntervaloLimpiezaImagenes - Persiste el nuevo intervalo")
    void actualizarIntervaloLimpiezaImagenes_guardaValor() {
        when(configuracionSistemaRepository.findByClave(any())).thenReturn(Optional.empty());

        configuracionSistemaService.actualizarIntervaloLimpiezaImagenes(6);

        ArgumentCaptor<ConfiguracionSistema> captor = ArgumentCaptor.forClass(ConfiguracionSistema.class);
        verify(configuracionSistemaRepository).save(captor.capture());
        assertEquals(ConfiguracionSistemaService.CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS,
                captor.getValue().getClave());
        assertEquals("6", captor.getValue().getValor());
    }

    @Test
    @DisplayName("actualizarIntervaloLimpiezaImagenes - Rechaza valores fuera de rango")
    void actualizarIntervaloLimpiezaImagenes_rechazaFueraDeRango() {
        assertThrows(IllegalArgumentException.class,
                () -> configuracionSistemaService.actualizarIntervaloLimpiezaImagenes(0));
        assertThrows(IllegalArgumentException.class,
                () -> configuracionSistemaService.actualizarIntervaloLimpiezaImagenes(169));
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
    @DisplayName("ejecutarBajaManual - Expira cuentas, perfiles y devuelve detalle")
    void ejecutarBajaManual_retornaResumen() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_DIAS_BAJA))
                .thenReturn(Optional.empty());
        when(expirarCuentaService.expirarVencidosConResumen(30))
                .thenReturn(new ResumenBajaDefinitiva(1, 2, 3, 4, 5, 6));
        when(expirarPerfilService.expirarVencidosConResumen(30))
                .thenReturn(new ResumenBajaDefinitiva(0, 1, 2, 1, 3, 2));

        EjecutarSchedulerBajaResponse response =
                configuracionSistemaService.ejecutarBajaManual();

        assertEquals(4, response.registrosAfectados());
        assertNotNull(response.ejecutadoEn());
        assertTrue(response.mensaje().contains("1 cuenta(s) y 3 perfil(es)"));
        assertTrue(response.mensaje().contains("Proyectos cancelados: 5"));
        assertTrue(response.mensaje().contains("Publicaciones eliminadas: 8"));
        assertTrue(response.mensaje().contains("Conexiones dadas de baja: 8"));

        assertEquals(1, response.detalle().cuentasExpiradas());
        assertEquals(3, response.detalle().perfilesExpirados());
        assertEquals(5, response.detalle().proyectosCancelados());
        assertEquals(5, response.detalle().membresiasBajaVoluntaria());
        assertEquals(8, response.detalle().publicacionesEliminadas());
        assertEquals(8, response.detalle().conexionesEliminadas());

        verify(expirarCuentaService).expirarVencidosConResumen(30);
        verify(expirarPerfilService).expirarVencidosConResumen(30);
    }

    @Test
    @DisplayName("ejecutarBajaManual - Sin vencidos retorna 0 y mensaje informativo")
    void ejecutarBajaManual_sinVencidos_retornaCero() {
        when(configuracionSistemaRepository.findByClave(ConfiguracionSistemaService.CLAVE_DIAS_BAJA))
                .thenReturn(Optional.empty());
        when(expirarCuentaService.expirarVencidosConResumen(30))
                .thenReturn(ResumenBajaDefinitiva.vacio());
        when(expirarPerfilService.expirarVencidosConResumen(30))
                .thenReturn(ResumenBajaDefinitiva.vacio());

        EjecutarSchedulerBajaResponse response =
                configuracionSistemaService.ejecutarBajaManual();

        assertEquals(0, response.registrosAfectados());
        assertTrue(response.mensaje().contains("No se encontraron"));
        assertEquals(0, response.detalle().proyectosCancelados());
        assertEquals(0, response.detalle().publicacionesEliminadas());
        assertEquals(0, response.detalle().conexionesEliminadas());
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

    // ---------------------------------------------------------------
    // Limites configurables de cantidad
    // ---------------------------------------------------------------

    @Test
    @DisplayName("obtenerMaxImagenesMoodboard - Retorna el valor configurado")
    void obtenerMaxImagenesMoodboard_valorConfigurado() {
        when(configuracionSistemaRepository.findByClave(
                ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_POR_MOODBOARD))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_POR_MOODBOARD)
                        .valor("12").build()));

        assertEquals(12, configuracionSistemaService.obtenerMaxImagenesMoodboard());
    }

    @Test
    @DisplayName("obtenerMaxImagenesMoodboard - Sin fila lanza IllegalStateException")
    void obtenerMaxImagenesMoodboard_sinFila_lanzaIllegalState() {
        when(configuracionSistemaRepository.findByClave(
                ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_POR_MOODBOARD))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> configuracionSistemaService.obtenerMaxImagenesMoodboard());
    }

    @Test
    @DisplayName("obtenerMaxImagenesMoodboard - Cero lanza IllegalStateException")
    void obtenerMaxImagenesMoodboard_cero_lanzaIllegalState() {
        when(configuracionSistemaRepository.findByClave(
                ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_POR_MOODBOARD))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_POR_MOODBOARD)
                        .valor("0").build()));

        assertThrows(IllegalStateException.class,
                () -> configuracionSistemaService.obtenerMaxImagenesMoodboard());
    }

    @Test
    @DisplayName("obtenerMaxImagenesPublicacion - Retorna el valor configurado")
    void obtenerMaxImagenesPublicacion_valorConfigurado() {
        when(configuracionSistemaRepository.findByClave(
                ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_POR_PUBLICACION))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_POR_PUBLICACION)
                        .valor("8").build()));

        assertEquals(8, configuracionSistemaService.obtenerMaxImagenesPublicacion());
    }

    @Test
    @DisplayName("obtenerMaxPolaroidsPorPerfil - Retorna el valor configurado")
    void obtenerMaxPolaroidsPorPerfil_valorConfigurado() {
        when(configuracionSistemaRepository.findByClave(
                ConfiguracionSistemaService.CLAVE_POLAROID_MAX_POR_PERFIL))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService.CLAVE_POLAROID_MAX_POR_PERFIL)
                        .valor("6").build()));

        assertEquals(6, configuracionSistemaService.obtenerMaxPolaroidsPorPerfil());
    }

    @Test
    @DisplayName("obtenerMaxPropuestasColaboracionPorImagen - Retorna el valor configurado")
    void obtenerMaxPropuestasColaboracionPorImagen_valorConfigurado() {
        when(configuracionSistemaRepository.findByClave(
                ConfiguracionSistemaService.CLAVE_COLABORACION_AUTOMATICA_MAX_PROPUESTAS_POR_IMAGEN))
                .thenReturn(Optional.of(ConfiguracionSistema.builder()
                        .clave(ConfiguracionSistemaService
                                .CLAVE_COLABORACION_AUTOMATICA_MAX_PROPUESTAS_POR_IMAGEN)
                        .valor("3").build()));

        assertEquals(3, configuracionSistemaService.obtenerMaxPropuestasColaboracionPorImagen());
    }
}
