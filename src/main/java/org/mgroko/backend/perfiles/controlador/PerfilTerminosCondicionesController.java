package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.mgroko.backend.perfiles.dto.ActualizarTerminosCondicionesRequest;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesResponse;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesVersionResponse;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesVersionResumenResponse;
import org.mgroko.backend.perfiles.servicio.GestionTerminosCondicionesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-23 — Configurar términos y condiciones de un perfil.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
@RequestMapping("/perfiles/{idPerfil}/terminos-condiciones")
public class PerfilTerminosCondicionesController {

    private final GestionTerminosCondicionesService gestionTerminosCondicionesService;

    public PerfilTerminosCondicionesController(
            GestionTerminosCondicionesService gestionTerminosCondicionesService) {
        this.gestionTerminosCondicionesService = gestionTerminosCondicionesService;
    }

    @GetMapping
    public ResponseEntity<TerminosCondicionesResponse> obtener(
            @PathVariable Long idPerfil,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        return ResponseEntity.ok(
                gestionTerminosCondicionesService.obtener(idUsuario, idPerfil));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<TerminosCondicionesVersionResumenResponse>> historial(
            @PathVariable Long idPerfil,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        return ResponseEntity.ok(
                gestionTerminosCondicionesService.obtenerHistorial(idUsuario, idPerfil));
    }

    @GetMapping("/historial/{idTyc}")
    public ResponseEntity<TerminosCondicionesVersionResponse> version(
            @PathVariable Long idPerfil,
            @PathVariable Long idTyc,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        return ResponseEntity.ok(
                gestionTerminosCondicionesService.obtenerVersion(
                        idUsuario, idPerfil, idTyc));
    }

    @PutMapping
    public ResponseEntity<TerminosCondicionesResponse> actualizar(
            @PathVariable Long idPerfil,
            @Valid @RequestBody ActualizarTerminosCondicionesRequest request,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        return ResponseEntity.ok(
                gestionTerminosCondicionesService.actualizar(idUsuario, idPerfil, request));
    }
}
