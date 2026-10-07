package org.mgroko.backend.usuario.dto;

import java.time.LocalDate;

import org.mgroko.backend.ubicacion.dto.UbicacionResponse;

/**
 * Datos personales del usuario con su ubicacion.
 *
 * {@code ubicacion} es la ubicacion completa y estructurada. Antes era un
 * {@code String} formado a mano como {@code "localidad, provincia"}, lo que
 * perdia la provincia cuando la localidad era nula, producia comas sueltas y
 * no distinguia una ubicacion ausente de una con datos.
 *
 * @param idUsuario      id del usuario
 * @param nombre         nombre
 * @param apellido       apellido
 * @param fechaNacimiento fecha de nacimiento
 * @param genero         codigo de genero
 * @param ubicacion      ubicacion del usuario, o {@code null} si no tiene
 */
public record DatosPersonalesResponse(
        Long idUsuario,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        String genero,
        UbicacionResponse ubicacion
) {
}
