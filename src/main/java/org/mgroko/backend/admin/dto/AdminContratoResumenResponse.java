package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

public record AdminContratoResumenResponse(
        Long idContrato,
        String nombre,
        String estado,
        Integer versionActual,
        Long idProyecto,
        String nombreProyecto,
        String estadoProyecto,
        Long idTipoAcuerdo,
        String codigoTipoAcuerdo,
        String nombreTipoAcuerdo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaEmision,
        LocalDateTime fechaModificacion,
        LocalDateTime fechaBaja,
        String nombreEmisor,
        int firmados,
        int totalPartes
) {
}
