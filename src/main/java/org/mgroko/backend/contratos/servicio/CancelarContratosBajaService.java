package org.mgroko.backend.contratos.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * Cancela los contratos de una membresia que se da de baja definitivamente.
 *
 * <p>El trigger de base de datos ya cancela los contratos con firma del
 * miembro, pero un contrato PENDIENTE_FIRMA sin firmas puede quedar abierto.
 * Este servicio completa la regla: solo se tocan contratos PENDIENTE_FIRMA o
 * ACTIVO.</p>
 */
@Service
public class CancelarContratosBajaService {

    private final ContratoRepository contratoRepository;
    private final EntityManager entityManager;

    public CancelarContratosBajaService(
            ContratoRepository contratoRepository,
            EntityManager entityManager) {
        this.contratoRepository = contratoRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public int cancelarPorBajaMiembro(
            Long idMiembro,
            Long idProyecto,
            String motivo,
            LocalDateTime fechaCancelacion) {

        List<Contrato> contratos = contratoRepository
                .findDeMiembroPorProyectoYEstados(
                        idMiembro,
                        idProyecto,
                        List.of(EstadoContrato.PENDIENTE_FIRMA, EstadoContrato.ACTIVO));

        if (contratos.isEmpty()) {
            return 0;
        }

        activarCancelacionAutomatica();

        try {
            for (Contrato contrato : contratos) {
                if (contrato.getEstado() != EstadoContrato.PENDIENTE_FIRMA
                        && contrato.getEstado() != EstadoContrato.ACTIVO) {
                    continue;
                }

                contrato.setEstado(EstadoContrato.CANCELADO);
                contrato.setMotivoCancelacion(motivo);
                contrato.setFechaCancelacion(fechaCancelacion);
                contratoRepository.saveAndFlush(contrato);
            }
        } finally {
            desactivarCancelacionAutomatica();
        }

        return contratos.size();
    }

    private void activarCancelacionAutomatica() {
        entityManager.createNativeQuery(
                "SELECT set_config('app.contract_auto_cancel', 'on', true)")
                .getSingleResult();
    }

    private void desactivarCancelacionAutomatica() {
        entityManager.createNativeQuery(
                "SELECT set_config('app.contract_auto_cancel', 'off', true)")
                .getSingleResult();
    }
}
