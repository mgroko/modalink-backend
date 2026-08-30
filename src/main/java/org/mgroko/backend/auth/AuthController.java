package org.mgroko.backend.auth;

import org.mgroko.backend.auth.dto.AuthResponse;
import org.mgroko.backend.auth.dto.LoginRequest;
import org.mgroko.backend.auth.dto.RegistroRequest;
import org.mgroko.backend.auth.dto.UsuarioResponse;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.mgroko.backend.security.JwtCookieFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final JwtCookieFactory jwtCookieFactory;

    public AuthController(AuthService authService, JwtCookieFactory jwtCookieFactory) {
        this.authService = authService;
        this.jwtCookieFactory = jwtCookieFactory;
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) {
        AuthService.RegistroResultado resultado = authService.registrar(request);
        AuthResponse authResponseSinToken = new AuthResponse(resultado.response());

        ResponseCookie cookie = jwtCookieFactory.crear(resultado.token());

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(authResponseSinToken);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResultado resultado = authService.login(request);

        ResponseCookie cookie = jwtCookieFactory.crear(resultado.token());

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(resultado.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, jwtCookieFactory.crearExpirada().toString())
            .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> obtenerUsuarioActual(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        String nombreArtisticoActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
            nombreArtisticoActivo = contexto.nombreArtisticoActivo();
        }

        UsuarioResponse usuarioActual = authService.obtenerUsuarioActual(idUsuario, idPerfilActivo, nombreArtisticoActivo);

        return ResponseEntity.ok(usuarioActual);
    }
}