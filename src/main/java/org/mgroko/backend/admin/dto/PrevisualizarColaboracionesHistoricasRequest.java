package org.mgroko.backend.admin.dto;

import java.time.LocalDate;

import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;

/**
 * Filtros para previsualizar el procesamiento histórico de colaboraciones.
 */
public record PrevisualizarColaboracionesHistoricasRequest(
        ModoProcesamientoHistorico modo,
        Long idPublicacion,
        Long idPerfil,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Integer limitePublicaciones) {
}
