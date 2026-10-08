package org.mgroko.backend.admin.controlador;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.ConfiguracionSchedulerResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerRequest;
import org.mgroko.backend.admin.dto.EjecutarSchedulerResponse;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AdminConfiguracionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminConfiguracionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConfiguracionSistemaService configuracionSistemaService;

    private UsernamePasswordAuthenticationToken autenticacionAdmin() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    @Test
    void obtenerConfiguracion_retorna200YDatosScheduler() throws Exception {
        LocalDateTime proxima = LocalDateTime.of(2026, 9, 15, 3, 0);
        ConfiguracionSchedulerResponse response =
                new ConfiguracionSchedulerResponse(3, 0, "0 0 3 * * *", proxima);

        when(configuracionSistemaService.obtenerConfiguracionDeshabilitacion()).thenReturn(response);

        mockMvc.perform(get("/admin/configuracion/schedulers/deshabilitacion")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hora").value(3))
                .andExpect(jsonPath("$.minuto").value(0))
                .andExpect(jsonPath("$.cron").value("0 0 3 * * *"))
                .andExpect(jsonPath("$.proximaEjecucion").exists());

        verify(configuracionSistemaService).obtenerConfiguracionDeshabilitacion();
    }

    @Test
    void actualizarConfiguracion_requestValido_retorna200() throws Exception {
        ConfigurarSchedulerRequest request = new ConfigurarSchedulerRequest(4, 30);
        LocalDateTime proxima = LocalDateTime.of(2026, 9, 15, 4, 30);
        ConfiguracionSchedulerResponse response =
                new ConfiguracionSchedulerResponse(4, 30, "0 30 4 * * *", proxima);

        when(configuracionSistemaService.actualizarConfiguracionDeshabilitacion(request))
                .thenReturn(response);

        mockMvc.perform(post("/admin/configuracion/schedulers/deshabilitacion")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hora").value(4))
                .andExpect(jsonPath("$.minuto").value(30))
                .andExpect(jsonPath("$.cron").value("0 30 4 * * *"))
                .andExpect(jsonPath("$.proximaEjecucion").exists());

        verify(configuracionSistemaService).actualizarConfiguracionDeshabilitacion(request);
    }

    @Test
    void actualizarConfiguracion_horaInvalida_retorna400() throws Exception {
        ConfigurarSchedulerRequest request = new ConfigurarSchedulerRequest(25, 0);

        mockMvc.perform(post("/admin/configuracion/schedulers/deshabilitacion")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ejecutarAhora_retorna200YMensajeResultado() throws Exception {
        EjecutarSchedulerResponse response =
                new EjecutarSchedulerResponse(2, LocalDateTime.now(), "Proceso de reactivación ejecutado exitosamente.");

        when(configuracionSistemaService.ejecutarDeshabilitacionManual()).thenReturn(response);

        mockMvc.perform(post("/admin/configuracion/schedulers/deshabilitacion/ejecutar-ahora")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrosAfectados").value(2))
                .andExpect(jsonPath("$.mensaje").value("Proceso de reactivación ejecutado exitosamente."));

        verify(configuracionSistemaService).ejecutarDeshabilitacionManual();
    }
}
