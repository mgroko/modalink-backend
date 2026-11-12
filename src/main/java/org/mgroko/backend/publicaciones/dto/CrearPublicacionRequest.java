package org.mgroko.backend.publicaciones.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Formulario multipart de UC-19. Los archivos se envían en el campo
 * {@code archivos}.
 */
@Getter
@Setter
public class CrearPublicacionRequest {

    @NotBlank(message = "El título de la publicación es obligatorio.")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres.")
    private String titulo;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres.")
    private String descripcion;

    @Positive(message = "El proyecto debe ser válido.")
    private Long idProyecto;

    private String localidadId;

    private String provinciaId;

    @NotEmpty(message = "Debés adjuntar al menos una imagen.")
    @Size(max = 10, message = "Una publicación puede tener como máximo 10 imágenes.")
    private List<MultipartFile> archivos;
}
