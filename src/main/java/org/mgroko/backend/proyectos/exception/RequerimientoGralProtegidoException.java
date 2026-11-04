package org.mgroko.backend.proyectos.exception;

/**
 * UC-36: un requerimiento general no se puede modificar ni eliminar porque
 * tiene cupo cubierto, miembros vinculados o postulaciones/invitaciones
 * pendientes o aceptadas.
 */
public class RequerimientoGralProtegidoException extends RuntimeException {

    private final Long idRequerimientoGral;

    public RequerimientoGralProtegidoException(Long idRequerimientoGral) {
        super("No se puede modificar ni eliminar el requerimiento " + idRequerimientoGral
                + " porque tiene cupo cubierto, miembros vinculados o "
                + "postulaciones/invitaciones pendientes o aceptadas.");
        this.idRequerimientoGral = idRequerimientoGral;
    }

    public Long getIdRequerimientoGral() {
        return idRequerimientoGral;
    }
}
