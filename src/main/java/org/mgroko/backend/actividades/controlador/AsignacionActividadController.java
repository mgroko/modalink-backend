package org.mgroko.backend.actividades.controlador;

import org.mgroko.backend.actividades.dto.AsignacionActividadResponse;
import org.mgroko.backend.actividades.dto.AsignarActividadRequest;
import org.mgroko.backend.actividades.servicio.AsignarActividadService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
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
 * UC-70: asignar una actividad a un miembro del proyecto.
 */
@RestController
public class AsignacionActividadController {

    private final AsignarActividadService asignarActividadService;

    public AsignacionActividadController(
            AsignarActividadService asignarActividadService) {
        this.asignarActividadService = asignarActividadService;
    }

    @PostMapping("/proyectos/{idProyecto}/actividades/{idActividad}/asignaciones")
    public ResponseEntity<AsignacionActividadResponse> asignar(
            @PathVariable Long idProyecto,
            @PathVariable Long idActividad,
            @Valid @RequestBody AsignarActividadRequest request,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        AsignacionActividadResponse response = asignarActividadService.asignar(
                idUsuario, idPerfilActivo, idProyecto, idActividad, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
