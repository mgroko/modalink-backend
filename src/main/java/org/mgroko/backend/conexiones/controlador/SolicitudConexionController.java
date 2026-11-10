package org.mgroko.backend.conexiones.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.conexiones.dto.BuscarSolicitudesConexionFiltro;
import org.mgroko.backend.conexiones.dto.EnviarSolicitudConexionRequest;
import org.mgroko.backend.conexiones.dto.SolicitudConexionBusquedaResponse;
import org.mgroko.backend.conexiones.dto.SolicitudConexionGestionadaResponse;
import org.mgroko.backend.conexiones.dto.SolicitudConexionResponse;
import org.mgroko.backend.conexiones.servicio.BuscarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.CancelarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.EnviarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.GestionarSolicitudConexionService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-43 - Enviar solicitud de conexión.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class SolicitudConexionController {

    private final EnviarSolicitudConexionService enviarSolicitudConexionService;
    private final CancelarSolicitudConexionService cancelarSolicitudConexionService;
    private final BuscarSolicitudConexionService buscarSolicitudConexionService;
    private final GestionarSolicitudConexionService gestionarSolicitudConexionService;

    public SolicitudConexionController(
            EnviarSolicitudConexionService enviarSolicitudConexionService,
            CancelarSolicitudConexionService cancelarSolicitudConexionService,
            BuscarSolicitudConexionService buscarSolicitudConexionService,
            GestionarSolicitudConexionService gestionarSolicitudConexionService) {
        this.enviarSolicitudConexionService = enviarSolicitudConexionService;
        this.cancelarSolicitudConexionService = cancelarSolicitudConexionService;
        this.buscarSolicitudConexionService = buscarSolicitudConexionService;
        this.gestionarSolicitudConexionService = gestionarSolicitudConexionService;
    }

    /**
     * UC-46 - Busca solicitudes de conexión asociadas al perfil activo.
     */
    @GetMapping("/solicitudes-conexion")
    public ResponseEntity<PaginaResponse<SolicitudConexionBusquedaResponse>> buscar(
            @ModelAttribute BuscarSolicitudesConexionFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        return ResponseEntity.ok(buscarSolicitudConexionService.buscar(
                idPerfilActivo(authentication), filtro, page, size));
    }

    @PostMapping("/solicitudes-conexion")
    public ResponseEntity<SolicitudConexionResponse> enviar(
            @Valid @RequestBody EnviarSolicitudConexionRequest request,
            Authentication authentication) {

        SolicitudConexionResponse response = enviarSolicitudConexionService
                .enviar(
                        idUsuario(authentication),
                        idPerfilActivo(authentication),
                        request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Extensión de UC-43: cancela una solicitud pendiente enviada por el
     * perfil activo. La baja es física y elimina la notificación del
     * destinatario.
     */
    @DeleteMapping("/solicitudes-conexion/{idSolicitudConexion}")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long idSolicitudConexion,
            Authentication authentication) {

        cancelarSolicitudConexionService.cancelar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idSolicitudConexion);

        return ResponseEntity.noContent().build();
    }

    /**
     * UC-45 - Acepta una solicitud de conexión recibida por el perfil activo.
     */
    @PostMapping("/solicitudes-conexion/{idSolicitudConexion}/aceptar")
    public ResponseEntity<SolicitudConexionGestionadaResponse> aceptar(
            @PathVariable Long idSolicitudConexion,
            Authentication authentication) {

        return ResponseEntity.ok(gestionarSolicitudConexionService.aceptar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idSolicitudConexion));
    }

    /**
     * UC-45 - Rechaza una solicitud de conexión recibida por el perfil activo.
     */
    @PostMapping("/solicitudes-conexion/{idSolicitudConexion}/rechazar")
    public ResponseEntity<SolicitudConexionGestionadaResponse> rechazar(
            @PathVariable Long idSolicitudConexion,
            Authentication authentication) {

        return ResponseEntity.ok(gestionarSolicitudConexionService.rechazar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idSolicitudConexion));
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
