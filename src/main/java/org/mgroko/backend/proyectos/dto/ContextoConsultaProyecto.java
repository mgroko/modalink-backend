package org.mgroko.backend.proyectos.dto;

import java.util.List;

import org.mgroko.backend.modelo.enums.RolProyectoUsuario;

/**
 * Relación del perfil consultante con el proyecto, incluida en la respuesta del
 * detalle (GET /proyectos/{idProyecto}) y de la creación (POST /proyectos).
 * Permite al frontend condicionar la UI según el rol y los permisos del
 * consultante sin llamadas adicionales.
 *
 * @param rolUsuario             rol del perfil en el proyecto; null si no es miembro activo
 * @param esMiembro              true si el perfil es miembro activo del proyecto
 * @param permisos               permisos del rol en el proyecto; vacío si no es miembro
 * @param elegibleParaPostularse true si, sin ser miembro, el proyecto acepta postulaciones
 *                               y la profesión del perfil coincide con algún requerimiento
 */
public record ContextoConsultaProyecto(
        RolProyectoUsuario rolUsuario,
        Boolean esMiembro,
        List<String> permisos,
        Boolean elegibleParaPostularse) {
}
