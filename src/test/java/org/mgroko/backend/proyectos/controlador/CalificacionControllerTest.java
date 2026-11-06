package org.mgroko.backend.proyectos.controlador;

import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.proyectos.dto.CalificacionRequest;
import org.mgroko.backend.proyectos.dto.CalificacionResponse;
import org.mgroko.backend.proyectos.dto.CalificacionesProyectoResponse;
import org.mgroko.backend.proyectos.dto.CalificarEquipoRequest;
import org.mgroko.backend.proyectos.dto.MiembroCalificableResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.CalificacionIncompletaException;
import org.mgroko.backend.proyectos.exception.CalificacionInvalidaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoCalificableException;
import org.mgroko.backend.proyectos.servicio.CalificarEquipoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class, controllers = CalificacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CalificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CalificarEquipoService calificarEquipoService;

    @Test
    void obtener_conPerfilActivo_devuelve200() throws Exception {
        var auth = autenticacion();
        when(calificarEquipoService.obtener(1L, 10L, 100L))
                .thenReturn(respuesta(false, 0));

        mockMvc.perform(get("/proyectos/100/calificaciones").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProyecto").value(100L))
                .andExpect(jsonPath("$.calificacionesPendientes").value(1))
                .andExpect(jsonPath("$.miembrosACalificar[0].idPerfil").value(20L));
    }

    @Test
    void calificar_conCalificacionesCompletas_devuelve200() throws Exception {
        var auth = autenticacion();
        CalificarEquipoRequest request = new CalificarEquipoRequest(
                List.of(new CalificacionRequest(20L, 5, "Excelente desempeño")));

        when(calificarEquipoService.calificar(
                eq(1L), eq(10L), eq(100L), any(CalificarEquipoRequest.class)))
                .thenReturn(respuesta(true, 1));

        mockMvc.perform(post("/proyectos/100/calificaciones")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calificacionesCompletas").value(true))
                .andExpect(jsonPath("$.calificacionesRealizadas").value(1))
                .andExpect(jsonPath("$.calificacionesPendientes").value(0));
    }

    @Test
    void calificar_conCalificacionesIncompletas_devuelve409() throws Exception {
        var auth = autenticacion();
        CalificarEquipoRequest request = new CalificarEquipoRequest(
                List.of(new CalificacionRequest(20L, 5, "Excelente desempeño")));

        when(calificarEquipoService.calificar(
                eq(1L), eq(10L), eq(100L), any(CalificarEquipoRequest.class)))
                .thenThrow(new CalificacionIncompletaException(
                        List.of(30L),
                        "Es obligatorio calificar a todos los integrantes activos pendientes."));

        mockMvc.perform(post("/proyectos/100/calificaciones")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CALIFICACIONES_INCOMPLETAS"))
                .andExpect(jsonPath("$.faltantes[0]").value(30L));
    }

    @Test
    void calificar_conProyectoNoFinalizado_devuelve409() throws Exception {
        var auth = autenticacion();
        CalificarEquipoRequest request = new CalificarEquipoRequest(
                List.of(new CalificacionRequest(20L, 5, "Excelente desempeño")));

        when(calificarEquipoService.calificar(
                eq(1L), eq(10L), eq(100L), any(CalificarEquipoRequest.class)))
                .thenThrow(new ProyectoNoCalificableException("Confirmado"));

        mockMvc.perform(post("/proyectos/100/calificaciones")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PROYECTO_NO_CALIFICABLE"));
    }

    @Test
    void calificar_conCalificacionInvalida_devuelve400() throws Exception {
        var auth = autenticacion();
        CalificarEquipoRequest request = new CalificarEquipoRequest(
                List.of(new CalificacionRequest(20L, 5, "Excelente desempeño")));

        when(calificarEquipoService.calificar(
                eq(1L), eq(10L), eq(100L), any(CalificarEquipoRequest.class)))
                .thenThrow(new CalificacionInvalidaException(
                        "No podés calificarte a vos mismo."));

        mockMvc.perform(post("/proyectos/100/calificaciones")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CALIFICACION_INVALIDA"));
    }

    @Test
    void calificar_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        CalificarEquipoRequest request = new CalificarEquipoRequest(
                List.of(new CalificacionRequest(20L, 5, "Excelente desempeño")));

        mockMvc.perform(post("/proyectos/100/calificaciones")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void calificar_actorNoMiembro_devuelve403() throws Exception {
        var auth = autenticacion();
        CalificarEquipoRequest request = new CalificarEquipoRequest(
                List.of(new CalificacionRequest(20L, 5, "Excelente desempeño")));

        when(calificarEquipoService.calificar(
                eq(1L), eq(10L), eq(100L), any(CalificarEquipoRequest.class)))
                .thenThrow(new AccesoDenegadoProyectoException(
                        "Debés ser miembro activo del proyecto para calificar al equipo."));

        mockMvc.perform(post("/proyectos/100/calificaciones")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken autenticacion() {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));
        return auth;
    }

    private CalificacionesProyectoResponse respuesta(
            boolean completas,
            int realizadas) {

        return new CalificacionesProyectoResponse(
                100L,
                completas,
                1,
                realizadas,
                completas ? 0 : 1,
                List.of(new MiembroCalificableResponse(
                        20L, "Miembro", "Nombre", "Apellido", "MIEMBRO", completas)),
                completas
                        ? List.of(new CalificacionResponse(
                                1L, 10L, 20L, 5, "Excelente desempeño",
                                LocalDateTime.of(2026, 11, 5, 15, 0)))
                        : List.of());
    }
}
