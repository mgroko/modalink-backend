package org.mgroko.backend.contratos.dto;

/**
 * Opción del filtro "Proyecto" de UC-68.
 *
 * <p>Solo incluye proyectos donde el perfil activo tiene al menos un contrato
 * visible: como director activo o como parte firmante. Esto permite listar
 * también proyectos históricos en los que el perfil ya no tiene membresía
 * activa pero conserva contratos firmados.</p>
 */
public record ProyectoFiltroContratoResponse(
        Long idProyecto,
        String nombre
) {
}
