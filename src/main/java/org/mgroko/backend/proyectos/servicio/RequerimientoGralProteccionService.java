package org.mgroko.backend.proyectos.servicio;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regla única de protección de un requerimiento general de proyecto.
 *
 * <p>Un requerimiento se protege cuando tiene un miembro {@code ACTIVO}
 * vinculado, una postulación/invitación {@code PENDIENTE} o un reemplazo
 * abierto. El reemplazo deja de proteger cuando se cierra, porque en ese
 * momento pasa a apuntar a un snapshot histórico.</p>
 */
@Service
public class RequerimientoGralProteccionService {

    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final InvitacionGralRepository invitacionGralRepository;
    private final ReemplazoRepository reemplazoRepository;

    public RequerimientoGralProteccionService(
            MiembroProyectoRepository miembroProyectoRepository,
            PostulacionGralRepository postulacionGralRepository,
            InvitacionGralRepository invitacionGralRepository,
            ReemplazoRepository reemplazoRepository) {
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.invitacionGralRepository = invitacionGralRepository;
        this.reemplazoRepository = reemplazoRepository;
    }

    @Transactional(readOnly = true)
    public boolean estaProtegido(RequerimientoGralProyecto requerimiento) {
        if (requerimiento == null || requerimiento.getIdRequerimientoGral() == null) {
            return false;
        }

        Long idRequerimiento = requerimiento.getIdRequerimientoGral();
        return miembroProyectoRepository
                .existsByRequerimientoGral_IdRequerimientoGralAndEstadoParticipacion(
                        idRequerimiento,
                        org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO)
                || postulacionGralRepository
                        .existsByRequerimientoGralIdRequerimientoGralAndEstado(
                                idRequerimiento, EstadoSolicitud.PENDIENTE)
                || invitacionGralRepository
                        .existsByRequerimientoGral_IdRequerimientoGralAndEstado(
                                idRequerimiento, EstadoSolicitud.PENDIENTE)
                || reemplazoRepository
                        .existsByRequerimientoGral_IdRequerimientoGral(idRequerimiento);
    }

    /**
     * Calcula la protección de todos los requerimientos con una cantidad
     * acotada de consultas.
     */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> calcularProtegidos(
            List<RequerimientoGralProyecto> requerimientos) {

        if (requerimientos == null || requerimientos.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = requerimientos.stream()
                .filter(Objects::nonNull)
                .map(RequerimientoGralProyecto::getIdRequerimientoGral)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        Set<Long> conMiembrosActivos = new HashSet<>(
                miembroProyectoRepository.findIdsRequerimientoGralConMiembrosActivos(ids));

        Set<Long> conPendientes = new HashSet<>();
        conPendientes.addAll(postulacionGralRepository
                .findIdsRequerimientoGralPorEstado(ids, EstadoSolicitud.PENDIENTE));
        conPendientes.addAll(invitacionGralRepository
                .findIdsRequerimientoGralPorEstado(ids, EstadoSolicitud.PENDIENTE));

        Set<Long> conReemplazos = new HashSet<>(
                reemplazoRepository.findIdsRequerimientoGralConReemplazos(ids));

        Map<Long, Boolean> resultado = new HashMap<>();
        for (Long id : ids) {
            resultado.put(id, conMiembrosActivos.contains(id)
                    || conPendientes.contains(id)
                    || conReemplazos.contains(id));
        }
        return resultado;
    }
}
