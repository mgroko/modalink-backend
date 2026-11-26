package org.mgroko.backend.proyectos.controlador;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse;
import org.mgroko.backend.proyectos.pdf.InformeProyectoPdfRenderer;
import org.mgroko.backend.proyectos.servicio.GenerarInformeProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Informe ejecutivo de estado y salud de un proyecto.
 *
 * <p>Solo el director activo del proyecto puede generarlo. El informe es una
 * foto del momento y no reemplaza la auditoría.</p>
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class InformeProyectoController {

    private static final DateTimeFormatter FECHA_ARCHIVO =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final GenerarInformeProyectoService generarInformeProyectoService;
    private final InformeProyectoPdfRenderer informeProyectoPdfRenderer;

    public InformeProyectoController(
            GenerarInformeProyectoService generarInformeProyectoService,
            InformeProyectoPdfRenderer informeProyectoPdfRenderer) {
        this.generarInformeProyectoService = generarInformeProyectoService;
        this.informeProyectoPdfRenderer = informeProyectoPdfRenderer;
    }

    @GetMapping("/proyectos/{idProyecto}/informe")
    public ResponseEntity<InformeProyectoResponse> obtener(
            @PathVariable Long idProyecto,
            Authentication authentication) {

        return ResponseEntity.ok(generarInformeProyectoService.generar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto));
    }

    @GetMapping("/proyectos/{idProyecto}/informe/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @PathVariable Long idProyecto,
            Authentication authentication) {

        InformeProyectoResponse informe = generarInformeProyectoService.generar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        informeProyectoPdfRenderer.render(informe, salida);

        String filename = "informe_proyecto_" + idProyecto + "_"
                + informe.generadoEn().format(FECHA_ARCHIVO) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(salida.toByteArray());
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }

    private Long idPerfilActivo(Authentication authentication) {
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            Long idPerfilActivo = contexto.idPerfilActivo();
            if (idPerfilActivo != null) {
                return idPerfilActivo;
            }
        }
        throw new PerfilActivoNoSeleccionadoException();
    }
}
