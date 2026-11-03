package org.mgroko.backend.proyectos.controlador;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.proyectos.dto.DarseDeBajaProyectoRequest;
import org.mgroko.backend.proyectos.dto.EliminarIntegranteRequest;
import org.mgroko.backend.proyectos.dto.MiembroProyectoResponse;
import org.mgroko.backend.proyectos.exception.UltimoDirectorActivoException;
import org.mgroko.backend.proyectos.servicio.DarseDeBajaProyectoService;
import org.mgroko.backend.proyectos.servicio.EliminarIntegranteService;
import org.mgroko.backend.proyectos.servicio.ListarMiembrosProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
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

    @MockitoBean
    private EliminarIntegranteService eliminarIntegranteService;

    @MockitoBean
    private DarseDeBajaProyectoService darseDeBajaProyectoService;

    @Test
    void listar_devuelveMiembrosActivos() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        PaginaResponse<MiembroProyectoResponse> response = new PaginaResponse<>(
                List.of(new MiembroProyectoResponse(
                        900L, 50L, 2L, "Luna", "Nombre", "Apellido",
                        2L, "Modelo", "MIEMBRO", "ACTIVO", true, null)),
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
                .andExpect(jsonPath("$.contenido[0].eliminable").value(true))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void eliminar_devuelve204YDelegaEnElServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        mockMvc.perform(delete("/proyectos/100/miembros/900")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"Falta de compromiso"}
                                """))
                .andExpect(status().isNoContent());

        verify(eliminarIntegranteService).eliminar(
                eq(1L),
                eq(10L),
                eq(100L),
                eq(900L),
                eq(new EliminarIntegranteRequest("Falta de compromiso")));
    }

    @Test
    void eliminar_motivoVacio_devuelve400() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        mockMvc.perform(delete("/proyectos/100/miembros/900")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
    }

    @Test
    void darseDeBaja_devuelve204YDelegaEnElServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        mockMvc.perform(post("/proyectos/100/baja")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"Nuevo proyecto"}
                                """))
                .andExpect(status().isNoContent());

        verify(darseDeBajaProyectoService).darseDeBaja(
                eq(1L),
                eq(10L),
                eq(100L),
                eq(new DarseDeBajaProyectoRequest("Nuevo proyecto")));
    }

    @Test
    void darseDeBaja_motivoVacio_devuelve400() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        mockMvc.perform(post("/proyectos/100/baja")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
    }

    @Test
    void darseDeBaja_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        mockMvc.perform(post("/proyectos/100/baja")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"Nuevo proyecto"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "No hay un perfil activo seleccionado en la sesión."));
    }

    @Test
    void darseDeBaja_ultimoDirector_devuelve409() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        doThrow(new UltimoDirectorActivoException())
                .when(darseDeBajaProyectoService)
                .darseDeBaja(eq(1L), eq(10L), eq(100L),
                        eq(new DarseDeBajaProyectoRequest("Nuevo proyecto")));

        mockMvc.perform(post("/proyectos/100/baja")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"Nuevo proyecto"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "El proyecto debe conservar al menos un director activo."));
    }
}
