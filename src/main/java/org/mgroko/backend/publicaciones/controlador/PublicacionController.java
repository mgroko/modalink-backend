package org.mgroko.backend.publicaciones.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import java.util.List;

import org.mgroko.backend.publicaciones.dto.BuscarPublicacionesFiltro;
import org.mgroko.backend.publicaciones.dto.CrearPublicacionRequest;
import org.mgroko.backend.publicaciones.dto.ModificarPublicacionRequest;
import org.mgroko.backend.publicaciones.dto.ProyectoPublicacionFiltroResponse;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.servicio.CrearPublicacionService;
import org.mgroko.backend.publicaciones.servicio.ListarFeedPublicacionesService;
import org.mgroko.backend.publicaciones.servicio.ListarProyectosPublicacionesPerfilService;
import org.mgroko.backend.publicaciones.servicio.ListarPublicacionesPerfilService;
import org.mgroko.backend.publicaciones.servicio.ModificarPublicacionService;
import org.mgroko.backend.publicaciones.servicio.ObtenerPublicacionService;
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
import org.springframework.web.bind.annotation.PutMapping;
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
    private final ListarProyectosPublicacionesPerfilService listarProyectosPublicacionesPerfilService;
    private final ListarFeedPublicacionesService listarFeedPublicacionesService;
    private final ObtenerPublicacionService obtenerPublicacionService;
    private final ModificarPublicacionService modificarPublicacionService;

    public PublicacionController(
            CrearPublicacionService crearPublicacionService,
            ListarPublicacionesPerfilService listarPublicacionesPerfilService,
            ListarProyectosPublicacionesPerfilService listarProyectosPublicacionesPerfilService,
            ListarFeedPublicacionesService listarFeedPublicacionesService,
            ObtenerPublicacionService obtenerPublicacionService,
            ModificarPublicacionService modificarPublicacionService) {
        this.crearPublicacionService = crearPublicacionService;
        this.listarPublicacionesPerfilService = listarPublicacionesPerfilService;
        this.listarProyectosPublicacionesPerfilService = listarProyectosPublicacionesPerfilService;
        this.listarFeedPublicacionesService = listarFeedPublicacionesService;
        this.obtenerPublicacionService = obtenerPublicacionService;
        this.modificarPublicacionService = modificarPublicacionService;
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

    /**
     * UC-21, paso 2: detalle de una publicación propia para editar.
     */
    @GetMapping("/publicaciones/{idPublicacion}")
    public ResponseEntity<PublicacionResponse> obtener(
            @PathVariable Long idPublicacion,
            Authentication authentication) {

        PublicacionResponse response = obtenerPublicacionService.obtener(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idPublicacion);

        return ResponseEntity.ok(response);
    }

    /**
     * UC-21: modificación atómica de datos e imágenes de una publicación
     * propia.
     */
    @PutMapping(value = "/publicaciones/{idPublicacion}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicacionResponse> modificar(
            @PathVariable Long idPublicacion,
            @Valid @ModelAttribute ModificarPublicacionRequest request,
            Authentication authentication) {

        PublicacionResponse response = modificarPublicacionService.modificar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idPublicacion,
                request);

        return ResponseEntity.ok(response);
    }

    /**
     * Feed de publicaciones de los contactos activos del perfil. Incluye las
     * publicaciones propias por defecto.
     */
    @GetMapping("/publicaciones/feed")
    public ResponseEntity<PaginaResponse<PublicacionResponse>> feed(
            @RequestParam(defaultValue = "true") boolean incluirPropias,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        PaginaResponse<PublicacionResponse> response = listarFeedPublicacionesService.listar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                incluirPropias,
                page,
                size);

        return ResponseEntity.ok(response);
    }

    /**
     * Proyectos con publicaciones activas del perfil, para el selector de
     * filtros de UC-22.
     */
    @GetMapping("/perfiles/{idPerfil}/publicaciones/proyectos")
    public ResponseEntity<List<ProyectoPublicacionFiltroResponse>> listarProyectos(
            @PathVariable Long idPerfil,
            Authentication authentication) {

        List<ProyectoPublicacionFiltroResponse> response =
                listarProyectosPublicacionesPerfilService.listar(
                        idPerfil,
                        idUsuario(authentication),
                        idPerfilActivo(authentication));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/perfiles/{idPerfil}/publicaciones")
    public ResponseEntity<PaginaResponse<PublicacionResponse>> listar(
            @PathVariable Long idPerfil,
            @ModelAttribute BuscarPublicacionesFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        PaginaResponse<PublicacionResponse> response = listarPublicacionesPerfilService.listar(
                idPerfil,
                idUsuario(authentication),
                idPerfilActivo(authentication),
                filtro,
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
