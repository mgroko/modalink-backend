package org.mgroko.backend.conexiones.especificacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.conexiones.dto.BuscarConexionesFiltro;
import org.mgroko.backend.conexiones.dto.EstadoPresenciaConexion;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.enums.EstadoConexion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Predicados de UC-44 bis. La red se acota al perfil activo y los filtros se
 * aplican a la contraparte de cada conexión.
 */
public final class ConexionSpecifications {

    private ConexionSpecifications() {
    }

    public static Specification<Conexion> conFiltros(
            Long idPerfilActivo,
            BuscarConexionesFiltro filtro) {
        return conFiltros(idPerfilActivo, filtro, null);
    }

    public static Specification<Conexion> conFiltros(
            Long idPerfilActivo,
            BuscarConexionesFiltro filtro,
            LocalDateTime limite) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Predicate activoEsPerfil1 = cb.and(
                    cb.equal(root.get("estado"), EstadoConexion.ACTIVA),
                    cb.equal(root.get("perfil1").get("idPerfil"), idPerfilActivo),
                    filtrosContraparte(root, cb, "perfil2", filtro, limite));

            Predicate activoEsPerfil2 = cb.and(
                    cb.equal(root.get("estado"), EstadoConexion.ACTIVA),
                    cb.equal(root.get("perfil2").get("idPerfil"), idPerfilActivo),
                    filtrosContraparte(root, cb, "perfil1", filtro, limite));

            predicates.add(cb.or(activoEsPerfil1, activoEsPerfil2));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate filtrosContraparte(
            Root<Conexion> root,
            CriteriaBuilder cb,
            String atributoPerfil,
            BuscarConexionesFiltro filtro,
            LocalDateTime limite) {

        if (filtro == null || !tieneFiltros(filtro)) {
            return cb.conjunction();
        }

        Join<Conexion, ?> perfil = root.join(atributoPerfil, JoinType.INNER);
        Join<?, ?> usuario = perfil.join("usuario", JoinType.INNER);
        Join<?, ?> profesion = perfil.join("profesion", JoinType.INNER);
        List<Predicate> predicates = new ArrayList<>();

        if (noEsBlanco(filtro.texto())) {
            predicates.add(cb.or(
                    likeSinAcentos(cb, usuario.get("nombre"), filtro.texto()),
                    likeSinAcentos(cb, usuario.get("apellido"), filtro.texto()),
                    likeSinAcentos(cb, perfil.get("nombreArtistico"), filtro.texto()),
                    likeSinAcentos(cb, profesion.get("nombre"), filtro.texto())));
        }

        if (noEsBlanco(filtro.nombre())) {
            predicates.add(likeSinAcentos(
                    cb, usuario.get("nombre"), filtro.nombre()));
        }

        if (noEsBlanco(filtro.apellido())) {
            predicates.add(likeSinAcentos(
                    cb, usuario.get("apellido"), filtro.apellido()));
        }

        if (noEsBlanco(filtro.nombreArtistico())) {
            predicates.add(likeSinAcentos(
                    cb, perfil.get("nombreArtistico"), filtro.nombreArtistico()));
        }

        if (filtro.idProfesion() != null) {
            predicates.add(cb.equal(
                    profesion.get("idProfesion"), filtro.idProfesion()));
        }

        if (filtro.estadoPresencia() != null && limite != null) {
            Predicate enLinea = cb.and(
                    cb.equal(perfil.get("estado"), EstadoPerfil.ACTIVO),
                    cb.equal(usuario.get("estado"), EstadoUsuario.ACTIVO),
                    cb.isNotNull(perfil.get("ultimaActividad")),
                    cb.greaterThanOrEqualTo(perfil.get("ultimaActividad"), limite));

            predicates.add(filtro.estadoPresencia() == EstadoPresenciaConexion.EN_LINEA
                    ? enLinea
                    : cb.not(enLinea));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private static Predicate likeSinAcentos(
            CriteriaBuilder cb,
            Expression<?> path,
            String valor) {

        return cb.like(
                cb.function("unaccent", String.class,
                        cb.lower(path.as(String.class))),
                LikePatrones.contieneSinAcentos(valor),
                LikePatrones.CARACTER_ESCAPE);
    }

    private static boolean tieneFiltros(BuscarConexionesFiltro filtro) {
        return noEsBlanco(filtro.texto())
                || noEsBlanco(filtro.nombre())
                || noEsBlanco(filtro.apellido())
                || noEsBlanco(filtro.nombreArtistico())
                || filtro.idProfesion() != null
                || filtro.estadoPresencia() != null;
    }

    private static boolean noEsBlanco(String valor) {
        return valor != null && !valor.isBlank();
    }
}
