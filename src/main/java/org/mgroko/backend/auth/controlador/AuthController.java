package org.mgroko.backend.auth.controlador;

import java.net.URI;

import org.mgroko.backend.auth.dto.AuthResponse;
import org.mgroko.backend.auth.dto.CompletarRegistroGoogleRequest;
import org.mgroko.backend.auth.dto.LoginRequest;
import org.mgroko.backend.auth.dto.RegistroPendienteResponse;
import org.mgroko.backend.auth.dto.RegistroRequest;
import org.mgroko.backend.auth.dto.UsuarioResponse;
import org.mgroko.backend.auth.exception.RegistroConSesionActivaException;
import org.mgroko.backend.auth.oauth.GoogleAuthService;
import org.mgroko.backend.auth.oauth.GoogleUserData;
import org.mgroko.backend.auth.oauth.RegistroPendienteService;
import org.mgroko.backend.auth.servicio.AuthService;
import org.mgroko.backend.auth.servicio.AuthService.LoginResultado;
import org.mgroko.backend.auth.servicio.AuthService.RegistroResultado;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.mgroko.backend.security.JwtCookieFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
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
    private final GoogleAuthService googleAuthService;
    private final RegistroPendienteService registroPendienteService;
    private final String frontendUrl;

    public AuthController(
            AuthService authService,
            JwtCookieFactory jwtCookieFactory,
            GoogleAuthService googleAuthService,
            RegistroPendienteService registroPendienteService,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.authService = authService;
        this.jwtCookieFactory = jwtCookieFactory;
        this.googleAuthService = googleAuthService;
        this.registroPendienteService = registroPendienteService;
        this.frontendUrl = frontendUrl != null && frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(
            @Valid @RequestBody RegistroRequest request,
            Authentication authentication) {
        if (haySesionAutenticada(authentication)) {
            throw new RegistroConSesionActivaException();
        }

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

    /**
     * Inicia el flujo OAuth2 con Google. Si ya hay una sesión local activa se
     * evita el cambio silencioso de cuenta.
     */
    @GetMapping("/google")
    public ResponseEntity<Void> iniciarGoogle(Authentication authentication) {
        if (haySesionAutenticada(authentication)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/login?googleError=sesion_activa"))
                    .build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/google"))
                .build();
    }

    /**
     * Devuelve los datos de Google que el frontend usa para prellenar el
     * formulario de onboarding. El ticket viaja en una cookie HttpOnly.
     */
    @GetMapping("/google/pendiente")
    public ResponseEntity<RegistroPendienteResponse> obtenerRegistroPendiente(
            @CookieValue(name = JwtCookieFactory.NOMBRE_COOKIE_REGISTRO_GOOGLE, required = false) String ticket) {
        GoogleUserData datos = registroPendienteService.leerTicket(ticket);
        return ResponseEntity.ok(new RegistroPendienteResponse(
                datos.nombre(),
                datos.apellido(),
                datos.correo()));
    }

    /**
     * Completa el registro iniciado con Google: crea la cuenta con los datos
     * obligatorios del dominio y emite el JWT en la misma respuesta.
     */
    @PostMapping("/google/completar")
    public ResponseEntity<AuthResponse> completarRegistroGoogle(
            @CookieValue(name = JwtCookieFactory.NOMBRE_COOKIE_REGISTRO_GOOGLE, required = false) String ticket,
            @Valid @RequestBody CompletarRegistroGoogleRequest request) {

        AuthService.LoginResultado resultado = googleAuthService.completarRegistro(ticket, request);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, jwtCookieFactory.crear(resultado.token()).toString());
        headers.add(HttpHeaders.SET_COOKIE,
                jwtCookieFactory.crearRegistroPendienteExpirada().toString());

        return new ResponseEntity<>(resultado.response(), headers, HttpStatus.OK);
    }

    /**
     * Reactiva la cuenta pendiente de baja y abre sesión en el mismo paso.
     * Pensado para el modal de "reactivar cuenta" que interrumpe el login:
     * al no existir sesión previa, se reenvían las credenciales.
     */
    @PostMapping("/reactivar-cuenta")
    public ResponseEntity<AuthResponse> reactivarCuenta(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResultado resultado = authService.reactivarCuenta(request);

        ResponseCookie cookie = jwtCookieFactory.crear(resultado.token());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(resultado.response());
    }

    /**
     * Cierra la sesión. El endpoint es público y está exento de CSRF: el
     * logout debe poder limpiar la cookie JWT incluso si el token ya venció,
     * si no hay sesión o si el cliente perdió el token CSRF. La operación es
     * idempotente y no tiene efectos en el servidor, solo borra la cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        SecurityContextHolder.clearContext();

        HttpStatus estado = haySesionAutenticada(authentication)
                ? HttpStatus.OK
                : HttpStatus.UNAUTHORIZED;

        return ResponseEntity.status(estado)
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

        UsuarioResponse usuarioActual = authService.obtenerUsuarioActual(idUsuario, idPerfilActivo,
                nombreArtisticoActivo);

        return ResponseEntity.ok(usuarioActual);
    }

    private boolean haySesionAutenticada(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}