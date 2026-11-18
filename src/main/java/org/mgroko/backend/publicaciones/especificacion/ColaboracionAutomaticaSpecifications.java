package org.mgroko.backend.publicaciones.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.ColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.publicaciones.dto.BuscarColaboracionesAutomaticasFiltro;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public final class ColaboracionAutomaticaSpecifications {

    private ColaboracionAutomaticaSpecifications() {
    }

    public static Specification<ColaboracionAutomatica> conFiltros(
            Long idPerfilActivo,
            BuscarColaboracionesAutomaticasFiltro filtro,
            EstadoColaboracionAutomatica estado) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<ColaboracionAutomatica, ?> perfilA = root.join("perfilA", JoinType.INNER);
            Join<ColaboracionAutomatica, ?> perfilB = root.join("perfilB", JoinType.INNER);
            Join<?, ?> usuarioA = perfilA.join("usuario", JoinType.INNER);
            Join<?, ?> usuarioB = perfilB.join("usuario", JoinType.INNER);

            predicates.add(cb.or(
                    cb.equal(perfilA.get("idPerfil"), idPerfilActivo),
                    cb.equal(perfilB.get("idPerfil"), idPerfilActivo)));
            predicates.add(cb.equal(perfilA.get("estado"), EstadoPerfil.ACTIVO));
            predicates.add(cb.equal(perfilB.get("estado"), EstadoPerfil.ACTIVO));
            predicates.add(cb.equal(usuarioA.get("estado"), EstadoUsuario.ACTIVO));
            predicates.add(cb.equal(usuarioB.get("estado"), EstadoUsuario.ACTIVO));

            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }

            if (filtro != null) {
                if (filtro.idPublicacion() != null) {
                    predicates.add(cb.or(
                            cb.equal(root.get("publicacionA").get("idPublicacion"),
                                    filtro.idPublicacion()),
                            cb.equal(root.get("publicacionB").get("idPublicacion"),
                                    filtro.idPublicacion())));
                }

                if (filtro.idPerfilContraparte() != null) {
                    Predicate activoEsA = cb.and(
                            cb.equal(perfilA.get("idPerfil"), idPerfilActivo),
                            cb.equal(perfilB.get("idPerfil"), filtro.idPerfilContraparte()));
                    Predicate activoEsB = cb.and(
                            cb.equal(perfilB.get("idPerfil"), idPerfilActivo),
                            cb.equal(perfilA.get("idPerfil"), filtro.idPerfilContraparte()));
                    predicates.add(cb.or(activoEsA, activoEsB));
                }

                if (filtro.fechaDesde() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("fechaDeteccion"),
                            filtro.fechaDesde().atStartOfDay()));
                }

                if (filtro.fechaHasta() != null) {
                    predicates.add(cb.lessThan(
                            root.get("fechaDeteccion"),
                            filtro.fechaHasta().plusDays(1).atStartOfDay()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
