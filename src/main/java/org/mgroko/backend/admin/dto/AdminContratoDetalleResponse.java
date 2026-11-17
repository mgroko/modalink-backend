package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle administrativo de un contrato, incluido el texto legal completo.
 * No expone flags de accion porque es una vista de solo lectura.
 */
public record AdminContratoDetalleResponse(
        Long idContrato,
        String nombre,
        String descripcion,
        String estado,
        Integer versionActual,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaEmision,
        LocalDateTime fechaModificacion,
        LocalDateTime fechaBaja,
        LocalDateTime fechaCancelacion,
        String motivoCancelacion,
        String texto,
        ProyectoResponse proyecto,
        TipoAcuerdoResponse tipoAcuerdo,
        EmisorResponse emisor,
        List<ParteResponse> partes,
        int firmados,
        int totalPartes
) {

    public record ProyectoResponse(
            Long idProyecto,
            String nombre,
            String descripcion,
            String estado
    ) {
    }

    public record TipoAcuerdoResponse(
            Long idTipoAcuerdo,
            String codigo,
            String nombre,
            String descripcion
    ) {
    }

    public record EmisorResponse(
            Long idMiembro,
            Long idPerfil,
            String nombreArtistico,
            String nombreCompleto,
            String dni
    ) {
    }

    public record ParteResponse(
            Long idMiembro,
            Long idPerfil,
            String nombreArtistico,
            String nombreCompleto,
            String dni,
            String rolParte,
            String estadoFirma,
            LocalDateTime fechaFirma
    ) {
    }
}
