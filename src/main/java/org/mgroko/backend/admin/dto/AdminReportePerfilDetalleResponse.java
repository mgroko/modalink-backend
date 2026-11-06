package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.MotivoReporte;

/**
 * Detalle completo de un reporte de perfil para el administrador (UC-15).
 */
public record AdminReportePerfilDetalleResponse(
        Long idReporte,
        EntidadReporte entidadTipo,
        Long entidadId,
        MotivoReporte motivo,
        String motivoDescripcion,
        String descripcion,
        EstadoReporte estado,
        LocalDateTime fechaReporte,
        PerfilReportadoResponse perfilReportado,
        UsuarioResponse usuarioReportado,
        AutorResponse autor,
        Long idAdminRevisor,
        String nombreAdminRevisor,
        LocalDateTime fechaResolucion
) {

    public record PerfilReportadoResponse(
            Long idPerfil,
            String nombreArtistico,
            String profesion,
            String estado
    ) {}

    public record UsuarioResponse(
            Long idUsuario,
            String nombre,
            String apellido,
            String correo,
            String dni,
            String estado
    ) {}

    public record AutorResponse(
            Long idUsuario,
            String nombre,
            String apellido,
            String correo,
            String dni,
            Long idPerfil,
            String nombreArtistico
    ) {}
}
