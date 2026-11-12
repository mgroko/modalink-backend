package org.mgroko.backend.storage.exception;

/**
 * Se lanza cuando se intenta ejecutar la limpieza de imágenes mientras otra
 * ejecución sigue en curso.
 */
public class LimpiezaImagenesEnCursoException extends RuntimeException {

    public LimpiezaImagenesEnCursoException() {
        super("Ya hay una limpieza de imágenes en curso. Esperá a que termine e intentá nuevamente.");
    }
}
