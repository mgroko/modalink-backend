package org.mgroko.backend.admin.controlador;

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
import org.mgroko.backend.admin.dto.AdminReportePerfilDetalleResponse;
import org.mgroko.backend.admin.dto.AdminReportePerfilResumenResponse;
import org.mgroko.backend.admin.dto.BuscarReportesPerfilFiltro;
import org.mgroko.backend.admin.servicio.AdminReportePerfilService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.MotivoReporte;
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

@WebMvcTest(controllers = AdminReportePerfilController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReportePerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminReportePerfilService adminReportePerfilService;

    @Test
    void listar_devuelvePaginaYFiltros() throws Exception {
        when(adminReportePerfilService.buscar(
                any(BuscarReportesPerfilFiltro.class), eq(0), eq(20), eq(false)))
                .thenReturn(new PaginaResponse<>(
                        List.of(resumen()), 0, 20, 1L, 1, true, true));

        mockMvc.perform(get("/admin/reportes-perfil")
                        .principal(autenticacion(99L))
                        .param("estado", "PENDIENTE")
                        .param("motivo", "SPAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].idReporte").value(1))
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.contenido[0].nombrePerfilReportado")
                        .value("Perfil Reportado"));

        verify(adminReportePerfilService).buscar(
                any(BuscarReportesPerfilFiltro.class), eq(0), eq(20), eq(false));
    }

    @Test
    void detalle_devuelveDatosCompletos() throws Exception {
        when(adminReportePerfilService.obtenerDetalle(1L)).thenReturn(detalle());

        mockMvc.perform(get("/admin/reportes-perfil/1")
                        .principal(autenticacion(99L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReporte").value(1))
                .andExpect(jsonPath("$.perfilReportado.idPerfil").value(20))
                .andExpect(jsonPath("$.usuarioReportado.correo").value("ana@example.com"))
                .andExpect(jsonPath("$.autor.idPerfil").value(10))
                .andExpect(jsonPath("$.autor.nombreArtistico").value("Luna Diseños"));
    }

    @Test
    void actualizarEstado_devuelveDetalleActualizado() throws Exception {
        when(adminReportePerfilService.actualizarEstado(
                eq(1L), eq(99L), any(ActualizarEstadoReporteRequest.class)))
                .thenReturn(detalleResuelto());

        mockMvc.perform(patch("/admin/reportes-perfil/1/estado")
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
        mockMvc.perform(patch("/admin/reportes-perfil/1/estado")
                        .principal(autenticacion(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarEstado_yaGestionado_devuelve409() throws Exception {
        doThrow(new EstadoReporteInvalidoException("El reporte ya fue gestionado."))
                .when(adminReportePerfilService)
                .actualizarEstado(eq(1L), eq(99L), any(ActualizarEstadoReporteRequest.class));

        mockMvc.perform(patch("/admin/reportes-perfil/1/estado")
                        .principal(autenticacion(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"DESCARTADO"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void detalle_reporteInexistente_devuelve404() throws Exception {
        when(adminReportePerfilService.obtenerDetalle(99L))
                .thenThrow(new ReporteNoEncontradoException());

        mockMvc.perform(get("/admin/reportes-perfil/99")
                        .principal(autenticacion(99L)))
                .andExpect(status().isNotFound());
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario) {
        var auth = new UsernamePasswordAuthenticationToken(String.valueOf(idUsuario), null, List.of());
        auth.setDetails(new ContextoAutenticacion(idUsuario, null, null));
        return auth;
    }

    private AdminReportePerfilResumenResponse resumen() {
        return new AdminReportePerfilResumenResponse(
                1L,
                MotivoReporte.SPAM,
                "Spam o publicidad engañosa",
                "Spam insistente",
                EstadoReporte.PENDIENTE,
                LocalDateTime.of(2026, 10, 29, 10, 0),
                20L,
                "Perfil Reportado",
                1L,
                "Luna Pérez",
                10L,
                "Luna Diseños");
    }

    private AdminReportePerfilDetalleResponse detalle() {
        return new AdminReportePerfilDetalleResponse(
                1L,
                EntidadReporte.PERFIL,
                20L,
                MotivoReporte.SPAM,
                "Spam o publicidad engañosa",
                "Spam insistente",
                EstadoReporte.PENDIENTE,
                LocalDateTime.of(2026, 10, 29, 10, 0),
                new AdminReportePerfilDetalleResponse.PerfilReportadoResponse(
                        20L, "Perfil Reportado", "Modelo", "ACTIVO"),
                new AdminReportePerfilDetalleResponse.UsuarioResponse(
                        2L, "Ana", "Gómez", "ana@example.com", "30000002", "Activo"),
                new AdminReportePerfilDetalleResponse.AutorResponse(
                        1L, "Luna", "Pérez", "luna@example.com", "30000001",
                        10L, "Luna Diseños"),
                null,
                null,
                null);
    }

    private AdminReportePerfilDetalleResponse detalleResuelto() {
        return new AdminReportePerfilDetalleResponse(
                1L,
                EntidadReporte.PERFIL,
                20L,
                MotivoReporte.SPAM,
                "Spam o publicidad engañosa",
                "Spam insistente",
                EstadoReporte.RESUELTO,
                LocalDateTime.of(2026, 10, 29, 10, 0),
                new AdminReportePerfilDetalleResponse.PerfilReportadoResponse(
                        20L, "Perfil Reportado", "Modelo", "ACTIVO"),
                new AdminReportePerfilDetalleResponse.UsuarioResponse(
                        2L, "Ana", "Gómez", "ana@example.com", "30000002", "Activo"),
                new AdminReportePerfilDetalleResponse.AutorResponse(
                        1L, "Luna", "Pérez", "luna@example.com", "30000001",
                        10L, "Luna Diseños"),
                99L,
                "Admin Sistema",
                LocalDateTime.of(2026, 10, 29, 12, 0));
    }
}
