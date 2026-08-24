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
import org.mgroko.backend.security.JwtService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
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

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------
    // /auth/registro
    // ---------------------------------------------------------------

    @Test
    void registro_datosValidos_devuelve200ConCookieJwt() throws Exception {
        UsuarioResponse usuarioResponse = new UsuarioResponse(
                1L, "Juan", "Perez", "12345678", "juan@test.com", "Usuario", "hombre");
        when(authService.registrar(any(RegistroRequest.class))).thenReturn(usuarioResponse);
        when(jwtService.generarToken(anyString(), anyMap())).thenReturn("token-simulado");
 
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

        verify(jwtService).generarToken(anyString(), anyMap());
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

    // ---------------------------------------------------------------
    // /auth/login
    // ---------------------------------------------------------------

    @Test
    void login_credencialesValidas_devuelve200ConCookieJwt() throws Exception {
        UsuarioResponse usuarioResponse = new UsuarioResponse(
                1L, "Juan", "Perez", "12345678", "juan@test.com", "Usuario", "hombre");
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse(usuarioResponse));
        when(jwtService.generarToken(anyString(), anyMap())).thenReturn("token-simulado");

        LoginRequest request = new LoginRequest("juan@test.com", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.correo").value("juan@test.com"));
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
        UsuarioResponse usuarioResponse = new UsuarioResponse(
                123L, "Juan", "Perez", "12345678", "juan@test.com", "Usuario", "hombre");
        when(authService.obtenerUsuarioActual(123L)).thenReturn(usuarioResponse);
 
        var authentication = new UsernamePasswordAuthenticationToken("123", null, List.of());

        mockMvc.perform(get("/auth/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("juan@test.com"))
                .andExpect(jsonPath("$.idUsuario").value(123));
    }
 
    @Test
    void me_usuarioYaNoExiste_devuelve401() throws Exception {
        // Token válido pero el usuario fue dado de baja después de emitirlo
        when(authService.obtenerUsuarioActual(999L))
                .thenThrow(new org.mgroko.backend.auth.exception.UsuarioNoEncontradoException(
                        "Usuario no encontrado."));
 
        var authentication = new UsernamePasswordAuthenticationToken("999", null, List.of());
 
        mockMvc.perform(get("/auth/me").principal(authentication))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_usuarioDeshabilitado_devuelve403() throws Exception {
        // Token válido pero el usuario fue deshabilitado después de emitirlo
        when(authService.obtenerUsuarioActual(888L))
                .thenThrow(new org.mgroko.backend.auth.exception.UsuarioDeshabilitadoException(
                        "Usuario deshabilitado."));
 
        var authentication = new UsernamePasswordAuthenticationToken("888", null, List.of());
 
        mockMvc.perform(get("/auth/me").principal(authentication))
                .andExpect(status().isForbidden());
    }
    
}