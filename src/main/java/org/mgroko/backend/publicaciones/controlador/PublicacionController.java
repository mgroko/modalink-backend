package org.mgroko.backend.publicaciones.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.publicaciones.dto.CrearPublicacionRequest;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.servicio.CrearPublicacionService;
import org.mgroko.backend.publicaciones.servicio.ListarPublicacionesPerfilService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-19 - Alta de publicación y lectura mínima del portfolio.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class PublicacionController {

    private final CrearPublicacionService crearPublicacionService;
    private final ListarPublicacionesPerfilService listarPublicacionesPerfilService;

    public PublicacionController(
            CrearPublicacionService crearPublicacionService,
            ListarPublicacionesPerfilService listarPublicacionesPerfilService) {
        this.crearPublicacionService = crearPublicacionService;
        this.listarPublicacionesPerfilService = listarPublicacionesPerfilService;
    }

    @PostMapping(value = "/publicaciones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicacionResponse> crear(
            @Valid @ModelAttribute CrearPublicacionRequest request,
            Authentication authentication) {

        Long idPerfilActivo = idPerfilActivoRequerido(authentication);
        PublicacionResponse response = crearPublicacionService.crear(
                idUsuario(authentication), idPerfilActivo, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/perfiles/{idPerfil}/publicaciones")
    public ResponseEntity<PaginaResponse<PublicacionResponse>> listar(
            @PathVariable Long idPerfil,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        PaginaResponse<PublicacionResponse> response = listarPublicacionesPerfilService.listar(
                idPerfil,
                idUsuario(authentication),
                idPerfilActivo(authentication),
                page,
                size);

        return ResponseEntity.ok(response);
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }

    private Long idPerfilActivoRequerido(Authentication authentication) {
        Long idPerfilActivo = idPerfilActivo(authentication);
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }
        return idPerfilActivo;
    }

    private Long idPerfilActivo(Authentication authentication) {
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            return contexto.idPerfilActivo();
        }
        return null;
    }
}
