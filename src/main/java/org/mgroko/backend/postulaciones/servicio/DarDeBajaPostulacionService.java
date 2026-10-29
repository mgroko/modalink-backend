package org.mgroko.backend.postulaciones.servicio;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.postulaciones.exception.PostulacionNoEncontradaException;
import org.mgroko.backend.postulaciones.exception.PostulacionYaGestionadaException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-27: dar de baja una postulación a proyecto emitida por el perfil activo.
 *
 * <p>Sólo se eliminan postulaciones {@code PENDIENTE}; una vez aceptadas o
 * rechazadas ya fueron gestionadas por el director y no pueden retirarse. La
 * eliminación es física, tal como define el contrato del caso de uso.</p>
 *
 * <p>La fila se carga con bloqueo pesimista para que una aceptación o rechazo
 * concurrente del director no pueda competir con la baja: el segundo en
 * entrar ve el estado actualizado y responde con el error correspondiente.</p>
 */
@Service
public class DarDeBajaPostulacionService {

    private final PerfilRepository perfilRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;

    public DarDeBajaPostulacionService(
            PerfilRepository perfilRepository,
            PostulacionGralRepository postulacionGralRepository,
            PostulacionActividadRepository postulacionActividadRepository) {
        this.perfilRepository = perfilRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
    }

    /**
     * Da de baja una postulación a un requerimiento general del proyecto.
     *
     * @throws PerfilActivoNoSeleccionadoException si no hay perfil activo
     * @throws PerfilNoEncontradoException         si el perfil no pertenece al usuario
     * @throws PerfilEnBajaException               si el perfil no está activo
     * @throws PostulacionNoEncontradaException    si no existe, es de otro
     *                                             proyecto o de otro perfil
     * @throws PostulacionYaGestionadaException    si ya no está pendiente
     */
    @Transactional
    public void darDeBajaPostulacionGeneral(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idPostulacion) {

        validarPerfilActivo(idUsuario, idPerfilActivo);

        PostulacionGral postulacion = postulacionGralRepository
                .findByIdParaBaja(idPostulacion)
                .orElseThrow(PostulacionNoEncontradaException::new);

        if (!postulacion.getRequerimientoGral().getProyecto().getIdProyecto().equals(idProyecto)
                || !postulacion.getPerfil().getIdPerfil().equals(idPerfilActivo)) {
            throw new PostulacionNoEncontradaException();
        }

        exigirPendiente(postulacion.getEstado());

        postulacionGralRepository.delete(postulacion);
        postulacionGralRepository.flush();
    }

    /**
     * Da de baja una postulación a un requerimiento de actividad.
     *
     * @throws PerfilActivoNoSeleccionadoException si no hay perfil activo
     * @throws PerfilNoEncontradoException         si el perfil no pertenece al usuario
     * @throws PerfilEnBajaException               si el perfil no está activo
     * @throws PostulacionNoEncontradaException    si no existe, es de otro
     *                                             proyecto o de otro perfil
     * @throws PostulacionYaGestionadaException    si ya no está pendiente
     */
    @Transactional
    public void darDeBajaPostulacionActividad(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idPostulacion) {

        validarPerfilActivo(idUsuario, idPerfilActivo);

        PostulacionActividad postulacion = postulacionActividadRepository
                .findByIdParaBaja(idPostulacion)
                .orElseThrow(PostulacionNoEncontradaException::new);

        if (!postulacion.getRequerimiento().getActividad().getPlanificacion()
                .getProyecto().getIdProyecto().equals(idProyecto)
                || !postulacion.getPerfil().getIdPerfil().equals(idPerfilActivo)) {
            throw new PostulacionNoEncontradaException();
        }

        exigirPendiente(postulacion.getEstado());

        postulacionActividadRepository.delete(postulacion);
        postulacionActividadRepository.flush();
    }

    private void validarPerfilActivo(Long idUsuario, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }
    }

    private void exigirPendiente(EstadoSolicitud estado) {
        if (estado != EstadoSolicitud.PENDIENTE) {
            throw new PostulacionYaGestionadaException();
        }
    }
}
