package org.mgroko.backend.invitaciones.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.invitaciones.dto.BuscarInvitacionesFiltro;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionActividadRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionGralRequest;
import org.mgroko.backend.invitaciones.dto.InvitacionBusquedaResponse;
import org.mgroko.backend.invitaciones.dto.InvitacionResponse;
import org.mgroko.backend.invitaciones.servicio.BuscarInvitacionService;
import org.mgroko.backend.invitaciones.servicio.CrearInvitacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
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
 * UC-30: buscar invitaciones.
 * UC-31: invitar a un proyecto de forma general o por actividad.
 */
@RestController
public class InvitacionController {

    private final CrearInvitacionService crearInvitacionService;
    private final BuscarInvitacionService buscarInvitacionService;

    public InvitacionController(
            CrearInvitacionService crearInvitacionService,
            BuscarInvitacionService buscarInvitacionService) {
        this.crearInvitacionService = crearInvitacionService;
        this.buscarInvitacionService = buscarInvitacionService;
    }

    // UC-30: invitaciones recibidas o enviadas por el perfil activo.
    @GetMapping("/invitaciones/buscar")
    public ResponseEntity<PaginaResponse<InvitacionBusquedaResponse>> buscar(
            @ModelAttribute BuscarInvitacionesFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean todos,
            Authentication authentication) {

        return ResponseEntity.ok(buscarInvitacionService.buscar(
                idPerfilActivo(authentication), filtro, page, size, todos));
    }

    @PostMapping("/proyectos/{idProyecto}/invitaciones/general")
    public ResponseEntity<InvitacionResponse> invitarGeneral(
            @PathVariable Long idProyecto,
            @Valid @RequestBody CrearInvitacionGralRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                crearInvitacionService.invitarAProyecto(
                        idUsuario(authentication),
                        idPerfilActivo(authentication),
                        idProyecto,
                        request));
    }

    @PostMapping("/proyectos/{idProyecto}/invitaciones/actividad")
    public ResponseEntity<InvitacionResponse> invitarActividad(
            @PathVariable Long idProyecto,
            @Valid @RequestBody CrearInvitacionActividadRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                crearInvitacionService.invitarAProyectoActividad(
                        idUsuario(authentication),
                        idPerfilActivo(authentication),
                        idProyecto,
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
