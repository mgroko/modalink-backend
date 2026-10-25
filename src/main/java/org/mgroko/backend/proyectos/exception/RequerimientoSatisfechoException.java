package org.mgroko.backend.proyectos.exception;

public class RequerimientoSatisfechoException extends RuntimeException {

    public RequerimientoSatisfechoException(Long idRequerimientoGral) {
        super("No se puede modificar ni eliminar el requerimiento " + idRequerimientoGral
                + " porque ya tiene postulaciones aceptadas.");
    }
}
