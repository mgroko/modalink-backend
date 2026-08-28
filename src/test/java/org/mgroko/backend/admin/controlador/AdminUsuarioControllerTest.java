package org.mgroko.backend.admin.controlador;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminUsuarioResponse;
import org.mgroko.backend.admin.exception.AutoDeshabilitacionException;
import org.mgroko.backend.admin.exception.UsuarioAdminNoEncontradoException;
import org.mgroko.backend.admin.exception.UsuarioEnBajaException;
import org.mgroko.backend.admin.servicio.AdminUsuarioService;
import org.mgroko.backend.modelo.Genero;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUsuarioService adminUsuarioService;

    private AdminUsuarioResponse response(Genero genero, String estado) {
        return new AdminUsuarioResponse(
                2L, "Maria", "Flores", "maria@test.com",
                estado, "USUARIO", null, "12345678", null, genero);
    }

    @Test
    void habilitar_usuarioValido_devuelve200() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        when(adminUsuarioService.habilitar(2L))
                .thenReturn(response(new Genero(1L, "mujer"), "Activo"));

        mockMvc.perform(patch("/admin/usuarios/2/habilitar")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(2))
                .andExpect(jsonPath("$.estado").value("Activo"));
    }

    @Test
    void deshabilitar_usuarioValido_devuelve200() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        when(adminUsuarioService.deshabilitar(2L, 1L))
                .thenReturn(response(new Genero(2L, "hombre"), "Deshabilitado"));

        mockMvc.perform(patch("/admin/usuarios/2/deshabilitar")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(2))
                .andExpect(jsonPath("$.estado").value("Deshabilitado"));
    }

    @Test
    void habilitar_usuarioNoExiste_devuelve404() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        when(adminUsuarioService.habilitar(999L))
                .thenThrow(new UsuarioAdminNoEncontradoException("Usuario no encontrado."));

        mockMvc.perform(patch("/admin/usuarios/999/habilitar")
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado."));
    }

    @Test
    void habilitar_usuarioEnBaja_devuelve403() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        when(adminUsuarioService.habilitar(2L))
                .thenThrow(new UsuarioEnBajaException(
                        "El usuario solicitó la baja de su cuenta; no puede habilitarse ni deshabilitarse desde el panel administrativo."));

        mockMvc.perform(patch("/admin/usuarios/2/habilitar")
                        .principal(authentication))
                .andExpect(status().isForbidden());
    }

    @Test
    void deshabilitar_autoDeshabilitacion_devuelve403() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        when(adminUsuarioService.deshabilitar(1L, 1L))
                .thenThrow(new AutoDeshabilitacionException("No podés deshabilitar tu propia cuenta."));

        mockMvc.perform(patch("/admin/usuarios/1/deshabilitar")
                        .principal(authentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No podés deshabilitar tu propia cuenta."));
    }

    @Test
    void deshabilitar_usuarioNoExiste_devuelve404() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        when(adminUsuarioService.deshabilitar(anyLong(), anyLong()))
                .thenThrow(new UsuarioAdminNoEncontradoException("Usuario no encontrado."));

        mockMvc.perform(patch("/admin/usuarios/999/deshabilitar")
                        .principal(authentication))
                .andExpect(status().isNotFound());
    }
}
