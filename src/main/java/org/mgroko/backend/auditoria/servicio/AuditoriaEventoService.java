package org.mgroko.backend.auditoria.servicio;

import java.util.List;

import org.mgroko.backend.auditoria.dto.AuditoriaDetalleRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

/**
 * Registra eventos de auditoría que no nacen de un cambio de fila.
 *
 * <p>Se usa para acciones de aplicación como ejecuciones de schedulers,
 * exportaciones o decisiones de proceso. Los cambios de estado persistidos
 * siguen auditándose con triggers de base de datos.</p>
 *
 * <p>El método exige una transacción activa para que el helper
 * {@code fn_auditoria_registrar} y el operador real ({@code app.id_usuario})
 * compartan el mismo ámbito transaccional.</p>
 */
@Service
public class AuditoriaEventoService {

    private final EntityManager entityManager;
    private final AuditoriaSesionService auditoriaSesionService;
    private final ObjectMapper objectMapper;

    public AuditoriaEventoService(
            EntityManager entityManager,
            AuditoriaSesionService auditoriaSesionService,
            ObjectMapper objectMapper) {
        this.entityManager = entityManager;
        this.auditoriaSesionService = auditoriaSesionService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(
            String entidad,
            Long id,
            String evento,
            String accion,
            List<AuditoriaDetalleRequest> detalle) {

        auditoriaSesionService.marcarOperadorActual();

        entityManager.createNativeQuery("""
                SELECT fn_auditoria_registrar(
                    :entidad,
                    :id,
                    :evento,
                    :accion,
                    CAST(:detalle AS jsonb))
                """)
                .setParameter("entidad", entidad)
                .setParameter("id", id)
                .setParameter("evento", evento)
                .setParameter("accion", accion)
                .setParameter("detalle", serializar(detalle))
                .getSingleResult();
    }

    private String serializar(List<AuditoriaDetalleRequest> detalle) {
        if (detalle == null || detalle.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("No se pudo serializar el detalle de auditoría.", ex);
        }
    }
}
