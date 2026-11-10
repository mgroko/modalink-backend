package org.mgroko.backend.admin.servicio;

import java.util.List;

import org.mgroko.backend.auditoria.dto.AuditoriaDetalleRequest;
import org.mgroko.backend.auditoria.servicio.AuditoriaEventoService;
import org.mgroko.backend.storage.servicio.LimpiezaImagenesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra la auditoría de una ejecución manual de la limpieza de imágenes en
 * una transacción propia. El job de limpieza no se envuelve en una transacción
 * global porque borra elemento por elemento y debe continuar ante fallos
 * individuales.
 */
@Service
public class AdminLimpiezaImagenesAuditoriaService {

    private final AuditoriaEventoService auditoriaEventoService;

    public AdminLimpiezaImagenesAuditoriaService(AuditoriaEventoService auditoriaEventoService) {
        this.auditoriaEventoService = auditoriaEventoService;
    }

    @Transactional
    public void registrarEjecucion(LimpiezaImagenesService.Resultado resultado) {
        auditoriaEventoService.registrar(
                "scheduler",
                0L,
                "SCHEDULER_EJECUTADO",
                "UPDATE",
                List.of(
                        new AuditoriaDetalleRequest("scheduler", null, "IMAGENES"),
                        new AuditoriaDetalleRequest(
                                "archivosHuerfanos", null, String.valueOf(resultado.archivosHuerfanos())),
                        new AuditoriaDetalleRequest(
                                "imagenesHuerfanas", null, String.valueOf(resultado.imagenesHuerfanas())),
                        new AuditoriaDetalleRequest(
                                "imagenesPerfilesBaja", null, String.valueOf(resultado.imagenesPerfilesBaja())),
                        new AuditoriaDetalleRequest(
                                "errores", null, String.valueOf(resultado.errores()))));
    }
}
