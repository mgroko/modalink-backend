package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.auditoria.especificacion.AuditoriaPredicados;
import org.mgroko.backend.modelo.Auditoria;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
public class AuditoriaResumenRepositoryImpl implements AuditoriaResumenRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> contarPorTipo(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            List<String> tiposEvento) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Auditoria> root = query.from(Auditoria.class);

        query.multiselect(root.get("tipoEvento"), cb.count(root));
        query.where(predicados(root, query, cb, fechaDesde, fechaHasta, tiposEvento));
        query.groupBy(root.get("tipoEvento"));
        query.orderBy(cb.desc(cb.count(root)));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> contarPorEntidad(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            List<String> tiposEvento) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Auditoria> root = query.from(Auditoria.class);

        query.multiselect(root.get("entidadAfectada"), cb.count(root));
        query.where(predicados(root, query, cb, fechaDesde, fechaHasta, tiposEvento));
        query.groupBy(root.get("entidadAfectada"));
        query.orderBy(cb.desc(cb.count(root)));

        return entityManager.createQuery(query).getResultList();
    }

    private Predicate[] predicados(
            Root<Auditoria> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            List<String> tiposEvento) {

        return AuditoriaPredicados.construir(
                        root, query, cb, fechaDesde, fechaHasta, tiposEvento)
                .toArray(new Predicate[0]);
    }
}
