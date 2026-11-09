package org.mgroko.backend.auditoria.exception;

public class ExportacionAuditoriaExcedidaException extends RuntimeException {

    public ExportacionAuditoriaExcedidaException(long total, int maximo) {
        super("La exportación contiene " + total
                + " registros y el máximo permitido es " + maximo
                + ". Reducí el rango de fechas o los filtros.");
    }
}
