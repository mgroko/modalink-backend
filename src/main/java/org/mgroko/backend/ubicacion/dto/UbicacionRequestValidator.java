package org.mgroko.backend.ubicacion.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UbicacionRequestValidator implements ConstraintValidator<ValidUbicacionRequest, UbicacionRequest> {

    @Override
    public boolean isValid(UbicacionRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        boolean tieneProvincia = request.provinciaId() != null && !request.provinciaId().isBlank();
        boolean tieneLocalidad = request.localidadId() != null && !request.localidadId().isBlank();

        if (tieneProvincia && !tieneLocalidad) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("No se puede enviar provincia sin localidad.")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
