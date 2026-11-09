package org.mgroko.backend.mensajeria.controlador;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.mensajeria.dto.ChatGrupalResponse;
import org.mgroko.backend.mensajeria.dto.MensajeGrupalResponse;
import org.mgroko.backend.mensajeria.dto.PerfilMensajeResponse;
import org.mgroko.backend.mensajeria.servicio.ChatGrupalService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = ChatGrupalController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatGrupalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatGrupalService chatGrupalService;

    @Test
    void obtenerChat_devuelve200() throws Exception {
        when(chatGrupalService.obtenerChat(1L, 10L, 50L))
                .thenReturn(new ChatGrupalResponse(
                        50L, "Campaña Primavera", "PUBLICADO", true, List.of()));

        mockMvc.perform(get("/proyectos/50/chat")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProyecto").value(50))
                .andExpect(jsonPath("$.puedeEnviar").value(true));
    }

    @Test
    void listarMensajes_devuelve200ConPagina() throws Exception {
        when(chatGrupalService.listarMensajes(1L, 10L, 50L, 0, 50))
                .thenReturn(new PaginaResponse<>(
                        List.of(mensaje()), 0, 50, 1, 1, true, true));

        mockMvc.perform(get("/proyectos/50/chat/mensajes")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idMensajeProyecto").value(99))
                .andExpect(jsonPath("$.contenido[0].esMio").value(true));
    }

    @Test
    void enviarMensaje_valido_devuelve201() throws Exception {
        when(chatGrupalService.enviarMensaje(1L, 10L, 50L, "Hola"))
                .thenReturn(mensaje());

        mockMvc.perform(post("/proyectos/50/chat/mensajes")
                        .principal(autenticacion(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"Hola\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenido").value("Hola"));
    }

    @Test
    void enviarMensaje_vacio_devuelve400() throws Exception {
        mockMvc.perform(post("/proyectos/50/chat/mensajes")
                        .principal(autenticacion(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void marcarLeido_devuelve204() throws Exception {
        doNothing().when(chatGrupalService).marcarLeido(1L, 10L, 50L);

        mockMvc.perform(patch("/proyectos/50/chat/leido")
                        .principal(autenticacion(10L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void obtenerChat_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(get("/proyectos/50/chat")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "1", null, List.of())))
                .andExpect(status().isNotFound());
    }

    private MensajeGrupalResponse mensaje() {
        return new MensajeGrupalResponse(
                99L,
                50L,
                "Hola",
                LocalDateTime.of(2026, 11, 9, 15, 0),
                10L,
                new PerfilMensajeResponse(
                        10L, "Ana Moda", "Ana", "Pérez", 2L, "Modelo", null),
                true);
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idPerfilActivo) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "1", null, List.of());
        authentication.setDetails(
                new ContextoAutenticacion(1L, idPerfilActivo, "Ana Moda"));
        return authentication;
    }
}
