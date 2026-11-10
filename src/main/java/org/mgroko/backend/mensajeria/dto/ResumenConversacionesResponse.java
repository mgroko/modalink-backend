package org.mgroko.backend.mensajeria.dto;

/**
 * Resumen para el badge de “Mensajes”: conversaciones con al menos un mensaje
 * no leído, no cantidad de mensajes.
 */
public record ResumenConversacionesResponse(
        long totalConversaciones,
        long conversacionesNoLeidas
) {
}
