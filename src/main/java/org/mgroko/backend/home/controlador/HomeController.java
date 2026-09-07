package org.mgroko.backend.home.controlador;

import java.util.Collections;
import java.util.Map;

import org.mgroko.backend.home.dto.HomeResumenResponse;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.UsuarioPerfilService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final UsuarioPerfilService usuarioPerfilService;

    public HomeController(UsuarioPerfilService usuarioPerfilService) {
        this.usuarioPerfilService = usuarioPerfilService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/home/resumen")
    public ResponseEntity<HomeResumenResponse> obtenerResumenHome(Authentication authentication) {
        PerfilResponse perfilActivo = null;

        if (authentication != null && authentication.getPrincipal() != null) {
            Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
            if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
                Long idPerfilActivo = contexto.idPerfilActivo();
                if (idPerfilActivo != null) {
                    perfilActivo = usuarioPerfilService.obtenerPerfilPropio(idUsuario, idPerfilActivo);
                }
            }
        }

        HomeResumenResponse response = new HomeResumenResponse(
                perfilActivo,
                Collections.emptyList(),
                Collections.emptyList());

        return ResponseEntity.ok(response);
    }

}
