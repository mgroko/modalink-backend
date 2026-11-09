package org.mgroko.backend.conexiones.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.conexiones.dto.BuscarSolicitudesConexionFiltro;
import org.mgroko.backend.conexiones.dto.DireccionSolicitudConexion;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Predicados de UC-46. La búsqueda siempre se acota al perfil activo y los
 * filtros de texto/profesión se aplican a la contraparte de cada solicitud.
 */
public final class SolicitudConexionSpecifications {

    private SolicitudConexionSpecifications() {
    }

    public static Specification<SolicitudConexion> conFiltros(
            Long idPerfilActivo,
            BuscarSolicitudesConexionFiltro filtro) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filtro != null
                    && filtro.direccion() == DireccionSolicitudConexion.RECIBIDA) {
                predicates.add(cb.equal(
                        root.get("perfilDestinatario").get("idPerfil"), idPerfilActivo));
                predicates.add(filtrosContraparte(root, cb, "perfilRemitente", filtro));
            } else if (filtro != null
                    && filtro.direccion() == DireccionSolicitudConexion.ENVIADA) {
                predicates.add(cb.equal(
                        root.get("perfilRemitente").get("idPerfil"), idPerfilActivo));
                predicates.add(filtrosContraparte(root, cb, "perfilDestinatario", filtro));
            } else {
                Predicate recibidas = cb.and(
                        cb.equal(root.get("perfilDestinatario").get("idPerfil"), idPerfilActivo),
                        filtrosContraparte(root, cb, "perfilRemitente", filtro));
                Predicate enviadas = cb.and(
                        cb.equal(root.get("perfilRemitente").get("idPerfil"), idPerfilActivo),
                        filtrosContraparte(root, cb, "perfilDestinatario", filtro));
                predicates.add(cb.or(recibidas, enviadas));
            }

            if (filtro != null && filtro.estado() != null) {
                predicates.add(cb.equal(root.get("estado"), filtro.estado()));
            }

            if (filtro != null && filtro.fechaDesde() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("fechaEnvio"), filtro.fechaDesde().atStartOfDay()));
            }

            if (filtro != null && filtro.fechaHasta() != null) {
                predicates.add(cb.lessThan(
                        root.get("fechaEnvio"),
                        filtro.fechaHasta().plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate filtrosContraparte(
            Root<SolicitudConexion> root,
            CriteriaBuilder cb,
            String atributoPerfil,
            BuscarSolicitudesConexionFiltro filtro) {

        if (filtro == null || !tieneFiltrosDePerfil(filtro)) {
            return cb.conjunction();
        }

        Join<SolicitudConexion, ?> perfil = root.join(atributoPerfil, JoinType.INNER);
        Join<?, ?> usuario = perfil.join("usuario", JoinType.INNER);
        Join<?, ?> profesion = perfil.join("profesion", JoinType.INNER);
        List<Predicate> predicates = new ArrayList<>();

        if (noEsBlanco(filtro.nombre())) {
            predicates.add(cb.like(
                    cb.function("unaccent", String.class, cb.lower(usuario.get("nombre"))),
                    LikePatrones.contieneSinAcentos(filtro.nombre()),
                    LikePatrones.CARACTER_ESCAPE));
        }

        if (noEsBlanco(filtro.apellido())) {
            predicates.add(cb.like(
                    cb.function("unaccent", String.class, cb.lower(usuario.get("apellido"))),
                    LikePatrones.contieneSinAcentos(filtro.apellido()),
                    LikePatrones.CARACTER_ESCAPE));
        }

        if (noEsBlanco(filtro.nombreArtistico())) {
            predicates.add(cb.like(
                    cb.function("unaccent", String.class, cb.lower(perfil.get("nombreArtistico"))),
                    LikePatrones.contieneSinAcentos(filtro.nombreArtistico()),
                    LikePatrones.CARACTER_ESCAPE));
        }

        if (filtro.idProfesion() != null) {
            predicates.add(cb.equal(
                    profesion.get("idProfesion"), filtro.idProfesion()));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private static boolean tieneFiltrosDePerfil(BuscarSolicitudesConexionFiltro filtro) {
        return noEsBlanco(filtro.nombre())
                || noEsBlanco(filtro.apellido())
                || noEsBlanco(filtro.nombreArtistico())
                || filtro.idProfesion() != null;
    }

    private static boolean noEsBlanco(String valor) {
        return valor != null && !valor.isBlank();
    }
}
