package org.mgroko.backend.auditoria.servicio;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ejecuta los métodos anotados con {@link AuditarOperacion} dentro de una
 * transacción y setea el operador real antes de que se disparen los triggers.
 *
 * <p>Si el método ya está dentro de una transacción, {@link TransactionTemplate}
 * se une a ella (propagación REQUIRED). Si no, crea una para que
 * {@code set_config(..., true)} tenga alcance transaccional y no se filtre por
 * el pool de conexiones.</p>
 */
@Aspect
@Component
public class AuditoriaOperadorAspect {

    private final AuditoriaSesionService auditoriaSesionService;
    private final TransactionTemplate transactionTemplate;

    public AuditoriaOperadorAspect(
            AuditoriaSesionService auditoriaSesionService,
            PlatformTransactionManager transactionManager) {
        this.auditoriaSesionService = auditoriaSesionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Around("@annotation(org.mgroko.backend.auditoria.servicio.AuditarOperacion)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        return transactionTemplate.execute(status -> {
            try {
                auditoriaSesionService.marcarOperadorActual();
                return joinPoint.proceed();
            } catch (RuntimeException | Error ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new IllegalStateException("Error al ejecutar la operación auditada", ex);
            }
        });
    }
}
