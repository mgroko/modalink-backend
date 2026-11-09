package org.mgroko.backend.proyectos.controlador;

import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.contratos.dto.HistorialContratoEventoResponse;
import org.mgroko.backend.contratos.servicio.ListarHistorialContratosProyectoService;
import org.mgroko.backend.proyectos.dto.ActividadHistorialResponse;
import org.mgroko.backend.proyectos.dto.HistorialProyectoEventoResponse;
import org.mgroko.backend.proyectos.servicio.ListarHistorialProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class, controllers = HistorialProyectoController.class)
@AutoConfigureMockMvc(addFilters = false)
class HistorialProyectoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarHistorialProyectoService listarHistorialProyectoService;

    @MockitoBean
    private ListarHistorialContratosProyectoService listarHistorialContratosService;

    @Test
    void listar_devuelvePaginaDeHistorial() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        PaginaResponse<HistorialProyectoEventoResponse> response =
                new PaginaResponse<>(
                        List.of(new HistorialProyectoEventoResponse(
                                55L,
                                "MIEMBRO_ELIMINADO",
                                LocalDateTime.of(2026, 11, 4, 18, 30),
                                123L,
                                "Luna",
                                "Luna",
                                "Pérez",
                                "Modelo",
                                "MIEMBRO",
                                10L,
                                456L,
                                "Dirección",
                                "Ana",
                                "Gómez",
                                11L,
                                "Falta de compromiso",
                                "ELIMINADO",
                                20L,
                                "Montaje",
                                List.of(new ActividadHistorialResponse(
                                        20L, "Montaje", "PENDIENTE",
                                        30L, "Modelo")))),
                        0, 20, 1, 1, true, true);

        when(listarHistorialProyectoService.listar(
                eq(1L), eq(10L), eq(100L), eq("MIEMBRO_ELIMINADO"),
                eq("Luna"), eq(0), eq(20)))
                .thenReturn(response);

        mockMvc.perform(get("/proyectos/100/historial")
                        .principal(auth)
                        .param("tipo", "MIEMBRO_ELIMINADO")
                        .param("nombre", "Luna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idEvento").value(55L))
                .andExpect(jsonPath("$.contenido[0].tipo")
                        .value("MIEMBRO_ELIMINADO"))
                .andExpect(jsonPath("$.contenido[0].nombreArtisticoAfectado")
                        .value("Luna"))
                .andExpect(jsonPath("$.contenido[0].idPerfilActor").value(456L))
                .andExpect(jsonPath("$.contenido[0].nombreActividad")
                        .value("Montaje"))
                .andExpect(jsonPath("$.contenido[0].actividades[0].idActividad")
                        .value(20L))
                .andExpect(jsonPath("$.contenido[0].actividades[0].nombreActividad")
                        .value("Montaje"))
                .andExpect(jsonPath("$.totalElementos").value(1));

        verify(listarHistorialProyectoService).listar(
                1L, 10L, 100L, "MIEMBRO_ELIMINADO", "Luna", 0, 20);
    }

    @Test
    void listarContratos_devuelveHistorialDeContratos() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        PaginaResponse<HistorialContratoEventoResponse> response =
                new PaginaResponse<>(
                        List.of(new HistorialContratoEventoResponse(
                                77L,
                                55L,
                                "Préstamo de indumentaria",
                                100L,
                                "Colección Coquetas",
                                3L,
                                "PRESTAMO",
                                "Préstamo de indumentaria",
                                "ELIMINADO",
                                LocalDateTime.of(2026, 11, 8, 12, 0),
                                11L,
                                7L,
                                "Luna",
                                1L,
                                "Ana Gómez",
                                1,
                                "Baja lógica",
                                "ELIMINADO",
                                "FINALIZADO",
                                LocalDateTime.of(2026, 11, 8, 12, 0))),
                        0, 20, 1, 1, true, true);

        when(listarHistorialContratosService.listar(
                1L, 10L, 100L, "ELIMINADO", "Préstamo", 0, 20))
                .thenReturn(response);

        mockMvc.perform(get("/proyectos/100/historial/contratos")
                        .principal(auth)
                        .param("tipo", "ELIMINADO")
                        .param("nombre", "Préstamo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idEvento").value(77L))
                .andExpect(jsonPath("$.contenido[0].idContrato").value(55L))
                .andExpect(jsonPath("$.contenido[0].tipoEvento")
                        .value("ELIMINADO"))
                .andExpect(jsonPath("$.contenido[0].estadoContrato")
                        .value("ELIMINADO"))
                .andExpect(jsonPath("$.contenido[0].estadoProyecto")
                        .value("FINALIZADO"))
                .andExpect(jsonPath("$.totalElementos").value(1));

        verify(listarHistorialContratosService).listar(
                1L, 10L, 100L, "ELIMINADO", "Préstamo", 0, 20);
    }
}
