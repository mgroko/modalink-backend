package org.mgroko.backend.ubicacion.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud para asignar una ubicación a partir de una localidad del catálogo
 * de Georef. Se usa para el usuario y queda preparada para reutilizarse en
 * otros módulos que referencien una ubicación (proyecto, actividad).
 *
 * @param localidadId id de la localidad en el catálogo de Georef
 * @param provinciaId id de la provincia en el catálogo de Georef (opcional;
 *                    si se envía, la localidad es obligatoria)
 */
@ValidUbicacionRequest
public record UbicacionRequest(
        @NotBlank(message = "La localidad es obligatoria.")
        String localidadId,

        String provinciaId
) {

    public UbicacionRequest(String localidadId) {
        this(localidadId, null);
    }
}