package org.mgroko.backend.actividades.enums;

public enum UnidadDuracion {

    HORAS(60),
    MINUTOS(1);

    private final int minutosPorUnidad;

    UnidadDuracion(int minutosPorUnidad) {
        this.minutosPorUnidad = minutosPorUnidad;
    }

    public long aMinutos(long cantidad) {
        return Math.multiplyExact(cantidad, minutosPorUnidad);
    }
}
