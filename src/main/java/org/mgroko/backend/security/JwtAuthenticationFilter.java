package org.mgroko.backend.security;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");

        // Si no hay header o no comienza con "Bearer ", dejar pasar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token (quitar "Bearer " del principio)
        String token = authHeader.substring(7);

        try {
            // Validar el token y obtener sus claims
            Claims claims = jwtService.validarYObtenerClaims(token);
            String subject = claims.getSubject();

            // Crear una autenticación y ponerla en el contexto de seguridad
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    subject,
                    null,
                    new ArrayList<>() // Sin roles por ahora; se pueden agregar desde los claims si es necesario
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JwtException e) {
            // Token inválido o expirado: no lanzar excepción acá, dejar que Spring Security lo resuelva
            // El SecurityContextHolder permanece sin autenticación, y el framework responderá 401
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
