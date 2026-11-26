package org.mgroko.backend.postulaciones.casting.controlador;

import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.postulaciones.casting.dto.TableroCastingResponse;
import org.mgroko.backend.postulaciones.casting.enums.OrdenCasting;
import org.mgroko.backend.postulaciones.casting.servicio.ObtenerTableroCastingService;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tablero de casting virtual de postulaciones de modelos.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class CastingController {

    private final ObtenerTableroCastingService obtenerTableroCastingService;

    public CastingController(
            ObtenerTableroCastingService obtenerTableroCastingService) {
        this.obtenerTableroCastingService = obtenerTableroCastingService;
    }

    @GetMapping("/proyectos/{idProyecto}/postulaciones/casting")
    public ResponseEntity<TableroCastingResponse> obtener(
            @PathVariable Long idProyecto,
            @RequestParam TipoPostulacion tipo,
            @RequestParam Long idRequerimiento,
            @RequestParam(required = false) List<EstadoSolicitud> estado,
            @RequestParam(defaultValue = "CUMPLIMIENTO") OrdenCasting orden,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Long idPostulacionDestacada,
            Authentication authentication) {

        return ResponseEntity.ok(obtenerTableroCastingService.obtener(
                idPerfilActivo(authentication),
                idProyecto,
                tipo,
                idRequerimiento,
                estado,
                orden,
                page,
                idPostulacionDestacada));
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
