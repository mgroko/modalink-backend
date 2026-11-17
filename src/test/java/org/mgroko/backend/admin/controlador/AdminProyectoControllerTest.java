package org.mgroko.backend.admin.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminProyectoDetalleResponse;
import org.mgroko.backend.admin.dto.AdminProyectoList;
import org.mgroko.backend.admin.servicio.AdminProyectoService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = AdminProyectoController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminProyectoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminProyectoService adminProyectoService;

    @Test
    void listar_incluyeTodosLosEstados() throws Exception {
        when(adminProyectoService.listarProyectos(any(), anyInt(), anyInt()))
                .thenReturn(new PaginaResponse<>(
                        List.of(new AdminProyectoList(
                                10L, "Proyecto Luna", LocalDateTime.now(),
                                EstadoProyecto.BORRADOR, "PUBLICO")),
                        0, 20, 1, 1, true, true));

        mockMvc.perform(get("/admin/proyectos")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("BORRADOR"))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Proyecto Luna"));
    }

    @Test
    void detalle_devuelveDatosCompletos() throws Exception {
        when(adminProyectoService.obtenerDetalle(10L)).thenReturn(detalle());

        mockMvc.perform(get("/admin/proyectos/10")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProyecto").value(10))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.miembros[0].nombreCompleto").value("Luna Pérez"))
                .andExpect(jsonPath("$.totalContratos").value(1));
    }

    @Test
    void detalle_proyectoInexistente_devuelve404() throws Exception {
        when(adminProyectoService.obtenerDetalle(999L))
                .thenThrow(new ProyectoNoEncontradoException(999L));

        mockMvc.perform(get("/admin/proyectos/999")
                        .principal(autenticacion()))
                .andExpect(status().isNotFound());
    }

    private UsernamePasswordAuthenticationToken autenticacion() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private AdminProyectoDetalleResponse detalle() {
        return new AdminProyectoDetalleResponse(
                10L,
                "Proyecto Luna",
                "Descripción",
                "BORRADOR",
                "PUBLICO",
                true,
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                false,
                null,
                new AdminProyectoDetalleResponse.DirectorResponse(7L, "Luna", "Luna Pérez"),
                List.of(),
                List.of(),
                null,
                List.of(new AdminProyectoDetalleResponse.MiembroResponse(
                        1L, 7L, "Luna", "Luna Pérez", "Fotógrafa",
                        "Director de proyecto", "ACTIVO", LocalDateTime.now(), null, null)),
                List.of(new AdminProyectoDetalleResponse.ContratoResumen(
                        1L, "Contrato", "BORRADOR", 1,
                        LocalDateTime.now(), null, 0, 1)),
                1);
    }
}
