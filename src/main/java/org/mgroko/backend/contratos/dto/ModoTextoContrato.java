package org.mgroko.backend.contratos.dto;

/**
 * Modo de resolucion del texto de un contrato.
 *
 * <p>{@link #AUTO} regenera el texto desde la plantilla del tipo de acuerdo y
 * las clausulas adicionales. {@link #MANUAL} usa el texto enviado por el
 * cliente. Si el modo no se informa, se conserva la precedencia historica: un
 * texto no vacio gana sobre la regeneracion.</p>
 */
public enum ModoTextoContrato {
    AUTO,
    MANUAL
}
