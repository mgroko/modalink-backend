package org.mgroko.backend.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mgroko.backend.auth.oauth.CookieOAuth2AuthorizationRequestRepository;
import org.mgroko.backend.auth.oauth.GoogleOAuth2FailureHandler;
import org.mgroko.backend.auth.oauth.GoogleOAuth2SuccessHandler;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Import(CsrfConfig.class)
public class SecurityConfig {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;
    private final GoogleOAuth2FailureHandler googleOAuth2FailureHandler;
    private final String frontendUrl;

    public SecurityConfig(
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
            GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler,
            GoogleOAuth2FailureHandler googleOAuth2FailureHandler,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.googleOAuth2SuccessHandler = googleOAuth2SuccessHandler;
        this.googleOAuth2FailureHandler = googleOAuth2FailureHandler;
        this.frontendUrl = frontendUrl != null && frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    /**
     * Configura CORS para permitir origen de desarrollo (Vite).
     * Lista extensible para agregar más orígenes en producción.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        
        CorsConfiguration corsConfig = new CorsConfiguration();
        List<String> origenes = new ArrayList<>();
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            origenes.add(frontendUrl);
        }
        if (!origenes.contains("http://localhost:5173")) {
            origenes.add("http://localhost:5173");
        }
        corsConfig.setAllowedOrigins(origenes);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        // El frontend necesita leer el nombre del archivo descargado (UC-67).
        corsConfig.setExposedHeaders(Arrays.asList("Content-Disposition"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository
    ) throws Exception {

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, usuarioRepository);

        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Público
                .requestMatchers(
                        "/auth/login",
                        "/auth/registro",
                        "/auth/reactivar-cuenta",
                        // El logout es idempotente y debe poder limpiar la cookie
                        // aunque el JWT esté vencido o ausente. Si quedara
                        // protegido, Spring respondería 401 antes de llegar al
                        // controlador y la cookie nunca se borraría.
                        "/auth/logout",
                        "/auth/google",
                        "/auth/google/**",
                        "/oauth2/**",
                        "/login/oauth2/**",
                        "/error",
                        "/uploads/**",
                        "/actuator/health",
                        "/actuator/health/**")
                    .permitAll()

                // Solo administrador
                .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")

                // Catálogo de características técnicas: exclusivo del área de usuario
                .requestMatchers("/profesiones/*/caracteristicas-tecnicas").hasRole("USUARIO")

                // Catálogos compartidos de solo lectura
                .requestMatchers(HttpMethod.GET, "/profesiones", "/ubicaciones/**")
                    .hasAnyRole("ADMINISTRADOR", "USUARIO")

                // Compartido entre ambos roles
                .requestMatchers("/auth/me", "/notificaciones/**", "/health")
                    .hasAnyRole("ADMINISTRADOR", "USUARIO")

                // Solo usuario
                .requestMatchers(
                        "/home/**",
                        "/perfiles/**",
                        "/publicaciones/**",
                        "/usuarios/me/**",
                        "/proyectos/**",
                        "/contratos/**",
                        "/postulaciones/**",
                        "/requerimientos-generales/**",
                        "/requerimientos-actividad/**",
                        "/invitaciones/**",
                        "/reemplazos/**",
                        "/solicitudes-conexion/**",
                        "/conexiones/**",
                        "/presencia",
                        "/presencia/**",
                        "/conversaciones",
                        "/conversaciones/**",
                        "/mensajes",
                        "/mensajes/**",
                        "/calendario/**",
                        "/usuario/**",
                        "/habilidades/**")
                    .hasRole("USUARIO")

                .anyRequest().denyAll()
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> escribirError(
                        response,
                        HttpStatus.UNAUTHORIZED,
                        "NO_AUTENTICADO",
                        "Necesitás iniciar sesión para acceder a este recurso."))
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (accessDeniedException instanceof CsrfException) {
                        escribirError(
                                response,
                                HttpStatus.FORBIDDEN,
                                "CSRF_INVALIDO",
                                "Token CSRF inválido o ausente.");
                        return;
                    }
                    escribirError(
                            response,
                            HttpStatus.FORBIDDEN,
                            "ROL_NO_PERMITIDO",
                            "Esta acción no está disponible para tu rol.");
                })
            )

            .cors(Customizer.withDefaults())

            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                        .authorizationRequestRepository(authorizationRequestRepository))
                .successHandler(googleOAuth2SuccessHandler)
                .failureHandler(googleOAuth2FailureHandler))

            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // El onboarding de Google viaja con un ticket firmado en cookie
                // HttpOnly y SameSite=Lax; no hay sesión previa para emitir un
                // XSRF-TOKEN antes del callback. El logout también se exonera:
                // es idempotente y debe limpiar la cookie aun cuando la sesión
                // ya no sea válida o falte el token CSRF.
                .ignoringRequestMatchers("/auth/google/**", "/auth/logout")
            )
            .addFilterAfter(new CsrfCookieFilter(csrfTokenRepository), UsernamePasswordAuthenticationFilter.class)

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Escribe una respuesta JSON estable para los errores generados por la
     * cadena de seguridad (antes de llegar a los controladores).
     */
    private void escribirError(
            HttpServletResponse response,
            HttpStatus status,
            String codigo,
            String mensaje) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String body = "{\"codigo\":\"" + codigo
                + "\",\"message\":\"" + mensaje
                + "\",\"httpStatus\":" + status.value()
                + ",\"timestamp\":" + System.currentTimeMillis() + "}";

        response.getWriter().write(body);
    }
}
