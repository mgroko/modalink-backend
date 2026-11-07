package org.mgroko.backend.auditoria.especificacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.Auditoria;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

public final class AuditoriaSpecifications {

    private AuditoriaSpecifications() {
    }

    public static Specification<Auditoria> conFiltros(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            List<String> tiposEvento) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("fechaHora"), fechaDesde, fechaHasta));

            if (tiposEvento != null && !tiposEvento.isEmpty()) {
                predicates.add(root.get("tipoEvento").in(tiposEvento));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
