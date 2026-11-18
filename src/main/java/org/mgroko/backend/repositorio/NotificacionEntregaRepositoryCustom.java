package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionEntregaRepositoryCustom {

    /**
     * Reclama un lote de entregas pendientes o fallidas listas para reintento.
     *
     * <p>Usa {@code FOR UPDATE SKIP LOCKED} y deja las filas en estado
     * {@code ENVIANDO} para que otra instancia no las tome. Devuelve los IDs
     * reclamados.</p>
     */
    List<Long> reclamarLote(LocalDateTime ahora, int lote);

    /**
     * Devuelve a {@code PENDIENTE} las entregas que quedaron en {@code ENVIANDO}
     * por una caída del proceso.
     */
    int recuperarEnviandoVencidas(LocalDateTime limite);
}
