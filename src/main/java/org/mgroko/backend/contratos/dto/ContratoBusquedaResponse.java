package org.mgroko.backend.contratos.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ContratoBusquedaResponse(
        Long idContrato,
        String nombre,
        Long idProyecto,
        String nombreProyecto,
        Long idTipoAcuerdo,
        String codigoTipoAcuerdo,
        String nombreTipoAcuerdo,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaEmision,
        LocalDateTime fechaModificacion,
        Integer versionActual,
        EmisorContratoResponse emisor,
        List<ParteContratoResumenResponse> partes,
        String miEstadoFirma,
        LocalDateTime miFechaFirma,
        boolean puedeModificar,
        boolean puedeFirmar,
        boolean puedeRechazar,
        int firmados,
        int totalPartes,
        boolean puedeEliminar
) {
}
