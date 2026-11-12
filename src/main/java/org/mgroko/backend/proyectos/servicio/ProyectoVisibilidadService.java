package org.mgroko.backend.proyectos.servicio;

import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regla única de visibilidad de proyectos, compartida por el detalle del
 * proyecto, la búsqueda de actividades y las publicaciones.
 */
@Service
public class ProyectoVisibilidadService {

    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ConexionRepository conexionRepository;

    public ProyectoVisibilidadService(
            MiembroProyectoRepository miembroProyectoRepository,
            ConexionRepository conexionRepository) {
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.conexionRepository = conexionRepository;
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

    /**
     * Visibilidad para un perfil que no es miembro activo:
     * <ul>
     *   <li>Público: visible si el proyecto no está en Borrador ni Cancelado.</li>
     *   <li>Privado: visible si el perfil es contacto de algún director activo
     *       del proyecto, y el proyecto no está en Borrador ni Cancelado.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public boolean esVisiblePorPrivacidad(Proyecto proyecto, Long idPerfil) {
        if (proyecto == null || proyecto.getEstado() == null) {
            return false;
        }

        if (proyecto.getEstado() == EstadoProyecto.BORRADOR
                || proyecto.getEstado() == EstadoProyecto.CANCELADO) {
            return false;
        }

        if (proyecto.getPrivacidad() == Privacidad.PUBLICO) {
            return true;
        }

        if (idPerfil == null || proyecto.getIdProyecto() == null) {
            return false;
        }

        return conexionRepository.existeContactoActivoConDirectorDeProyecto(
                idPerfil, proyecto.getIdProyecto());
    }

    @Transactional(readOnly = true)
    public boolean puedeVer(Proyecto proyecto, Long idPerfil) {
        if (proyecto == null || idPerfil == null) {
            return false;
        }
        return esMiembroActivo(proyecto.getIdProyecto(), idPerfil)
                || esExMiembroDeProyectoCancelado(proyecto, idPerfil)
                || esVisiblePorPrivacidad(proyecto, idPerfil);
    }

    @Transactional(readOnly = true)
    public void validarAcceso(Proyecto proyecto, Long idPerfil) {
        if (!puedeVer(proyecto, idPerfil)) {
            throw new AccesoDenegadoProyectoException("No tienes acceso a este proyecto.");
        }
    }
}
