package org.mgroko.backend.polaroids.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Alta de polaroid en un solo paso: la imagen y el tipo elegido llegan juntos.
 */
@Getter
@Setter
public class CrearPolaroidRequest {

    @NotNull(message = "Debés adjuntar una imagen.")
    private MultipartFile archivo;

    @NotNull(message = "Debés indicar el tipo de polaroid.")
    @Positive(message = "El tipo de polaroid debe ser válido.")
    private Long idTipoPolaroid;
}
