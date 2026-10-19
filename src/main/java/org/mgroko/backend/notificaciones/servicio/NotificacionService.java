package org.mgroko.backend.notificaciones.servicio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService {

    public static final String TIPO_PROYECTO_CANCELADO = "PROYECTO_CANCELADO";
    public static final String TIPO_POSTULACION_RECHAZADA = "POSTULACION_RECHAZADA";
    public static final String TIPO_INVITACION_RECHAZADA = "INVITACION_RECHAZADA";

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public Notificacion crear(
            Usuario usuarioDestino,
            String tipo,
            String mensaje,
            String entidadTipo,
            Long entidadId) {

        return Notificacion.builder()
                .usuarioDestino(usuarioDestino)
                .tipo(tipo)
                .mensaje(mensaje)
                .entidadTipo(entidadTipo)
                .entidadId(entidadId)
                .leida(false)
                .build();
    }

    @Transactional
    public List<Notificacion> registrarTodas(Collection<Notificacion> notificaciones) {
        if (notificaciones == null || notificaciones.isEmpty()) {
            return List.of();
        }
        return notificacionRepository.saveAll(notificaciones);
    }
}
