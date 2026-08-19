package org.mgroko.backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.mgroko.backend.auth.dto.AuthResponse;
import org.mgroko.backend.auth.dto.LoginRequest;
import org.mgroko.backend.auth.dto.RegistroRequest;
import org.mgroko.backend.auth.dto.UsuarioResponse;
import org.mgroko.backend.security.JwtService;

import java.util.List;

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
                1L, "Juan", "Perez", "12345678", "juan@test.com", "Usuario");
        when(authService.registrar(any(RegistroRequest.class))).thenReturn(usuarioResponse);
        when(jwtService.generarToken(anyString(), anyMap())).thenReturn("token-simulado");

        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "juan@test.com", "password123");

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String cookie = result.getResponse().getHeader("Set-Cookie");
                    org.junit.jupiter.api.Assertions.assertNotNull(cookie);
                    org.junit.jupiter.api.Assertions.assertTrue(cookie.contains("jwt=token-simulado"));
                })
                .andExpect(jsonPath("$.usuario.correo").value("juan@test.com"));
    }

    @Test
    void registro_correoInvalido_devuelve400() throws Exception {
        // @Valid + @Email en RegistroRequest debe rechazar esto antes de
        // que el controller llame a authService.
        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "no-es-un-correo", "password123");

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // /auth/login
    // ---------------------------------------------------------------

    @Test
    void login_credencialesValidas_devuelve200ConCookieJwt() throws Exception {
        UsuarioResponse usuarioResponse = new UsuarioResponse(
                1L, "Juan", "Perez", "12345678", "juan@test.com", "Usuario");
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
                .thenThrow(new org.mgroko.backend.auth.exception.CredencialesInvalidasException(
                        "Correo o contraseña inválidos."));

        LoginRequest request = new LoginRequest("juan@test.com", "passwordMala");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // El endpoint no debería intentar generar token si las credenciales fallaron
        org.mockito.Mockito.verify(jwtService, org.mockito.Mockito.never())
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
    void me_autenticado_devuelveSubject() throws Exception {
        // Spring resuelve el parámetro Authentication del controller
        // leyendo el SecurityContextHolder del hilo actual; como MockMvc
        // corre síncrono en el mismo hilo del test, alcanza con setearlo
        // acá antes de la request.
        var authentication = new UsernamePasswordAuthenticationToken("123", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("123"));
    }
}