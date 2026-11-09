package org.mgroko.backend.contratos.servicio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.RolParteContrato;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconciliar las firmas de UC-53 antes de actualizar el contrato, para que el
 * trigger de notificacion vea las partes nuevas.
 */
@Service
public class ContratoFirmaReconciliacionService {

    private final FirmaContratoRepository firmaContratoRepository;

    public ContratoFirmaReconciliacionService(
            FirmaContratoRepository firmaContratoRepository) {
        this.firmaContratoRepository = firmaContratoRepository;
    }

    @Transactional
    public void reconciliar(
            Contrato contrato,
            List<FirmaContrato> firmasActuales,
            List<MiembroProyecto> destinatarios) {

        Long idContrato = contrato.getIdContrato();
        Long idEmisor = contrato.getMiembroEmisor().getIdMiembro();

        Set<Long> idsSeleccionados = new HashSet<>();
        for (MiembroProyecto destinatario : destinatarios) {
            idsSeleccionados.add(destinatario.getIdMiembro());
        }

        Map<Long, FirmaContrato> actualesPorMiembro = new HashMap<>();
        for (FirmaContrato firma : firmasActuales) {
            actualesPorMiembro.put(firma.getIdMiembro(), firma);
        }

        List<FirmaContrato> paraGuardar = new ArrayList<>();

        FirmaContrato director = actualesPorMiembro.get(idEmisor);
        RolParteContrato rolDirector = idsSeleccionados.contains(idEmisor)
                ? RolParteContrato.DIRECTOR_DESTINATARIO
                : RolParteContrato.DIRECTOR;

        if (director == null) {
            director = FirmaContrato.builder()
                    .idMiembro(idEmisor)
                    .idContrato(idContrato)
                    .rolParte(rolDirector)
                    .estadoFirma(EstadoFirmaContrato.PENDIENTE)
                    .build();
        } else {
            director.setRolParte(rolDirector);
            resetear(director);
        }
        paraGuardar.add(director);

        for (MiembroProyecto destinatario : destinatarios) {
            if (destinatario.getIdMiembro().equals(idEmisor)) {
                continue;
            }

            FirmaContrato firma = actualesPorMiembro.get(destinatario.getIdMiembro());
            if (firma == null) {
                firma = FirmaContrato.builder()
                        .idMiembro(destinatario.getIdMiembro())
                        .idContrato(idContrato)
                        .rolParte(RolParteContrato.DESTINATARIO)
                        .estadoFirma(EstadoFirmaContrato.PENDIENTE)
                        .build();
            } else {
                firma.setRolParte(RolParteContrato.DESTINATARIO);
                resetear(firma);
            }
            paraGuardar.add(firma);
        }

        List<FirmaContrato> paraEliminar = new ArrayList<>();
        for (FirmaContrato firma : firmasActuales) {
            if (firma.getIdMiembro().equals(idEmisor)) {
                continue;
            }
            if (!idsSeleccionados.contains(firma.getIdMiembro())) {
                paraEliminar.add(firma);
            }
        }

        if (!paraEliminar.isEmpty()) {
            firmaContratoRepository.deleteAll(paraEliminar);
        }

        firmaContratoRepository.saveAll(paraGuardar);
        firmaContratoRepository.flush();
    }

    private void resetear(FirmaContrato firma) {
        firma.setEstadoFirma(EstadoFirmaContrato.PENDIENTE);
        firma.setFechaFirma(null);
        firma.setFechaRechazo(null);
        firma.setMotivoRechazo(null);
        firma.setContratoVersion(null);
    }
}
