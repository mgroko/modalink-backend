package org.mgroko.backend.polaroids.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Modificación parcial de una polaroid: imagen, tipo o ambos. La validación
 * de "al menos uno" se hace en el servicio para devolver un error de negocio.
 */
@Getter
@Setter
public class ModificarPolaroidRequest {

    private MultipartFile archivo;

    @Positive(message = "El tipo de polaroid debe ser válido.")
    private Long idTipoPolaroid;
}
