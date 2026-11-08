package org.mgroko.backend.admin.dto;

/**
 * Fila del listado administrativo de tipos de contrato. No incluye la
 * plantilla completa para mantener liviano el listado.
 */
public record AdminTipoContratoResumenResponse(
        Long idTipoAcuerdo,
        String codigo,
        String nombre,
        String descripcion,
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
