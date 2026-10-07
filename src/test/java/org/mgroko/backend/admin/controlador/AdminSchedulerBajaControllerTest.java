package org.mgroko.backend.admin.controlador;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.ConfiguracionSchedulerBajaResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerBajaRequest;
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

@WebMvcTest(controllers = AdminSchedulerBajaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSchedulerBajaControllerTest {

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
        ConfiguracionSchedulerBajaResponse response =
                new ConfiguracionSchedulerBajaResponse(3, 0, "0 0 3 * * *", proxima, 30);

        when(configuracionSistemaService.obtenerConfiguracionBaja()).thenReturn(response);

        mockMvc.perform(get("/admin/configuracion/schedulers/baja")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hora").value(3))
                .andExpect(jsonPath("$.minuto").value(0))
                .andExpect(jsonPath("$.cron").value("0 0 3 * * *"))
                .andExpect(jsonPath("$.diasBaja").value(30))
                .andExpect(jsonPath("$.proximaEjecucion").exists());

        verify(configuracionSistemaService).obtenerConfiguracionBaja();
    }

    @Test
    void actualizarConfiguracion_requestValido_retorna200() throws Exception {
        ConfigurarSchedulerBajaRequest request = new ConfigurarSchedulerBajaRequest(4, 30, 45);
        LocalDateTime proxima = LocalDateTime.of(2026, 9, 15, 4, 30);
        ConfiguracionSchedulerBajaResponse response =
                new ConfiguracionSchedulerBajaResponse(4, 30, "0 30 4 * * *", proxima, 45);

        when(configuracionSistemaService.actualizarConfiguracionBaja(request)).thenReturn(response);

        mockMvc.perform(post("/admin/configuracion/schedulers/baja")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hora").value(4))
                .andExpect(jsonPath("$.minuto").value(30))
                .andExpect(jsonPath("$.cron").value("0 30 4 * * *"))
                .andExpect(jsonPath("$.diasBaja").value(45));

        verify(configuracionSistemaService).actualizarConfiguracionBaja(request);
    }

    @Test
    void actualizarConfiguracion_diasInvalidos_retorna400() throws Exception {
        ConfigurarSchedulerBajaRequest request = new ConfigurarSchedulerBajaRequest(4, 30, 0);

        mockMvc.perform(post("/admin/configuracion/schedulers/baja")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ejecutarAhora_retorna200YMensajeResultado() throws Exception {
        EjecutarSchedulerResponse response =
                new EjecutarSchedulerResponse(3, LocalDateTime.now(), "Se dieron de baja 1 cuenta(s) y 2 perfil(es).");

        when(configuracionSistemaService.ejecutarBajaManual()).thenReturn(response);

        mockMvc.perform(post("/admin/configuracion/schedulers/baja/ejecutar-ahora")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrosAfectados").value(3))
                .andExpect(jsonPath("$.mensaje").value("Se dieron de baja 1 cuenta(s) y 2 perfil(es)."));

        verify(configuracionSistemaService).ejecutarBajaManual();
    }
}
