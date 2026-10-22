package org.mgroko.backend.actividades.exception;

public class RequerimientoConVinculosException extends RuntimeException {

    public RequerimientoConVinculosException(Long idRequerimiento) {
        super("No se puede modificar ni eliminar el requerimiento " + idRequerimiento
                + " porque tiene postulaciones o invitaciones asociadas.");
    }
}
