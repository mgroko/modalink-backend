package org.mgroko.backend.actividades.servicio;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.springframework.stereotype.Service;

/**
 * Regla única de protección de un requerimiento de actividad.
 *
 * <p>Bloquea si tiene una postulación o invitación {@code ACEPTADA}, o si tiene
 * alguna asignación asociada. Las asignaciones se incluyen porque la FK de
 * {@code asignacion_actividad} impide borrar el requerimiento; además, una
 * asignación directa puede existir sin postulación.</p>
 */
@Service
public class RequerimientoProteccionService {

    private final PostulacionActividadRepository postulacionActividadRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;

    public RequerimientoProteccionService(
            PostulacionActividadRepository postulacionActividadRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            AsignacionActividadRepository asignacionActividadRepository) {
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
    }

    public boolean estaProtegido(Long idRequerimiento) {
        return estaProtegido(idRequerimiento, false);
    }

    /**
     * Variante para proyectos confirmados.
     *
     * @param incluirPendientes si {@code true}, también bloquea cuando hay
     *                          postulaciones o invitaciones {@code PENDIENTE}.
     *                          Las {@code RECHAZADA} nunca bloquean.
     */
    public boolean estaProtegido(
            Long idRequerimiento,
            boolean incluirPendientes) {
        if (idRequerimiento == null) {
            return false;
        }

        return postulacionActividadRepository
                .existsByRequerimiento_IdRequerimientoAndEstado(
                        idRequerimiento, EstadoSolicitud.ACEPTADA)
                || invitacionActividadRepository
                        .existsByRequerimiento_IdRequerimientoAndEstado(
                                idRequerimiento, EstadoSolicitud.ACEPTADA)
                || asignacionActividadRepository
                        .countTodasPorRequerimientoActividad(idRequerimiento) > 0
                || (incluirPendientes
                        && (postulacionActividadRepository
                                .existsByRequerimiento_IdRequerimientoAndEstado(
                                        idRequerimiento, EstadoSolicitud.PENDIENTE)
                                || invitacionActividadRepository
                                        .existsByRequerimiento_IdRequerimientoAndEstado(
                                                idRequerimiento,
                                                EstadoSolicitud.PENDIENTE)));
    }
}
