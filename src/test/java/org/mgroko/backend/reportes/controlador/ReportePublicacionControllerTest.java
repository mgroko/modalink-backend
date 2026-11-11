package org.mgroko.backend.reportes.controlador;

import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;

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
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.reportes.dto.CrearReportePublicacionRequest;
import org.mgroko.backend.reportes.exception.AutoreportePublicacionException;
import org.mgroko.backend.reportes.exception.DescripcionReporteObligatoriaException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.reportes.exception.ReportePendienteExistenteException;
import org.mgroko.backend.reportes.servicio.ReportarPublicacionService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = ReportePublicacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportePublicacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportarPublicacionService reportarPublicacionService;

    @Test
    void reportar_conPerfilActivo_devuelve204() throws Exception {
        mockMvc.perform(post("/publicaciones/30/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"CONTENIDO_INAPROPIADO","descripcion":"Contenido"}
                                """))
                .andExpect(status().isNoContent());

        verify(reportarPublicacionService).reportar(
                eq(1L),
                eq(10L),
                eq(30L),
                any(CrearReportePublicacionRequest.class));
    }

    @Test
    void reportar_sinPerfilActivo_devuelve404YNoInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(post("/publicaciones/30/reportes")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"SPAM"}
                                """))
                .andExpect(status().isNotFound());

        verify(reportarPublicacionService, never())
                .reportar(any(), any(), any(), any());
    }

    @Test
    void reportar_sinMotivo_devuelve400() throws Exception {
        mockMvc.perform(post("/publicaciones/30/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(reportarPublicacionService, never())
                .reportar(any(), any(), any(), any());
    }

    @Test
    void reportar_motivoInvalido_devuelve400() throws Exception {
        mockMvc.perform(post("/publicaciones/30/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"PERFIL_FALSO"}
                                """))
                .andExpect(status().isBadRequest());

        verify(reportarPublicacionService, never())
                .reportar(any(), any(), any(), any());
    }

    @Test
    void reportar_descripcionMayorA200_devuelve400() throws Exception {
        mockMvc.perform(post("/publicaciones/30/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"SPAM","descripcion":"%s"}
                                """.formatted("x".repeat(201))))
                .andExpect(status().isBadRequest());

        verify(reportarPublicacionService, never())
                .reportar(any(), any(), any(), any());
    }

    @Test
    void reportar_otroSinDescripcion_devuelve400() throws Exception {
        doThrow(new DescripcionReporteObligatoriaException())
                .when(reportarPublicacionService)
                .reportar(eq(1L), eq(10L), eq(30L), any(CrearReportePublicacionRequest.class));

        mockMvc.perform(post("/publicaciones/30/reportes")
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
        doThrow(new AutoreportePublicacionException())
                .when(reportarPublicacionService)
                .reportar(eq(1L), eq(10L), eq(30L), any(CrearReportePublicacionRequest.class));

        mockMvc.perform(post("/publicaciones/30/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"SPAM"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void reportar_duplicadoPendiente_devuelve409() throws Exception {
        doThrow(new ReportePendienteExistenteException())
                .when(reportarPublicacionService)
                .reportar(eq(1L), eq(10L), eq(30L), any(CrearReportePublicacionRequest.class));

        mockMvc.perform(post("/publicaciones/30/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"SPAM"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void reportar_publicacionInexistente_devuelve404() throws Exception {
        doThrow(new PublicacionNoEncontradaException(30L))
                .when(reportarPublicacionService)
                .reportar(eq(1L), eq(10L), eq(30L), any(CrearReportePublicacionRequest.class));

        mockMvc.perform(post("/publicaciones/30/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"SPAM"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void reportar_admin_devuelve403() throws Exception {
        doThrow(new ReporteNoPermitidoException(
                "El administrador no puede reportar publicaciones."))
                .when(reportarPublicacionService)
                .reportar(eq(1L), eq(10L), eq(30L), any(CrearReportePublicacionRequest.class));

        mockMvc.perform(post("/publicaciones/30/reportes")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"SPAM"}
                                """))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario, Long idPerfil) {
        var auth = new UsernamePasswordAuthenticationToken(
                String.valueOf(idUsuario), null, List.of());
        auth.setDetails(new ContextoAutenticacion(idUsuario, idPerfil, "Luna Diseños"));
        return auth;
    }
}
