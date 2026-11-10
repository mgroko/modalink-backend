package org.mgroko.backend.publicaciones.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.publicaciones.dto.BuscarPublicacionesFiltro;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros de UC-22 para el repositorio de publicaciones de un perfil.
 */
public final class PublicacionSpecifications {

    private PublicacionSpecifications() {
    }

    public static Specification<Publicacion> conFiltros(
            Long idPerfil,
            BuscarPublicacionesFiltro filtro) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("perfil").get("idPerfil"), idPerfil));
            predicates.add(cb.equal(root.get("estado"), EstadoPublicacion.ACTIVA));

            if (filtro != null) {
                if (filtro.fechaDesde() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("fechaPublicacion"),
                            filtro.fechaDesde().atStartOfDay()));
                }

                if (filtro.fechaHasta() != null) {
                    predicates.add(cb.lessThan(
                            root.get("fechaPublicacion"),
                            filtro.fechaHasta().plusDays(1).atStartOfDay()));
                }

                if (filtro.idProyecto() != null) {
                    predicates.add(cb.equal(
                            root.get("proyecto").get("idProyecto"),
                            filtro.idProyecto()));
                }

                if (tieneTexto(filtro.titulo())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("titulo")),
                            LikePatrones.contiene(filtro.titulo()),
                            LikePatrones.CARACTER_ESCAPE));
                }

                if (tieneTexto(filtro.descripcion())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("descripcion")),
                            LikePatrones.contiene(filtro.descripcion()),
                            LikePatrones.CARACTER_ESCAPE));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
