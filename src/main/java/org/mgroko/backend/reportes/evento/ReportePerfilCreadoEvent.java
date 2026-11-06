package org.mgroko.backend.reportes.evento;

import org.mgroko.backend.modelo.enums.MotivoReporte;

/**
 * Evento publicado al registrar un reporte de perfil. El listener de
 * notificaciones lo consume después del commit para no notificar reportes que
 * terminen revertidos.
 */
public record ReportePerfilCreadoEvent(
        Long idReporte,
        Long idPerfilReportado,
        String nombrePerfilReportado,
        Long idUsuarioAutor,
        String nombreUsuarioAutor,
        Long idPerfilAutor,
        String nombrePerfilAutor,
        MotivoReporte motivo,
        String descripcion
) {}
