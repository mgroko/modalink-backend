package org.mgroko.backend.conexiones.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.conexiones.dto.BuscarConexionesFiltro;
import org.mgroko.backend.conexiones.dto.ConexionResponse;
import org.mgroko.backend.conexiones.dto.ResumenConexionesResponse;
import org.mgroko.backend.conexiones.servicio.BuscarConexionService;
import org.mgroko.backend.conexiones.servicio.EliminarConexionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-44 y UC-44 bis - Red de contactos.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class ConexionController {

    private final BuscarConexionService buscarConexionService;
    private final EliminarConexionService eliminarConexionService;

    public ConexionController(
            BuscarConexionService buscarConexionService,
            EliminarConexionService eliminarConexionService) {
        this.buscarConexionService = buscarConexionService;
        this.eliminarConexionService = eliminarConexionService;
    }

    /**
     * UC-44 bis - Lista las conexiones activas del perfil activo.
     */
    @GetMapping("/conexiones")
    public ResponseEntity<PaginaResponse<ConexionResponse>> buscar(
            @ModelAttribute BuscarConexionesFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        return ResponseEntity.ok(buscarConexionService.buscar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                filtro,
                page,
                size));
    }

    /**
     * Resumen de la red: total, en línea y conexiones por profesión.
     */
    @GetMapping("/conexiones/resumen")
    public ResponseEntity<ResumenConexionesResponse> resumen(
            Authentication authentication) {

        return ResponseEntity.ok(buscarConexionService.resumen(
                idUsuario(authentication),
                idPerfilActivo(authentication)));
    }

    /**
     * UC-44 - Elimina lógicamente una conexión de la red.
     */
    @DeleteMapping("/conexiones/{idConexion}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long idConexion,
            Authentication authentication) {

        eliminarConexionService.eliminar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idConexion);

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
