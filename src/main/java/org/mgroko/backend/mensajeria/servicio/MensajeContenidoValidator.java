package org.mgroko.backend.mensajeria.servicio;

import org.mgroko.backend.mensajeria.exception.MensajeInvalidoException;

/**
 * Regla única de validación y normalización del contenido de un mensaje.
 * Se comparte entre el chat privado (UC-42) y el chat grupal (UC-41).
 */
public final class MensajeContenidoValidator {

    public static final int MAX_CONTENIDO = 1000;

    private MensajeContenidoValidator() {
    }

    public static String normalizar(String contenido) {
        String texto = contenido == null ? "" : contenido.trim();

        if (texto.isEmpty()) {
            throw new MensajeInvalidoException("El mensaje no puede estar vacío.");
        }

        if (texto.length() > MAX_CONTENIDO) {
            throw new MensajeInvalidoException(
                    "El mensaje no puede superar los 1000 caracteres.");
        }

        return texto;
    }
}
