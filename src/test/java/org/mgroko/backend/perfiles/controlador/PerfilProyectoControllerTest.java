package org.mgroko.backend.perfiles.controlador;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.dto.HistorialProyectoPerfilResponse;
import org.mgroko.backend.perfiles.servicio.ListarHistorialProyectosPerfilService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = PerfilProyectoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PerfilProyectoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarHistorialProyectosPerfilService listarHistorialProyectosPerfilService;

    @Test
    void historial_devuelveProyectosFinalizadosYCancelados() throws Exception {
        when(listarHistorialProyectosPerfilService.listar(7L, 1L, 3L, 0, 10))
                .thenReturn(new PaginaResponse<>(
                        List.of(
                                new HistorialProyectoPerfilResponse(
                                        25L,
                                        "Editorial Otoño",
                                        "Finalizado",
                                        "Modelo",
                                        "MODELO",
                                        LocalDateTime.of(2026, 3, 15, 18, 0),
                                        null,
                                        true),
                                new HistorialProyectoPerfilResponse(
                                        26L,
                                        "Campaña cancelada",
                                        "Cancelado",
                                        "Modelo",
                                        "MODELO",
                                        null,
                                        LocalDateTime.of(2026, 4, 1, 10, 0),
                                        false)),
                        0,
                        10,
                        2,
                        1,
                        true,
                        true));

        mockMvc.perform(get("/perfiles/7/proyectos/historial")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("Finalizado"))
                .andExpect(jsonPath("$.contenido[0].rol").value("Modelo"))
                .andExpect(jsonPath("$.contenido[1].estado").value("Cancelado"))
                .andExpect(jsonPath("$.contenido[1].fechaCancelacion")
                        .value("2026-04-01T10:00:00"));
    }

    private UsernamePasswordAuthenticationToken autenticacion() {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 3L, "Visitante"));
        return auth;
    }
}
