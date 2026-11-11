package org.mgroko.backend.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.auth.oauth.CookieOAuth2AuthorizationRequestRepository;
import org.mgroko.backend.auth.oauth.GoogleOAuth2FailureHandler;
import org.mgroko.backend.auth.oauth.GoogleOAuth2SuccessHandler;
import org.mgroko.backend.polaroids.controlador.PolaroidPerfilController;
import org.mgroko.backend.polaroids.servicio.CrearPolaroidPerfilService;
import org.mgroko.backend.polaroids.servicio.EliminarPolaroidPerfilService;
import org.mgroko.backend.polaroids.servicio.ListarPolaroidsPerfilService;
import org.mgroko.backend.polaroids.servicio.ModificarPolaroidPerfilService;
import org.mgroko.backend.polaroids.servicio.PolaroidCatalogoService;
import org.mgroko.backend.polaroids.servicio.ReordenarPolaroidsPerfilService;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El catálogo global de polaroids vive fuera de /perfiles, por lo que necesita
 * su propia regla en {@link SecurityConfig}.
 */
@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = PolaroidPerfilController.class)
@Import(SecurityConfig.class)
class PolaroidAreaAccessSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PolaroidCatalogoService polaroidCatalogoService;

    @MockitoBean
    private ListarPolaroidsPerfilService listarPolaroidsPerfilService;

    @MockitoBean
    private CrearPolaroidPerfilService crearPolaroidPerfilService;

    @MockitoBean
    private ModificarPolaroidPerfilService modificarPolaroidPerfilService;

    @MockitoBean
    private ReordenarPolaroidsPerfilService reordenarPolaroidsPerfilService;

    @MockitoBean
    private EliminarPolaroidPerfilService eliminarPolaroidPerfilService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @MockitoBean
    private GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;

    @MockitoBean
    private GoogleOAuth2FailureHandler googleOAuth2FailureHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void usuario_puedeLeerElCatalogoDePolaroids() throws Exception {
        when(polaroidCatalogoService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/polaroids/tipos")
                        .with(user("usuario").roles("USUARIO")))
                .andExpect(status().isOk());
    }

    @Test
    void sinAutenticacion_elCatalogoDevuelve401() throws Exception {
        mockMvc.perform(get("/polaroids/tipos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administrador_noPuedeLeerElCatalogoDePolaroids() throws Exception {
        mockMvc.perform(get("/polaroids/tipos")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }
}
