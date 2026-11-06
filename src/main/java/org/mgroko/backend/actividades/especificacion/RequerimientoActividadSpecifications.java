package org.mgroko.backend.actividades.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.actividades.dto.BuscarRequerimientosActividadFiltro;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActCaractValor;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class RequerimientoActividadSpecifications {

    private RequerimientoActividadSpecifications() {
    }

    public static Specification<RequerimientoActividad> conFiltros(
            Long idProyecto,
            BuscarRequerimientosActividadFiltro filtro) {

        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("fechaArchivado")));
            predicates.add(cb.equal(
                    root.get("actividad").get("planificacion").get("proyecto").get("idProyecto"),
                    idProyecto));

            if (filtro == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (filtro.idActividad() != null) {
                predicates.add(cb.equal(root.get("actividad").get("idActividad"),
                        filtro.idActividad()));
            }

            if (filtro.idProfesion() != null) {
                predicates.add(cb.equal(root.get("profesion").get("idProfesion"),
                        filtro.idProfesion()));
            }

            if (filtro.idCaracteristica() != null || filtro.idValorCaracteristica() != null) {
                Join<RequerimientoActividad, RequerimientoActCaract> caracteristicas =
                        root.join("caracteristicas", JoinType.INNER);

                if (filtro.idCaracteristica() != null) {
                    predicates.add(cb.equal(
                            caracteristicas.get("caracteristica").get("idCaracteristica"),
                            filtro.idCaracteristica()));
                }

                if (filtro.idValorCaracteristica() != null && query != null) {
                    Subquery<Long> valorSubquery = query.subquery(Long.class);
                    Root<RequerimientoActCaractValor> valor =
                            valorSubquery.from(RequerimientoActCaractValor.class);
                    valorSubquery.select(valor.get("idReqActCaract"))
                            .where(cb.equal(valor.get("idValor"),
                                    filtro.idValorCaracteristica()));
                    predicates.add(cb.in(caracteristicas.get("idReqActCaract"))
                            .value(valorSubquery));
                }
            }

            if (filtro.idsHabilidades() != null && !filtro.idsHabilidades().isEmpty()) {
                Join<RequerimientoActividad, Habilidad> habilidades =
                        root.join("habilidades", JoinType.INNER);
                predicates.add(habilidades.get("idHabilidad").in(filtro.idsHabilidades()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
