package org.mgroko.backend.reportes.exception;

/**
 * Ya existe un reporte pendiente del mismo perfil autor contra la misma
 * entidad.
 */
public class ReportePendienteExistenteException extends RuntimeException {

    public ReportePendienteExistenteException() {
        super("Ya existe un reporte pendiente de este perfil contra la misma entidad.");
    }
}
