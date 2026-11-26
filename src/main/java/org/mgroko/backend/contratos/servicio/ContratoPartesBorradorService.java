package org.mgroko.backend.contratos.servicio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.RolParteContrato;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sincroniza las partes seleccionadas de un contrato en BORRADOR.
 *
 * <p>No agrega la firma del director: esa fila la crea el trigger de emision.
 * Solo se persisten los destinatarios elegidos por el director para poder
 * reconstruir el borrador sin agregar tablas nuevas.</p>
 */
@Service
public class ContratoPartesBorradorService {

    private final FirmaContratoRepository firmaContratoRepository;

    public ContratoPartesBorradorService(
            FirmaContratoRepository firmaContratoRepository) {
        this.firmaContratoRepository = firmaContratoRepository;
    }

    @Transactional
    public void sincronizar(
            Long idContrato,
            Long idEmisor,
            List<MiembroProyecto> destinatarios) {

        List<FirmaContrato> actuales =
                firmaContratoRepository.findByIdContratoOrderByIdMiembroAsc(idContrato);

        Map<Long, FirmaContrato> actualesPorMiembro = new HashMap<>();
        for (FirmaContrato firma : actuales) {
            actualesPorMiembro.put(firma.getIdMiembro(), firma);
        }

        Set<Long> idsSeleccionados = new HashSet<>();
        for (MiembroProyecto destinatario : destinatarios) {
            idsSeleccionados.add(destinatario.getIdMiembro());
        }

        List<FirmaContrato> paraGuardar = new ArrayList<>();
        for (MiembroProyecto destinatario : destinatarios) {
            Long idMiembro = destinatario.getIdMiembro();
            FirmaContrato firma = actualesPorMiembro.get(idMiembro);

            RolParteContrato rol = idEmisor != null && idEmisor.equals(idMiembro)
                    ? RolParteContrato.DIRECTOR_DESTINATARIO
                    : RolParteContrato.DESTINATARIO;

            if (firma == null) {
                firma = FirmaContrato.builder()
                        .idMiembro(idMiembro)
                        .idContrato(idContrato)
                        .rolParte(rol)
                        .estadoFirma(EstadoFirmaContrato.PENDIENTE)
                        .build();
            } else {
                firma.setRolParte(rol);
            }
            paraGuardar.add(firma);
        }

        List<FirmaContrato> paraEliminar = new ArrayList<>();
        for (FirmaContrato firma : actuales) {
            if (!idsSeleccionados.contains(firma.getIdMiembro())
                    && firma.getEstadoFirma() == EstadoFirmaContrato.PENDIENTE) {
                paraEliminar.add(firma);
            }
        }

        if (!paraEliminar.isEmpty()) {
            firmaContratoRepository.deleteAll(paraEliminar);
            firmaContratoRepository.flush();
        }

        if (!paraGuardar.isEmpty()) {
            firmaContratoRepository.saveAll(paraGuardar);
            firmaContratoRepository.flush();
        }
    }
}
