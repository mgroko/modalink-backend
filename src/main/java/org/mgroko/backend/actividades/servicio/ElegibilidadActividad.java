package org.mgroko.backend.actividades.servicio;

import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;

/**
 * Regla única de elegibilidad para postularse a un requerimiento de actividad.
 *
 * <p>La comparten la búsqueda de actividades (UC-52) y el detalle de actividad,
 * de modo que ambos endpoints devuelvan el mismo {@code puedePostularse}.</p>
 */
final class ElegibilidadActividad {

    private ElegibilidadActividad() {
    }

    static boolean proyectoHabilitado(Proyecto proyecto) {
        return proyecto != null
                && (proyecto.getEstado() == EstadoProyecto.PUBLICADO
                        || proyecto.getEstado() == EstadoProyecto.CONFIRMADO);
    }

    static boolean puedePostularse(
            Proyecto proyecto,
            RequerimientoActividad requerimiento,
            CupoRequerimiento cupo,
            Long idProfesionPerfil,
            boolean puedeSerPostulante) {

        if (requerimiento == null || cupo == null) {
            return false;
        }

        boolean actividadPendiente = requerimiento.getActividad() != null
                && EstadoActividad.PENDIENTE.equals(requerimiento.getActividad().getEstado());

        boolean profesionCoincide = idProfesionPerfil != null
                && requerimiento.getProfesion() != null
                && idProfesionPerfil.equals(requerimiento.getProfesion().getIdProfesion());

        return proyectoHabilitado(proyecto)
                && actividadPendiente
                && profesionCoincide
                && cupo.faltantes() > 0
                && puedeSerPostulante;
    }
}
