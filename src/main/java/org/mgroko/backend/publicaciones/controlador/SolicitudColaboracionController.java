package org.mgroko.backend.publicaciones.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.publicaciones.dto.BuscarSolicitudesColaboracionFiltro;
import org.mgroko.backend.publicaciones.dto.EnviarSolicitudColaboracionRequest;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionBusquedaResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionGestionadaResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.servicio.BuscarSolicitudColaboracionService;
import org.mgroko.backend.publicaciones.servicio.EnviarSolicitudColaboracionService;
import org.mgroko.backend.publicaciones.servicio.GestionarSolicitudColaboracionService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * UC-61 - Enviar solicitud de colaboración.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class SolicitudColaboracionController {

    private final EnviarSolicitudColaboracionService enviarSolicitudColaboracionService;
    private final BuscarSolicitudColaboracionService buscarSolicitudColaboracionService;
    private final GestionarSolicitudColaboracionService gestionarSolicitudColaboracionService;

    public SolicitudColaboracionController(
            EnviarSolicitudColaboracionService enviarSolicitudColaboracionService,
            BuscarSolicitudColaboracionService buscarSolicitudColaboracionService,
            GestionarSolicitudColaboracionService gestionarSolicitudColaboracionService) {
        this.enviarSolicitudColaboracionService = enviarSolicitudColaboracionService;
        this.buscarSolicitudColaboracionService = buscarSolicitudColaboracionService;
        this.gestionarSolicitudColaboracionService = gestionarSolicitudColaboracionService;
    }

    /**
     * UC-65 - Busca solicitudes de colaboración del perfil activo.
     */
    @GetMapping("/solicitudes-colaboracion")
    public ResponseEntity<PaginaResponse<SolicitudColaboracionBusquedaResponse>> buscar(
            @ModelAttribute BuscarSolicitudesColaboracionFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        PaginaResponse<SolicitudColaboracionBusquedaResponse> response =
                buscarSolicitudColaboracionService.buscar(
                        idPerfilActivoRequerido(authentication), filtro, page, size);

        return ResponseEntity.ok(response);
    }

    /**
     * UC-62 - Aceptar una solicitud de colaboración recibida.
     */
    @PostMapping("/solicitudes-colaboracion/{idSolicitudColaboracion}/aceptar")
    public ResponseEntity<SolicitudColaboracionGestionadaResponse> aceptar(
            @PathVariable Long idSolicitudColaboracion,
            Authentication authentication) {

        return ResponseEntity.ok(gestionarSolicitudColaboracionService.aceptar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idSolicitudColaboracion));
    }

    /**
     * UC-63 - Rechazar una solicitud de colaboración recibida.
     */
    @PostMapping("/solicitudes-colaboracion/{idSolicitudColaboracion}/rechazar")
    public ResponseEntity<SolicitudColaboracionGestionadaResponse> rechazar(
            @PathVariable Long idSolicitudColaboracion,
            Authentication authentication) {

        return ResponseEntity.ok(gestionarSolicitudColaboracionService.rechazar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idSolicitudColaboracion));
    }

    @PostMapping("/publicaciones/{idPublicacion}/solicitudes-colaboracion")
    public ResponseEntity<SolicitudColaboracionResponse> enviar(
            @PathVariable Long idPublicacion,
            @Valid @RequestBody EnviarSolicitudColaboracionRequest request,
            Authentication authentication) {

        SolicitudColaboracionResponse response = enviarSolicitudColaboracionService.enviar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idPublicacion,
                request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }

    private Long idPerfilActivoRequerido(Authentication authentication) {
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
