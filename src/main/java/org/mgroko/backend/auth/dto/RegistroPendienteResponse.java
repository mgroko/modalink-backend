package org.mgroko.backend.auth.dto;

/**
 * Datos que el frontend usa para prellenar el formulario de onboarding de
 * Google. No incluye el "sub" ni datos sensibles del ticket.
 */
public record RegistroPendienteResponse(
        String nombre,
        String apellido,
        String correo) {
}
