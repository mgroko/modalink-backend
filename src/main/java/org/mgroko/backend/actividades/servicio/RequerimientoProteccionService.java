package org.mgroko.backend.actividades.servicio;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.springframework.stereotype.Service;

/**
 * Regla única de protección de un requerimiento de actividad.
 *
 * <p>Un requerimiento se protege únicamente cuando tiene una asignación de un
 * miembro {@code ACTIVO} o una postulación/invitación {@code PENDIENTE}. El
 * historial de miembros dados de baja o eliminados no bloquea la edición ni la
 * eliminación del requerimiento activo.</p>
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
        if (idRequerimiento == null) {
            return false;
        }

        return asignacionActividadRepository
                .countActivasPorRequerimientoActividad(idRequerimiento) > 0
                || postulacionActividadRepository
                        .existsByRequerimiento_IdRequerimientoAndEstado(
                                idRequerimiento, EstadoSolicitud.PENDIENTE)
                || invitacionActividadRepository
                        .existsByRequerimiento_IdRequerimientoAndEstado(
                                idRequerimiento, EstadoSolicitud.PENDIENTE);
    }
}
