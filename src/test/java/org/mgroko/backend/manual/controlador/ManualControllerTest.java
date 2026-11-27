package org.mgroko.backend.manual.controlador;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.manual.dto.ManualCapituloResumen;
import org.mgroko.backend.manual.dto.ManualError;
import org.mgroko.backend.manual.dto.ManualIndiceResponse;
import org.mgroko.backend.manual.servicio.ManualService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = ManualController.class)
@AutoConfigureMockMvc(addFilters = false)
class ManualControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManualService manualService;

    @Test
    void indice_devuelveLosCapitulosDelRol() throws Exception {
        when(manualService.indice(false)).thenReturn(new ManualIndiceResponse(
                "1.0",
                List.of(new ManualCapituloResumen(
                        "01-introduccion", 1, null, "Introducción"))));

        mockMvc.perform(get("/ayuda/indice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.capitulos[0].id").value("01-introduccion"))
                .andExpect(jsonPath("$.capitulos[0].titulo").value("Introducción"));
    }

    @Test
    void buscar_devuelveResultados() throws Exception {
        when(manualService.buscar("normas", false)).thenReturn(List.of());

        mockMvc.perform(get("/ayuda/buscar").param("q", "normas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void errores_devuelveElCatalogo() throws Exception {
        when(manualService.errores(false)).thenReturn(List.of(new ManualError(
                "1",
                "01-introduccion",
                "Introducción",
                "seccion-1-1",
                "1.1. Qué es ModaLink",
                "Mensaje de prueba",
                "Causa",
                "Solución")));

        mockMvc.perform(get("/ayuda/errores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mensaje").value("Mensaje de prueba"))
                .andExpect(jsonPath("$[0].capituloId").value("01-introduccion"));
    }
}
