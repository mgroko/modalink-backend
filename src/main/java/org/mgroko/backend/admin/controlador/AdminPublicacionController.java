package org.mgroko.backend.admin.controlador;

import org.mgroko.backend.admin.dto.AdminPublicacionResponse;
import org.mgroko.backend.admin.dto.ModerarPublicacionRequest;
import org.mgroko.backend.admin.servicio.AdminPublicacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-69: moderación administrativa de publicaciones.
 *
 * <p>Solo accesible para usuarios con rol global Administrador. No requiere un
 * perfil activo.</p>
 */
@RestController
@RequestMapping("/admin/publicaciones")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminPublicacionController {

    private final AdminPublicacionService adminPublicacionService;

    public AdminPublicacionController(AdminPublicacionService adminPublicacionService) {
        this.adminPublicacionService = adminPublicacionService;
    }

    @PatchMapping("/{idPublicacion}/estado")
    public ResponseEntity<AdminPublicacionResponse> moderar(
            @PathVariable Long idPublicacion,
            @Valid @RequestBody ModerarPublicacionRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(adminPublicacionService.moderar(
                idPublicacion, idUsuario(authentication), request));
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }
}
