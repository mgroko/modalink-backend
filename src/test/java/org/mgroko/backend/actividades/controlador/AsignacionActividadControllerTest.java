package org.mgroko.backend.actividades.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.AsignacionActividadResponse;
import org.mgroko.backend.actividades.dto.AsignarActividadRequest;
import org.mgroko.backend.actividades.exception.AsignacionNoDisponibleException;
import org.mgroko.backend.actividades.servicio.AsignarActividadService;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.dto.ResultadoPreferencia;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AsignacionActividadController.class)
@AutoConfigureMockMvc(addFilters = false)
class AsignacionActividadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsignarActividadService asignarActividadService;

    @Test
    void asignar_conDatosValidos_devuelve201() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        AsignacionActividadResponse response = new AsignacionActividadResponse(
                1200L,
                100L,
                77L,
                900L,
                50L,
                2L,
                123L,
                true,
                false,
                LocalDateTime.of(2026, 11, 2, 15, 0),
                LocalDateTime.of(2026, 11, 2, 16, 0),
                ResultadoPreferencia.EXACTO,
                0L);

        when(asignarActividadService.asignar(
                eq(1L), eq(10L), eq(100L), eq(77L), any(AsignarActividadRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/proyectos/100/actividades/77/asignaciones")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idPerfil": 50
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idAsignacionActividad").value(1200L))
                .andExpect(jsonPath("$.idRequerimientoActividad").value(123L))
                .andExpect(jsonPath("$.requerimientoSatisfecho").value(true))
                .andExpect(jsonPath("$.provisional").value(false));
    }

    @Test
    void asignar_sinIdPerfil_devuelve400() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        mockMvc.perform(post("/proyectos/100/actividades/77/asignaciones")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.idPerfil").exists());
    }

    @Test
    void asignar_sinDisponibilidad_devuelve409ConDiagnostico() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        ResultadoDisponibilidad resultado = new ResultadoDisponibilidad(
                false,
                true,
                LocalDateTime.of(2026, 11, 2, 9, 0),
                LocalDateTime.of(2026, 11, 2, 18, 0),
                null,
                null,
                "SIN_HUECO",
                List.of(),
                ResultadoPreferencia.SIN_PREFERENCIA,
                null,
                null,
                List.of());

        when(asignarActividadService.asignar(
                eq(1L), eq(10L), eq(100L), eq(77L), any(AsignarActividadRequest.class)))
                .thenThrow(new AsignacionNoDisponibleException(resultado));

        mockMvc.perform(post("/proyectos/100/actividades/77/asignaciones")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idPerfil": 50
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("SIN_HUECO"))
                .andExpect(jsonPath("$.provisional").value(true))
                .andExpect(jsonPath("$.httpStatus").value(409));
    }
}
