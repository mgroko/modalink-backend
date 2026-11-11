package org.mgroko.backend.admin.controlador;

import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.ActualizarEstadoReporteRequest;
import org.mgroko.backend.admin.dto.AdminReportePublicacionDetalleResponse;
import org.mgroko.backend.admin.dto.AdminReportePublicacionResumenResponse;
import org.mgroko.backend.admin.dto.BuscarReportesPublicacionFiltro;
import org.mgroko.backend.admin.servicio.AdminReportePublicacionService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;
import org.mgroko.backend.reportes.exception.EstadoReporteInvalidoException;
import org.mgroko.backend.reportes.exception.ReporteNoEncontradoException;
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
        controllers = AdminReportePublicacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReportePublicacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminReportePublicacionService adminReportePublicacionService;

    @Test
    void listar_devuelvePaginaYFiltros() throws Exception {
        when(adminReportePublicacionService.buscar(
                any(BuscarReportesPublicacionFiltro.class), eq(0), eq(20), eq(false)))
                .thenReturn(new PaginaResponse<>(
                        List.of(resumen()), 0, 20, 1L, 1, true, true));

        mockMvc.perform(get("/admin/reportes-publicacion")
                        .principal(autenticacion(99L))
                        .param("estado", "PENDIENTE")
                        .param("motivo", "CONTENIDO_INAPROPIADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].idReporte").value(1))
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.contenido[0].tituloPublicacion")
                        .value("Publicación de prueba"))
                .andExpect(jsonPath("$.contenido[0].publicacionEliminada").value(false));

        verify(adminReportePublicacionService).buscar(
                any(BuscarReportesPublicacionFiltro.class), eq(0), eq(20), eq(false));
    }

    @Test
    void detalle_devuelveDatosCompletos() throws Exception {
        when(adminReportePublicacionService.obtenerDetalle(1L)).thenReturn(detalle());

        mockMvc.perform(get("/admin/reportes-publicacion/1")
                        .principal(autenticacion(99L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReporte").value(1))
                .andExpect(jsonPath("$.entidadTipo").value("PUBLICACION"))
                .andExpect(jsonPath("$.publicacion.idPublicacion").value(30))
                .andExpect(jsonPath("$.publicacion.titulo").value("Publicación de prueba"))
                .andExpect(jsonPath("$.autor.idPerfil").value(10))
                .andExpect(jsonPath("$.autor.nombreArtistico").value("Luna Diseños"));
    }

    @Test
    void actualizarEstado_devuelveDetalleActualizado() throws Exception {
        when(adminReportePublicacionService.actualizarEstado(
                eq(1L), eq(99L), any(ActualizarEstadoReporteRequest.class)))
                .thenReturn(detalleResuelto());

        mockMvc.perform(patch("/admin/reportes-publicacion/1/estado")
                        .principal(autenticacion(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"RESUELTO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTO"))
                .andExpect(jsonPath("$.idAdminRevisor").value(99))
                .andExpect(jsonPath("$.nombreAdminRevisor").value("Admin Sistema"));
    }

    @Test
    void actualizarEstado_sinEstado_devuelve400() throws Exception {
        mockMvc.perform(patch("/admin/reportes-publicacion/1/estado")
                        .principal(autenticacion(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarEstado_yaGestionado_devuelve409() throws Exception {
        doThrow(new EstadoReporteInvalidoException("El reporte ya fue gestionado."))
                .when(adminReportePublicacionService)
                .actualizarEstado(eq(1L), eq(99L), any(ActualizarEstadoReporteRequest.class));

        mockMvc.perform(patch("/admin/reportes-publicacion/1/estado")
                        .principal(autenticacion(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"DESCARTADO"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void detalle_reporteInexistente_devuelve404() throws Exception {
        when(adminReportePublicacionService.obtenerDetalle(99L))
                .thenThrow(new ReporteNoEncontradoException());

        mockMvc.perform(get("/admin/reportes-publicacion/99")
                        .principal(autenticacion(99L)))
                .andExpect(status().isNotFound());
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario) {
        var auth = new UsernamePasswordAuthenticationToken(
                String.valueOf(idUsuario), null, List.of());
        auth.setDetails(new ContextoAutenticacion(idUsuario, null, null));
        return auth;
    }

    private AdminReportePublicacionResumenResponse resumen() {
        return new AdminReportePublicacionResumenResponse(
                1L,
                MotivoReportePublicacion.CONTENIDO_INAPROPIADO,
                "Contenido inapropiado",
                "Contenido ofensivo",
                EstadoReporte.PENDIENTE,
                LocalDateTime.of(2026, 11, 11, 10, 0),
                false,
                30L,
                "Publicación de prueba",
                EstadoPublicacion.ACTIVA,
                20L,
                "Perfil Dueño",
                1L,
                "Luna Pérez",
                10L,
                "Luna Diseños");
    }

    private AdminReportePublicacionDetalleResponse detalle() {
        return new AdminReportePublicacionDetalleResponse(
                1L,
                EntidadReporte.PUBLICACION,
                30L,
                MotivoReportePublicacion.CONTENIDO_INAPROPIADO,
                "Contenido inapropiado",
                MotivoReporte.CONTENIDO_OFENSIVO,
                "Contenido ofensivo",
                EstadoReporte.PENDIENTE,
                LocalDateTime.of(2026, 11, 11, 10, 0),
                false,
                new AdminReportePublicacionDetalleResponse.PublicacionResponse(
                        30L,
                        "Publicación de prueba",
                        "Contenido",
                        LocalDateTime.of(2026, 11, 10, 9, 0),
                        EstadoPublicacion.ACTIVA,
                        20L,
                        "Perfil Dueño",
                        2L,
                        "Ana Gómez"),
                new AdminReportePublicacionDetalleResponse.AutorResponse(
                        1L, "Luna", "Pérez", "luna@example.com",
                        10L, "Luna Diseños"),
                null,
                null,
                null);
    }

    private AdminReportePublicacionDetalleResponse detalleResuelto() {
        return new AdminReportePublicacionDetalleResponse(
                1L,
                EntidadReporte.PUBLICACION,
                30L,
                MotivoReportePublicacion.CONTENIDO_INAPROPIADO,
                "Contenido inapropiado",
                MotivoReporte.CONTENIDO_OFENSIVO,
                "Contenido ofensivo",
                EstadoReporte.RESUELTO,
                LocalDateTime.of(2026, 11, 11, 10, 0),
                false,
                new AdminReportePublicacionDetalleResponse.PublicacionResponse(
                        30L,
                        "Publicación de prueba",
                        "Contenido",
                        LocalDateTime.of(2026, 11, 10, 9, 0),
                        EstadoPublicacion.ACTIVA,
                        20L,
                        "Perfil Dueño",
                        2L,
                        "Ana Gómez"),
                new AdminReportePublicacionDetalleResponse.AutorResponse(
                        1L, "Luna", "Pérez", "luna@example.com",
                        10L, "Luna Diseños"),
                99L,
                "Admin Sistema",
                LocalDateTime.of(2026, 11, 11, 12, 0));
    }
}
