package org.mgroko.backend.actividades.controlador;

import java.util.List;

import org.mgroko.backend.actividades.dto.ActividadBusquedaResponse;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.BuscarActividadesFiltro;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.dto.ModificarActividadRequest;
import org.mgroko.backend.actividades.servicio.BuscarActividadService;
import org.mgroko.backend.actividades.servicio.CrearActividadService;
import org.mgroko.backend.actividades.servicio.EliminarActividadService;
import org.mgroko.backend.actividades.servicio.ModificarActividadService;
import org.mgroko.backend.actividades.servicio.ObtenerActividadService;
import org.mgroko.backend.actividades.servicio.ObtenerDisponibilidadActividadService;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class ActividadController {

    private final BuscarActividadService buscarActividadService;
    private final CrearActividadService crearActividadService;
    private final ModificarActividadService modificarActividadService;
    private final ObtenerActividadService obtenerActividadService;
    private final ObtenerDisponibilidadActividadService obtenerDisponibilidadActividadService;
    private final EliminarActividadService eliminarActividadService;

    public ActividadController(
            BuscarActividadService buscarActividadService,
            CrearActividadService crearActividadService,
            ModificarActividadService modificarActividadService,
            ObtenerActividadService obtenerActividadService,
            ObtenerDisponibilidadActividadService obtenerDisponibilidadActividadService,
            EliminarActividadService eliminarActividadService) {
        this.buscarActividadService = buscarActividadService;
        this.crearActividadService = crearActividadService;
        this.modificarActividadService = modificarActividadService;
        this.obtenerActividadService = obtenerActividadService;
        this.obtenerDisponibilidadActividadService = obtenerDisponibilidadActividadService;
        this.eliminarActividadService = eliminarActividadService;
    }

    @PostMapping("/proyectos/{idProyecto}/actividades")
    public ResponseEntity<ActividadResponse> crear(
            @PathVariable Long idProyecto,
            @Valid @RequestBody CrearActividadRequest request,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        ActividadResponse response = crearActividadService.crear(
                idUsuario, idPerfilActivo, idProyecto, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/proyectos/{idProyecto}/actividades/{idActividad}")
    public ResponseEntity<ActividadResponse> modificar(
            @PathVariable Long idProyecto,
            @PathVariable Long idActividad,
            @Valid @RequestBody ModificarActividadRequest request,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        ActividadResponse response = modificarActividadService.modificar(
                idUsuario, idPerfilActivo, idProyecto, idActividad, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/proyectos/{idProyecto}/actividades/{idActividad}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long idProyecto,
            @PathVariable Long idActividad,
            @RequestParam String motivo,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        eliminarActividadService.eliminar(
                idUsuario, idPerfilActivo, idProyecto, idActividad, motivo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/proyectos/{idProyecto}/actividades/{idActividad}")
    public ResponseEntity<ActividadResponse> obtener(
            @PathVariable Long idProyecto,
            @PathVariable Long idActividad,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        ActividadResponse response = obtenerActividadService.obtener(
                idUsuario, idPerfilActivo, idProyecto, idActividad);
        return ResponseEntity.ok(response);
    }

    // UC-26/UC-70: Consultar la disponibilidad de uno o más perfiles para una actividad.
    @GetMapping("/proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad")
    public ResponseEntity<ResultadoDisponibilidad> obtenerDisponibilidad(
            @PathVariable Long idProyecto,
            @PathVariable Long idActividad,
            @RequestParam(required = false) List<Long> idPerfil,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        return ResponseEntity.ok(obtenerDisponibilidadActividadService.obtener(
                idUsuario, idPerfilActivo, idProyecto, idActividad, idPerfil));
    }

    // UC-52: Buscar actividades dentro de la planificación de un proyecto
    @GetMapping("/proyectos/{idProyecto}/actividades/buscar")
    public ResponseEntity<PaginaResponse<ActividadBusquedaResponse>> buscar(
            @PathVariable Long idProyecto,
            @ModelAttribute BuscarActividadesFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean todos,
            Authentication authentication) {

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        return ResponseEntity.ok(
                buscarActividadService.buscar(idPerfilActivo, idProyecto, filtro, page, size, todos));
    }
}
