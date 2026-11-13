package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;

/**
 * Estado y contadores de un procesamiento histórico.
 */
public record ProcesamientoHistoricoResponse(
        Long idProcesamiento,
        EstadoProcesamientoHistorico estado,
        ModoProcesamientoHistorico modo,
        Long filtroIdPublicacion,
        Long filtroIdPerfil,
        LocalDateTime filtroFechaDesde,
        LocalDateTime filtroFechaHasta,
        boolean notificar,
        int tamanoLote,
        Integer limitePublicaciones,
        int totalPublicaciones,
        int publicacionesProcesadas,
        int totalImagenes,
        int imagenesProcesadas,
        int hashesCalculados,
        int propuestasCreadas,
        int propuestasOmitidas,
        int errores,
        Long ultimoIdPublicacion,
        Long idUsuarioAdmin,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String mensajeError) {
}
