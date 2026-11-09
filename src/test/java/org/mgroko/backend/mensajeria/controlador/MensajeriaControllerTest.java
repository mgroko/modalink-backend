package org.mgroko.backend.mensajeria.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.mensajeria.config.MensajeriaProperties;
import org.mgroko.backend.mensajeria.dto.ConversacionResponse;
import org.mgroko.backend.mensajeria.dto.MensajeResponse;
import org.mgroko.backend.mensajeria.dto.PerfilMensajeResponse;
import org.mgroko.backend.mensajeria.dto.ResumenConversacionesResponse;
import org.mgroko.backend.mensajeria.servicio.MensajeEventBus;
import org.mgroko.backend.mensajeria.servicio.MensajeriaService;
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
        controllers = MensajeriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class MensajeriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MensajeriaService mensajeriaService;

    @MockitoBean
    private MensajeEventBus mensajeEventBus;

    @MockitoBean
    private MensajeriaProperties properties;

    @BeforeEach
    void setUp() {
        when(properties.getTimeoutStream()).thenReturn(Duration.ofMinutes(30));
    }

    @Test
    void listarConversaciones_devuelve200ConPagina() throws Exception {
        when(mensajeriaService.listarConversaciones(1L, 10L, 0, 20))
                .thenReturn(new PaginaResponse<>(
                        List.of(conversacion()), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/conversaciones")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idConversacion").value(30))
                .andExpect(jsonPath("$.contenido[0].tieneNoLeidos").value(true))
                .andExpect(jsonPath("$.contenido[0].puedeEnviar").value(true));
    }

    @Test
    void resumen_devuelveConversacionesNoLeidas() throws Exception {
        when(mensajeriaService.resumen(1L, 10L))
                .thenReturn(new ResumenConversacionesResponse(4, 2));

        mockMvc.perform(get("/conversaciones/resumen")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConversaciones").value(4))
                .andExpect(jsonPath("$.conversacionesNoLeidas").value(2));
    }

    @Test
    void crearConversacion_devuelve201() throws Exception {
        when(mensajeriaService.crearConversacion(1L, 10L, 20L))
                .thenReturn(new MensajeriaService.ResultadoConversacion(
                        conversacion(), true));

        mockMvc.perform(post("/conversaciones")
                        .principal(autenticacion(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idConversacion").value(30));
    }

    @Test
    void enviarMensaje_valido_devuelve201() throws Exception {
        when(mensajeriaService.enviarMensaje(1L, 10L, 30L, "Hola"))
                .thenReturn(mensaje());

        mockMvc.perform(post("/conversaciones/30/mensajes")
                        .principal(autenticacion(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"Hola\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenido").value("Hola"))
                .andExpect(jsonPath("$.esMio").value(true));
    }

    @Test
    void enviarMensaje_vacio_devuelve400() throws Exception {
        mockMvc.perform(post("/conversaciones/30/mensajes")
                        .principal(autenticacion(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void marcarLeido_devuelve204() throws Exception {
        doNothing().when(mensajeriaService)
                .marcarLeido(1L, 10L, 30L, 50L);

        mockMvc.perform(patch("/conversaciones/30/leido")
                        .principal(autenticacion(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hastaIdMensaje\":50}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void stream_respondeTextEventStreamYRegistraSuscriptor() throws Exception {
        mockMvc.perform(get("/mensajes/stream")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.TEXT_EVENT_STREAM));

        verify(mensajeEventBus).registrarSuscriptor(eq(10L), any());
    }

    @Test
    void sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(get("/conversaciones")
                        .principal(autenticacion(null)))
                .andExpect(status().isNotFound());
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idPerfilActivo) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "1", null, List.of());
        authentication.setDetails(
                new ContextoAutenticacion(1L, idPerfilActivo, "Ana Moda"));
        return authentication;
    }

    private ConversacionResponse conversacion() {
        return new ConversacionResponse(
                30L,
                new PerfilMensajeResponse(
                        20L, "Bruno Estilo", "Bruno", "Gómez",
                        2L, "Fotógrafo", null),
                LocalDateTime.of(2026, 11, 1, 10, 0),
                LocalDateTime.of(2026, 11, 9, 15, 0),
                "Hola",
                true,
                true);
    }

    private MensajeResponse mensaje() {
        return new MensajeResponse(
                50L,
                30L,
                "Hola",
                LocalDateTime.of(2026, 11, 9, 15, 0),
                10L,
                new PerfilMensajeResponse(
                        10L, "Ana Moda", "Ana", "Pérez",
                        1L, "Modelo", null),
                true,
                false,
                null);
    }
}
