package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.MotivoReporte;

/**
 * Fila del listado de reportes de perfil para la bandeja administrativa.
 */
public record AdminReportePerfilResumenResponse(
        Long idReporte,
        MotivoReporte motivo,
        String motivoDescripcion,
        String descripcion,
        EstadoReporte estado,
        LocalDateTime fechaReporte,
        Long idPerfilReportado,
        String nombrePerfilReportado,
        Long idUsuarioAutor,
        String nombreUsuarioAutor,
        Long idPerfilAutor,
        String nombrePerfilAutor
) {}
