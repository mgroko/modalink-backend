package org.mgroko.backend.admin.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.admin.dto.BuscarPerfilesAdminFiltro;
import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.Perfil;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros del listado administrativo de perfiles. No aplica restricciones de
 * visibilidad: el administrador puede consultar cualquier estado.
 */
public final class AdminPerfilSpecifications {

    private AdminPerfilSpecifications() {
    }

    public static Specification<Perfil> conFiltros(BuscarPerfilesAdminFiltro filtro) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();
            if (filtro == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (tieneTexto(filtro.texto())) {
                String patron = LikePatrones.contiene(filtro.texto());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nombreArtistico")), patron,
                                LikePatrones.CARACTER_ESCAPE),
                        cb.like(cb.lower(root.get("usuario").get("nombre")), patron,
                                LikePatrones.CARACTER_ESCAPE),
                        cb.like(cb.lower(root.get("usuario").get("apellido")), patron,
                                LikePatrones.CARACTER_ESCAPE),
                        cb.like(cb.lower(root.get("usuario").get("correo")), patron,
                                LikePatrones.CARACTER_ESCAPE),
                        cb.like(cb.lower(root.get("usuario").get("dni")), patron,
                                LikePatrones.CARACTER_ESCAPE)));
            }

            if (filtro.idProfesion() != null) {
                predicates.add(cb.equal(
                        root.get("profesion").get("idProfesion"),
                        filtro.idProfesion()));
            }

            if (filtro.estado() != null) {
                predicates.add(cb.equal(root.get("estado"), filtro.estado()));
            }

            if (filtro.idUsuario() != null) {
                predicates.add(cb.equal(
                        root.get("usuario").get("idUsuario"),
                        filtro.idUsuario()));
            }

            if (filtro.idGenero() != null) {
                predicates.add(cb.equal(
                        root.get("usuario").get("genero").get("idGenero"),
                        filtro.idGenero()));
            }

            if (filtro.idCiudad() != null) {
                predicates.add(cb.equal(
                        root.get("usuario").get("ubicacion").get("ciudad").get("idCiudad"),
                        filtro.idCiudad()));
            }

            if (filtro.idProvincia() != null) {
                predicates.add(cb.equal(
                        root.get("usuario").get("ubicacion").get("ciudad")
                                .get("provincia").get("idProvincia"),
                        filtro.idProvincia()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
