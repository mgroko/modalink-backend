package org.mgroko.backend.admin.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminPerfilDetalleResponse;
import org.mgroko.backend.admin.dto.AdminPerfilResumenResponse;
import org.mgroko.backend.admin.dto.BuscarPerfilesAdminFiltro;
import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.admin.servicio.AdminPerfilService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesVersionResponse;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesVersionResumenResponse;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = AdminPerfilController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPerfilService adminPerfilService;

    @Test
    void listar_devuelvePaginaDePerfiles() throws Exception {
        when(adminPerfilService.buscar(any(BuscarPerfilesAdminFiltro.class), eq(0), eq(20), eq(false)))
                .thenReturn(new PaginaResponse<>(List.of(resumen()), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/admin/perfiles")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].idPerfil").value(7))
                .andExpect(jsonPath("$.contenido[0].correo").value("luna@test.com"));
    }

    @Test
    void listar_estadoSeEnlazaAlFiltro() throws Exception {
        when(adminPerfilService.buscar(any(BuscarPerfilesAdminFiltro.class), eq(0), eq(20), eq(false)))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/admin/perfiles")
                        .param("estado", "PENDIENTE_BAJA")
                        .principal(autenticacion()))
                .andExpect(status().isOk());

        ArgumentCaptor<BuscarPerfilesAdminFiltro> captor =
                ArgumentCaptor.forClass(BuscarPerfilesAdminFiltro.class);
        verify(adminPerfilService).buscar(captor.capture(), eq(0), eq(20), eq(false));
        assertEquals(EstadoPerfil.PENDIENTE_BAJA, captor.getValue().estado());
    }

    @Test
    void detalle_devuelveDatosCompletos() throws Exception {
        when(adminPerfilService.obtenerDetalle(7L)).thenReturn(detalle());

        mockMvc.perform(get("/admin/perfiles/7")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPerfil").value(7))
                .andExpect(jsonPath("$.usuario.correo").value("luna@test.com"))
                .andExpect(jsonPath("$.resumenRelacionados.totalProyectos").value(2))
                .andExpect(jsonPath("$.proyectos[0].nombre").value("Proyecto Luna"));
    }

    @Test
    void detalle_perfilInexistente_devuelve404() throws Exception {
        when(adminPerfilService.obtenerDetalle(999L))
                .thenThrow(new PerfilNoEncontradoException("Perfil no encontrado."));

        mockMvc.perform(get("/admin/perfiles/999")
                        .principal(autenticacion()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Perfil no encontrado."));
    }

    @Test
    void historialTerminos_devuelveVersiones() throws Exception {
        when(adminPerfilService.obtenerHistorialTerminos(7L))
                .thenReturn(List.of(
                        new TerminosCondicionesVersionResumenResponse(
                                12L,
                                1,
                                LocalDateTime.of(2026, 11, 1, 10, 0),
                                null,
                                true)));

        mockMvc.perform(get("/admin/perfiles/7/terminos-condiciones/historial")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idTyc").value(12L))
                .andExpect(jsonPath("$[0].numeroVersion").value(1))
                .andExpect(jsonPath("$[0].vigente").value(true));
    }

    @Test
    void versionTerminos_devuelveTexto() throws Exception {
        when(adminPerfilService.obtenerVersionTerminos(7L, 12L))
                .thenReturn(new TerminosCondicionesVersionResponse(
                        7L,
                        12L,
                        1,
                        "Texto administrativo",
                        LocalDateTime.of(2026, 11, 1, 10, 0),
                        null,
                        true));

        mockMvc.perform(get("/admin/perfiles/7/terminos-condiciones/historial/12")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTyc").value(12L))
                .andExpect(jsonPath("$.numeroVersion").value(1))
                .andExpect(jsonPath("$.descripcion").value("Texto administrativo"));
    }

    private UsernamePasswordAuthenticationToken autenticacion() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private AdminPerfilResumenResponse resumen() {
        return new AdminPerfilResumenResponse(
                7L, "Luna", "ACTIVO", "Fotógrafa", 3L, "Luna", "Pérez",
                "luna@test.com", null, "/uploads/luna.jpg");
    }

    private AdminPerfilDetalleResponse detalle() {
        return new AdminPerfilDetalleResponse(
                7L,
                "Luna",
                "Biografía",
                "ACTIVO",
                null,
                null,
                1L,
                "Fotógrafa",
                null,
                null,
                new AdminPerfilDetalleResponse.UsuarioAdmin(
                        3L, "Luna", "Pérez", "luna@test.com", "12345678",
                        null, "MUJER", "Activo", "Usuario", null, null, null),
                null,
                List.of("Retrato"),
                List.of(),
                null,
                new AdminPerfilDetalleResponse.RelacionadosResumen(2, 1, 3),
                List.of(new AdminPerfilDetalleResponse.ProyectoRelacionado(
                        11L, "Proyecto Luna", "CONFIRMADO", "Director", "ACTIVO",
                        LocalDateTime.now(), null)),
                List.of(),
                List.of());
    }
}
