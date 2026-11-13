package org.mgroko.backend.admin.mapper;

import org.mgroko.backend.admin.dto.ErrorProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.ProcesamientoHistoricoResponse;
import org.mgroko.backend.modelo.ProcesamientoColaboracionHistorica;
import org.mgroko.backend.modelo.ProcesamientoColaboracionHistoricaError;

public final class ProcesamientoHistoricoMapper {

    private ProcesamientoHistoricoMapper() {
    }

    public static ProcesamientoHistoricoResponse toResponse(
            ProcesamientoColaboracionHistorica procesamiento) {

        return new ProcesamientoHistoricoResponse(
                procesamiento.getIdProcesamiento(),
                procesamiento.getEstado(),
                procesamiento.getModo(),
                procesamiento.getFiltroIdPublicacion(),
                procesamiento.getFiltroIdPerfil(),
                procesamiento.getFiltroFechaDesde(),
                procesamiento.getFiltroFechaHasta(),
                procesamiento.isNotificar(),
                procesamiento.getTamanoLote(),
                procesamiento.getLimitePublicaciones(),
                procesamiento.getTotalPublicaciones(),
                procesamiento.getPublicacionesProcesadas(),
                procesamiento.getTotalImagenes(),
                procesamiento.getImagenesProcesadas(),
                procesamiento.getHashesCalculados(),
                procesamiento.getPropuestasCreadas(),
                procesamiento.getPropuestasOmitidas(),
                procesamiento.getErrores(),
                procesamiento.getUltimoIdPublicacion(),
                procesamiento.getIdUsuarioAdmin(),
                procesamiento.getFechaInicio(),
                procesamiento.getFechaFin(),
                procesamiento.getMensajeError());
    }

    public static ErrorProcesamientoHistoricoResponse toErrorResponse(
            ProcesamientoColaboracionHistoricaError error) {

        return new ErrorProcesamientoHistoricoResponse(
                error.getIdError(),
                error.getIdPublicacion(),
                error.getIdImagen(),
                error.getNombreArchivo(),
                error.getMensaje(),
                error.getFechaRegistro());
    }
}
