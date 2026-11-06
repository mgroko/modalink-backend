package org.mgroko.backend.modelo.enums;

import java.util.Locale;

/**
 * Tipos de evento expuestos por el historial de membresías del proyecto.
 *
 * <p>El historial se deriva de las membresías que ya no están activas, por lo
 * que solo se contemplan los dos estados terminales de
 * {@link EstadoParticipacion}.</p>
 */
public enum TipoEventoHistorial {

    MIEMBRO_ELIMINADO(EstadoParticipacion.ELIMINADO),
    MIEMBRO_BAJA_VOLUNTARIA(EstadoParticipacion.BAJA_VOLUNTARIA);

    private final EstadoParticipacion estadoParticipacion;

    TipoEventoHistorial(EstadoParticipacion estadoParticipacion) {
        this.estadoParticipacion = estadoParticipacion;
    }

    public EstadoParticipacion getEstadoParticipacion() {
        return estadoParticipacion;
    }

    /**
     * Convierte el parámetro {@code tipo} del endpoint en un tipo válido.
     *
     * @param valor valor recibido; puede ser null o vacío
     * @return el tipo correspondiente, o null si no se envió filtro
     * @throws IllegalArgumentException si el valor no corresponde a un tipo
     */
    public static TipoEventoHistorial desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        for (TipoEventoHistorial tipo : values()) {
            if (tipo.name().equals(normalizado)) {
                return tipo;
            }
        }

        throw new IllegalArgumentException(
                "Tipo de historial inválido: " + valor);
    }
}
