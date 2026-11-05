package org.mgroko.backend.invitaciones.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.invitaciones.dto.InvitacionResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.invitaciones.servicio.CrearInvitacionService;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InvitacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvitacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrearInvitacionService crearInvitacionService;

    @Test
    void invitarGeneral_conPerfilActivo_devuelve201() throws Exception {
        when(crearInvitacionService.invitarAProyecto(any(), any(), any(), any()))
                .thenReturn(respuesta(TipoInvitacion.GENERAL, null));

        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20,\"mensaje\":\"Hola\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idInvitacion").value(77))
                .andExpect(jsonPath("$.tipo").value("GENERAL"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.idProyecto").value(5))
                .andExpect(jsonPath("$.idRequerimientoActividad").doesNotExist());
    }

    @Test
    void invitarActividad_conPerfilActivo_devuelve201() throws Exception {
        when(crearInvitacionService.invitarAProyectoActividad(any(), any(), any(), any()))
                .thenReturn(respuesta(TipoInvitacion.ACTIVIDAD, 30L));

        mockMvc.perform(post("/proyectos/5/invitaciones/actividad")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoActividad\":30,"
                                + "\"idPerfilDestinatario\":20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.idRequerimientoActividad").value(30));
    }

    @Test
    void invitarGeneral_sinPerfilDestinatario_devuelve400() throws Exception {
        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(crearInvitacionService, never())
                .invitarAProyecto(any(), any(), any(), any());
    }

    @Test
    void invitarGeneral_conMensajeDemasiadoLargo_devuelve400() throws Exception {
        String mensaje = "x".repeat(201);

        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20,\"mensaje\":\""
                                + mensaje + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitarActividad_sinRequerimiento_devuelve400() throws Exception {
        mockMvc.perform(post("/proyectos/5/invitaciones/actividad")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitarGeneral_sinPerfilActivo_noInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20}"))
                .andExpect(status().isNotFound());

        verify(crearInvitacionService, never())
                .invitarAProyecto(any(), any(), any(), any());
    }

    @Test
    void invitarGeneral_duplicada_devuelve409() throws Exception {
        when(crearInvitacionService.invitarAProyecto(any(), any(), any(), any()))
                .thenThrow(new InvitacionNoPermitidaException(
                        "El perfil ya tiene una invitación activa para este proyecto.",
                        CrearInvitacionService.CODIGO_INVITACION_DUPLICADA));

        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INVITACION_DUPLICADA"));
    }

    @Test
    void invitarGeneral_perfilInexistente_devuelve404() throws Exception {
        when(crearInvitacionService.invitarAProyecto(any(), any(), any(), any()))
                .thenThrow(new PerfilNoEncontradoException());

        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20}"))
                .andExpect(status().isNotFound());
    }

    private InvitacionResponse respuesta(TipoInvitacion tipo, Long idRequerimientoActividad) {
        return new InvitacionResponse(
                77L,
                tipo,
                EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 4, 12, 0),
                5L,
                null,
                idRequerimientoActividad,
                10L,
                20L,
                "Hola");
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario, Long idPerfil) {
        var token = new UsernamePasswordAuthenticationToken(
                String.valueOf(idUsuario), null, List.of());
        token.setDetails(new ContextoAutenticacion(idUsuario, idPerfil, "Perfil activo"));
        return token;
    }
}
