package org.mgroko.backend.publicaciones.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.EnviarSolicitudColaboracionRequest;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionDuplicadaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionInvalidaException;
import org.mgroko.backend.publicaciones.servicio.EnviarSolicitudColaboracionService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.mockito.ArgumentCaptor;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = SolicitudColaboracionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SolicitudColaboracionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnviarSolicitudColaboracionService enviarSolicitudColaboracionService;

    private static UsernamePasswordAuthenticationToken auth(Long idUsuario, Long idPerfilActivo) {
        var token = new UsernamePasswordAuthenticationToken(String.valueOf(idUsuario), null, List.of());
        token.setDetails(new ContextoAutenticacion(idUsuario, idPerfilActivo, "Luna Diseños"));
        return token;
    }

    private static SolicitudColaboracionResponse response() {
        return new SolicitudColaboracionResponse(
                50L,
                EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 10, 20, 30),
                99L,
                "Editorial Otoño",
                new AutorPublicacionResponse(7L, "Luna Diseños", "Diseñadora", "/uploads/luna.webp"),
                new AutorPublicacionResponse(8L, "Sol", "Fotógrafa", "/uploads/sol.webp"));
    }

    @Test
    void enviar_devuelve201ConLaSolicitudCreada() throws Exception {
        when(enviarSolicitudColaboracionService.enviar(eq(1L), eq(7L), eq(99L), any()))
                .thenReturn(response());

        mockMvc.perform(post("/publicaciones/{idPublicacion}/solicitudes-colaboracion", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":8}")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idSolicitudColaboracion").value(50L))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.idPublicacion").value(99L))
                .andExpect(jsonPath("$.remitente.idPerfil").value(7L))
                .andExpect(jsonPath("$.destinatario.idPerfil").value(8L));

        ArgumentCaptor<EnviarSolicitudColaboracionRequest> captor =
                ArgumentCaptor.forClass(EnviarSolicitudColaboracionRequest.class);
        verify(enviarSolicitudColaboracionService).enviar(
                eq(1L), eq(7L), eq(99L), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                8L, captor.getValue().idPerfilDestinatario());
    }

    @Test
    void enviar_sinDestinatario_devuelve400() throws Exception {
        mockMvc.perform(post("/publicaciones/{idPublicacion}/solicitudes-colaboracion", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enviar_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(post("/publicaciones/{idPublicacion}/solicitudes-colaboracion", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":8}")
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void enviar_destinatarioFueraDeRed_devuelve409ConCodigo() throws Exception {
        when(enviarSolicitudColaboracionService.enviar(eq(1L), eq(7L), eq(99L), any()))
                .thenThrow(new SolicitudColaboracionInvalidaException(
                        "El perfil destinatario no pertenece a tu red de contactos.",
                        "DESTINATARIO_FUERA_DE_RED"));

        mockMvc.perform(post("/publicaciones/{idPublicacion}/solicitudes-colaboracion", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":8}")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DESTINATARIO_FUERA_DE_RED"));
    }

    @Test
    void enviar_solicitudDuplicada_devuelve409ConCodigo() throws Exception {
        when(enviarSolicitudColaboracionService.enviar(eq(1L), eq(7L), eq(99L), any()))
                .thenThrow(new SolicitudColaboracionDuplicadaException(
                        "Ya existe una solicitud de colaboración pendiente para ese perfil."));

        mockMvc.perform(post("/publicaciones/{idPublicacion}/solicitudes-colaboracion", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":8}")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_COLABORACION_PENDIENTE"));
    }
}
