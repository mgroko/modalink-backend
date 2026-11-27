package org.mgroko.backend.perfiles.evento;

/**
 * Evento publicado cuando el administrador modifica una característica
 * técnica. {@code motivo} permite distinguir cambios de datos del catálogo
 * ({@code DATOS}) de cambios en sus valores ({@code VALOR_AGREGADO},
 * {@code VALOR_MODIFICADO}).
 */
public record CaracteristicaTecnicaModificadaEvent(
        Long idCaracteristica,
        String codigo,
        String nombre,
        Long idProfesion,
        String nombreProfesion,
        String motivo
) {
}
