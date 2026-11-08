package org.mgroko.backend.contratos.dto;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoContrato;

/**
 * Filtros opcionales de UC-68. El backend aplica la visibilidad por perfil y,
 * si no se envia estado, usa el conjunto por defecto sin BORRADOR. Los
 * contratos ELIMINADO se incluyen por defecto y el cliente puede excluirlos
 * enviando explicitamente la lista de estados.
 */
public record BuscarContratosFiltro(
        Long idProyecto,
        List<EstadoContrato> estados,
        String parte,
        LocalDate fechaEmisionDesde,
        LocalDate fechaEmisionHasta,
        LocalDate fechaFirmaDesde,
        LocalDate fechaFirmaHasta,
        Long idTipoAcuerdo,
        String nombreContrato
) {
}
