package org.mgroko.backend.perfiles.evento;

/**
 * Evento publicado cuando el administrador crea una característica técnica
 * para una profesión. El listener resuelve los perfiles destinatarios después
 * del commit.
 */
public record CaracteristicaTecnicaCreadaEvent(
        Long idCaracteristica,
        String codigo,
        String nombre,
        Long idProfesion,
        String nombreProfesion
) {
}
