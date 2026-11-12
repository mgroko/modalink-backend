package org.mgroko.backend.reportes.evento;

import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;

/**
 * Evento publicado al registrar un reporte de publicación. El listener de
 * notificaciones lo consume después del commit para no notificar reportes que
 * terminen revertidos.
 */
public record ReportePublicacionCreadoEvent(
        Long idReporte,
        Long idPublicacion,
        String tituloPublicacion,
        Long idPerfilPublicacion,
        String nombrePerfilPublicacion,
        Long idUsuarioAutor,
        String nombreUsuarioAutor,
        Long idPerfilAutor,
        String nombrePerfilAutor,
        MotivoReportePublicacion motivo,
        String descripcion
) {}
