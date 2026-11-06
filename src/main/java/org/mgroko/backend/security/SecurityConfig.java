package org.mgroko.backend.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public SecurityConfig(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Configura CORS para permitir origen de desarrollo (Vite).
     * Lista extensible para agregar más orígenes en producción.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("http://localhost:5173"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
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
                .requestMatchers("/auth/me", "/auth/logout", "/notificaciones/**", "/health")
                    .hasAnyRole("ADMINISTRADOR", "USUARIO")

                // Solo usuario
                .requestMatchers(
                        "/home/**",
                        "/perfiles/**",
                        "/usuarios/me/**",
                        "/proyectos/**",
                        "/postulaciones/**",
                        "/invitaciones/**",
                        "/reemplazos/**",
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

            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )
            .addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class)

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
