package org.mgroko.backend.publicaciones.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.publicaciones.dto.BuscarColaboracionesAutomaticasFiltro;
import org.mgroko.backend.publicaciones.dto.ColaboracionAutomaticaResponse;
import org.mgroko.backend.publicaciones.servicio.BuscarColaboracionAutomaticaService;
import org.mgroko.backend.publicaciones.servicio.GestionarColaboracionAutomaticaService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Colaboraciones automáticas detectadas por coincidencia de imágenes.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class ColaboracionAutomaticaController {

    private final BuscarColaboracionAutomaticaService buscarService;
    private final GestionarColaboracionAutomaticaService gestionarService;

    public ColaboracionAutomaticaController(
            BuscarColaboracionAutomaticaService buscarService,
            GestionarColaboracionAutomaticaService gestionarService) {
        this.buscarService = buscarService;
        this.gestionarService = gestionarService;
    }

    @GetMapping("/colaboraciones-automaticas")
    public ResponseEntity<PaginaResponse<ColaboracionAutomaticaResponse>> buscar(
            @ModelAttribute BuscarColaboracionesAutomaticasFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        return ResponseEntity.ok(buscarService.buscar(
                idPerfilActivoRequerido(authentication), filtro, page, size));
    }

    @GetMapping("/colaboraciones-automaticas/{idColaboracionAutomatica}")
    public ResponseEntity<ColaboracionAutomaticaResponse> obtener(
            @PathVariable Long idColaboracionAutomatica,
            Authentication authentication) {

        return ResponseEntity.ok(buscarService.obtener(
                idPerfilActivoRequerido(authentication), idColaboracionAutomatica));
    }

    @PostMapping("/colaboraciones-automaticas/{idColaboracionAutomatica}/confirmar")
    public ResponseEntity<ColaboracionAutomaticaResponse> confirmar(
            @PathVariable Long idColaboracionAutomatica,
            Authentication authentication) {

        return ResponseEntity.ok(gestionarService.confirmar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idColaboracionAutomatica));
    }

    @PostMapping("/colaboraciones-automaticas/{idColaboracionAutomatica}/rechazar")
    public ResponseEntity<ColaboracionAutomaticaResponse> rechazar(
            @PathVariable Long idColaboracionAutomatica,
            Authentication authentication) {

        return ResponseEntity.ok(gestionarService.rechazar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idColaboracionAutomatica));
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
