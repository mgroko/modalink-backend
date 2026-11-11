package org.mgroko.backend.publicaciones.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.publicaciones.dto.BuscarSolicitudesColaboracionFiltro;
import org.mgroko.backend.publicaciones.dto.DireccionSolicitudColaboracion;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Predicados de UC-65. La búsqueda siempre se acota al perfil activo y oculta
 * las solicitudes cuya contraparte o publicación ya no sea visible.
 */
public final class SolicitudColaboracionSpecifications {

    private SolicitudColaboracionSpecifications() {
    }

    public static Specification<SolicitudColaboracion> conFiltros(
            Long idPerfilActivo,
            BuscarSolicitudesColaboracionFiltro filtro) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<SolicitudColaboracion, ?> remitente = root.join("perfilRemitente", JoinType.INNER);
            Join<SolicitudColaboracion, ?> destinatario = root.join("perfilDestinatario", JoinType.INNER);
            Join<SolicitudColaboracion, ?> publicacion = root.join("publicacion", JoinType.INNER);

            Predicate remitenteVisible = visible(cb, remitente);
            Predicate destinatarioVisible = visible(cb, destinatario);

            if (filtro != null
                    && filtro.direccion() == DireccionSolicitudColaboracion.RECIBIDA) {
                predicates.add(cb.equal(destinatario.get("idPerfil"), idPerfilActivo));
                predicates.add(remitenteVisible);
            } else if (filtro != null
                    && filtro.direccion() == DireccionSolicitudColaboracion.ENVIADA) {
                predicates.add(cb.equal(remitente.get("idPerfil"), idPerfilActivo));
                predicates.add(destinatarioVisible);
            } else {
                Predicate recibidas = cb.and(
                        cb.equal(destinatario.get("idPerfil"), idPerfilActivo),
                        remitenteVisible);
                Predicate enviadas = cb.and(
                        cb.equal(remitente.get("idPerfil"), idPerfilActivo),
                        destinatarioVisible);
                predicates.add(cb.or(recibidas, enviadas));
            }

            predicates.add(cb.equal(publicacion.get("estado"), EstadoPublicacion.ACTIVA));

            if (filtro != null) {
                if (filtro.idPerfilEmisor() != null) {
                    predicates.add(cb.equal(
                            remitente.get("idPerfil"), filtro.idPerfilEmisor()));
                }
                if (filtro.idPerfilReceptor() != null) {
                    predicates.add(cb.equal(
                            destinatario.get("idPerfil"), filtro.idPerfilReceptor()));
                }
                if (filtro.estado() != null) {
                    predicates.add(cb.equal(root.get("estado"), filtro.estado()));
                }
                if (filtro.idPublicacion() != null) {
                    predicates.add(cb.equal(
                            publicacion.get("idPublicacion"), filtro.idPublicacion()));
                }
                if (filtro.fechaDesde() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("fechaEnvio"), filtro.fechaDesde().atStartOfDay()));
                }
                if (filtro.fechaHasta() != null) {
                    predicates.add(cb.lessThan(
                            root.get("fechaEnvio"),
                            filtro.fechaHasta().plusDays(1).atStartOfDay()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate visible(
            CriteriaBuilder cb,
            Join<SolicitudColaboracion, ?> perfil) {

        Join<?, ?> usuario = perfil.join("usuario", JoinType.INNER);
        return cb.and(
                cb.equal(perfil.get("estado"), EstadoPerfil.ACTIVO),
                cb.equal(usuario.get("estado"), EstadoUsuario.ACTIVO));
    }
}
