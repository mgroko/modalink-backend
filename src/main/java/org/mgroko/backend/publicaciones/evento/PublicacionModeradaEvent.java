package org.mgroko.backend.publicaciones.evento;

import org.mgroko.backend.modelo.enums.EstadoPublicacion;

/**
 * Evento publicado después de una acción administrativa de moderación.
 *
 * <p>El listener de notificaciones lo procesa después del commit para no
 * notificar acciones que terminen revertidas.</p>
 */
public record PublicacionModeradaEvent(
        Long idPublicacion,
        String titulo,
        Long idPerfil,
        Long idUsuarioDestino,
        EstadoPublicacion estado
) {
}
