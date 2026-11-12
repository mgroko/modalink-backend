package org.mgroko.backend.actividades.servicio;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.springframework.stereotype.Service;

/**
 * Regla única de protección de un requerimiento de actividad.
 *
 * <p>Un requerimiento se protege cuando tiene una asignación de un miembro
 * {@code ACTIVO}, una postulación/invitación {@code PENDIENTE} o un reemplazo
 * abierto. El reemplazo deja de proteger cuando se cierra, porque en ese
 * momento pasa a apuntar a un snapshot histórico.</p>
 */
@Service
public class RequerimientoProteccionService {

    private final PostulacionActividadRepository postulacionActividadRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final ReemplazoRepository reemplazoRepository;

    public RequerimientoProteccionService(
            PostulacionActividadRepository postulacionActividadRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            ReemplazoRepository reemplazoRepository) {
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.reemplazoRepository = reemplazoRepository;
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
                                idRequerimiento, EstadoSolicitud.PENDIENTE)
                || reemplazoRepository
                        .existsByRequerimientoActividad_IdRequerimiento(idRequerimiento);
    }
}
