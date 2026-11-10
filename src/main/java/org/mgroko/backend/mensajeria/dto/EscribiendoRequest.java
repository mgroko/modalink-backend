package org.mgroko.backend.mensajeria.dto;

/**
 * Aviso de escritura del chat. Si se omite, se asume {@code true}.
 */
public record EscribiendoRequest(
        Boolean escribiendo
) {
}
