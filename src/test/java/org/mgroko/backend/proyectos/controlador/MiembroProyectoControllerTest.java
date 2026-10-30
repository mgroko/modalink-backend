package org.mgroko.backend.proyectos.controlador;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.proyectos.dto.MiembroProyectoResponse;
import org.mgroko.backend.proyectos.servicio.ListarMiembrosProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MiembroProyectoController.class)
@AutoConfigureMockMvc(addFilters = false)
class MiembroProyectoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarMiembrosProyectoService listarMiembrosProyectoService;

    @Test
    void listar_devuelveMiembrosActivos() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        PaginaResponse<MiembroProyectoResponse> response = new PaginaResponse<>(
                List.of(new MiembroProyectoResponse(
                        900L, 50L, 2L, "Luna", "Nombre", "Apellido",
                        2L, "Modelo", "MIEMBRO", "ACTIVO")),
                0, 20, 1, 1, true, true);

        when(listarMiembrosProyectoService.listar(
                eq(1L), eq(10L), eq(100L), eq("Luna"), eq("MIEMBRO"),
                eq(2L), eq(0), eq(20), eq(false)))
                .thenReturn(response);

        mockMvc.perform(get("/proyectos/100/miembros")
                        .principal(auth)
                        .param("nombre", "Luna")
                        .param("rol", "MIEMBRO")
                        .param("idProfesion", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idMiembro").value(900L))
                .andExpect(jsonPath("$.contenido[0].idPerfil").value(50L))
                .andExpect(jsonPath("$.contenido[0].rol").value("MIEMBRO"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }
}
