package org.mgroko.backend.contratos.especificacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.contratos.dto.BuscarContratosFiltro;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Filtros y visibilidad de UC-68.
 *
 * <p>Visibilidad: el perfil activo es director activo del proyecto o es parte
 * firmante del contrato. Un usuario ajeno al proyecto no ve nada.</p>
 */
public final class ContratoSpecifications {

    private static final String PATRON_ROL_DIRECTOR = "director%";

    private ContratoSpecifications() {
    }

    public static Specification<Contrato> conFiltros(
            BuscarContratosFiltro filtro,
            Long idPerfilActivo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(conVisibilidad(query, cb, root, idPerfilActivo));

            if (filtro == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (filtro.idProyecto() != null) {
                predicates.add(cb.equal(
                        root.get("proyecto").get("idProyecto"),
                        filtro.idProyecto()));
            }

            if (filtro.estados() != null && !filtro.estados().isEmpty()) {
                predicates.add(root.get("estado").in(filtro.estados()));
            }

            if (tieneTexto(filtro.parte())) {
                predicates.add(conParte(
                        query, cb, root, LikePatrones.contiene(filtro.parte())));
            }

            if (filtro.fechaEmisionDesde() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("fechaEmision"),
                        filtro.fechaEmisionDesde().atStartOfDay()));
            }

            if (filtro.fechaEmisionHasta() != null) {
                predicates.add(cb.lessThan(
                        root.get("fechaEmision"),
                        filtro.fechaEmisionHasta().plusDays(1).atStartOfDay()));
            }

            if (filtro.fechaFirmaDesde() != null || filtro.fechaFirmaHasta() != null) {
                LocalDateTime desde = filtro.fechaFirmaDesde() != null
                        ? filtro.fechaFirmaDesde().atStartOfDay()
                        : LocalDateTime.of(1900, 1, 1, 0, 0);
                LocalDateTime hasta = filtro.fechaFirmaHasta() != null
                        ? filtro.fechaFirmaHasta().plusDays(1).atStartOfDay()
                        : LocalDateTime.of(9999, 12, 31, 0, 0);

                predicates.add(conFechaFirma(query, cb, root, desde, hasta));
            }

            if (filtro.idTipoAcuerdo() != null) {
                predicates.add(cb.equal(
                        root.get("tipoAcuerdo").get("idTipoAcuerdo"),
                        filtro.idTipoAcuerdo()));
            }

            if (tieneTexto(filtro.nombreContrato())) {
                predicates.add(cb.like(
                        cb.lower(root.get("nombre")),
                        LikePatrones.contiene(filtro.nombreContrato()),
                        LikePatrones.CARACTER_ESCAPE));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static Predicate conVisibilidad(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Contrato> root,
            Long idPerfilActivo) {

        return cb.or(
                conDirectorActivo(query, cb, root, idPerfilActivo),
                conParteFirmante(query, cb, root, idPerfilActivo));
    }

    private static Predicate conDirectorActivo(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Contrato> root,
            Long idPerfilActivo) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<MiembroProyecto> miembro = subquery.from(MiembroProyecto.class);
        Join<MiembroProyecto, RolProyecto> rol = miembro.join("rolProyecto");

        subquery.select(miembro.get("idMiembro"));
        subquery.where(
                cb.equal(miembro.get("proyecto").get("idProyecto"),
                        root.get("proyecto").get("idProyecto")),
                cb.equal(miembro.get("perfil").get("idPerfil"), idPerfilActivo),
                cb.equal(miembro.get("estadoParticipacion"),
                        EstadoParticipacion.ACTIVO),
                cb.or(
                        cb.equal(cb.lower(rol.get("codigo")), "director"),
                        cb.like(cb.lower(rol.get("nombre")),
                                PATRON_ROL_DIRECTOR,
                                LikePatrones.CARACTER_ESCAPE)));

        return cb.exists(subquery);
    }

    private static Predicate conParteFirmante(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Contrato> root,
            Long idPerfilActivo) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<FirmaContrato> firma = subquery.from(FirmaContrato.class);

        subquery.select(firma.get("idContrato"));
        subquery.where(
                cb.equal(firma.get("idContrato"), root.get("idContrato")),
                cb.equal(firma.get("miembro").get("perfil").get("idPerfil"),
                        idPerfilActivo));

        return cb.exists(subquery);
    }

    private static Predicate conParte(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Contrato> root,
            String patron) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<FirmaContrato> firma = subquery.from(FirmaContrato.class);

        subquery.select(firma.get("idContrato"));
        subquery.where(
                cb.equal(firma.get("idContrato"), root.get("idContrato")),
                cb.or(
                        cb.like(cb.lower(firma.get("miembro").get("perfil")
                                        .get("nombreArtistico")),
                                patron, LikePatrones.CARACTER_ESCAPE),
                        cb.like(cb.lower(firma.get("miembro").get("perfil")
                                        .get("usuario").get("nombre")),
                                patron, LikePatrones.CARACTER_ESCAPE),
                        cb.like(cb.lower(firma.get("miembro").get("perfil")
                                        .get("usuario").get("apellido")),
                                patron, LikePatrones.CARACTER_ESCAPE)));

        return cb.exists(subquery);
    }

    private static Predicate conFechaFirma(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Contrato> root,
            LocalDateTime desde,
            LocalDateTime hasta) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<FirmaContrato> firma = subquery.from(FirmaContrato.class);

        subquery.select(firma.get("idContrato"));
        subquery.where(
                cb.equal(firma.get("idContrato"), root.get("idContrato")),
                cb.equal(firma.get("estadoFirma"), EstadoFirmaContrato.FIRMADO),
                cb.greaterThanOrEqualTo(firma.get("fechaFirma"), desde),
                cb.lessThan(firma.get("fechaFirma"), hasta));

        return cb.exists(subquery);
    }
}
