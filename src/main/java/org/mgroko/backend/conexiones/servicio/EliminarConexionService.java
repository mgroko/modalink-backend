package org.mgroko.backend.conexiones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.conexiones.exception.ConexionNoActivaException;
import org.mgroko.backend.conexiones.exception.ConexionNoEncontradaException;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoConexion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-44 - Eliminar una conexión de la red de contactos.
 *
 * <p>La baja es lógica: la conexión pasa a {@code ELIMINADA} y conserva su
 * historial. La auditoría la registra el trigger de base de datos.</p>
 */
@Service
public class EliminarConexionService {

    private final PerfilActivoService perfilActivoService;
    private final ConexionRepository conexionRepository;
    private final NotificacionService notificacionService;
    private final Clock reloj;

    public EliminarConexionService(
            PerfilActivoService perfilActivoService,
            ConexionRepository conexionRepository,
            NotificacionService notificacionService,
            Clock reloj) {
        this.perfilActivoService = perfilActivoService;
        this.conexionRepository = conexionRepository;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
    }

    @Transactional
    public void eliminar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idConexion) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        Conexion conexion = conexionRepository.findByIdForUpdate(idConexion)
                .orElseThrow(ConexionNoEncontradaException::new);

        boolean participa = conexion.getPerfil1().getIdPerfil()
                .equals(perfilActivo.getIdPerfil())
                || conexion.getPerfil2().getIdPerfil()
                        .equals(perfilActivo.getIdPerfil());

        if (!participa) {
            throw new ConexionNoEncontradaException();
        }

        if (conexion.getEstado() != EstadoConexion.ACTIVA) {
            throw new ConexionNoActivaException(
                    "La conexión ya fue eliminada.");
        }

        conexion.setEstado(EstadoConexion.ELIMINADA);
        conexion.setFechaFinConexion(LocalDateTime.now(reloj));
        conexionRepository.saveAndFlush(conexion);

        notificar(conexion, perfilActivo);
    }

    private void notificar(Conexion conexion, Perfil perfilActivo) {
        Perfil contraparte = conexion.getPerfil1().getIdPerfil()
                .equals(perfilActivo.getIdPerfil())
                ? conexion.getPerfil2()
                : conexion.getPerfil1();

        List<Notificacion> notificaciones = new ArrayList<>();

        if (puedeNotificar(perfilActivo)) {
            notificaciones.add(notificacionService.crear(
                    perfilActivo.getUsuario(),
                    perfilActivo,
                    NotificacionService.TIPO_CONEXION_ELIMINADA,
                    "Eliminaste la conexión con '"
                            + contraparte.getNombreArtistico() + "'.",
                    NotificacionService.ENTIDAD_CONEXION,
                    conexion.getIdConexion()));
        }

        if (puedeNotificar(contraparte)) {
            notificaciones.add(notificacionService.crear(
                    contraparte.getUsuario(),
                    contraparte,
                    NotificacionService.TIPO_CONEXION_ELIMINADA,
                    "La conexión con '"
                            + perfilActivo.getNombreArtistico() + "' fue eliminada.",
                    NotificacionService.ENTIDAD_CONEXION,
                    conexion.getIdConexion()));
        }

        if (!notificaciones.isEmpty()) {
            notificacionService.registrarTodas(notificaciones);
        }
    }

    private boolean puedeNotificar(Perfil perfil) {
        return perfil != null
                && perfil.getUsuario() != null
                && perfil.getUsuario().getEstado() == EstadoUsuario.ACTIVO;
    }
}
