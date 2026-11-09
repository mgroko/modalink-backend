package org.mgroko.backend.admin.exception;

import java.util.List;

import org.mgroko.backend.admin.dto.ValidacionPlantillaResponse;

public class PlantillaContratoInvalidaException extends RuntimeException {

    private final ValidacionPlantillaResponse validacion;
    private final List<String> errores;

    public PlantillaContratoInvalidaException(
            String message,
            List<String> errores,
            ValidacionPlantillaResponse validacion) {
        super(message);
        this.errores = errores == null ? List.of() : List.copyOf(errores);
        this.validacion = validacion;
    }

    public List<String> getErrores() {
        return errores;
    }

    public ValidacionPlantillaResponse getValidacion() {
        return validacion;
    }
}
