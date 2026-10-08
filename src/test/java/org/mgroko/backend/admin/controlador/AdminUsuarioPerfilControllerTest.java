package org.mgroko.backend.admin.controlador;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminPerfilResponse;
import org.mgroko.backend.perfiles.servicio.UsuarioPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUsuarioPerfilController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUsuarioPerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioPerfilService usuarioPerfilService;

    private UsernamePasswordAuthenticationToken autenticacionAdmin() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    @Test
    void listarPerfiles_usuarioConPerfiles_retorna200YLista() throws Exception {
        AdminPerfilResponse perfil = new AdminPerfilResponse(
                10L,
                "Luna Modelo",
                "Biografía de prueba",
                "Activo",
                "modelo",
                LocalDateTime.now().minusDays(1)
        );

        when(usuarioPerfilService.listarPerfiles(5L)).thenReturn(List.of(perfil));

        mockMvc.perform(get("/admin/usuarios/5/perfiles")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idPerfil").value(10))
                .andExpect(jsonPath("$[0].nombreArtistico").value("Luna Modelo"))
                .andExpect(jsonPath("$[0].profesion").value("modelo"))
                .andExpect(jsonPath("$[0].estado").value("Activo"));

        verify(usuarioPerfilService).listarPerfiles(5L);
    }

    @Test
    void listarPerfiles_usuarioSinPerfiles_retorna200YListaVacia() throws Exception {
        when(usuarioPerfilService.listarPerfiles(8L)).thenReturn(List.of());

        mockMvc.perform(get("/admin/usuarios/8/perfiles")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(usuarioPerfilService).listarPerfiles(8L);
    }
}
