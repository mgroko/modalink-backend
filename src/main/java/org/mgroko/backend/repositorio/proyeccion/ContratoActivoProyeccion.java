package org.mgroko.backend.repositorio.proyeccion;

/**
 * Proyeccion de un contrato ACTIVO y la parte destinataria que lo tiene.
 * Se usa en UC-39 para advertir conflictos antes de generar el contrato.
 */
public interface ContratoActivoProyeccion {

    Long getIdMiembro();

    Long getIdTipoAcuerdo();

    Long getIdContrato();

    String getNombre();
}
