package org.mgroko.backend.admin.dto;

import java.time.LocalDate;

import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;

/**
 * Solicitud para iniciar un procesamiento histórico de colaboraciones.
 */
public record ProcesarColaboracionesHistoricasRequest(
        ModoProcesamientoHistorico modo,
        Long idPublicacion,
        Long idPerfil,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Boolean notificar,
        Integer tamanoLote,
        Integer limitePublicaciones) {
}
