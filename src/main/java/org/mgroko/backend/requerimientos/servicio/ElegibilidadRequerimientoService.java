package org.mgroko.backend.requerimientos.servicio;

import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.requerimientos.dto.MotivoNoPostulable;

import org.springframework.stereotype.Service;

/**
 * Regla única de elegibilidad estructural para los requerimientos expuestos
 * por el buscador de oportunidades.
 *
 * <p>No evalúa postulaciones previas ni disponibilidad de calendario: esas
 * validaciones se exponen por separado y se re-verifican al crear la
 * postulación.</p>
 */
@Service
public class ElegibilidadRequerimientoService {

    public ResultadoElegibilidad evaluarGeneral(
            Proyecto proyecto,
            RequerimientoGralProyecto requerimiento,
            CupoRequerimiento cupo,
            Long idProfesionPerfil,
            RolProyectoUsuario rol) {

        if (!proyectoHabilitado(proyecto)) {
            return ResultadoElegibilidad.bloqueado(MotivoNoPostulable.PROYECTO_NO_HABILITADO);
        }
        if (!Boolean.TRUE.equals(proyecto.getAceptaPostulacionGral())) {
            return ResultadoElegibilidad.bloqueado(MotivoNoPostulable.PROYECTO_NO_ACEPTA_GENERAL);
        }
        if (rol != null) {
            return ResultadoElegibilidad.bloqueado(MotivoNoPostulable.MIEMBRO_ACTIVO);
        }
        if (!profesionCoincide(requerimiento, idProfesionPerfil)) {
            return ResultadoElegibilidad.bloqueado(MotivoNoPostulable.PROFESION);
        }
        if (!tieneCupo(cupo)) {
            return ResultadoElegibilidad.bloqueado(MotivoNoPostulable.CUPO);
        }
        return ResultadoElegibilidad.habilitado();
    }

    public ResultadoElegibilidad evaluarActividad(
            Proyecto proyecto,
            RequerimientoActividad requerimiento,
            CupoRequerimiento cupo,
            Long idProfesionPerfil,
            RolProyectoUsuario rol) {

        if (!proyectoHabilitado(proyecto)) {
            return ResultadoElegibilidad.bloqueado(MotivoNoPostulable.PROYECTO_NO_HABILITADO);
        }
        if (requerimiento.getActividad() == null
                || requerimiento.getActividad().getEstado() != EstadoActividad.PENDIENTE) {
            return ResultadoElegibilidad.bloqueado(MotivoNoPostulable.ACTIVIDAD_NO_PENDIENTE);
        }
        if (rol == RolProyectoUsuario.DIRECTOR) {
            return ResultadoElegibilidad.bloqueado(MotivoNoPostulable.DIRECTOR);
        }
        if (!profesionCoincide(requerimiento, idProfesionPerfil)) {
            return ResultadoElegibilidad.bloqueado(MotivoNoPostulable.PROFESION);
        }
        if (!tieneCupo(cupo)) {
            return ResultadoElegibilidad.bloqueado(MotivoNoPostulable.CUPO);
        }
        return ResultadoElegibilidad.habilitado();
    }

    public boolean proyectoHabilitado(Proyecto proyecto) {
        return proyecto != null
                && (proyecto.getEstado() == EstadoProyecto.PUBLICADO
                        || proyecto.getEstado() == EstadoProyecto.CONFIRMADO);
    }

    private boolean profesionCoincide(RequerimientoGralProyecto requerimiento, Long idProfesionPerfil) {
        return idProfesionPerfil != null
                && requerimiento.getProfesion() != null
                && idProfesionPerfil.equals(requerimiento.getProfesion().getIdProfesion());
    }

    private boolean profesionCoincide(RequerimientoActividad requerimiento, Long idProfesionPerfil) {
        return idProfesionPerfil != null
                && requerimiento.getProfesion() != null
                && idProfesionPerfil.equals(requerimiento.getProfesion().getIdProfesion());
    }

    private boolean tieneCupo(CupoRequerimiento cupo) {
        return cupo != null && cupo.faltantes() > 0;
    }
}
