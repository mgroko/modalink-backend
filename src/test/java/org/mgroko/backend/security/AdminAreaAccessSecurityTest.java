package org.mgroko.backend.security;

import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.controlador.AdminUsuarioController;
import org.mgroko.backend.admin.servicio.AdminUsuarioService;
import org.mgroko.backend.auth.oauth.CookieOAuth2AuthorizationRequestRepository;
import org.mgroko.backend.auth.oauth.GoogleOAuth2FailureHandler;
import org.mgroko.backend.auth.oauth.GoogleOAuth2SuccessHandler;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.home.controlador.HomeController;
import org.mgroko.backend.home.servicio.HomeOportunidadesService;
import org.mgroko.backend.perfiles.controlador.CaracteristicaTecnicaController;
import org.mgroko.backend.perfiles.controlador.ProfesionController;
import org.mgroko.backend.perfiles.servicio.CaracteristicaTecnicaService;
import org.mgroko.backend.perfiles.servicio.ProfesionService;
import org.mgroko.backend.perfiles.servicio.UsuarioPerfilService;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.requerimientos.controlador.RequerimientoBusquedaController;
import org.mgroko.backend.requerimientos.servicio.BuscarRequerimientosActividadGlobalService;
import org.mgroko.backend.requerimientos.servicio.BuscarRequerimientosGeneralesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica la separación de áreas: el administrador no puede entrar al
 * dashboard de usuario y el usuario común no puede entrar al panel admin.
 */
@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class, controllers = {
        HomeController.class,
        AdminUsuarioController.class,
        ProfesionController.class,
        CaracteristicaTecnicaController.class,
        RequerimientoBusquedaController.class
})
@Import(SecurityConfig.class)
class AdminAreaAccessSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioPerfilService usuarioPerfilService;

    @MockitoBean
    private HomeOportunidadesService homeOportunidadesService;

    @MockitoBean
    private AdminUsuarioService adminUsuarioService;

    @MockitoBean
    private ProfesionService profesionService;

    @MockitoBean
    private CaracteristicaTecnicaService caracteristicaTecnicaService;

    @MockitoBean
    private BuscarRequerimientosGeneralesService buscarRequerimientosGeneralesService;

    @MockitoBean
    private BuscarRequerimientosActividadGlobalService buscarRequerimientosActividadService;

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
    private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void administrador_noPuedeAccederAlDashboardDeUsuario() throws Exception {
        mockMvc.perform(get("/home/resumen")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuario_noPuedeAccederAlPanelAdministrativo() throws Exception {
        mockMvc.perform(get("/admin/usuarios")
                        .with(user("usuario").roles("USUARIO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administrador_puedeAccederAlPanelAdministrativo() throws Exception {
        when(adminUsuarioService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/admin/usuarios")
                        .with(user("admin").authorities(
                                new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"),
                                new SimpleGrantedAuthority("VER_USUARIOS"))))
                .andExpect(status().isOk());
    }

    @Test
    void administrador_puedeLeerElCatalogoDeProfesiones() throws Exception {
        when(profesionService.buscar(null)).thenReturn(List.of());

        mockMvc.perform(get("/profesiones")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void administrador_noPuedeLeerCaracteristicasDelAreaDeUsuario() throws Exception {
        mockMvc.perform(get("/profesiones/1/caracteristicas-tecnicas")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuario_puedeLeerCaracteristicasDelAreaDeUsuario() throws Exception {
        when(caracteristicaTecnicaService.buscar(1L, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/profesiones/1/caracteristicas-tecnicas")
                        .with(user("usuario").roles("USUARIO")))
                .andExpect(status().isOk());
    }

    @Test
    void sinAutenticacion_noPuedeAccederAlDashboardDeUsuario() throws Exception {
        mockMvc.perform(get("/home/resumen"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuario_puedeBuscarRequerimientosGenerales() throws Exception {
        when(buscarRequerimientosGeneralesService.buscar(
                any(), any(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/requerimientos-generales/buscar")
                        .with(authentication(autenticacionUsuario(10L))))
                .andExpect(status().isOk());
    }

    @Test
    void usuario_puedeBuscarRequerimientosDeActividad() throws Exception {
        when(buscarRequerimientosActividadService.buscar(
                any(), any(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/requerimientos-actividad/buscar")
                        .with(authentication(autenticacionUsuario(10L))))
                .andExpect(status().isOk());
    }

    @Test
    void administrador_noPuedeBuscarRequerimientosGenerales() throws Exception {
        mockMvc.perform(get("/requerimientos-generales/buscar")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken autenticacionUsuario(Long idPerfilActivo) {
        var token = new UsernamePasswordAuthenticationToken(
                "2", null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
        token.setDetails(new ContextoAutenticacion(2L, idPerfilActivo, "Perfil activo"));
        return token;
    }
}
