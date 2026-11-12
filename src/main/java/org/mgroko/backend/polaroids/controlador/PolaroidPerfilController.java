package org.mgroko.backend.polaroids.controlador;

import java.util.List;

import org.mgroko.backend.polaroids.dto.CrearPolaroidRequest;
import org.mgroko.backend.polaroids.dto.GrupoPolaroidResponse;
import org.mgroko.backend.polaroids.dto.ModificarPolaroidRequest;
import org.mgroko.backend.polaroids.dto.PolaroidResponse;
import org.mgroko.backend.polaroids.dto.PolaroidsPerfilResponse;
import org.mgroko.backend.polaroids.dto.ReordenarPolaroidsRequest;
import org.mgroko.backend.polaroids.servicio.CrearPolaroidPerfilService;
import org.mgroko.backend.polaroids.servicio.EliminarPolaroidPerfilService;
import org.mgroko.backend.polaroids.servicio.ListarPolaroidsPerfilService;
import org.mgroko.backend.polaroids.servicio.ModificarPolaroidPerfilService;
import org.mgroko.backend.polaroids.servicio.PolaroidCatalogoService;
import org.mgroko.backend.polaroids.servicio.ReordenarPolaroidsPerfilService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Catálogo y gestión de polaroids de perfiles de modelo.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class PolaroidPerfilController {

    private final PolaroidCatalogoService polaroidCatalogoService;
    private final ListarPolaroidsPerfilService listarPolaroidsPerfilService;
    private final CrearPolaroidPerfilService crearPolaroidPerfilService;
    private final ModificarPolaroidPerfilService modificarPolaroidPerfilService;
    private final ReordenarPolaroidsPerfilService reordenarPolaroidsPerfilService;
    private final EliminarPolaroidPerfilService eliminarPolaroidPerfilService;

    public PolaroidPerfilController(
            PolaroidCatalogoService polaroidCatalogoService,
            ListarPolaroidsPerfilService listarPolaroidsPerfilService,
            CrearPolaroidPerfilService crearPolaroidPerfilService,
            ModificarPolaroidPerfilService modificarPolaroidPerfilService,
            ReordenarPolaroidsPerfilService reordenarPolaroidsPerfilService,
            EliminarPolaroidPerfilService eliminarPolaroidPerfilService) {
        this.polaroidCatalogoService = polaroidCatalogoService;
        this.listarPolaroidsPerfilService = listarPolaroidsPerfilService;
        this.crearPolaroidPerfilService = crearPolaroidPerfilService;
        this.modificarPolaroidPerfilService = modificarPolaroidPerfilService;
        this.reordenarPolaroidsPerfilService = reordenarPolaroidsPerfilService;
        this.eliminarPolaroidPerfilService = eliminarPolaroidPerfilService;
    }

    @GetMapping("/polaroids/tipos")
    public ResponseEntity<List<GrupoPolaroidResponse>> listarTipos() {
        return ResponseEntity.ok(polaroidCatalogoService.listar());
    }

    @GetMapping("/perfiles/{idPerfil}/polaroids")
    public ResponseEntity<PolaroidsPerfilResponse> listar(
            @PathVariable Long idPerfil,
            Authentication authentication) {
        return ResponseEntity.ok(listarPolaroidsPerfilService.listar(
                idPerfil, idUsuario(authentication)));
    }

    @PostMapping(
            value = "/perfiles/{idPerfil}/polaroids",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PolaroidResponse> crear(
            @PathVariable Long idPerfil,
            @Valid @ModelAttribute CrearPolaroidRequest request,
            Authentication authentication) {

        PolaroidResponse response = crearPolaroidPerfilService.crear(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idPerfil,
                request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(
            value = "/perfiles/{idPerfil}/polaroids/{idPolaroid}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PolaroidResponse> modificar(
            @PathVariable Long idPerfil,
            @PathVariable Long idPolaroid,
            @Valid @ModelAttribute ModificarPolaroidRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(modificarPolaroidPerfilService.modificar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idPerfil,
                idPolaroid,
                request));
    }

    @PutMapping("/perfiles/{idPerfil}/polaroids/orden")
    public ResponseEntity<PolaroidsPerfilResponse> reordenar(
            @PathVariable Long idPerfil,
            @Valid @RequestBody ReordenarPolaroidsRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(reordenarPolaroidsPerfilService.reordenar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idPerfil,
                request));
    }

    @DeleteMapping("/perfiles/{idPerfil}/polaroids/{idPolaroid}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long idPerfil,
            @PathVariable Long idPolaroid,
            Authentication authentication) {

        eliminarPolaroidPerfilService.eliminar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idPerfil,
                idPolaroid);

        return ResponseEntity.noContent().build();
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }

    private Long idPerfilActivo(Authentication authentication) {
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            return contexto.idPerfilActivo();
        }
        return null;
    }
}
