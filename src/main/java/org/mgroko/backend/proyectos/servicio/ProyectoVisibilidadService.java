package org.mgroko.backend.proyectos.servicio;

import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regla única de visibilidad de proyectos, compartida por el detalle del
 * proyecto y la búsqueda de actividades (UC-52).
 */
@Service
public class ProyectoVisibilidadService {

    private final MiembroProyectoRepository miembroProyectoRepository;

    public ProyectoVisibilidadService(MiembroProyectoRepository miembroProyectoRepository) {
        this.miembroProyectoRepository = miembroProyectoRepository;
    }

    @Transactional(readOnly = true)
    public boolean esMiembroActivo(Long idProyecto, Long idPerfil) {
        if (idProyecto == null || idPerfil == null) {
            return false;
        }
        return miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(
                        idProyecto, idPerfil, EstadoParticipacion.ACTIVO);
    }

    @Transactional(readOnly = true)
    public boolean esExMiembroDeProyectoCancelado(Proyecto proyecto, Long idPerfil) {
        if (proyecto == null || idPerfil == null || proyecto.getIdProyecto() == null) {
            return false;
        }
        if (proyecto.getEstado() != EstadoProyecto.CANCELADO) {
            return false;
        }
        return miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfil(
                        proyecto.getIdProyecto(), idPerfil);
    }

    public boolean esVisibleParaNoMiembro(Proyecto proyecto) {
        if (proyecto == null || proyecto.getEstado() == null) {
            return false;
        }
        boolean estadoVisible = proyecto.getEstado() != EstadoProyecto.BORRADOR
                && proyecto.getEstado() != EstadoProyecto.CANCELADO;
        return estadoVisible && proyecto.getPrivacidad() == Privacidad.PUBLICO;
    }

    @Transactional(readOnly = true)
    public boolean puedeVer(Proyecto proyecto, Long idPerfil) {
        if (proyecto == null || idPerfil == null) {
            return false;
        }
        return esMiembroActivo(proyecto.getIdProyecto(), idPerfil)
                || esExMiembroDeProyectoCancelado(proyecto, idPerfil)
                || esVisibleParaNoMiembro(proyecto);
    }

    @Transactional(readOnly = true)
    public void validarAcceso(Proyecto proyecto, Long idPerfil) {
        if (!puedeVer(proyecto, idPerfil)) {
            throw new AccesoDenegadoProyectoException("No tienes acceso a este proyecto.");
        }
    }
}
