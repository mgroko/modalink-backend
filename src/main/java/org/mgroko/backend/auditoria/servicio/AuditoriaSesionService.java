package org.mgroko.backend.auditoria.servicio;

import jakarta.persistence.EntityManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Propaga el usuario autenticado a la sesión de PostgreSQL.
 *
 * <p>El valor se setea como local a la transacción ({@code true}) para que no
 * se filtre entre requests a través del pool de conexiones. Los triggers de
 * auditoría lo leen con {@code current_setting('app.id_usuario', true)}.</p>
 */
@Service
public class AuditoriaSesionService {

    private final EntityManager entityManager;

    public AuditoriaSesionService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Setea el operador actual a partir del {@code SecurityContext}. Si no hay
     * usuario autenticado (por ejemplo, un scheduler), deja el valor vacío.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void marcarOperadorActual() {
        marcarOperador(idUsuarioAutenticado());
    }

    /**
     * Setea un operador explícito. Útil para procesos internos o pruebas.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void marcarOperador(Long idUsuario) {
        entityManager.createNativeQuery(
                "SELECT set_config('app.id_usuario', :id, true)")
                .setParameter("id", idUsuario == null ? "" : idUsuario.toString())
                .getSingleResult();
    }

    private Long idUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String valor && valor.matches("^[0-9]+$")) {
            return Long.valueOf(valor);
        }
        return null;
    }
}
