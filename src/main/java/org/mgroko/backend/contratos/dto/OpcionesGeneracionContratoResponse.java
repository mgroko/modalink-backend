package org.mgroko.backend.contratos.dto;

import java.util.List;

public record OpcionesGeneracionContratoResponse(
        ProyectoContratoResponse proyecto,
        EmisorContratoResponse emisor,
        List<TipoAcuerdoContratoResponse> tiposAcuerdo,
        List<MiembroDestinatarioResponse> miembros,
        List<ContratoActivoMiembroResponse> contratosActivos
) {
}
