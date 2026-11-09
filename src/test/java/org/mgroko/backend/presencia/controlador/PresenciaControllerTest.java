package org.mgroko.backend.presencia.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.presencia.config.PresenciaProperties;
import org.mgroko.backend.presencia.dto.PresenciaPerfilResponse;
import org.mgroko.backend.presencia.servicio.PresenciaEventBus;
import org.mgroko.backend.presencia.servicio.PresenciaService;
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
        controllers = PresenciaController.class)
@AutoConfigureMockMvc(addFilters = false)
class PresenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PresenciaService presenciaService;

    @MockitoBean
    private PresenciaEventBus presenciaEventBus;

    @MockitoBean
    private PresenciaProperties properties;

    @BeforeEach
    void setUp() {
        when(properties.getMaxIds()).thenReturn(200);
        when(properties.getTimeoutStream()).thenReturn(Duration.ofMinutes(30));
    }

    @Test
    void heartbeat_conPerfilActivo_devuelve204() throws Exception {
        doNothing().when(presenciaService)
                .registrarActividad(1L, 10L);

        mockMvc.perform(post("/presencia/heartbeat")
                        .principal(autenticacion(10L)))
                .andExpect(status().isNoContent());

        verify(presenciaService).registrarActividad(1L, 10L);
    }

    @Test
    void heartbeat_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(post("/presencia/heartbeat")
                        .principal(autenticacion(null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void snapshot_devuelveSoloLosIdsSolicitados() throws Exception {
        when(presenciaService.snapshot(any()))
                .thenReturn(List.of(
                        new PresenciaPerfilResponse(10L, true),
                        new PresenciaPerfilResponse(20L, false)));

        mockMvc.perform(get("/presencia")
                        .param("idsPerfil", "10,20")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].idPerfil").value(10))
                .andExpect(jsonPath("$[0].enLinea").value(true))
                .andExpect(jsonPath("$[1].enLinea").value(false));
    }

    @Test
    void snapshot_conDemasiadosIds_devuelve400() throws Exception {
        when(properties.getMaxIds()).thenReturn(1);

        mockMvc.perform(get("/presencia")
                        .param("idsPerfil", "10,20")
                        .principal(autenticacion(10L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("IDS_PERFIL_INVALIDOS"));
    }

    @Test
    void stream_respondeTextEventStreamYEnviaSnapshot() throws Exception {
        when(presenciaService.snapshot(any()))
                .thenReturn(List.of(new PresenciaPerfilResponse(10L, true)));

        mockMvc.perform(get("/presencia/stream")
                        .param("idsPerfil", "10")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.TEXT_EVENT_STREAM));

        verify(presenciaEventBus).registrarSuscriptor(
                eq(List.of(10L)), any());
    }

    @Test
    void stream_sinIds_devuelve400() throws Exception {
        mockMvc.perform(get("/presencia/stream")
                        .principal(autenticacion(10L)))
                .andExpect(status().isBadRequest());
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idPerfilActivo) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "1", null, List.of());
        authentication.setDetails(
                new ContextoAutenticacion(1L, idPerfilActivo, "Ana Moda"));
        return authentication;
    }
}
