package org.mgroko.backend.admin.servicio;

import java.util.List;

import org.mgroko.backend.modelo.ProcesamientoColaboracionHistorica;
import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Ejecuta un procesamiento histórico completo, lote por lote, en un hilo
 * dedicado.
 */
@Service
public class ProcesamientoHistoricoWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(ProcesamientoHistoricoWorker.class);

    private final ProcesamientoHistoricoBatchService batchService;

    public ProcesamientoHistoricoWorker(
            ProcesamientoHistoricoBatchService batchService) {
        this.batchService = batchService;
    }

    @Async("procesamientoHistoricoExecutor")
    public void ejecutar(Long idProcesamiento) {
        try {
            ProcesamientoColaboracionHistorica job =
                    batchService.iniciar(idProcesamiento);

            if (job.getEstado() != EstadoProcesamientoHistorico.EN_PROGRESO) {
                return;
            }

            boolean cancelado = false;

            while (!cancelado) {
                if (batchService.estaCancelado(idProcesamiento)) {
                    cancelado = true;
                    break;
                }

                List<Long> ids = batchService.siguienteLote(idProcesamiento);
                if (ids.isEmpty()) {
                    break;
                }

                for (Long idPublicacion : ids) {
                    if (batchService.estaCancelado(idProcesamiento)) {
                        cancelado = true;
                        break;
                    }

                    try {
                        batchService.procesarPublicacion(
                                idProcesamiento, idPublicacion);
                    } catch (RuntimeException ex) {
                        LOG.warn(
                                "Error al procesar la publicación {} del job {}.",
                                idPublicacion, idProcesamiento, ex);
                        batchService.registrarError(
                                idProcesamiento,
                                idPublicacion,
                                ex.getMessage() != null
                                        ? ex.getMessage()
                                        : ex.getClass().getSimpleName());
                    }
                }
            }

            batchService.finalizar(idProcesamiento);
        } catch (RuntimeException ex) {
            LOG.error("El procesamiento histórico {} falló.", idProcesamiento, ex);
            batchService.fallar(
                    idProcesamiento,
                    ex.getMessage() != null
                            ? ex.getMessage()
                            : ex.getClass().getSimpleName());
        }
    }
}
