package org.mgroko.backend.security;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CsrfCookieFilter extends OncePerRequestFilter {

    private final CsrfTokenRepository csrfTokenRepository;

    public CsrfCookieFilter(CsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Fuerza la escritura de la cookie XSRF-TOKEN.
            csrfToken.getToken();
        } else {
            // Rutas exentas de CSRF (por ejemplo, el onboarding de Google):
            // igualmente se emite un token para que el frontend pueda hacer
            // logout y el resto de peticiones mutantes.
            CsrfToken nuevo = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(nuevo, request, response);
        }

        filterChain.doFilter(request, response);
    }
}
