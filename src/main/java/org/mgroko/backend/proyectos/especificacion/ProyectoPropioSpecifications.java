package org.mgroko.backend.proyectos.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.proyectos.dto.BuscarProyectosPropiosFiltro;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Búsqueda de proyectos propios: proyectos donde el perfil activo es miembro
 * ACTIVO (director o miembro) y, si el proyecto está CANCELADO, también
 * proyectos donde participó en el pasado (historial).
 */
public class ProyectoPropioSpecifications {

    private ProyectoPropioSpecifications() {}

    public static Specification<Proyecto> conFiltros(Long idPerfilActivo, BuscarProyectosPropiosFiltro filtro) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            // Alcance obligatorio: membresía activa del perfil activo
            predicates.add(conMembresiaActiva(query, cb, root, idPerfilActivo, filtro));

            if (filtro == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // Filtrado por nombre del proyecto (parcial, insensible a mayúsculas)
            if (filtro.nombre() != null && !filtro.nombre().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")),
                        LikePatrones.contiene(filtro.nombre()), LikePatrones.CARACTER_ESCAPE));
            }

            // Filtrado por estado del proyecto
            if (filtro.estado() != null) {
                predicates.add(cb.equal(root.get("estado"), filtro.estado()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate conMembresiaActiva(
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> root,
            Long idPerfilActivo,
            BuscarProyectosPropiosFiltro filtro) {

        var subquery = query.subquery(Long.class);
        var miembro = subquery.from(MiembroProyecto.class);

        subquery.select(miembro.get("idMiembro"));

        List<Predicate> subqueryPredicates = new ArrayList<>();
        subqueryPredicates.add(cb.equal(miembro.get("proyecto"), root));
        subqueryPredicates.add(cb.equal(miembro.get("perfil").get("idPerfil"), idPerfilActivo));
        subqueryPredicates.add(cb.or(
                cb.equal(miembro.get("estadoParticipacion"), EstadoParticipacion.ACTIVO),
                cb.equal(root.get("estado"), EstadoProyecto.CANCELADO)));

        if (filtro != null && filtro.rol() != null) {
            subqueryPredicates.add(conRol(filtro.rol(), miembro, cb));
        }

        subquery.where(subqueryPredicates.toArray(new Predicate[0]));
        return cb.exists(subquery);
    }

    private static Predicate conRol(RolProyectoUsuario rol, Path<MiembroProyecto> miembro, CriteriaBuilder cb) {
        return switch (rol) {
            case DIRECTOR -> ProyectoSpecifications.esRolDirector(miembro, cb);
            case MIEMBRO -> cb.or(
                    cb.equal(cb.lower(miembro.get("rolProyecto").get("codigo")), "miembro"),
                    cb.like(cb.lower(miembro.get("rolProyecto").get("nombre")),
                            ProyectoSpecifications.PATRON_ROL_MIEMBRO, LikePatrones.CARACTER_ESCAPE));
        };
    }
}
