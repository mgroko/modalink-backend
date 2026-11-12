package org.mgroko.backend.auditoria.especificacion;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.Auditoria;
import org.springframework.data.jpa.domain.Specification;

public final class AuditoriaSpecifications {

    private AuditoriaSpecifications() {
    }

    public static Specification<Auditoria> conFiltros(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            List<String> tiposEvento,
            String operador,
            String texto) {

        return (root, query, cb) -> cb.and(
                AuditoriaPredicados.construir(
                        root, query, cb, fechaDesde, fechaHasta, tiposEvento,
                        operador, texto)
                        .toArray(new jakarta.persistence.criteria.Predicate[0]));
    }
}
