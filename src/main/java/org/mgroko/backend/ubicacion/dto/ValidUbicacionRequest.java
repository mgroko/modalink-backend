package org.mgroko.backend.ubicacion.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida la coherencia entre los campos de {@link UbicacionRequest}.
 * En particular, verifica que si se envía {@code provinciaId} también se
 * envíe {@code localidadId}.
 */
@Documented
@Constraint(validatedBy = UbicacionRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUbicacionRequest {

    String message() default "No se puede enviar provincia sin localidad.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
