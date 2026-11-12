package org.mgroko.backend.reportes.controlador;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.reportes.dto.CrearReportePublicacionRequest;
import org.mgroko.backend.reportes.servicio.ReportarPublicacionService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-69: alta de un reporte de publicación.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class ReportePublicacionController {

    private final ReportarPublicacionService reportarPublicacionService;

    public ReportePublicacionController(
            ReportarPublicacionService reportarPublicacionService) {
        this.reportarPublicacionService = reportarPublicacionService;
    }

    @PostMapping("/publicaciones/{idPublicacion}/reportes")
    public ResponseEntity<Void> reportar(
            @PathVariable Long idPublicacion,
            @Valid @RequestBody CrearReportePublicacionRequest request,
            Authentication authentication) {

        reportarPublicacionService.reportar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idPublicacion,
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
