package org.mgroko.backend.auth.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lanzado al intentar iniciar sesión con una cuenta cuya solicitud de baja
 * todavía está dentro del plazo de recuperación: no se emite sesión y el
 * cliente debe ofrecer la opción de reactivar la cuenta.
 */
public class CuentaPendienteBajaException extends RuntimeException {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LocalDateTime fechaSolicitudBaja;
    private final LocalDateTime fechaLimite;
    private final int diasBaja;

    public CuentaPendienteBajaException(LocalDateTime fechaSolicitudBaja, LocalDateTime fechaLimite, int diasBaja) {
        super(fechaLimite != null
                ? "Solicitaste la baja de tu cuenta. Reactívala antes del "
                        + fechaLimite.format(FORMATO_FECHA)
                        + " para poder iniciar sesión."
                : "Solicitaste la baja de tu cuenta. Reactívala para poder iniciar sesión.");
        this.fechaSolicitudBaja = fechaSolicitudBaja;
        this.fechaLimite = fechaLimite;
        this.diasBaja = diasBaja;
    }

    public LocalDateTime getFechaSolicitudBaja() {
        return fechaSolicitudBaja;
    }

    public LocalDateTime getFechaLimite() {
        return fechaLimite;
    }

    public int getDiasBaja() {
        return diasBaja;
    }
}
