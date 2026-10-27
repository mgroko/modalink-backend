package org.mgroko.backend.postulaciones.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.postulaciones.dto.BuscarPostulacionesFiltro;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

/**
 * Filtros de UC-74. Cada tipo de postulación vive en su propia tabla y se
 * consulta por separado; el servicio une y ordena los resultados.
 */
public final class PostulacionSpecifications {

    private PostulacionSpecifications() {
    }

    public static Specification<PostulacionGral> generales(
            Long idProyecto,
            BuscarPostulacionesFiltro filtro) {

        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }

            Join<PostulacionGral, Perfil> perfil = root.join("perfil");
            Join<PostulacionGral, RequerimientoGralProyecto> requerimiento =
                    root.join("requerimientoGral");
            Join<RequerimientoGralProyecto, Proyecto> proyecto =
                    requerimiento.join("proyecto");

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(proyecto.get("idProyecto"), idProyecto));

            aplicarFiltrosComunes(predicates, cb, root, perfil, filtro);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<PostulacionActividad> actividades(
            Long idProyecto,
            BuscarPostulacionesFiltro filtro) {

        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }

            Join<PostulacionActividad, Perfil> perfil = root.join("perfil");
            Join<PostulacionActividad, RequerimientoActividad> requerimiento =
                    root.join("requerimiento");
            Join<RequerimientoActividad, Actividad> actividad =
                    requerimiento.join("actividad");
            Join<Actividad, Planificacion> planificacion =
                    actividad.join("planificacion");
            Join<Planificacion, Proyecto> proyecto =
                    planificacion.join("proyecto");

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(proyecto.get("idProyecto"), idProyecto));

            aplicarFiltrosComunes(predicates, cb, root, perfil, filtro);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void aplicarFiltrosComunes(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Root<?> root,
            Join<?, Perfil> perfil,
            BuscarPostulacionesFiltro filtro) {

        if (filtro == null) {
            return;
        }

        if (filtro.estado() != null && !filtro.estado().isEmpty()) {
            predicates.add(root.get("estado").in(filtro.estado()));
        }

        if (filtro.nombrePerfil() != null && !filtro.nombrePerfil().isBlank()) {
            predicates.add(cb.like(
                    cb.lower(perfil.get("nombreArtistico")),
                    LikePatrones.contiene(filtro.nombrePerfil()),
                    LikePatrones.CARACTER_ESCAPE));
        }

        if (filtro.idProfesion() != null) {
            predicates.add(cb.equal(
                    perfil.get("profesion").get("idProfesion"),
                    filtro.idProfesion()));
        }

        if (filtro.fechaDesde() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    root.get("fechaPostulacion"),
                    filtro.fechaDesde().atStartOfDay()));
        }

        if (filtro.fechaHasta() != null) {
            predicates.add(cb.lessThan(
                    root.get("fechaPostulacion"),
                    filtro.fechaHasta().plusDays(1).atStartOfDay()));
        }
    }
}
