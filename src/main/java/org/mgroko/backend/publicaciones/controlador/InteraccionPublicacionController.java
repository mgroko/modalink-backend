package org.mgroko.backend.publicaciones.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.publicaciones.dto.ComentarioResponse;
import org.mgroko.backend.publicaciones.dto.CrearComentarioRequest;
import org.mgroko.backend.publicaciones.dto.MeGustaResponse;
import org.mgroko.backend.publicaciones.servicio.CrearComentarioService;
import org.mgroko.backend.publicaciones.servicio.GestionarMeGustaPublicacionService;
import org.mgroko.backend.publicaciones.servicio.ListarComentariosService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-66 - Interactuar con publicación: Me gusta y comentarios.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class InteraccionPublicacionController {

    private final GestionarMeGustaPublicacionService gestionarMeGustaService;
    private final CrearComentarioService crearComentarioService;
    private final ListarComentariosService listarComentariosService;

    public InteraccionPublicacionController(
            GestionarMeGustaPublicacionService gestionarMeGustaService,
            CrearComentarioService crearComentarioService,
            ListarComentariosService listarComentariosService) {
        this.gestionarMeGustaService = gestionarMeGustaService;
        this.crearComentarioService = crearComentarioService;
        this.listarComentariosService = listarComentariosService;
    }

    @PostMapping("/publicaciones/{idPublicacion}/me-gusta")
    public ResponseEntity<MeGustaResponse> darMeGusta(
            @PathVariable Long idPublicacion,
            Authentication authentication) {

        MeGustaResponse response = gestionarMeGustaService.dar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idPublicacion);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/publicaciones/{idPublicacion}/me-gusta")
    public ResponseEntity<MeGustaResponse> quitarMeGusta(
            @PathVariable Long idPublicacion,
            Authentication authentication) {

        MeGustaResponse response = gestionarMeGustaService.quitar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idPublicacion);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/publicaciones/{idPublicacion}/comentarios")
    public ResponseEntity<ComentarioResponse> comentar(
            @PathVariable Long idPublicacion,
            @Valid @RequestBody CrearComentarioRequest request,
            Authentication authentication) {

        ComentarioResponse response = crearComentarioService.crear(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idPublicacion,
                request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/publicaciones/{idPublicacion}/comentarios")
    public ResponseEntity<PaginaResponse<ComentarioResponse>> listarComentarios(
            @PathVariable Long idPublicacion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        PaginaResponse<ComentarioResponse> response = listarComentariosService.listar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idPublicacion,
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
