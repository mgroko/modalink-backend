package org.mgroko.backend.reportes.especificacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.admin.dto.BuscarReportesPerfilFiltro;
import org.mgroko.backend.admin.dto.BuscarReportesPublicacionFiltro;
import org.mgroko.backend.modelo.Reporte;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.reportes.mapper.MotivoReportePublicacionMapper;
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
        return construir(
                EntidadReporte.PERFIL,
                filtro != null ? filtro.estado() : null,
                filtro != null ? filtro.motivo() : null,
                filtro != null ? filtro.fechaDesde() : null,
                filtro != null ? filtro.fechaHasta() : null);
    }

    /**
     * Filtros de la bandeja de reportes de publicación (UC-69). El motivo de
     * publicación se traduce al código persistido en {@code reporte.motivo}.
     */
    public static Specification<Reporte> conFiltrosPublicacion(
            BuscarReportesPublicacionFiltro filtro) {

        return construir(
                EntidadReporte.PUBLICACION,
                filtro != null ? filtro.estado() : null,
                MotivoReportePublicacionMapper.aPersistido(
                        filtro != null ? filtro.motivo() : null),
                filtro != null ? filtro.fechaDesde() : null,
                filtro != null ? filtro.fechaHasta() : null);
    }

    private static Specification<Reporte> construir(
            EntidadReporte entidadTipo,
            EstadoReporte estado,
            MotivoReporte motivo,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("entidadTipo"), entidadTipo));

            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (motivo != null) {
                predicates.add(cb.equal(root.get("motivo"), motivo));
            }
            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("fechaReporte"),
                        fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                predicates.add(cb.lessThan(
                        root.get("fechaReporte"),
                        fechaHasta.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
