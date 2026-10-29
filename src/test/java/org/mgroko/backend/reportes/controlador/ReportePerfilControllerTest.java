package org.mgroko.backend.reportes.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.reportes.dto.CrearReportePerfilRequest;
import org.mgroko.backend.reportes.exception.AutoreporteException;
import org.mgroko.backend.reportes.exception.DescripcionReporteObligatoriaException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.reportes.servicio.ReportarPerfilService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReportePerfilController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportePerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportarPerfilService reportarPerfilService;

    @Test
    void reportar_conPerfilActivo_devuelve204() throws Exception {
        mockMvc.perform(post("/perfiles/20/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"SPAM","descripcion":"Spam insistente"}
                                """))
                .andExpect(status().isNoContent());

        verify(reportarPerfilService).reportar(
                eq(1L),
                eq(10L),
                eq(20L),
                any(CrearReportePerfilRequest.class));
    }

    @Test
    void reportar_sinPerfilActivo_devuelve404YNoInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(post("/perfiles/20/reportes")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"SPAM"}
                                """))
                .andExpect(status().isNotFound());

        verify(reportarPerfilService, never())
                .reportar(any(), any(), any(), any());
    }

    @Test
    void reportar_sinMotivo_devuelve400() throws Exception {
        mockMvc.perform(post("/perfiles/20/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(reportarPerfilService, never())
                .reportar(any(), any(), any(), any());
    }

    @Test
    void reportar_motivoInvalido_devuelve400() throws Exception {
        mockMvc.perform(post("/perfiles/20/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"NO_EXISTE"}
                                """))
                .andExpect(status().isBadRequest());

        verify(reportarPerfilService, never())
                .reportar(any(), any(), any(), any());
    }

    @Test
    void reportar_descripcionMayorA200_devuelve400() throws Exception {
        mockMvc.perform(post("/perfiles/20/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"SPAM","descripcion":"%s"}
                                """.formatted("x".repeat(201))))
                .andExpect(status().isBadRequest());

        verify(reportarPerfilService, never())
                .reportar(any(), any(), any(), any());
    }

    @Test
    void reportar_otroSinDescripcion_devuelve400() throws Exception {
        doThrow(new DescripcionReporteObligatoriaException())
                .when(reportarPerfilService)
                .reportar(eq(1L), eq(10L), eq(20L), any(CrearReportePerfilRequest.class));

        mockMvc.perform(post("/perfiles/20/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"OTRO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "La descripción es obligatoria cuando el motivo es OTRO."));
    }

    @Test
    void reportar_autorreporte_devuelve409() throws Exception {
        doThrow(new AutoreporteException())
                .when(reportarPerfilService)
                .reportar(eq(1L), eq(10L), eq(20L), any(CrearReportePerfilRequest.class));

        mockMvc.perform(post("/perfiles/20/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"SPAM"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void reportar_admin_devuelve403() throws Exception {
        doThrow(new ReporteNoPermitidoException("El administrador no puede reportar perfiles."))
                .when(reportarPerfilService)
                .reportar(eq(1L), eq(10L), eq(20L), any(CrearReportePerfilRequest.class));

        mockMvc.perform(post("/perfiles/20/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"SPAM"}
                                """))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario, Long idPerfil) {
        var auth = new UsernamePasswordAuthenticationToken(String.valueOf(idUsuario), null, List.of());
        auth.setDetails(new ContextoAutenticacion(idUsuario, idPerfil, "Luna Diseños"));
        return auth;
    }
}
