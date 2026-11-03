package org.mgroko.backend.calendario.servicio;

/**
 * Reparto del margen configurado entre dos actividades consecutivas.
 *
 * <p>El margen es <strong>total</strong>: con 30 minutos, la separación entre
 * el fin de una actividad y el inicio de la siguiente es de 30 minutos. Para
 * aplicarlo de forma simétrica, cada actividad reserva la mitad del margen a
 * cada lado. Con valores impares, la mitad izquierda se redondea hacia abajo y
 * la diferencia se asigna a la derecha, de modo que la suma siempre coincide
 * con el margen configurado.</p>
 */
public final class MargenActividad {

    private MargenActividad() {
    }

    public static int izquierdo(int margen) {
        int efectivo = Math.max(0, margen);
        return efectivo / 2;
    }

    public static int derecho(int margen) {
        int efectivo = Math.max(0, margen);
        return efectivo - izquierdo(efectivo);
    }

    public static long izquierdo(long margen) {
        long efectivo = Math.max(0L, margen);
        return efectivo / 2L;
    }

    public static long derecho(long margen) {
        long efectivo = Math.max(0L, margen);
        return efectivo - izquierdo(efectivo);
    }
}
