package org.mgroko.backend.admin.exception;

public class ProcesamientoHistoricoSinDatosException extends RuntimeException {

    public ProcesamientoHistoricoSinDatosException() {
        super("No hay publicaciones que cumplan los filtros.");
    }
}
