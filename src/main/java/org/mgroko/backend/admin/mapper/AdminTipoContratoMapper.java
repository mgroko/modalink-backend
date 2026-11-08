package org.mgroko.backend.admin.mapper;

import org.mgroko.backend.admin.dto.AdminTipoContratoResponse;
import org.mgroko.backend.admin.dto.AdminTipoContratoResumenResponse;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;

public final class AdminTipoContratoMapper {

    private AdminTipoContratoMapper() {
    }

    public record Uso(
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

    public static AdminTipoContratoResumenResponse toResumen(
            TipoAcuerdoContrato tipo, Uso uso) {
        return new AdminTipoContratoResumenResponse(
                tipo.getIdTipoAcuerdo(),
                tipo.getCodigo(),
                tipo.getNombre(),
                tipo.getDescripcion(),
                tipo.getActivo(),
                uso.contratosAsociados(),
                uso.contratosVivos(),
                uso.contratosPendientes(),
                uso.puedeEditar(),
                uso.puedeDesactivar(),
                uso.puedeEliminar(),
                uso.motivoNoEditable(),
                uso.motivoNoDesactivable(),
                uso.motivoNoEliminable());
    }

    public static AdminTipoContratoResponse toResponse(
            TipoAcuerdoContrato tipo, Uso uso) {
        return new AdminTipoContratoResponse(
                tipo.getIdTipoAcuerdo(),
                tipo.getCodigo(),
                tipo.getNombre(),
                tipo.getDescripcion(),
                tipo.getPlantilla(),
                tipo.getActivo(),
                uso.contratosAsociados(),
                uso.contratosVivos(),
                uso.contratosPendientes(),
                uso.puedeEditar(),
                uso.puedeDesactivar(),
                uso.puedeEliminar(),
                uso.motivoNoEditable(),
                uso.motivoNoDesactivable(),
                uso.motivoNoEliminable());
    }
}
