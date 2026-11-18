package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class NotificacionEntregaRepositoryImpl implements NotificacionEntregaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Long> reclamarLote(LocalDateTime ahora, int lote) {
        List<Object> filas = entityManager.createNativeQuery("""
                SELECT id_entrega
                FROM notificacion_entrega
                WHERE estado IN ('PENDIENTE', 'FALLIDO')
                  AND (proximo_intento IS NULL OR proximo_intento <= :ahora)
                ORDER BY id_entrega
                LIMIT :lote
                FOR UPDATE SKIP LOCKED
                """)
                .setParameter("ahora", ahora)
                .setParameter("lote", lote)
                .getResultList();

        List<Long> ids = filas.stream()
                .map(this::aLong)
                .toList();

        if (ids.isEmpty()) {
            return ids;
        }

        entityManager.createNativeQuery("""
                UPDATE notificacion_entrega
                SET estado = 'ENVIANDO',
                    intentos = intentos + 1,
                    fecha_ultimo_intento = :ahora
                WHERE id_entrega IN (:ids)
                """)
                .setParameter("ahora", ahora)
                .setParameter("ids", ids)
                .executeUpdate();

        return ids;
    }

    @Override
    @Transactional
    public int recuperarEnviandoVencidas(LocalDateTime limite) {
        return entityManager.createNativeQuery("""
                UPDATE notificacion_entrega
                SET estado = 'PENDIENTE'
                WHERE estado = 'ENVIANDO'
                  AND (fecha_ultimo_intento IS NULL OR fecha_ultimo_intento < :limite)
                """)
                .setParameter("limite", limite)
                .executeUpdate();
    }

    private Long aLong(Object valor) {
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        return Long.valueOf(valor.toString());
    }
}
