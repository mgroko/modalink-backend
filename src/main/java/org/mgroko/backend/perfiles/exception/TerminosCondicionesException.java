package org.mgroko.backend.perfiles.exception;

import org.mgroko.backend.modelo.PerfilTyc;

/**
 * Conflicto de aceptación de términos y condiciones de un perfil.
 *
 * <p>Incluye la versión vigente para que el frontend pueda refrescar el texto
 * sin perder el contexto de la operación.</p>
 */
public class TerminosCondicionesException extends RuntimeException {

    public static final String NO_ACEPTADOS = "TERMINOS_NO_ACEPTADOS";
    public static final String DESACTUALIZADOS = "TERMINOS_DESACTUALIZADOS";
    public static final String NO_VIGENTES = "TERMINOS_NO_VIGENTES";

    private final String codigo;
    private final Long idPerfil;
    private final Long idTycVigente;
    private final String descripcionVigente;
    private final boolean esPlantilla;

    private TerminosCondicionesException(
            String codigo,
            String mensaje,
            Long idPerfil,
            Long idTycVigente,
            String descripcionVigente,
            boolean esPlantilla) {
        super(mensaje);
        this.codigo = codigo;
        this.idPerfil = idPerfil;
        this.idTycVigente = idTycVigente;
        this.descripcionVigente = descripcionVigente;
        this.esPlantilla = esPlantilla;
    }

    public static TerminosCondicionesException noAceptados(
            Long idPerfil,
            PerfilTyc vigente) {
        return new TerminosCondicionesException(
                NO_ACEPTADOS,
                "Debe aceptar los términos y condiciones vigentes del perfil antes de continuar.",
                idPerfil,
                vigente != null ? vigente.getIdTyc() : null,
                vigente != null ? vigente.getDescripcion() : null,
                vigente == null);
    }

    public static TerminosCondicionesException desactualizados(
            Long idPerfil,
            PerfilTyc vigente) {
        return new TerminosCondicionesException(
                DESACTUALIZADOS,
                "Los términos y condiciones del perfil cambiaron. Revisá la versión vigente antes de continuar.",
                idPerfil,
                vigente != null ? vigente.getIdTyc() : null,
                vigente != null ? vigente.getDescripcion() : null,
                vigente == null);
    }

    public static TerminosCondicionesException noVigentes(Long idPerfil) {
        return new TerminosCondicionesException(
                NO_VIGENTES,
                "El perfil ya no tiene una versión vigente de términos y condiciones.",
                idPerfil,
                null,
                null,
                true);
    }

    public String getCodigo() {
        return codigo;
    }

    public Long getIdPerfil() {
        return idPerfil;
    }

    public Long getIdTycVigente() {
        return idTycVigente;
    }

    public String getDescripcionVigente() {
        return descripcionVigente;
    }

    public boolean isEsPlantilla() {
        return esPlantilla;
    }
}
