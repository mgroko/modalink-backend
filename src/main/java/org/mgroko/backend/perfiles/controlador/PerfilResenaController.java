package org.mgroko.backend.perfiles.controlador;

import org.mgroko.backend.perfiles.dto.ResenaPerfilResponse;
import org.mgroko.backend.perfiles.dto.ResenasPerfilResponse;
import org.mgroko.backend.perfiles.servicio.ConsultarResenasPerfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reseñas recibidas por un perfil. Solo lectura: la creación de reseñas se
 * realiza al finalizar un proyecto (UC-37).
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class PerfilResenaController {

    private final ConsultarResenasPerfilService consultarResenasPerfilService;

    public PerfilResenaController(
            ConsultarResenasPerfilService consultarResenasPerfilService) {
        this.consultarResenasPerfilService = consultarResenasPerfilService;
    }

    @GetMapping("/perfiles/{idPerfil}/calificaciones")
    public ResponseEntity<ResenasPerfilResponse> listar(
            @PathVariable Long idPerfil,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        return ResponseEntity.ok(consultarResenasPerfilService.listar(
                idPerfil, idUsuario, page, size));
    }

    @GetMapping("/perfiles/{idPerfil}/calificaciones/{idCalificacion}")
    public ResponseEntity<ResenaPerfilResponse> obtener(
            @PathVariable Long idPerfil,
            @PathVariable Long idCalificacion,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        return ResponseEntity.ok(consultarResenasPerfilService.obtener(
                idPerfil, idCalificacion, idUsuario));
    }
}
