package org.mgroko.backend.proyectos.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
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

    public static Specification<Proyecto> conFiltros(BuscarProyectosFiltro filtro) {
        return (root, query, cb) -> {
            // Evitamos registros duplicados al hacer joins con colecciones
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filtro == null) {
                return cb.conjunction();
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
