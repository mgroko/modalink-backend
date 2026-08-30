package org.mgroko.backend.auth;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.auth.dto.AuthResponse;
import org.mgroko.backend.auth.dto.LoginRequest;
import org.mgroko.backend.auth.dto.RegistroRequest;
import org.mgroko.backend.auth.dto.UsuarioResponse;
import org.mgroko.backend.auth.exception.CredencialesInvalidasException;
import org.mgroko.backend.auth.exception.GeneroNoEncontradoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.mgroko.backend.security.JwtCookieFactory;
import org.mgroko.backend.security.JwtService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // probamos el controller, no la cadena de seguridad
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtCookieFactory jwtCookieFactory;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private UsuarioResponse usuarioResponse(Long id, String correo) {
        return new UsuarioResponse(
                id, "Juan", "Perez", "12345678", correo, "Usuario", "hombre",
                LocalDate.of(2003, 12, 10), "Activo", null, null);
    }

    // ---------------------------------------------------------------
    // /auth/registro
    // ---------------------------------------------------------------

    @Test
    void registro_datosValidos_devuelve200ConCookieJwt() throws Exception {
        UsuarioResponse usuarioResponse = usuarioResponse(1L, "juan@test.com");
        when(authService.registrar(any(RegistroRequest.class)))
                .thenReturn(new AuthService.RegistroResultado("token-simulado", usuarioResponse));
        when(jwtCookieFactory.crear(anyString()))
                .thenReturn(ResponseCookie.from("jwt", "token-simulado").build());

        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "juan@test.com", "hombre", "password123");

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String cookie = result.getResponse().getHeader("Set-Cookie");
                    org.junit.jupiter.api.Assertions.assertNotNull(cookie);
                    org.junit.jupiter.api.Assertions.assertTrue(cookie.contains("jwt=token-simulado"));
                })
                .andExpect(jsonPath("$.usuario.correo").value("juan@test.com"))
                .andExpect(jsonPath("$.usuario.genero").value("hombre"));
    }

    @Test
    void registro_correoInvalido_devuelve400() throws Exception {

        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "no-es-un-correo", "hombre", "password123");

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registro_generoVacio_devuelve400() throws Exception {
        // Bean Validation rechaza el request antes de llegar al servicio
        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "juan@test.com", "", "password123");

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registrar(any());
    }

    @Test
    void registro_generoInexistente_devuelve400() throws Exception {

        when(authService.registrar(any(RegistroRequest.class)))
                .thenThrow(new GeneroNoEncontradoException("El género indicado no existe."));

        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "juan@test.com", "genero_inventado", "password123");

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El género indicado no existe."));

        verify(authService).registrar(any());
    }

    // ---------------------------------------------------------------
    // /auth/login
    // ---------------------------------------------------------------

    @Test
    void login_credencialesValidas_devuelve200ConCookieJwt() throws Exception {
        UsuarioResponse usuarioResponse = usuarioResponse(1L, "juan@test.com");
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthService.LoginResultado("token-simulado", new AuthResponse(usuarioResponse)));
        when(jwtCookieFactory.crear(anyString()))
                .thenReturn(ResponseCookie.from("jwt", "token-simulado").build());

        LoginRequest request = new LoginRequest("juan@test.com", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.correo").value("juan@test.com"));
    }

    @Test
    void login_conPerfilActivo_devuelvePerfilActivoEnUsuario() throws Exception {
        UsuarioResponse usuarioResponse = new UsuarioResponse(
                1L, "Juan", "Perez", "12345678", "juan@test.com", "Usuario", "hombre",
                LocalDate.of(2003, 12, 10), "Activo", 10L, "Luna");
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthService.LoginResultado("token-simulado", new AuthResponse(usuarioResponse)));
        when(jwtCookieFactory.crear(anyString()))
                .thenReturn(ResponseCookie.from("jwt", "token-simulado").build());

        LoginRequest request = new LoginRequest("juan@test.com", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.idPerfilActivo").value(10))
                .andExpect(jsonPath("$.usuario.nombreArtisticoActivo").value("Luna"));
    }

    @Test
    void login_credencialesInvalidas_devuelve401() throws Exception {
        // GlobalExceptionHandler mapea CredencialesInvalidasException -> 401
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new CredencialesInvalidasException(
                        "Correo o contraseña inválidos."));

        LoginRequest request = new LoginRequest("juan@test.com", "passwordMala");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // El endpoint no debería intentar generar token si las credenciales fallaron
        verify(jwtService, never())
                .generarToken(anyString(), anyMap());
    }

    // ---------------------------------------------------------------
    // /auth/me
    // ---------------------------------------------------------------

     @Test
    void me_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_autenticado_devuelveDatosDelUsuario() throws Exception {
        UsuarioResponse usuarioResponse = usuarioResponse(123L, "juan@test.com");
        when(authService.obtenerUsuarioActual(anyLong(), any(), any())).thenReturn(usuarioResponse);

        var authentication = new UsernamePasswordAuthenticationToken("123", null, List.of());

        mockMvc.perform(get("/auth/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("juan@test.com"))
                .andExpect(jsonPath("$.idUsuario").value(123));
    }

    @Test
    void me_autenticado_conPerfilActivo_pasaContextoAlServicio() throws Exception {
        UsuarioResponse usuarioResponse = new UsuarioResponse(
                1L, "Juan", "Perez", "12345678", "juan@test.com", "Usuario", "hombre",
                LocalDate.of(2003, 12, 10), "Activo", 10L, "Luna");
        when(authService.obtenerUsuarioActual(anyLong(), any(), any())).thenReturn(usuarioResponse);

        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());
        authentication.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        mockMvc.perform(get("/auth/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPerfilActivo").value(10))
                .andExpect(jsonPath("$.nombreArtisticoActivo").value("Luna"));

        verify(authService).obtenerUsuarioActual(1L, 10L, "Luna");
    }

    @Test
    void me_usuarioYaNoExiste_devuelve401() throws Exception {
        // Token válido pero el usuario fue dado de baja después de emitirlo
        when(authService.obtenerUsuarioActual(anyLong(), any(), any()))
                .thenThrow(new org.mgroko.backend.auth.exception.UsuarioNoEncontradoException(
                        "Usuario no encontrado."));

        var authentication = new UsernamePasswordAuthenticationToken("999", null, List.of());

        mockMvc.perform(get("/auth/me").principal(authentication))
                .andExpect(status().isUnauthorized());
    }

    
}