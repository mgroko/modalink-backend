package org.mgroko.backend.admin.dto;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoContrato;

public record BuscarContratosAdminFiltro(
        Long idProyecto,
        String nombreProyecto,
        List<EstadoContrato> estados,
        Long idTipoAcuerdo,
        Long idPerfilParte,
        String parte,
        LocalDate fechaEmisionDesde,
        LocalDate fechaEmisionHasta,
        LocalDate fechaFirmaDesde,
        LocalDate fechaFirmaHasta,
        String texto
) {
}
