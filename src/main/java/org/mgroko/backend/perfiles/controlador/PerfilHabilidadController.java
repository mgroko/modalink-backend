package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.mgroko.backend.perfiles.dto.AgregarHabilidadPerfilRequest;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.perfiles.servicio.GestionHabilidadPerfilService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-47 — Gestionar habilidades desde el perfil.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
@RequestMapping("/perfiles/{idPerfil}/habilidades")
public class PerfilHabilidadController {

    private final GestionHabilidadPerfilService gestionHabilidadPerfilService;

    public PerfilHabilidadController(
            GestionHabilidadPerfilService gestionHabilidadPerfilService) {
        this.gestionHabilidadPerfilService = gestionHabilidadPerfilService;
    }

    @GetMapping
    public ResponseEntity<List<HabilidadResponse>> listar(
            @PathVariable Long idPerfil,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        return ResponseEntity.ok(
                gestionHabilidadPerfilService.listar(idUsuario, idPerfil));
    }

    @PostMapping
    public ResponseEntity<HabilidadResponse> agregar(
            @PathVariable Long idPerfil,
            @Valid @RequestBody AgregarHabilidadPerfilRequest request,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gestionHabilidadPerfilService.agregar(
                        idUsuario, idPerfil, request.idHabilidad()));
    }

    @DeleteMapping("/{idHabilidad}")
    public ResponseEntity<Void> quitar(
            @PathVariable Long idPerfil,
            @PathVariable Long idHabilidad,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        gestionHabilidadPerfilService.quitar(idUsuario, idPerfil, idHabilidad);
        return ResponseEntity.noContent().build();
    }
}
