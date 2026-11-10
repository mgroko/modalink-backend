package org.mgroko.backend.admin.controlador;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.ConfiguracionSchedulerLimpiezaImagenesResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerLimpiezaImagenesRequest;
import org.mgroko.backend.admin.dto.EjecutarLimpiezaImagenesResponse;
import org.mgroko.backend.admin.servicio.AdminLimpiezaImagenesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = AdminSchedulerLimpiezaImagenesController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSchedulerLimpiezaImagenesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminLimpiezaImagenesService adminLimpiezaImagenesService;

    private UsernamePasswordAuthenticationToken autenticacionAdmin() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    @Test
    void obtenerConfiguracion_retorna200YDatosDelScheduler() throws Exception {
        ConfiguracionSchedulerLimpiezaImagenesResponse response =
                new ConfiguracionSchedulerLimpiezaImagenesResponse(
                        24,
                        60,
                        LocalDateTime.of(2026, 11, 11, 12, 0),
                        LocalDateTime.of(2026, 11, 10, 12, 0));

        when(adminLimpiezaImagenesService.obtenerConfiguracion()).thenReturn(response);

        mockMvc.perform(get("/admin/configuracion/schedulers/limpieza-imagenes")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervaloHoras").value(24))
                .andExpect(jsonPath("$.graciaMinutos").value(60))
                .andExpect(jsonPath("$.proximaEjecucion").exists())
                .andExpect(jsonPath("$.ultimaEjecucion").exists());

        verify(adminLimpiezaImagenesService).obtenerConfiguracion();
    }

    @Test
    void actualizarConfiguracion_requestValido_retorna200() throws Exception {
        ConfigurarSchedulerLimpiezaImagenesRequest request =
                new ConfigurarSchedulerLimpiezaImagenesRequest(6);
        ConfiguracionSchedulerLimpiezaImagenesResponse response =
                new ConfiguracionSchedulerLimpiezaImagenesResponse(
                        6,
                        60,
                        LocalDateTime.of(2026, 11, 10, 18, 0),
                        null);

        when(adminLimpiezaImagenesService.actualizarConfiguracion(request)).thenReturn(response);

        mockMvc.perform(post("/admin/configuracion/schedulers/limpieza-imagenes")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervaloHoras").value(6))
                .andExpect(jsonPath("$.graciaMinutos").value(60));

        verify(adminLimpiezaImagenesService).actualizarConfiguracion(request);
    }

    @Test
    void actualizarConfiguracion_intervaloInvalido_retorna400() throws Exception {
        ConfigurarSchedulerLimpiezaImagenesRequest request =
                new ConfigurarSchedulerLimpiezaImagenesRequest(0);

        mockMvc.perform(post("/admin/configuracion/schedulers/limpieza-imagenes")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ejecutarAhora_retorna200ConResultado() throws Exception {
        EjecutarLimpiezaImagenesResponse response = new EjecutarLimpiezaImagenesResponse(
                4,
                2,
                1,
                1,
                0,
                LocalDateTime.of(2026, 11, 10, 13, 0),
                "Se eliminaron 2 archivo(s) huérfano(s), 1 imagen(es) sin referencia y 1 imagen(es) de perfil en baja.");

        when(adminLimpiezaImagenesService.ejecutarAhora()).thenReturn(response);

        mockMvc.perform(post("/admin/configuracion/schedulers/limpieza-imagenes/ejecutar-ahora")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrosAfectados").value(4))
                .andExpect(jsonPath("$.archivosHuerfanos").value(2))
                .andExpect(jsonPath("$.errores").value(0));

        verify(adminLimpiezaImagenesService).ejecutarAhora();
    }
}
