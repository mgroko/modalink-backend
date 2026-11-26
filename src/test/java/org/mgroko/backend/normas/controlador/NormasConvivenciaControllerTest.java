package org.mgroko.backend.normas.controlador;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.normas.dto.NormasConvivenciaResponse;
import org.mgroko.backend.normas.dto.NormasConvivenciaSeccion;
import org.mgroko.backend.normas.dto.NormasConvivenciaVersionResumen;
import org.mgroko.backend.normas.servicio.NormasConvivenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = NormasConvivenciaController.class)
@AutoConfigureMockMvc(addFilters = false)
class NormasConvivenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NormasConvivenciaService normasConvivenciaService;

    @Test
    void vigente_devuelveLaVersionPublica() throws Exception {
        when(normasConvivenciaService.vigente()).thenReturn(new NormasConvivenciaResponse(
                "Normas de convivencia",
                "1.0",
                "2026-11-26",
                null,
                "Regla de convivencia.",
                List.of(new NormasConvivenciaSeccion(
                        "que-son",
                        "Qué son las normas de convivencia",
                        List.of("Texto."),
                        List.of())),
                List.of(new NormasConvivenciaVersionResumen(
                        "1.0", "2026-11-26", null, "Versión inicial."))));

        mockMvc.perform(get("/normas-convivencia/vigente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.vigenteDesde").value("2026-11-26"))
                .andExpect(jsonPath("$.secciones[0].id").value("que-son"))
                .andExpect(jsonPath("$.historial[0].version").value("1.0"));
    }

    @Test
    void versiones_devuelveElHistorial() throws Exception {
        when(normasConvivenciaService.versiones()).thenReturn(List.of(
                new NormasConvivenciaVersionResumen(
                        "1.0", "2026-11-26", null, "Versión inicial.")));

        mockMvc.perform(get("/normas-convivencia/versiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value("1.0"));
    }
}
