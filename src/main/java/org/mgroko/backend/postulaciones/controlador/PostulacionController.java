package org.mgroko.backend.postulaciones.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.postulaciones.dto.BuscarPostulacionesFiltro;
import org.mgroko.backend.postulaciones.dto.CrearPostulacionActividadRequest;
import org.mgroko.backend.postulaciones.dto.CrearPostulacionGralRequest;
import org.mgroko.backend.postulaciones.dto.PostulacionBusquedaResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionGestionadaResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionResponse;
import org.mgroko.backend.postulaciones.servicio.AceptarPostulacionService;
import org.mgroko.backend.postulaciones.servicio.BuscarPostulacionesProyectoService;
import org.mgroko.backend.postulaciones.servicio.CrearPostulacionService;
import org.mgroko.backend.postulaciones.servicio.RechazarPostulacionService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-26: dar de alta una postulación a un proyecto.
 */
@RestController
public class PostulacionController {

    private final CrearPostulacionService crearPostulacionService;
    private final BuscarPostulacionesProyectoService buscarPostulacionesProyectoService;
    private final AceptarPostulacionService aceptarPostulacionService;
    private final RechazarPostulacionService rechazarPostulacionService;

    public PostulacionController(
            CrearPostulacionService crearPostulacionService,
            BuscarPostulacionesProyectoService buscarPostulacionesProyectoService,
            AceptarPostulacionService aceptarPostulacionService,
            RechazarPostulacionService rechazarPostulacionService) {
        this.crearPostulacionService = crearPostulacionService;
        this.buscarPostulacionesProyectoService = buscarPostulacionesProyectoService;
        this.aceptarPostulacionService = aceptarPostulacionService;
        this.rechazarPostulacionService = rechazarPostulacionService;
    }

    @PostMapping("/proyectos/{idProyecto}/postulaciones/general/{idPostulacion}/aceptar")
    public ResponseEntity<PostulacionGestionadaResponse> aceptarGeneral(
            @PathVariable Long idProyecto,
            @PathVariable Long idPostulacion,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        return ResponseEntity.ok(aceptarPostulacionService.aceptarGeneral(
                idUsuario, idPerfilActivo(authentication), idProyecto, idPostulacion));
    }

    @PostMapping("/proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/aceptar")
    public ResponseEntity<PostulacionGestionadaResponse> aceptarActividad(
            @PathVariable Long idProyecto,
            @PathVariable Long idPostulacion,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        return ResponseEntity.ok(aceptarPostulacionService.aceptarActividad(
                idUsuario, idPerfilActivo(authentication), idProyecto, idPostulacion));
    }

    @PostMapping("/proyectos/{idProyecto}/postulaciones/general/{idPostulacion}/rechazar")
    public ResponseEntity<PostulacionGestionadaResponse> rechazarGeneral(
            @PathVariable Long idProyecto,
            @PathVariable Long idPostulacion,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        return ResponseEntity.ok(rechazarPostulacionService.rechazarGeneral(
                idUsuario, idPerfilActivo(authentication), idProyecto, idPostulacion));
    }

    @PostMapping("/proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/rechazar")
    public ResponseEntity<PostulacionGestionadaResponse> rechazarActividad(
            @PathVariable Long idProyecto,
            @PathVariable Long idPostulacion,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        return ResponseEntity.ok(rechazarPostulacionService.rechazarActividad(
                idUsuario, idPerfilActivo(authentication), idProyecto, idPostulacion));
    }

    @GetMapping("/proyectos/{idProyecto}/postulaciones/buscar")
    public ResponseEntity<PaginaResponse<PostulacionBusquedaResponse>> buscar(
            @PathVariable Long idProyecto,
            @ModelAttribute BuscarPostulacionesFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean todos,
            Authentication authentication) {

        return ResponseEntity.ok(buscarPostulacionesProyectoService.buscar(
                idPerfilActivo(authentication), idProyecto, filtro, page, size, todos));
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
