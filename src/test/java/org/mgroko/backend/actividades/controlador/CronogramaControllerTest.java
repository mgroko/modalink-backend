package org.mgroko.backend.actividades.controlador;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ActividadCronogramaResponse;
import org.mgroko.backend.actividades.dto.CronogramaResponse;
import org.mgroko.backend.actividades.dto.ResumenCronogramaResponse;
import org.mgroko.backend.actividades.servicio.ObtenerCronogramaService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CronogramaController.class)
@AutoConfigureMockMvc(addFilters = false)
class CronogramaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObtenerCronogramaService obtenerCronogramaService;

    @Test
    void obtener_conPerfilActivo_devuelveResumenYActividades() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        CronogramaResponse response = new CronogramaResponse(
                new ResumenCronogramaResponse(
                        LocalDateTime.of(2026, 10, 14, 0, 0),
                        LocalDateTime.of(2026, 12, 4, 0, 0),
                        LocalDate.of(2027, 3, 15),
                        73440L,
                        146879L,
                        false,
                        false,
                        "NORMAL",
                        true,
                        8,
                        5,
                        List.of(1L, 2L, 5L, 6L, 7L)),
                List.of(new ActividadCronogramaResponse(
                        1L,
                        "Bocetos",
                        "Bocetos",
                        "PENDIENTE",
                        11520,
                        List.of(),
                        LocalDateTime.of(2026, 10, 14, 0, 0),
                        LocalDateTime.of(2026, 10, 22, 0, 0),
                        LocalDateTime.of(2026, 10, 22, 0, 0),
                        true,
                        true,
                        0L,
                        0L,
                        null,
                        null,
                        false,
                        null,
                        false,
                        false)),
                List.of());

        when(obtenerCronogramaService.obtener(1L, 10L, 100L)).thenReturn(response);

        mockMvc.perform(get("/proyectos/100/cronograma").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.makespanMinutos").value(73440L))
                .andExpect(jsonPath("$.resumen.rutaCritica[0]").value(1L))
                .andExpect(jsonPath("$.actividades[0].idActividad").value(1L))
                .andExpect(jsonPath("$.actividades[0].enRutaCritica").value(true))
                .andExpect(jsonPath("$.actividades[0].finConHolgura")
                        .value("2026-10-22T00:00:00"));
    }

    @Test
    void obtener_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        mockMvc.perform(get("/proyectos/100/cronograma").principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No hay un perfil activo seleccionado en la sesión."));

        verify(obtenerCronogramaService, never()).obtener(any(), any(), any());
    }
}
