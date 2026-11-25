package org.mgroko.backend.usuario.servicio;

/**
 * Metricas de una ejecucion de baja definitiva. Se usan para el resumen
 * informativo del panel de administracion.
 */
public record ResumenBajaDefinitiva(
        int cuentas,
        int perfiles,
        int proyectosCancelados,
        int membresiasBaja,
        int publicacionesEliminadas,
        int conexionesEliminadas) {

    public static ResumenBajaDefinitiva vacio() {
        return new ResumenBajaDefinitiva(0, 0, 0, 0, 0, 0);
    }

    public ResumenBajaDefinitiva sumar(ResumenBajaDefinitiva otro) {
        if (otro == null) {
            return this;
        }
        return new ResumenBajaDefinitiva(
                cuentas + otro.cuentas,
                perfiles + otro.perfiles,
                proyectosCancelados + otro.proyectosCancelados,
                membresiasBaja + otro.membresiasBaja,
                publicacionesEliminadas + otro.publicacionesEliminadas,
                conexionesEliminadas + otro.conexionesEliminadas);
    }

    public int totalCuentasPerfiles() {
        return cuentas + perfiles;
    }
}
