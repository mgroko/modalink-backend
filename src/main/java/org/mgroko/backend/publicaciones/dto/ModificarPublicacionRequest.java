package org.mgroko.backend.publicaciones.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Formulario multipart de UC-21. {@code imagenesConservadas} es obligatorio:
 * debe contener los IDs de todas las imágenes existentes que se quieren
 * conservar, en el orden deseado. Las que no se incluyan se eliminan. Las
 * imágenes nuevas se envían en {@code archivos} y se agregan al final.
 */
@Getter
@Setter
public class ModificarPublicacionRequest {

    @NotBlank(message = "El título de la publicación es obligatorio.")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres.")
    private String titulo;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres.")
    private String descripcion;

    @Positive(message = "El proyecto debe ser válido.")
    private Long idProyecto;

    private String localidadId;

    private String provinciaId;

    @NotNull(message = "Debés indicar las imágenes que se conservan.")
    @Size(max = 10, message = "Una publicación puede tener como máximo 10 imágenes.")
    private List<@Positive(message = "La imagen debe ser válida.") Long> imagenesConservadas;

    @Size(max = 10, message = "Una publicación puede tener como máximo 10 imágenes.")
    private List<MultipartFile> archivos;
}
