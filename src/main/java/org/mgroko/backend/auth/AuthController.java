package org.mgroko.backend.auth;

import java.util.HashMap;
import java.util.Map;

import org.mgroko.backend.auth.dto.AuthResponse;
import org.mgroko.backend.auth.dto.LoginRequest;
import org.mgroko.backend.auth.dto.RegistroRequest;
import org.mgroko.backend.auth.dto.UsuarioResponse;
import org.mgroko.backend.security.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
     * Devuelve token JWT en cookies y datos básicos del usuario.
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
       AuthResponse authResponseSinToken = new AuthResponse(usuarioResponse); 
        
       // nuevo agregado para probar http cookies
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
            .httpOnly(true)
            .secure(false) // false para desarrollo, en produc debe ser true
            .path("/")
            .maxAge(24 * 60 * 60)
            .sameSite("Lax")
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(authResponseSinToken); 
    }

    /**
     * Endpoint de login: autentica un usuario con correo y contraseña.
     * Devuelve token JWT en cookies y datos básicos del usuario.
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

        // nuevo agregado para probar http cookies
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(24 * 60 * 60) 
            .sameSite("Lax") 
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(authResponseSinToken);

    }

    // agrego este endpoint para generar una cookie CSRF (y preparar al navegaodr con la cookie de seguridad)
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> obtenerUsuarioActual(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        UsuarioResponse usuarioActual = authService.obtenerUsuarioActual(idUsuario);

        return ResponseEntity.ok(usuarioActual);
    }
}
