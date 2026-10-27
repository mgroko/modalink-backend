package org.mgroko.backend.postulaciones.controlador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.postulaciones.dto.PostulacionResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.exception.PostulacionNoPermitidaException;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.postulaciones.servicio.CrearPostulacionService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PostulacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostulacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CrearPostulacionService crearPostulacionService;

    @Test
    void postularAActividad_conPerfilActivo_devuelve201() throws Exception {
        var auth = autenticacion(1L, 10L);

        when(crearPostulacionService.darDeAltaPostulacionActividad(1L, 10L, 5L, 20L))
                .thenReturn(respuesta(TipoPostulacion.ACTIVIDAD, false));

        mockMvc.perform(post("/proyectos/5/postulaciones/actividad")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoActividad\":20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPostulacion").value(77))
                .andExpect(jsonPath("$.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.idProyecto").value(5))
                .andExpect(jsonPath("$.idRequerimiento").value(20))
                .andExpect(jsonPath("$.tentativa").value(false));
    }

    @Test
    void postularAGeneral_conPerfilActivo_devuelve201() throws Exception {
        var auth = autenticacion(1L, 10L);

        when(crearPostulacionService.darDeAltaPostulacionGeneral(1L, 10L, 5L, 30L))
                .thenReturn(respuesta(TipoPostulacion.GENERAL, false));

        mockMvc.perform(post("/proyectos/5/postulaciones/general")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoGral\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("GENERAL"))
                .andExpect(jsonPath("$.idRequerimiento").value(30));
    }

    @Test
    void postularAActividad_sinRequerimiento_devuelve400() throws Exception {
        mockMvc.perform(post("/proyectos/5/postulaciones/actividad")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(crearPostulacionService, never())
                .darDeAltaPostulacionActividad(any(), any(), any(), any());
    }

    @Test
    void postularAActividad_reglaDeNegocioIncumplida_devuelve409() throws Exception {
        when(crearPostulacionService.darDeAltaPostulacionActividad(1L, 10L, 5L, 20L))
                .thenThrow(new PostulacionNoPermitidaException("No tenés disponibilidad."));

        mockMvc.perform(post("/proyectos/5/postulaciones/actividad")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoActividad\":20}"))
                .andExpect(status().isConflict());
    }

    @Test
    void postularAGeneral_requerimientoInexistente_devuelve404() throws Exception {
        when(crearPostulacionService.darDeAltaPostulacionGeneral(1L, 10L, 5L, 30L))
                .thenThrow(new RequerimientoNoEncontradoException("No existe."));

        mockMvc.perform(post("/proyectos/5/postulaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoGral\":30}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postularAActividad_sinPerfilActivo_noInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(post("/proyectos/5/postulaciones/actividad")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoActividad\":20}"))
                .andExpect(status().isNotFound());

        verify(crearPostulacionService, never()).darDeAltaPostulacionActividad(any(), any(), any(), any());
    }

    @Test
    void postularAActividad_mapeaIdPerfilEIdProyectoDelPath() throws Exception {
        when(crearPostulacionService.darDeAltaPostulacionActividad(eq(1L), eq(10L), eq(9L), eq(20L)))
                .thenReturn(respuesta(TipoPostulacion.ACTIVIDAD, true));

        mockMvc.perform(post("/proyectos/9/postulaciones/actividad")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("idRequerimientoActividad", 20))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tentativa").value(true));

        verify(crearPostulacionService).darDeAltaPostulacionActividad(1L, 10L, 9L, 20L);
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario, Long idPerfil) {
        var auth = new UsernamePasswordAuthenticationToken(String.valueOf(idUsuario), null, List.of());
        auth.setDetails(new ContextoAutenticacion(idUsuario, idPerfil, "Luna Diseños"));
        return auth;
    }

    private PostulacionResponse respuesta(TipoPostulacion tipo, boolean tentativa) {
        Long idRequerimiento = tipo == TipoPostulacion.ACTIVIDAD ? 20L : 30L;
        return new PostulacionResponse(
                77L,
                tipo,
                EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 10, 25, 15, 0),
                5L,
                idRequerimiento,
                tentativa);
    }
}
