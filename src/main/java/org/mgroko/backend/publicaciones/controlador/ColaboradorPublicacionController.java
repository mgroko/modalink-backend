package org.mgroko.backend.publicaciones.controlador;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.publicaciones.dto.AgregarColaboradorPropioRequest;
import org.mgroko.backend.publicaciones.servicio.AgregarColaboradorPropioService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Etiquetado de perfiles propios como colaboradores.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class ColaboradorPublicacionController {

    private final AgregarColaboradorPropioService agregarColaboradorPropioService;

    public ColaboradorPublicacionController(
            AgregarColaboradorPropioService agregarColaboradorPropioService) {
        this.agregarColaboradorPropioService = agregarColaboradorPropioService;
    }

    /**
     * Agrega un perfil del mismo usuario como colaborador de una publicación
     * propia, sin solicitud ni conexión.
     */
    @PostMapping("/publicaciones/{idPublicacion}/colaboradores/propios")
    public ResponseEntity<Void> agregarPropio(
            @PathVariable Long idPublicacion,
            @Valid @RequestBody AgregarColaboradorPropioRequest request,
            Authentication authentication) {

        agregarColaboradorPropioService.agregar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idPublicacion,
                request.idPerfil());

        return ResponseEntity.noContent().build();
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
