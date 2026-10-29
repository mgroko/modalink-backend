package org.mgroko.backend.reportes.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.admin.dto.BuscarReportesPerfilFiltro;
import org.mgroko.backend.modelo.Reporte;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

public final class ReporteSpecifications {

    private ReporteSpecifications() {
    }

    /**
     * Filtros de la bandeja de reportes de perfil. Siempre restringe a
     * {@code entidadTipo = PERFIL} porque esta bandeja es específica de UC-15.
     */
    public static Specification<Reporte> conFiltros(BuscarReportesPerfilFiltro filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("entidadTipo"), EntidadReporte.PERFIL));

            if (filtro != null) {
                if (filtro.estado() != null) {
                    predicates.add(cb.equal(root.get("estado"), filtro.estado()));
                }
                if (filtro.motivo() != null) {
                    predicates.add(cb.equal(root.get("motivo"), filtro.motivo()));
                }
                if (filtro.fechaDesde() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("fechaReporte"),
                            filtro.fechaDesde().atStartOfDay()));
                }
                if (filtro.fechaHasta() != null) {
                    predicates.add(cb.lessThan(
                            root.get("fechaReporte"),
                            filtro.fechaHasta().plusDays(1).atStartOfDay()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
