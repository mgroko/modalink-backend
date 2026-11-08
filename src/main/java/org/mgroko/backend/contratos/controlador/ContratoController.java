package org.mgroko.backend.contratos.controlador;

import java.util.List;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.contratos.dto.BuscarContratosFiltro;
import org.mgroko.backend.contratos.dto.ContratoBusquedaResponse;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.dto.ContratoEventoResponse;
import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.GenerarContratoRequest;
import org.mgroko.backend.contratos.dto.ModificarContratoRequest;
import org.mgroko.backend.contratos.dto.OpcionesGeneracionContratoResponse;
import org.mgroko.backend.contratos.dto.PrevisualizacionContratoResponse;
import org.mgroko.backend.contratos.dto.PrevisualizacionModificacionResponse;
import org.mgroko.backend.contratos.dto.ProyectoFiltroContratoResponse;
import org.mgroko.backend.contratos.dto.RechazarContratoRequest;
import org.mgroko.backend.contratos.servicio.BuscarContratoService;
import org.mgroko.backend.contratos.servicio.EliminarContratoService;
import org.mgroko.backend.contratos.servicio.FirmarContratoService;
import org.mgroko.backend.contratos.servicio.GenerarContratoService;
import org.mgroko.backend.contratos.servicio.ListarEventosContratoService;
import org.mgroko.backend.contratos.servicio.ListarProyectosContratoService;
import org.mgroko.backend.contratos.servicio.ModificarContratoService;
import org.mgroko.backend.contratos.servicio.ObtenerContratoService;
import org.mgroko.backend.contratos.servicio.ObtenerOpcionesContratoService;
import org.mgroko.backend.contratos.servicio.PrevisualizarContratoService;
import org.mgroko.backend.contratos.servicio.PrevisualizarModificacionContratoService;
import org.mgroko.backend.contratos.servicio.RechazarContratoService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * UC-39: generar contrato.
 *
 * <p>Los endpoints cuelgan de /proyectos/{idProyecto}/contratos, por lo que
 * quedan cubiertos por la regla existente de SecurityConfig para
 * /proyectos/** con rol USUARIO.</p>
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class ContratoController {

    private final ObtenerOpcionesContratoService opcionesService;
    private final PrevisualizarContratoService previsualizarService;
    private final GenerarContratoService generarService;
    private final BuscarContratoService buscarContratoService;
    private final ListarEventosContratoService listarEventosContratoService;
    private final ListarProyectosContratoService listarProyectosService;
    private final ObtenerContratoService obtenerContratoService;
    private final PrevisualizarModificacionContratoService previsualizarModificacionService;
    private final ModificarContratoService modificarContratoService;
    private final FirmarContratoService firmarContratoService;
    private final RechazarContratoService rechazarContratoService;
    private final EliminarContratoService eliminarContratoService;

    public ContratoController(
            ObtenerOpcionesContratoService opcionesService,
            PrevisualizarContratoService previsualizarService,
            GenerarContratoService generarService,
            BuscarContratoService buscarContratoService,
            ListarEventosContratoService listarEventosContratoService,
            ListarProyectosContratoService listarProyectosService,
            ObtenerContratoService obtenerContratoService,
            PrevisualizarModificacionContratoService previsualizarModificacionService,
            ModificarContratoService modificarContratoService,
            FirmarContratoService firmarContratoService,
            RechazarContratoService rechazarContratoService,
            EliminarContratoService eliminarContratoService) {
        this.opcionesService = opcionesService;
        this.previsualizarService = previsualizarService;
        this.generarService = generarService;
        this.buscarContratoService = buscarContratoService;
        this.listarEventosContratoService = listarEventosContratoService;
        this.listarProyectosService = listarProyectosService;
        this.obtenerContratoService = obtenerContratoService;
        this.previsualizarModificacionService = previsualizarModificacionService;
        this.modificarContratoService = modificarContratoService;
        this.firmarContratoService = firmarContratoService;
        this.rechazarContratoService = rechazarContratoService;
        this.eliminarContratoService = eliminarContratoService;
    }

    @GetMapping("/proyectos/{idProyecto}/contratos/opciones")
    public ResponseEntity<OpcionesGeneracionContratoResponse> opciones(
            @PathVariable Long idProyecto,
            Authentication authentication) {

        return ResponseEntity.ok(opcionesService.obtener(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto));
    }

    @PostMapping("/proyectos/{idProyecto}/contratos/previsualizar")
    public ResponseEntity<PrevisualizacionContratoResponse> previsualizar(
            @PathVariable Long idProyecto,
            @Valid @RequestBody GenerarContratoRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(previsualizarService.previsualizar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                request));
    }

    @PostMapping("/proyectos/{idProyecto}/contratos")
    public ResponseEntity<ContratoGeneradoResponse> generar(
            @PathVariable Long idProyecto,
            @Valid @RequestBody GenerarContratoRequest request,
            Authentication authentication) {

        ContratoGeneradoResponse response = generarService.generar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/contratos/buscar")
    public ResponseEntity<PaginaResponse<ContratoBusquedaResponse>> buscar(
            @ModelAttribute BuscarContratosFiltro filtro,
            @RequestParam(defaultValue = "false") boolean miFirmaPendiente,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        return ResponseEntity.ok(buscarContratoService.buscar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                filtro,
                page,
                size,
                miFirmaPendiente));
    }

    /**
     * Opciones del filtro "Proyecto": solo proyectos con contratos visibles
     * para el perfil activo, incluidos los históricos donde fue parte firmante.
     */
    @GetMapping("/contratos/proyectos")
    public ResponseEntity<List<ProyectoFiltroContratoResponse>> proyectos(
            Authentication authentication) {

        return ResponseEntity.ok(listarProyectosService.listar(
                idUsuario(authentication),
                idPerfilActivo(authentication)));
    }

    @GetMapping("/proyectos/{idProyecto}/contratos/{idContrato}/eventos")
    public ResponseEntity<List<ContratoEventoResponse>> eventos(
            @PathVariable Long idProyecto,
            @PathVariable Long idContrato,
            Authentication authentication) {

        return ResponseEntity.ok(listarEventosContratoService.listar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idContrato));
    }

    @GetMapping("/proyectos/{idProyecto}/contratos/{idContrato}")
    public ResponseEntity<ContratoDetalleResponse> detalle(
            @PathVariable Long idProyecto,
            @PathVariable Long idContrato,
            Authentication authentication) {

        return ResponseEntity.ok(obtenerContratoService.obtener(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idContrato));
    }

    @PostMapping("/proyectos/{idProyecto}/contratos/{idContrato}/previsualizar")
    public ResponseEntity<PrevisualizacionModificacionResponse> previsualizarModificacion(
            @PathVariable Long idProyecto,
            @PathVariable Long idContrato,
            @Valid @RequestBody ModificarContratoRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(previsualizarModificacionService.previsualizar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idContrato,
                request));
    }

    @PutMapping("/proyectos/{idProyecto}/contratos/{idContrato}")
    public ResponseEntity<ContratoDetalleResponse> modificar(
            @PathVariable Long idProyecto,
            @PathVariable Long idContrato,
            @Valid @RequestBody ModificarContratoRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(modificarContratoService.modificar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idContrato,
                request));
    }

    @DeleteMapping("/proyectos/{idProyecto}/contratos/{idContrato}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long idProyecto,
            @PathVariable Long idContrato,
            Authentication authentication) {

        eliminarContratoService.eliminar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idContrato);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/proyectos/{idProyecto}/contratos/{idContrato}/firmar")
    public ResponseEntity<ContratoDetalleResponse> firmar(
            @PathVariable Long idProyecto,
            @PathVariable Long idContrato,
            Authentication authentication) {

        return ResponseEntity.ok(firmarContratoService.firmar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idContrato));
    }

    @PostMapping("/proyectos/{idProyecto}/contratos/{idContrato}/rechazar")
    public ResponseEntity<ContratoDetalleResponse> rechazar(
            @PathVariable Long idProyecto,
            @PathVariable Long idContrato,
            @Valid @RequestBody RechazarContratoRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(rechazarContratoService.rechazar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idContrato,
                request));
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
