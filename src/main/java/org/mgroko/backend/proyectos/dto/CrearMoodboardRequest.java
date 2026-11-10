package org.mgroko.backend.proyectos.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearMoodboardRequest(
        @Size(max = 200, message = "La descripción del moodboard no puede superar los 200 caracteres.")
        String descripcion,

        /*
         * UC-36: lista completa de imágenes que deben conservarse. null/omitido
         * significa "no tocar las imágenes"; [] elimina todas. En la creación
         * (UC-24) este campo se ignora porque las imágenes se suben después.
         */
        @Size(max = 10, message = "El moodboard no puede conservar más de 10 imágenes.")
        List<@NotNull(message = "El id de imagen no puede ser nulo.")
             @Positive(message = "El id de imagen debe ser positivo.") Long> imagenesConservadas
) {

    /**
     * Constructor histórico para los consumidores que solo envían descripción.
     */
    public CrearMoodboardRequest(String descripcion) {
        this(descripcion, null);
    }
}
