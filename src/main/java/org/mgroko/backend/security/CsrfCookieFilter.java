package org.mgroko.backend.security;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Desde Spring Security 6, el CsrfToken se resuelve de forma diferida
 * (no se escribe la cookie XSRF-TOKEN hasta que algo lo lee explícitamente).
 * Este filtro fuerza esa lectura en cada request para que la cookie
 * siempre se envíe al cliente. Patrón oficial recomendado por Spring
 * para integraciones con SPA.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }

        filterChain.doFilter(request, response);
    }
}