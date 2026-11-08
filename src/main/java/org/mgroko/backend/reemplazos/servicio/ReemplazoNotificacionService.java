package org.mgroko.backend.reemplazos.servicio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.springframework.stereotype.Service;

/**
 * Notificaciones propias del módulo de reemplazos.
 */
@Service
public class ReemplazoNotificacionService {

    public static final String ENTIDAD_REEMPLAZO = "REEMPLAZO";
    public static final String ENTIDAD_REEMPLAZO_CANDIDATO = "REEMPLAZO_CANDIDATO";

    private final NotificacionService notificacionService;
    private final MiembroProyectoRepository miembroProyectoRepository;

    public ReemplazoNotificacionService(
            NotificacionService notificacionService,
            MiembroProyectoRepository miembroProyectoRepository) {
        this.notificacionService = notificacionService;
        this.miembroProyectoRepository = miembroProyectoRepository;
    }

    public void notificarDirectores(
            Reemplazo reemplazo,
            String tipo,
            String mensaje) {

        List<MiembroProyecto> directores = miembroProyectoRepository
                .buscarDirectoresDeProyectos(List.of(reemplazo.getProyecto().getIdProyecto()));

        if (directores.isEmpty()) {
            return;
        }

        List<Notificacion> notificaciones = new ArrayList<>();
        Set<Long> destinatarios = new HashSet<>();
        for (MiembroProyecto director : directores) {
            Perfil perfil = director.getPerfil();
            if (perfil == null || perfil.getUsuario() == null) {
                continue;
            }
            if (!destinatarios.add(perfil.getIdPerfil())) {
                continue;
            }
            notificaciones.add(notificacionService.crear(
                    perfil.getUsuario(),
                    perfil,
                    tipo,
                    mensaje,
                    ENTIDAD_REEMPLAZO,
                    reemplazo.getIdReemplazo(),
                    reemplazo.getProyecto().getIdProyecto()));
        }
        notificacionService.registrarTodas(notificaciones);
    }

    public void notificarCandidato(
            ReemplazoCandidato candidato,
            String tipo,
            String mensaje) {

        if (candidato.getUsuario() == null || candidato.getPerfil() == null) {
            return;
        }

        notificacionService.registrarTodas(List.of(
                notificacionService.crear(
                        candidato.getUsuario(),
                        candidato.getPerfil(),
                        tipo,
                        mensaje,
                        ENTIDAD_REEMPLAZO_CANDIDATO,
                        candidato.getIdCandidato(),
                        candidato.getIdProyecto())));
    }

    /**
     * Elimina la notificación original de invitación de reemplazo cuando la
     * invitación deja de estar vigente.
     */
    public void eliminarInvitacionRecibida(ReemplazoCandidato candidato) {
        if (candidato == null || candidato.getIdCandidato() == null) {
            return;
        }

        notificacionService.eliminarPorEntidad(
                NotificacionService.TIPO_REEMPLAZO_INVITACION_RECIBIDA,
                ENTIDAD_REEMPLAZO_CANDIDATO,
                candidato.getIdCandidato());
    }
}
