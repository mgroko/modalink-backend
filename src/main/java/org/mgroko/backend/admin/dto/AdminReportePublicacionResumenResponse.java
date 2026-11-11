package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;

/**
 * Fila del listado de reportes de publicación para la bandeja administrativa.
 */
public record AdminReportePublicacionResumenResponse(
        Long idReporte,
        MotivoReportePublicacion motivo,
        String motivoDescripcion,
        String descripcion,
        EstadoReporte estado,
        LocalDateTime fechaReporte,
        boolean publicacionEliminada,
        Long idPublicacion,
        String tituloPublicacion,
        EstadoPublicacion estadoPublicacion,
        Long idPerfilPublicacion,
        String nombrePerfilPublicacion,
        Long idUsuarioAutor,
        String nombreUsuarioAutor,
        Long idPerfilAutor,
        String nombrePerfilAutor
) {}
