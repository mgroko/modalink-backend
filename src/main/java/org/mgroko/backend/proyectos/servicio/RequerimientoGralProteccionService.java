package org.mgroko.backend.proyectos.servicio;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regla única de protección de un requerimiento general de proyecto.
 *
 * <p>Un requerimiento queda protegido cuando:</p>
 *
 * <ul>
 *   <li>el cupo está completo ({@code satisfecho});</li>
 *   <li>tiene cualquier fila de miembro vinculada, activa o histórica (la FK
 *       de {@code miembros_proyecto} impide borrarlo);</li>
 *   <li>tiene una postulación o invitación {@code ACEPTADA};</li>
 *   <li>en un proyecto {@code CONFIRMADO}, tiene una postulación o invitación
 *       {@code PENDIENTE}.</li>
 * </ul>
 *
 * <p>Las postulaciones e invitaciones {@code RECHAZADA} nunca bloquean.</p>
 */
@Service
public class RequerimientoGralProteccionService {

    private final CupoRequerimientoService cupoRequerimientoService;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final InvitacionGralRepository invitacionGralRepository;

    public RequerimientoGralProteccionService(
            CupoRequerimientoService cupoRequerimientoService,
            MiembroProyectoRepository miembroProyectoRepository,
            PostulacionGralRepository postulacionGralRepository,
            InvitacionGralRepository invitacionGralRepository) {
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.invitacionGralRepository = invitacionGralRepository;
    }

    @Transactional(readOnly = true)
    public boolean estaProtegido(
            RequerimientoGralProyecto requerimiento,
            EstadoProyecto estadoProyecto) {

        if (requerimiento == null || requerimiento.getIdRequerimientoGral() == null) {
            return false;
        }

        Long idRequerimiento = requerimiento.getIdRequerimientoGral();

        if (cupoRequerimientoService.calcularGeneral(requerimiento).satisfecho()) {
            return true;
        }

        if (miembroProyectoRepository
                .existsByRequerimientoGral_IdRequerimientoGral(idRequerimiento)) {
            return true;
        }

        if (tieneVinculoAceptado(idRequerimiento)) {
            return true;
        }

        return estadoProyecto == EstadoProyecto.CONFIRMADO
                && tieneVinculoPendiente(idRequerimiento);
    }

    /**
     * Calcula la protección de todos los requerimientos con una cantidad
     * acotada de consultas. Se usa para armar el detalle del proyecto y la
     * respuesta del PUT sin un SELECT por requerimiento.
     */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> calcularProtegidos(
            List<RequerimientoGralProyecto> requerimientos,
            EstadoProyecto estadoProyecto) {

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

        Map<Long, CupoRequerimiento> cupos =
                cupoRequerimientoService.calcularGenerales(requerimientos);

        Set<Long> conMiembros = new HashSet<>(
                miembroProyectoRepository.findIdsRequerimientoGralConMiembros(ids));

        Set<Long> conAceptadas = new HashSet<>();
        conAceptadas.addAll(postulacionGralRepository
                .findIdsRequerimientoGralPorEstado(ids, EstadoSolicitud.ACEPTADA));
        conAceptadas.addAll(invitacionGralRepository
                .findIdsRequerimientoGralPorEstado(ids, EstadoSolicitud.ACEPTADA));

        Set<Long> conPendientes = Set.of();
        if (estadoProyecto == EstadoProyecto.CONFIRMADO) {
            Set<Long> pendientes = new HashSet<>();
            pendientes.addAll(postulacionGralRepository
                    .findIdsRequerimientoGralPorEstado(ids, EstadoSolicitud.PENDIENTE));
            pendientes.addAll(invitacionGralRepository
                    .findIdsRequerimientoGralPorEstado(ids, EstadoSolicitud.PENDIENTE));
            conPendientes = pendientes;
        }

        Map<Long, Boolean> resultado = new HashMap<>();
        for (Long id : ids) {
            CupoRequerimiento cupo = cupos.get(id);
            boolean satisfecho = cupo != null && cupo.satisfecho();
            resultado.put(id, satisfecho
                    || conMiembros.contains(id)
                    || conAceptadas.contains(id)
                    || conPendientes.contains(id));
        }
        return resultado;
    }

    private boolean tieneVinculoAceptado(Long idRequerimiento) {
        return postulacionGralRepository
                .existsByRequerimientoGralIdRequerimientoGralAndEstado(
                        idRequerimiento, EstadoSolicitud.ACEPTADA)
                || invitacionGralRepository
                        .existsByRequerimientoGral_IdRequerimientoGralAndEstado(
                                idRequerimiento, EstadoSolicitud.ACEPTADA);
    }

    private boolean tieneVinculoPendiente(Long idRequerimiento) {
        return postulacionGralRepository
                .existsByRequerimientoGralIdRequerimientoGralAndEstado(
                        idRequerimiento, EstadoSolicitud.PENDIENTE)
                || invitacionGralRepository
                        .existsByRequerimientoGral_IdRequerimientoGralAndEstado(
                                idRequerimiento, EstadoSolicitud.PENDIENTE);
    }
}
