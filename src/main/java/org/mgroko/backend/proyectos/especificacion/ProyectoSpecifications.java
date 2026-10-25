package org.mgroko.backend.proyectos.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public class ProyectoSpecifications {

    private ProyectoSpecifications() {}

    /** Patrón de rol director: código 'DIRECTOR' o nombre 'Director...' (semilla de BD). */
    public static final String PATRON_ROL_DIRECTOR = "director%";

    /** Patrón de rol miembro: código 'MIEMBRO' o nombre 'Miembro...' (semilla de BD). */
    public static final String PATRON_ROL_MIEMBRO = "miembro%";

    /**
     * Búsqueda global (UC-29) con regla de visibilidad base:
     * un proyecto es visible para el perfil activo si es miembro activo,
     * o si no está en Borrador ni Cancelado y es Público. Los proyectos
     * cancelados solo se consultan desde "proyectos propios" (historial).
     */
    public static Specification<Proyecto> conFiltros(BuscarProyectosFiltro filtro, Long idPerfilActivo) {
        return (root, query, cb) -> {
            // Evitamos registros duplicados al hacer joins con colecciones
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(conPredicadoVisibilidad(query, cb, root, idPerfilActivo));

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

            // Filtrado por director: miembro activo con rol director cuyo perfil coincide
            if (filtro.director() != null && !filtro.director().isBlank()) {
                predicates.add(conSubqueryDirector(query, cb, root, filtro.director()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Visibilidad: el perfil activo es miembro activo del proyecto, o el proyecto
     * es Público, no está en Borrador y no está Cancelado (misma regla que el
     * detalle del proyecto).
     */
    private static Predicate conPredicadoVisibilidad(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> root,
            Long idPerfilActivo) {

        Predicate esMiembroActivo = conSubqueryMiembroActivo(query, cb, root, idPerfilActivo);

        Predicate publicoNoBorrador = cb.and(
                cb.notEqual(root.get("estado"), EstadoProyecto.BORRADOR),
                cb.equal(root.get("privacidad"), Privacidad.PUBLICO));

        Predicate noCancelado = cb.notEqual(root.get("estado"), EstadoProyecto.CANCELADO);

        return cb.and(noCancelado, cb.or(esMiembroActivo, publicoNoBorrador));
    }

    /** Subquery: el perfil activo tiene una membresía ACTIVA en el proyecto. */
    private static Predicate conSubqueryMiembroActivo(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> root,
            Long idPerfilActivo) {

        var subquery = query.subquery(Long.class);
        var miembro = subquery.from(MiembroProyecto.class);

        subquery.select(miembro.get("proyecto").get("idProyecto"));
        subquery.where(
                cb.equal(miembro.get("proyecto"), root),
                cb.equal(miembro.get("estadoParticipacion"), EstadoParticipacion.ACTIVO),
                cb.equal(miembro.get("perfil").get("idPerfil"), idPerfilActivo));

        return cb.exists(subquery);
    }

    /** Predicado genérico: el miembro posee el rol Director (por codigo o por nombre). */
    public static Predicate esRolDirector(Path<MiembroProyecto> miembro, CriteriaBuilder cb) {
        return cb.or(
                cb.equal(cb.lower(miembro.get("rolProyecto").get("codigo")), "director"),
                cb.like(cb.lower(miembro.get("rolProyecto").get("nombre")),
                        PATRON_ROL_DIRECTOR, LikePatrones.CARACTER_ESCAPE));
    }

    private static Predicate conSubqueryDirector(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> root,
            String director) {

        var subquery = query.subquery(Long.class);
        var miembro = subquery.from(MiembroProyecto.class);

        subquery.select(miembro.get("proyecto").get("idProyecto"));

        List<Predicate> subqueryPredicates = new ArrayList<>();
        subqueryPredicates.add(cb.equal(miembro.get("proyecto"), root));
        subqueryPredicates.add(cb.equal(miembro.get("estadoParticipacion"), EstadoParticipacion.ACTIVO));
        subqueryPredicates.add(esRolDirector(miembro, cb));

        Join<MiembroProyecto, Perfil> perfil = miembro.join("perfil");
        var usuario = perfil.join("usuario");

        String patron = LikePatrones.contiene(director);
        subqueryPredicates.add(cb.or(
                cb.like(cb.lower(perfil.get("nombreArtistico")), patron, LikePatrones.CARACTER_ESCAPE),
                cb.like(cb.lower(usuario.get("nombre")), patron, LikePatrones.CARACTER_ESCAPE),
                cb.like(cb.lower(usuario.get("apellido")), patron, LikePatrones.CARACTER_ESCAPE)));

        subquery.where(subqueryPredicates.toArray(new Predicate[0]));
        return cb.exists(subquery);
    }
}
