package org.mgroko.backend.admin.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.ConfiguracionSchedulerResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerRequest;
import org.mgroko.backend.admin.dto.EjecutarSchedulerResponse;
import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.mgroko.backend.repositorio.ConfiguracionSistemaRepository;
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
}
