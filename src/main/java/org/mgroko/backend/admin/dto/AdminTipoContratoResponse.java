package org.mgroko.backend.admin.dto;

/**
 * Detalle administrativo de un tipo de contrato, con la plantilla completa.
 */
public record AdminTipoContratoResponse(
        Long idTipoAcuerdo,
        String codigo,
        String nombre,
        String descripcion,
        String plantilla,
        Boolean activo,
        long contratosAsociados,
        long contratosVivos,
        long contratosPendientes,
        boolean puedeEditar,
        boolean puedeDesactivar,
        boolean puedeEliminar,
        String motivoNoEditable,
        String motivoNoDesactivable,
        String motivoNoEliminable
) {
}
