package org.mgroko.backend.proyectos.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro;
import org.mgroko.backend.proyectos.dto.BuscarProyectosPropiosFiltro;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.ModificarProyectoRequest;
import org.mgroko.backend.proyectos.dto.ProyectoBusquedaResponse;
import org.mgroko.backend.proyectos.dto.ProyectoPropioResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.servicio.BuscarProyectoService;
import org.mgroko.backend.proyectos.servicio.BuscarProyectosPropiosService;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.ModificarProyectoService;
import org.mgroko.backend.proyectos.servicio.ObtenerProyectoService;
import org.mgroko.backend.proyectos.servicio.PublicarProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
public class ProyectoController {

    private final CrearProyectoService crearProyectoService;
    private final ObtenerProyectoService obtenerProyectoService;
    private final BuscarProyectoService buscarProyectoService;
    private final BuscarProyectosPropiosService buscarProyectosPropiosService;
    private final PublicarProyectoService publicarProyectoService;
    private final ModificarProyectoService modificarProyectoService;

    public ProyectoController(CrearProyectoService crearProyectoService,
            ObtenerProyectoService obtenerProyectoService,
            BuscarProyectoService buscarProyectoService,
            BuscarProyectosPropiosService buscarProyectosPropiosService,
            PublicarProyectoService publicarProyectoService,
            ModificarProyectoService modificarProyectoService) {
        this.crearProyectoService = crearProyectoService;
        this.obtenerProyectoService = obtenerProyectoService;
        this.buscarProyectoService = buscarProyectoService;
        this.buscarProyectosPropiosService = buscarProyectosPropiosService;
        this.publicarProyectoService = publicarProyectoService;
        this.modificarProyectoService = modificarProyectoService;
    }

    // UC-24: Crear proyecto
    @PostMapping("/proyectos")
    public ResponseEntity<ProyectoResponse> crear(
            @Valid @RequestBody CrearProyectoRequest request,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        ProyectoResponse response = crearProyectoService.crear(idUsuario, idPerfilActivo, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // UC-29 - Buscar proyectos con criterios de filtrado y paginación parametrizable
    @GetMapping("/proyectos/buscar")
    public ResponseEntity<PaginaResponse<ProyectoBusquedaResponse>> buscar(
            @ModelAttribute BuscarProyectosFiltro filtro,
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
                buscarProyectoService.buscarProyectos(idPerfilActivo, filtro, page, size, todos));
    }

    // UC-29 bis - Buscar proyectos propios: proyectos asociados al perfil activo
    // como director o miembro (catálogo "Proyectos / Inicio")
    @GetMapping("/proyectos/propios")
    public ResponseEntity<PaginaResponse<ProyectoPropioResponse>> buscarPropios(
            @ModelAttribute BuscarProyectosPropiosFiltro filtro,
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
                buscarProyectosPropiosService.buscar(idPerfilActivo, filtro, page, size, todos));
    }

    // Dashboard de proyecto: detalle para la vista /proyectos/{id}
    @GetMapping("/proyectos/{idProyecto}")
    public ResponseEntity<ProyectoResponse> obtener(
            @PathVariable Long idProyecto,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        ProyectoResponse response = obtenerProyectoService.obtener(idUsuario, idPerfilActivo, idProyecto);
        return ResponseEntity.ok(response);
    }

    // UC-25: Publicar proyecto (Borrador -> Publicado)
    @PostMapping("/proyectos/{idProyecto}/publicar")
    public ResponseEntity<ProyectoResponse> publicar(
            @PathVariable Long idProyecto,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        ProyectoResponse response = publicarProyectoService.publicar(idUsuario, idPerfilActivo, idProyecto);
        return ResponseEntity.ok(response);
    }

    // UC-36: Modificar proyecto
    @PutMapping("/proyectos/{idProyecto}")
    public ResponseEntity<ProyectoResponse> modificar(
            @PathVariable Long idProyecto,
            @Valid @RequestBody ModificarProyectoRequest request,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        ProyectoResponse response = modificarProyectoService.modificar(
                idUsuario, idPerfilActivo, idProyecto, request);
        return ResponseEntity.ok(response);
    }
}
