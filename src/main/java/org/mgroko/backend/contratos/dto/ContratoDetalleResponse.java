package org.mgroko.backend.contratos.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ContratoDetalleResponse(
        Long idContrato,
        Long idProyecto,
        String nombreProyecto,
        String estadoProyecto,
        Long idTipoAcuerdo,
        String codigoTipoAcuerdo,
        String nombreTipoAcuerdo,
        String nombre,
        String descripcion,
        String estado,
        Integer versionActual,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaEmision,
        LocalDateTime fechaModificacion,
        LocalDateTime fechaBaja,
        String texto,
        EmisorContratoResponse emisor,
        List<ParteContratoResumenResponse> partes,
        String miEstadoFirma,
        LocalDateTime miFechaFirma,
        boolean puedeModificarse,
        String motivoNoModificable,
        boolean puedeFirmar,
        boolean puedeRechazar,
        int firmados,
        int totalPartes,
        boolean puedeEliminar,
        String motivoNoEliminable
) {
}
