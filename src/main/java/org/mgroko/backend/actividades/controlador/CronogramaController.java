package org.mgroko.backend.actividades.controlador;

import org.mgroko.backend.actividades.dto.CronogramaResponse;
import org.mgroko.backend.actividades.servicio.ObtenerCronogramaService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CronogramaController {

    private final ObtenerCronogramaService obtenerCronogramaService;

    public CronogramaController(ObtenerCronogramaService obtenerCronogramaService) {
        this.obtenerCronogramaService = obtenerCronogramaService;
    }

    // UC-33: Visualizar cronograma de proyecto.
    @GetMapping("/proyectos/{idProyecto}/cronograma")
    public ResponseEntity<CronogramaResponse> obtener(
            @PathVariable Long idProyecto,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        return ResponseEntity.ok(
                obtenerCronogramaService.obtener(idUsuario, idPerfilActivo, idProyecto));
    }
}
