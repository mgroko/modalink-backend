package org.mgroko.backend.presencia.controlador;

import java.io.IOException;
import java.util.List;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.presencia.config.PresenciaProperties;
import org.mgroko.backend.presencia.dto.PresenciaPerfilResponse;
import org.mgroko.backend.presencia.exception.PresenciaSolicitudInvalidaException;
import org.mgroko.backend.presencia.servicio.PresenciaEventBus;
import org.mgroko.backend.presencia.servicio.PresenciaService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Presencia por perfil: heartbeat, snapshot y stream SSE.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class PresenciaController {

    private final PresenciaService presenciaService;
    private final PresenciaEventBus presenciaEventBus;
    private final PresenciaProperties properties;

    public PresenciaController(
            PresenciaService presenciaService,
            PresenciaEventBus presenciaEventBus,
            PresenciaProperties properties) {
        this.presenciaService = presenciaService;
        this.presenciaEventBus = presenciaEventBus;
        this.properties = properties;
    }

    /**
     * Heartbeat del perfil activo del JWT. No recibe body.
     */
    @PostMapping("/presencia/heartbeat")
    public ResponseEntity<Void> heartbeat(Authentication authentication) {
        presenciaService.registrarActividad(
                Long.parseLong((String) authentication.getPrincipal()),
                idPerfilActivo(authentication));

        return ResponseEntity.noContent().build();
    }

    /**
     * Snapshot de presencia para un conjunto de perfiles.
     */
    @GetMapping("/presencia")
    public ResponseEntity<List<PresenciaPerfilResponse>> snapshot(
            @RequestParam(name = "idsPerfil", required = false) List<Long> idsPerfil) {

        validarIdsComunes(idsPerfil);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(presenciaService.snapshot(idsPerfil));
    }

    /**
     * Stream SSE con snapshot inicial y cambios de presencia de los perfiles
     * suscriptos.
     */
    @GetMapping(value = "/presencia/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(
            @RequestParam(name = "idsPerfil") List<Long> idsPerfil,
            @RequestParam(name = "clientId", required = false) String clientId) {

        validarIdsComunes(idsPerfil);
        if (idsPerfil == null || idsPerfil.isEmpty()) {
            throw new PresenciaSolicitudInvalidaException(
                    "Debe indicar al menos un idPerfil para el stream.");
        }

        SseEmitter emitter = new SseEmitter(properties.getTimeoutStream().toMillis());

        try {
            emitter.send(SseEmitter.event()
                    .name("snapshot")
                    .data(presenciaService.snapshot(idsPerfil),
                            MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
        }

        if (clientId == null || clientId.isBlank()) {
            presenciaEventBus.registrarSuscriptor(idsPerfil, emitter);
        } else {
            presenciaEventBus.registrarSuscriptor(idsPerfil, emitter, clientId);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    private void validarIdsComunes(List<Long> idsPerfil) {
        if (idsPerfil == null) {
            return;
        }

        if (idsPerfil.size() > properties.getMaxIds()) {
            throw new PresenciaSolicitudInvalidaException(
                    "No se pueden consultar más de "
                            + properties.getMaxIds() + " perfiles a la vez.");
        }

        if (idsPerfil.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new PresenciaSolicitudInvalidaException(
                    "Los identificadores de perfil deben ser positivos.");
        }
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
