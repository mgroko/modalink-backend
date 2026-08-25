package org.mgroko.backend.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
        
        String jwt = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        if (jwt == null) {
                filterChain.doFilter(request, response);
                return;
            }

       try {
            Claims claims = jwtService.validarYObtenerClaims(jwt);
            String subject = claims.getSubject();

            List<GrantedAuthority> authorities = new ArrayList<>();

            // Rol como authority con prefijo ROLE_ (convención de Spring Security para hasRole())
            String rolGlobal = claims.get("rolGlobal", String.class);
            if (rolGlobal != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + rolGlobal.toUpperCase()));
            }

            // Permisos finos como authorities planas(para hasAuthority())
            List<?> permisosGlobales = claims.get("permisosGlobales", List.class);
            if (permisosGlobales != null) {
                for (Object permiso : permisosGlobales) {
                    authorities.add(new SimpleGrantedAuthority(String.valueOf(permiso)));
                }
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    subject,
                    null,
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}

