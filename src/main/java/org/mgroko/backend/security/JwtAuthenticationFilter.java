package org.mgroko.backend.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.mgroko.backend.modelo.PermisoGlobal;
import org.mgroko.backend.modelo.RolGlobal;
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

            Optional<Usuario> usuarioOpt = usuarioRepository.findByIdConRolYPermisos(Long.parseLong(subject));
            if (usuarioOpt.isEmpty() || !usuarioOpt.get().getEstado().permiteAcceso()) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            Usuario usuario = usuarioOpt.get();
            List<GrantedAuthority> authorities = new ArrayList<>();

            // Las autoridades se construyen desde la base de datos, no desde
            // el claim: así un cambio de rol aplica sin esperar al próximo login.
            RolGlobal rolGlobal = usuario.getRolGlobal();
            if (rolGlobal != null) {
                String codigoRol = rolGlobal.getCodigo() != null
                        ? rolGlobal.getCodigo()
                        : rolGlobal.getNombre();
                if (codigoRol != null && !codigoRol.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(
                            "ROLE_" + codigoRol.toUpperCase(Locale.ROOT)));
                }

                if (rolGlobal.getPermisos() != null) {
                    for (PermisoGlobal permiso : rolGlobal.getPermisos()) {
                        if (permiso.getNombre() != null && !permiso.getNombre().isBlank()) {
                            authorities.add(new SimpleGrantedAuthority(permiso.getNombre()));
                        }
                    }
                }
            }

            Long idPerfilActivo = obtenerClaimLong(claims, "idPerfilActivo");
            String nombreArtisticoActivo = claims.get("nombreArtisticoActivo", String.class);

            // El administrador nunca opera con un perfil activo, aunque un
            // token viejo o un dato inconsistente lo incluyan.
            if (PerfilAccessPolicy.esAdministrador(usuario)) {
                idPerfilActivo = null;
                nombreArtisticoActivo = null;
            }

            ContextoAutenticacion contexto = new ContextoAutenticacion(
                    Long.parseLong(subject),
                    idPerfilActivo,
                    nombreArtisticoActivo
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