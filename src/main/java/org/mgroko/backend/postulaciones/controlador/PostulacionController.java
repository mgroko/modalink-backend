package org.mgroko.backend.postulaciones.controlador;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.postulaciones.dto.CrearPostulacionActividadRequest;
import org.mgroko.backend.postulaciones.dto.CrearPostulacionGralRequest;
import org.mgroko.backend.postulaciones.dto.PostulacionResponse;
import org.mgroko.backend.postulaciones.servicio.CrearPostulacionService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-26: dar de alta una postulación a un proyecto.
 */
@RestController
public class PostulacionController {

    private final CrearPostulacionService crearPostulacionService;

    public PostulacionController(CrearPostulacionService crearPostulacionService) {
        this.crearPostulacionService = crearPostulacionService;
    }

    @PostMapping("/proyectos/{idProyecto}/postulaciones/actividad")
    public ResponseEntity<PostulacionResponse> postularAActividad(
            @PathVariable Long idProyecto,
            @Valid @RequestBody CrearPostulacionActividadRequest request,
            Authentication authentication) {

        PostulacionResponse response = crearPostulacionService
                .darDeAltaPostulacionActividad(
                        idUsuario(authentication),
                        idPerfilActivo(authentication),
                        idProyecto,
                        request.idRequerimientoActividad());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/proyectos/{idProyecto}/postulaciones/general")
    public ResponseEntity<PostulacionResponse> postularARequerimientoGeneral(
            @PathVariable Long idProyecto,
            @Valid @RequestBody CrearPostulacionGralRequest request,
            Authentication authentication) {

        PostulacionResponse response = crearPostulacionService
                .darDeAltaPostulacionGeneral(
                        idUsuario(authentication),
                        idPerfilActivo(authentication),
                        idProyecto,
                        request.idRequerimientoGral());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }

    private Long idPerfilActivo(Authentication authentication) {
        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }
        return idPerfilActivo;
    }
}
