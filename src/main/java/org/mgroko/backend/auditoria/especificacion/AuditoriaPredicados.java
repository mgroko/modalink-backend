package org.mgroko.backend.auditoria.especificacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.Auditoria;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Predicados compartidos por el listado, el resumen y las exportaciones de
 * UC-67. Mantenerlos en un único lugar evita que los filtros diverjan.
 */
public final class AuditoriaPredicados {

    private AuditoriaPredicados() {
    }

    public static List<Predicate> construir(
            Root<Auditoria> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            List<String> tiposEvento) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.between(root.get("fechaHora"), fechaDesde, fechaHasta));

        // Sin tipos seleccionados no se filtra: así aparecen los históricos
        // con tipo_evento = NULL y los eventos que no están en el catálogo.
        if (tiposEvento != null && !tiposEvento.isEmpty()) {
            predicates.add(root.get("tipoEvento").in(tiposEvento));
        }

        return predicates;
    }
}
