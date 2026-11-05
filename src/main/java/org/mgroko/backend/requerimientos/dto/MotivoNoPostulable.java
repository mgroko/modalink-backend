package org.mgroko.backend.requerimientos.dto;

/**
 * Motivo estructural por el que un requerimiento abierto no puede ser
 * postulado por el perfil activo.
 *
 * <p>La postulación previa no se informa acá: se expone por separado en
 * {@code yaPostulado} y {@code estadoPostulacion}, porque el requerimiento
 * sigue siendo estructuralmente postulable.</p>
 */
public enum MotivoNoPostulable {
    PROYECTO_NO_HABILITADO,
    PROYECTO_NO_ACEPTA_GENERAL,
    ACTIVIDAD_NO_PENDIENTE,
    MIEMBRO_ACTIVO,
    DIRECTOR,
    PROFESION,
    CUPO
}
