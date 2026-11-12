package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;

/**
 * Detalle completo de un reporte de publicación para el administrador
 * (UC-69).
 */
public record AdminReportePublicacionDetalleResponse(
        Long idReporte,
        EntidadReporte entidadTipo,
        Long entidadId,
        MotivoReportePublicacion motivo,
        String motivoDescripcion,
        MotivoReporte motivoPersistido,
        String descripcion,
        EstadoReporte estado,
        LocalDateTime fechaReporte,
        boolean publicacionEliminada,
        AdminPublicacionResponse publicacion,
        AutorResponse autor,
        Long idAdminRevisor,
        String nombreAdminRevisor,
        LocalDateTime fechaResolucion
) {

    public record AutorResponse(
            Long idUsuario,
            String nombre,
            String apellido,
            String correo,
            Long idPerfil,
            String nombreArtistico
    ) {}
}
