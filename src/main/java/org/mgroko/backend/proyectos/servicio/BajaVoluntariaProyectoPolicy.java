package org.mgroko.backend.proyectos.servicio;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.springframework.stereotype.Component;

/**
 * Regla única para indicar si el perfil consultante puede darse de baja
 * voluntariamente del proyecto. Se usa tanto en la validación del servicio
 * como en el indicador {@code puedeDarseDeBaja} del detalle del proyecto.
 */
@Component
public class BajaVoluntariaProyectoPolicy {

    private final MiembroProyectoRepository miembroProyectoRepository;

    public BajaVoluntariaProyectoPolicy(MiembroProyectoRepository miembroProyectoRepository) {
        this.miembroProyectoRepository = miembroProyectoRepository;
    }

    public boolean puedeDarseDeBaja(Proyecto proyecto, MiembroProyecto miembro) {
        if (proyecto == null || miembro == null) {
            return false;
        }
        if (miembro.getEstadoParticipacion() != EstadoParticipacion.ACTIVO) {
            return false;
        }
        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            return false;
        }
        if (RolProyectoUsuario.desde(miembro.getRolProyecto()) != RolProyectoUsuario.DIRECTOR) {
            return true;
        }
        return miembroProyectoRepository.countOtrosDirectoresActivos(
                proyecto.getIdProyecto(), miembro.getIdMiembro()) > 0;
    }
}
