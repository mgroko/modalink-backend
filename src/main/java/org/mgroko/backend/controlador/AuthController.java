package org.mgroko.backend.controlador;

import java.util.HashMap;
import java.util.Map;

import org.mgroko.backend.auth.AuthService;
import org.mgroko.backend.dto.AuthResponse;
import org.mgroko.backend.dto.LoginRequest;
import org.mgroko.backend.dto.RegistroRequest;
import org.mgroko.backend.dto.UsuarioResponse;
import org.mgroko.backend.security.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    /**
     * Endpoint de registro: crea un nuevo usuario.
     * Devuelve token JWT y datos básicos del usuario.
     */
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) {
        // AuthService valida duplicados y crea el usuario
        UsuarioResponse usuarioResponse = authService.registrar(request);

        // Generar JWT con el ID del usuario como subject
        Map<String, Object> claims = new HashMap<>();
        claims.put("correo", usuarioResponse.correo());
        claims.put("rolGlobal", usuarioResponse.rolGlobal());

        String token = jwtService.generarToken(usuarioResponse.idUsuario().toString(), claims);
        AuthResponse authResponse = new AuthResponse(token, usuarioResponse);
        return ResponseEntity.ok(authResponse); 
    }

    /**
     * Endpoint de login: autentica un usuario con correo y contraseña.
     * Devuelve token JWT y datos básicos del usuario.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // AuthService valida credenciales y devuelve el usuario
        AuthResponse authResponseSinToken = authService.login(request);
        UsuarioResponse usuario = authResponseSinToken.usuario();

        // Generar JWT con el ID del usuario como subject
        Map<String, Object> claims = new HashMap<>();
        claims.put("correo", usuario.correo());
        claims.put("rolGlobal", usuario.rolGlobal());

        String token = jwtService.generarToken(usuario.idUsuario().toString(), claims);

        /* AuthResponse authResponse = new AuthResponse(token, usuario);
        return ResponseEntity.ok(authResponse); */

        // nuevo agregado para probar http cookies
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
            .httpOnly(true)
            .secure(false) // false para desarrollo local sin HTTPS. En producción debe ser true.
            .path("/")
            .maxAge(24 * 60 * 60) // 1 día en segundos
            .sameSite("Lax") // Lax es necesario para desarrollo local en puertos distintos
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(authResponseSinToken);

    }
}
