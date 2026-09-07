package org.mgroko.backend.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.repositorio.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
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

            Optional<Usuario> usuarioOpt = usuarioRepository.findById(Long.parseLong(subject));
            if (usuarioOpt.isEmpty() || !usuarioOpt.get().getEstado().permiteAcceso()) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            List<GrantedAuthority> authorities = new ArrayList<>();

            String rolGlobal = claims.get("rolGlobal", String.class);
            if (rolGlobal != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + rolGlobal.toUpperCase()));
            }

            List<?> permisosGlobales = claims.get("permisosGlobales", List.class);
            if (permisosGlobales != null) {
                for (Object permiso : permisosGlobales) {
                    authorities.add(new SimpleGrantedAuthority(String.valueOf(permiso)));
                }
            }
            
            ContextoAutenticacion contexto = new ContextoAutenticacion(
                    Long.parseLong(subject),
                    obtenerClaimLong(claims, "idPerfilActivo"),
                    claims.get("nombreArtisticoActivo", String.class)
            );

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    subject,
                    null,
                    authorities
            );

            auth.setDetails(contexto);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JwtException | NumberFormatException e) {
        
            SecurityContextHolder.clearContext();
        }
 
        filterChain.doFilter(request, response);
    }
    private Long obtenerClaimLong(Claims claims, String key) {
        Number valor = claims.get(key, Number.class);
        return valor != null ? valor.longValue() : null;
    }
}