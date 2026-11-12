package org.mgroko.backend.reemplazos.servicio;

import java.time.LocalDateTime;

import org.mgroko.backend.proyectos.servicio.RequerimientoSnapshotService;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Desvincula un reemplazo cerrado de su requerimiento activo.
 *
 * <p>El proceso de reemplazo conserva su historial, pero pasa a apuntar a un
 * snapshot archivado. De esta manera el requerimiento activo puede editarse o
 * eliminarse sin que la FK del reemplazo lo bloquee.</p>
 */
@Service
public class ReemplazoSnapshotService {

    private final RequerimientoSnapshotService requerimientoSnapshotService;

    public ReemplazoSnapshotService(
            RequerimientoSnapshotService requerimientoSnapshotService) {
        this.requerimientoSnapshotService = requerimientoSnapshotService;
    }

    @Transactional
    public void repuntarAlCerrar(Reemplazo reemplazo, LocalDateTime fecha) {
        if (reemplazo == null || fecha == null) {
            return;
        }

        if (reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD
                && reemplazo.getRequerimientoActividad() != null) {
            reemplazo.setRequerimientoActividad(
                    requerimientoSnapshotService.snapshotParaReemplazo(
                            reemplazo.getRequerimientoActividad(), fecha));
            return;
        }

        if (reemplazo.getTipo() == TipoReemplazo.GENERAL
                && reemplazo.getRequerimientoGral() != null) {
            reemplazo.setRequerimientoGral(
                    requerimientoSnapshotService.snapshotParaReemplazo(
                            reemplazo.getRequerimientoGral(), fecha));
        }
    }
}
