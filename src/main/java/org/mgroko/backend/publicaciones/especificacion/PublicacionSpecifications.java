package org.mgroko.backend.publicaciones.especificacion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.publicaciones.dto.BuscarPublicacionesFiltro;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

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

            Subquery<Long> colaborador = query.subquery(Long.class);
            Root<Publicacion> publicacionColaborador = colaborador.from(Publicacion.class);
            var colaboradores = publicacionColaborador.join("colaboradores");
            colaborador.select(publicacionColaborador.<Long>get("idPublicacion"));
            colaborador.where(
                    cb.equal(
                            publicacionColaborador.get("idPublicacion"),
                            root.get("idPublicacion")),
                    cb.equal(colaboradores.get("idPerfil"), idPerfil));

            predicates.add(cb.or(
                    cb.equal(root.get("perfil").get("idPerfil"), idPerfil),
                    cb.and(
                            cb.exists(colaborador),
                            cb.equal(root.get("perfil").get("estado"), EstadoPerfil.ACTIVO),
                            cb.equal(
                                    root.get("perfil").get("usuario").get("estado"),
                                    EstadoUsuario.ACTIVO))));
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

    /**
     * Feed de publicaciones de un conjunto de perfiles (contactos activos y,
     * opcionalmente, el perfil propio).
     */
    public static Specification<Publicacion> conPerfiles(Collection<Long> idsPerfiles) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(root.get("perfil").get("idPerfil").in(idsPerfiles));
            predicates.add(cb.equal(root.get("estado"), EstadoPublicacion.ACTIVA));
            predicates.add(cb.equal(root.get("perfil").get("estado"), EstadoPerfil.ACTIVO));
            predicates.add(cb.equal(root.get("perfil").get("usuario").get("estado"), EstadoUsuario.ACTIVO));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * UC-69: publicaciones ocultas del propio perfil. Solo se incluyen las que
     * pertenecen al perfil indicado, nunca las que solo tiene como colaborador.
     */
    public static Specification<Publicacion> conFiltrosOcultas(
            Long idPerfil,
            String texto) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("perfil").get("idPerfil"), idPerfil));
            predicates.add(cb.equal(root.get("estado"), EstadoPublicacion.OCULTA));

            if (tieneTexto(texto)) {
                String patron = LikePatrones.contiene(texto);
                predicates.add(cb.or(
                        cb.like(
                                cb.lower(root.get("titulo")),
                                patron,
                                LikePatrones.CARACTER_ESCAPE),
                        cb.like(
                                cb.lower(root.get("descripcion")),
                                patron,
                                LikePatrones.CARACTER_ESCAPE)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
