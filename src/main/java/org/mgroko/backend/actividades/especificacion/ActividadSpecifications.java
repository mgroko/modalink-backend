package org.mgroko.backend.actividades.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.actividades.dto.BuscarActividadesFiltro;
import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Filtros de UC-52. El proyecto es siempre obligatorio: la búsqueda está
 * acotada a la planificación del proyecto indicado.
 */
public class ActividadSpecifications {

    private ActividadSpecifications() {}

    public static Specification<Actividad> conFiltros(Long idProyecto, BuscarActividadesFiltro filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(
                    root.get("planificacion").get("proyecto").get("idProyecto"),
                    idProyecto));

            if (filtro == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (tieneTexto(filtro.nombre())) {
                predicates.add(likeLower(cb, root.get("nombre"), filtro.nombre()));
            }

            if (tieneTexto(filtro.descripcion())) {
                predicates.add(likeLower(cb, root.get("descripcion"), filtro.descripcion()));
            }

            if (filtro.duracionMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("duracionMinutos"), filtro.duracionMin()));
            }

            if (filtro.duracionMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("duracionMinutos"), filtro.duracionMax()));
            }

            if (filtro.estado() != null && !filtro.estado().isEmpty()) {
                predicates.add(root.get("estado").in(filtro.estado()));
            }

            if (tieneTexto(filtro.requerimiento())) {
                predicates.add(conRequerimiento(query, cb, root, filtro.requerimiento()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static Predicate likeLower(CriteriaBuilder cb, Path<String> campo, String valor) {
        return cb.like(
                cb.lower(campo),
                LikePatrones.contiene(valor),
                LikePatrones.CARACTER_ESCAPE);
    }

    /**
     * EXISTS sobre requerimientos de la actividad. El texto puede coincidir
     * con la profesión, la descripción del requerimiento, una habilidad o una
     * característica técnica.
     */
    private static Predicate conRequerimiento(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Actividad> root,
            String valor) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<RequerimientoActividad> requerimiento = subquery.from(RequerimientoActividad.class);

        String patron = LikePatrones.contiene(valor);

        Predicate coincideProfesion = cb.or(
                cb.like(cb.lower(requerimiento.get("profesion").get("nombre")),
                        patron, LikePatrones.CARACTER_ESCAPE),
                cb.like(cb.lower(requerimiento.get("profesion").get("codigo")),
                        patron, LikePatrones.CARACTER_ESCAPE));

        Predicate coincideDescripcion = cb.like(
                cb.lower(requerimiento.get("descripcion")),
                patron,
                LikePatrones.CARACTER_ESCAPE);

        Predicate coincideHabilidad = existeHabilidad(query, cb, requerimiento, patron);
        Predicate coincideCaracteristica = existeCaracteristica(query, cb, requerimiento, patron);

        subquery.select(requerimiento.get("idRequerimiento"));
        subquery.where(
                cb.equal(requerimiento.get("actividad"), root),
                cb.or(coincideProfesion, coincideDescripcion, coincideHabilidad, coincideCaracteristica));

        return cb.exists(subquery);
    }

    private static Predicate existeHabilidad(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoActividad> requerimiento,
            String patron) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<RequerimientoActividad> correlacionado = subquery.correlate(requerimiento);
        Join<RequerimientoActividad, Habilidad> habilidad = correlacionado.join("habilidades");

        subquery.select(habilidad.get("idHabilidad"));
        subquery.where(cb.or(
                cb.like(cb.lower(habilidad.get("nombre")), patron, LikePatrones.CARACTER_ESCAPE),
                cb.like(cb.lower(habilidad.get("codigo")), patron, LikePatrones.CARACTER_ESCAPE)));

        return cb.exists(subquery);
    }

    private static Predicate existeCaracteristica(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoActividad> requerimiento,
            String patron) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<RequerimientoActividad> correlacionado = subquery.correlate(requerimiento);
        Join<RequerimientoActividad, CaracteristicaTecnica> caracteristica =
                correlacionado.join("caracteristicas");

        subquery.select(caracteristica.get("idCaracteristica"));
        subquery.where(cb.or(
                cb.like(cb.lower(caracteristica.get("nombre")), patron, LikePatrones.CARACTER_ESCAPE),
                cb.like(cb.lower(caracteristica.get("codigo")), patron, LikePatrones.CARACTER_ESCAPE)));

        return cb.exists(subquery);
    }
}
