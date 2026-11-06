package org.mgroko.backend.postulaciones.exception;

public class PostulacionYaGestionadaException extends RuntimeException {

    public PostulacionYaGestionadaException() {
        super("La postulación ya fue gestionada. Actualizá el listado e intentá nuevamente.");
    }
}
