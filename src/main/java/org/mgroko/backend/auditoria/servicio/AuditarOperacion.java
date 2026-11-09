package org.mgroko.backend.auditoria.servicio;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de servicio que debe registrar al usuario autenticado como
 * operador real de la auditoría.
 *
 * <p>El aspecto {@link AuditoriaOperadorAspect} setea
 * {@code app.id_usuario} en la sesión de BD para que los triggers de
 * auditoría lo lean con {@code current_setting('app.id_usuario', true)}.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditarOperacion {
}
