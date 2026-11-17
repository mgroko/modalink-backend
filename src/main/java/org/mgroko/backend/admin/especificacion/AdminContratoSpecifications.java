package org.mgroko.backend.admin.especificacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.admin.dto.BuscarContratosAdminFiltro;
import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Filtros del listado administrativo de contratos. No aplica reglas de
 * visibilidad por perfil ni exclusiones de estado por defecto.
 */
public final class AdminContratoSpecifications {

    private AdminContratoSpecifications() {
    }

    public static Specification<Contrato> conFiltros(BuscarContratosAdminFiltro filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filtro == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (filtro.idProyecto() != null) {
                predicates.add(cb.equal(
                        root.get("proyecto").get("idProyecto"),
                        filtro.idProyecto()));
            }

            if (tieneTexto(filtro.nombreProyecto())) {
                predicates.add(cb.like(
                        cb.lower(root.get("proyecto").get("nombre")),
                        LikePatrones.contiene(filtro.nombreProyecto()),
                        LikePatrones.CARACTER_ESCAPE));
            }

            if (filtro.estados() != null && !filtro.estados().isEmpty()) {
                predicates.add(root.get("estado").in(filtro.estados()));
            }

            if (filtro.idTipoAcuerdo() != null) {
                predicates.add(cb.equal(
                        root.get("tipoAcuerdo").get("idTipoAcuerdo"),
                        filtro.idTipoAcuerdo()));
            }

            if (filtro.idPerfilParte() != null) {
                predicates.add(cb.or(
                        cb.equal(
                                root.get("miembroEmisor").get("perfil").get("idPerfil"),
                                filtro.idPerfilParte()),
                        conParte(query, cb, root, filtro.idPerfilParte())));
            }

            if (tieneTexto(filtro.parte())) {
                predicates.add(conParteTexto(
                        query, cb, root,
                        LikePatrones.contiene(filtro.parte())));
            }

            if (tieneTexto(filtro.texto())) {
                String patron = LikePatrones.contiene(filtro.texto());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron,
                                LikePatrones.CARACTER_ESCAPE),
                        cb.like(cb.lower(root.get("descripcion")), patron,
                                LikePatrones.CARACTER_ESCAPE),
                        cb.like(cb.lower(root.get("texto")), patron,
                                LikePatrones.CARACTER_ESCAPE)));
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate conParte(
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Path<Contrato> root,
            Long idPerfil) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<FirmaContrato> firma = subquery.from(FirmaContrato.class);
        subquery.select(firma.get("idContrato"));
        subquery.where(
                cb.equal(firma.get("idContrato"), root.get("idContrato")),
                cb.equal(firma.get("miembro").get("perfil").get("idPerfil"), idPerfil));
        return cb.exists(subquery);
    }

    private static Predicate conParteTexto(
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Path<Contrato> root,
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
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Path<Contrato> root,
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

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
