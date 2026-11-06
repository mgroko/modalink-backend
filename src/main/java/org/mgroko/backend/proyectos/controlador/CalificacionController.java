package org.mgroko.backend.proyectos.controlador;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.CalificacionesProyectoResponse;
import org.mgroko.backend.proyectos.dto.CalificarEquipoRequest;
import org.mgroko.backend.proyectos.servicio.CalificarEquipoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-37: calificaciones entre los participantes activos de un proyecto
 * finalizado.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class CalificacionController {

    private final CalificarEquipoService calificarEquipoService;

    public CalificacionController(
            CalificarEquipoService calificarEquipoService) {
        this.calificarEquipoService = calificarEquipoService;
    }

    @GetMapping("/proyectos/{idProyecto}/calificaciones")
    public ResponseEntity<CalificacionesProyectoResponse> obtener(
            @PathVariable Long idProyecto,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        Long idPerfilActivo = idPerfilActivo(authentication);

        return ResponseEntity.ok(calificarEquipoService.obtener(
                idUsuario, idPerfilActivo, idProyecto));
    }

    @PostMapping("/proyectos/{idProyecto}/calificaciones")
    public ResponseEntity<CalificacionesProyectoResponse> calificar(
            @PathVariable Long idProyecto,
            @Valid @RequestBody CalificarEquipoRequest request,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        Long idPerfilActivo = idPerfilActivo(authentication);

        return ResponseEntity.ok(calificarEquipoService.calificar(
                idUsuario, idPerfilActivo, idProyecto, request));
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
