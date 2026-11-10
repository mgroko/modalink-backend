package org.mgroko.backend.proyectos.exception;

public class ImagenMoodboardNoEncontradaException extends RuntimeException {

    public ImagenMoodboardNoEncontradaException(Long idImagen) {
        super(idImagen != null
                ? "La imagen con id " + idImagen + " no pertenece al moodboard del proyecto."
                : "La imagen indicada no pertenece al moodboard del proyecto.");
    }
}
