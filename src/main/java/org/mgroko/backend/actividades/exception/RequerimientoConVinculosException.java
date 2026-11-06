package org.mgroko.backend.actividades.exception;

public class RequerimientoConVinculosException extends RuntimeException {

    private final Long idRequerimiento;

    public RequerimientoConVinculosException(Long idRequerimiento) {
        super("No se puede modificar ni eliminar el requerimiento " + idRequerimiento
                + " porque tiene postulaciones o invitaciones aceptadas, "
                + "o asignaciones asociadas.");
        this.idRequerimiento = idRequerimiento;
    }

    public Long getIdRequerimiento() {
        return idRequerimiento;
    }
}
