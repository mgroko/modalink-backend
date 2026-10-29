package org.mgroko.backend.reportes.controlador;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.reportes.dto.CrearReportePerfilRequest;
import org.mgroko.backend.reportes.servicio.ReportarPerfilService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-15: alta de un reporte de perfil.
 */
@RestController
public class ReportePerfilController {

    private final ReportarPerfilService reportarPerfilService;

    public ReportePerfilController(ReportarPerfilService reportarPerfilService) {
        this.reportarPerfilService = reportarPerfilService;
    }

    @PostMapping("/perfiles/{idPerfil}/reportes")
    public ResponseEntity<Void> reportar(
            @PathVariable Long idPerfil,
            @Valid @RequestBody CrearReportePerfilRequest request,
            Authentication authentication) {

        reportarPerfilService.reportar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idPerfil,
                request);

        return ResponseEntity.noContent().build();
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
