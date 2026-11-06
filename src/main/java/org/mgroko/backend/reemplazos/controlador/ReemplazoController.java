package org.mgroko.backend.reemplazos.controlador;

import java.util.List;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.reemplazos.dto.CandidatoReemplazoResponse;
import org.mgroko.backend.reemplazos.dto.CancelarReemplazoRequest;
import org.mgroko.backend.reemplazos.dto.ConfirmarReemplazoRequest;
import org.mgroko.backend.reemplazos.dto.InvitacionReemplazoResponse;
import org.mgroko.backend.reemplazos.dto.RechazarCandidatoRequest;
import org.mgroko.backend.reemplazos.dto.ReemplazoDetalleResponse;
import org.mgroko.backend.reemplazos.dto.ReemplazoResumenResponse;
import org.mgroko.backend.reemplazos.servicio.AprobacionIncorporacionService;
import org.mgroko.backend.reemplazos.servicio.CancelarReemplazoService;
import org.mgroko.backend.reemplazos.servicio.ConfirmarReemplazoService;
import org.mgroko.backend.reemplazos.servicio.InvitacionReemplazoService;
import org.mgroko.backend.reemplazos.servicio.ReemplazoQueryService;
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
 * API del módulo de reemplazo automático.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class ReemplazoController {

    private final ReemplazoQueryService queryService;
    private final ConfirmarReemplazoService confirmarService;
    private final InvitacionReemplazoService invitacionService;
    private final AprobacionIncorporacionService aprobacionService;
    private final CancelarReemplazoService cancelarService;

    public ReemplazoController(
            ReemplazoQueryService queryService,
            ConfirmarReemplazoService confirmarService,
            InvitacionReemplazoService invitacionService,
            AprobacionIncorporacionService aprobacionService,
            CancelarReemplazoService cancelarService) {
        this.queryService = queryService;
        this.confirmarService = confirmarService;
        this.invitacionService = invitacionService;
        this.aprobacionService = aprobacionService;
        this.cancelarService = cancelarService;
    }

    @GetMapping("/proyectos/{idProyecto}/reemplazos")
    public ResponseEntity<List<ReemplazoResumenResponse>> listar(
            @PathVariable Long idProyecto,
            Authentication authentication) {

        return ResponseEntity.ok(queryService.listar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto));
    }

    @GetMapping("/proyectos/{idProyecto}/reemplazos/{idReemplazo}")
    public ResponseEntity<ReemplazoDetalleResponse> detalle(
            @PathVariable Long idProyecto,
            @PathVariable Long idReemplazo,
            Authentication authentication) {

        return ResponseEntity.ok(queryService.detalle(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idReemplazo));
    }

    @PostMapping("/proyectos/{idProyecto}/reemplazos/{idReemplazo}/confirmar")
    public ResponseEntity<ReemplazoDetalleResponse> confirmar(
            @PathVariable Long idProyecto,
            @PathVariable Long idReemplazo,
            @Valid @RequestBody ConfirmarReemplazoRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(confirmarService.confirmar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idReemplazo,
                request));
    }

    @PostMapping("/proyectos/{idProyecto}/reemplazos/{idReemplazo}/regenerar")
    public ResponseEntity<ReemplazoDetalleResponse> regenerar(
            @PathVariable Long idProyecto,
            @PathVariable Long idReemplazo,
            Authentication authentication) {

        return ResponseEntity.ok(confirmarService.regenerar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idReemplazo));
    }

    @PostMapping("/proyectos/{idProyecto}/reemplazos/{idReemplazo}/cancelar")
    public ResponseEntity<ReemplazoDetalleResponse> cancelar(
            @PathVariable Long idProyecto,
            @PathVariable Long idReemplazo,
            @RequestBody(required = false) @Valid CancelarReemplazoRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(cancelarService.cancelar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idReemplazo,
                request != null ? request.motivo() : null));
    }

    @PostMapping("/proyectos/{idProyecto}/reemplazos/{idReemplazo}/candidatos/{idCandidato}/aprobar")
    public ResponseEntity<CandidatoReemplazoResponse> aprobar(
            @PathVariable Long idProyecto,
            @PathVariable Long idReemplazo,
            @PathVariable Long idCandidato,
            Authentication authentication) {

        return ResponseEntity.ok(aprobacionService.aprobar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idReemplazo,
                idCandidato));
    }

    @PostMapping("/proyectos/{idProyecto}/reemplazos/{idReemplazo}/candidatos/{idCandidato}/rechazar")
    public ResponseEntity<CandidatoReemplazoResponse> rechazarCandidato(
            @PathVariable Long idProyecto,
            @PathVariable Long idReemplazo,
            @PathVariable Long idCandidato,
            @RequestBody(required = false) @Valid RechazarCandidatoRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(aprobacionService.rechazar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idReemplazo,
                idCandidato,
                request != null ? request.motivo() : null));
    }

    @GetMapping("/reemplazos/invitaciones")
    public ResponseEntity<List<InvitacionReemplazoResponse>> invitaciones(
            Authentication authentication) {

        return ResponseEntity.ok(queryService.listarInvitaciones(
                idUsuario(authentication),
                idPerfilActivo(authentication)));
    }

    @PostMapping("/reemplazos/invitaciones/{idCandidato}/aceptar")
    public ResponseEntity<CandidatoReemplazoResponse> aceptar(
            @PathVariable Long idCandidato,
            Authentication authentication) {

        return ResponseEntity.ok(invitacionService.aceptar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idCandidato));
    }

    @PostMapping("/reemplazos/invitaciones/{idCandidato}/rechazar")
    public ResponseEntity<CandidatoReemplazoResponse> rechazarInvitacion(
            @PathVariable Long idCandidato,
            @RequestBody(required = false) @Valid RechazarCandidatoRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(invitacionService.rechazar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idCandidato,
                request != null ? request.motivo() : null));
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
