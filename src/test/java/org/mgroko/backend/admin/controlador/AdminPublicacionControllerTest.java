package org.mgroko.backend.admin.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminPublicacionResponse;
import org.mgroko.backend.admin.dto.ModerarPublicacionRequest;
import org.mgroko.backend.admin.servicio.AdminPublicacionService;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.publicaciones.exception.EstadoPublicacionInvalidoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = AdminPublicacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPublicacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPublicacionService adminPublicacionService;

    @Test
    void moderar_devuelvePublicacionActualizada() throws Exception {
        when(adminPublicacionService.moderar(
                eq(30L), eq(99L), any(ModerarPublicacionRequest.class)))
                .thenReturn(publicacion());

        mockMvc.perform(patch("/admin/publicaciones/30/estado")
                        .principal(autenticacion(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"OCULTA","motivo":"Contenido inapropiado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPublicacion").value(30))
                .andExpect(jsonPath("$.estado").value("OCULTA"))
                .andExpect(jsonPath("$.puedeOcultar").value(false))
                .andExpect(jsonPath("$.puedeReactivar").value(true));
    }

    @Test
    void moderar_sinEstado_devuelve400() throws Exception {
        mockMvc.perform(patch("/admin/publicaciones/30/estado")
                        .principal(autenticacion(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moderar_estadoInvalido_devuelve409() throws Exception {
        doThrow(new EstadoPublicacionInvalidoException("La publicación ya se encuentra oculta."))
                .when(adminPublicacionService)
                .moderar(eq(30L), eq(99L), any(ModerarPublicacionRequest.class));

        mockMvc.perform(patch("/admin/publicaciones/30/estado")
                        .principal(autenticacion(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"OCULTA","motivo":"Contenido inapropiado"}
                                """))
                .andExpect(status().isConflict());
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario) {
        var auth = new UsernamePasswordAuthenticationToken(
                String.valueOf(idUsuario), null, List.of());
        auth.setDetails(new ContextoAutenticacion(idUsuario, null, null));
        return auth;
    }

    private AdminPublicacionResponse publicacion() {
        return new AdminPublicacionResponse(
                30L,
                "Publicación de prueba",
                "Contenido",
                LocalDateTime.of(2026, 11, 10, 9, 0),
                EstadoPublicacion.OCULTA,
                20L,
                "Perfil Dueño",
                2L,
                "Ana Gómez",
                List.of(),
                null,
                null,
                List.of(),
                0,
                0,
                2L,
                1L,
                false,
                true);
    }
}
