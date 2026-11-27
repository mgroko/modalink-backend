package org.mgroko.backend.notificaciones.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.perfiles.evento.CaracteristicaTecnicaCreadaEvent;
import org.mgroko.backend.perfiles.evento.CaracteristicaTecnicaModificadaEvent;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifica a los perfiles activos de una profesión cuando el administrador
 * crea o modifica una característica técnica de esa profesión.
 *
 * <p>La notificación se crea después del commit y queda asociada al perfil
 * destinatario ({@code perfilContexto}), de modo que no aparece en perfiles de
 * otras profesiones del mismo usuario. Si ya existe una notificación no leída
 * del mismo tipo y característica para ese perfil, se actualiza en lugar de
 * duplicarse.</p>
 */
@Component
public class CaracteristicaTecnicaNotificacionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(CaracteristicaTecnicaNotificacionListener.class);

    private final PerfilRepository perfilRepository;
    private final NotificacionService notificacionService;

    public CaracteristicaTecnicaNotificacionListener(
            PerfilRepository perfilRepository,
            NotificacionService notificacionService) {
        this.perfilRepository = perfilRepository;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCrearCaracteristica(CaracteristicaTecnicaCreadaEvent evento) {
        try {
            String nombre = nombreVisible(evento.nombre(), evento.codigo());
            String profesion = nombreProfesion(evento.nombreProfesion());
            registrar(
                    evento.idCaracteristica(),
                    evento.idProfesion(),
                    NotificacionService.TIPO_CARACTERISTICA_TECNICA_CREADA,
                    "Se agregó la característica \"" + nombre + "\" a la profesión "
                            + profesion + ". Completala en tu perfil.");
        } catch (RuntimeException ex) {
            LOG.warn("No se pudieron registrar las notificaciones de la característica creada {}.",
                    evento.idCaracteristica(), ex);
        }
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alModificarCaracteristica(CaracteristicaTecnicaModificadaEvent evento) {
        try {
            String nombre = nombreVisible(evento.nombre(), evento.codigo());
            String profesion = nombreProfesion(evento.nombreProfesion());
            String mensaje = switch (evento.motivo() != null ? evento.motivo() : "DATOS") {
                case "VALOR_AGREGADO" ->
                        "Se agregó un nuevo valor a la característica \"" + nombre
                                + "\" de la profesión " + profesion
                                + ". Revisá tu perfil.";
                case "VALOR_MODIFICADO" ->
                        "Se actualizó un valor de la característica \"" + nombre
                                + "\" de la profesión " + profesion
                                + ". Revisá tu perfil.";
                default ->
                        "Se modificó la característica \"" + nombre + "\" de la profesión "
                                + profesion + ". Revisá tu perfil.";
            };
            registrar(
                    evento.idCaracteristica(),
                    evento.idProfesion(),
                    NotificacionService.TIPO_CARACTERISTICA_TECNICA_MODIFICADA,
                    mensaje);
        } catch (RuntimeException ex) {
            LOG.warn("No se pudieron registrar las notificaciones de la característica modificada {}.",
                    evento.idCaracteristica(), ex);
        }
    }

    private void registrar(
            Long idCaracteristica,
            Long idProfesion,
            String tipo,
            String mensaje) {

        if (idCaracteristica == null || idProfesion == null) {
            return;
        }

        List<Perfil> perfiles = perfilRepository
                .findActivosPorProfesionConUsuario(idProfesion);

        if (perfiles.isEmpty()) {
            return;
        }

        Map<Long, Perfil> porId = new LinkedHashMap<>();
        for (Perfil perfil : perfiles) {
            if (perfil != null && perfil.getIdPerfil() != null) {
                porId.putIfAbsent(perfil.getIdPerfil(), perfil);
            }
        }

        List<Notificacion> notificaciones = new ArrayList<>(porId.size());
        for (Perfil perfil : porId.values()) {
            if (perfil.getUsuario() == null) {
                continue;
            }
            notificaciones.add(notificacionService.crearOActualizarNoLeida(
                    perfil.getUsuario(),
                    perfil,
                    tipo,
                    mensaje,
                    NotificacionService.ENTIDAD_CARACTERISTICA_TECNICA,
                    idCaracteristica));
        }

        if (notificaciones.isEmpty()) {
            return;
        }

        notificacionService.registrarTodas(notificaciones);
    }

    private String nombreVisible(String nombre, String codigo) {
        if (nombre != null && !nombre.isBlank()) {
            return nombre;
        }
        return codigo != null && !codigo.isBlank() ? codigo : "sin nombre";
    }

    private String nombreProfesion(String nombreProfesion) {
        return nombreProfesion != null && !nombreProfesion.isBlank()
                ? nombreProfesion
                : "tu profesión";
    }
}
