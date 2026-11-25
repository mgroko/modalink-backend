package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.mgroko.backend.contratos.servicio.CancelarContratosBajaService;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.reemplazos.enums.OrigenReemplazo;
import org.mgroko.backend.reemplazos.evento.ReemplazoNecesarioEvent;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baja de una membresia desde un proceso de sistema (baja definitiva de cuenta
 * o perfil). No exige que el perfil este activo ni valida el ultimo director:
 * el proyecto ya fue cancelado o la baja fue autorizada por el flujo de
 * expiracion.
 */
@Service
public class BajaMembresiaProyectoService {

    private static final int MOTIVO_MAX_LENGTH = 100;

    private final MiembroProyectoRepository miembroProyectoRepository;
    private final NotificacionService notificacionService;
    private final RequerimientoSnapshotService requerimientoSnapshotService;
    private final CancelarContratosBajaService cancelarContratosBajaService;

    private ApplicationEventPublisher eventPublisher;

    public BajaMembresiaProyectoService(
            MiembroProyectoRepository miembroProyectoRepository,
            NotificacionService notificacionService,
            RequerimientoSnapshotService requerimientoSnapshotService,
            CancelarContratosBajaService cancelarContratosBajaService) {
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.notificacionService = notificacionService;
        this.requerimientoSnapshotService = requerimientoSnapshotService;
        this.cancelarContratosBajaService = cancelarContratosBajaService;
    }

    @Autowired(required = false)
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public boolean darDeBajaPorSistema(
            Long idProyecto,
            Long idPerfil,
            String motivo,
            LocalDateTime fechaBaja) {

        Optional<MiembroProyecto> membresia = miembroProyectoRepository
                .findMiembroActivoDeProyectoParaActualizar(idProyecto, idPerfil);

        if (membresia.isEmpty()) {
            return false;
        }

        MiembroProyecto miembro = membresia.get();
        Perfil perfil = miembro.getPerfil();
        String motivoNormalizado = normalizarMotivo(motivo);

        miembro.setEstadoParticipacion(EstadoParticipacion.BAJA_VOLUNTARIA);
        miembro.setMotivoBaja(motivoNormalizado);
        miembro.setFechaBaja(fechaBaja);
        miembro.setPerfilAutorBaja(perfil);

        requerimientoSnapshotService.prepararHistorialBaja(
                miembro, idProyecto, fechaBaja);

        miembroProyectoRepository.saveAndFlush(miembro);

        cancelarContratosBajaService.cancelarPorBajaMiembro(
                miembro.getIdMiembro(), idProyecto, motivoNormalizado, fechaBaja);

        notificarDirectores(miembro.getProyecto(), perfil, motivoNormalizado, idPerfil);
        publicarReemplazo(miembro);

        return true;
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de baja es obligatorio.");
        }

        String normalizado = motivo.trim();
        if (normalizado.length() > MOTIVO_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "El motivo de baja no puede superar los "
                            + MOTIVO_MAX_LENGTH + " caracteres.");
        }
        return normalizado;
    }

    private void notificarDirectores(
            Proyecto proyecto,
            Perfil perfilBaja,
            String motivo,
            Long idPerfilExcluido) {

        if (proyecto == null) {
            return;
        }

        List<MiembroProyecto> directores = miembroProyectoRepository
                .buscarDirectoresDeProyectos(List.of(proyecto.getIdProyecto()));

        if (directores.isEmpty()) {
            return;
        }

        String nombre = perfilBaja != null
                && perfilBaja.getNombreArtistico() != null
                && !perfilBaja.getNombreArtistico().isBlank()
                        ? perfilBaja.getNombreArtistico()
                        : "Un integrante";

        String mensaje = "El integrante '" + nombre + "' se dio de baja del proyecto '"
                + proyecto.getNombre() + "'. Motivo: " + motivo;

        List<Notificacion> notificaciones = new ArrayList<>();
        Set<Long> destinatarios = new HashSet<>();

        for (MiembroProyecto director : directores) {
            Perfil perfilDirector = director.getPerfil();
            if (perfilDirector == null || perfilDirector.getUsuario() == null) {
                continue;
            }
            if (idPerfilExcluido != null
                    && idPerfilExcluido.equals(perfilDirector.getIdPerfil())) {
                continue;
            }
            if (!destinatarios.add(perfilDirector.getIdPerfil())) {
                continue;
            }

            notificaciones.add(notificacionService.crear(
                    perfilDirector.getUsuario(),
                    perfilDirector,
                    NotificacionService.TIPO_INTEGRANTE_BAJA_VOLUNTARIA,
                    mensaje,
                    NotificacionDestinoResolver.ENTIDAD_PROYECTO,
                    proyecto.getIdProyecto(),
                    proyecto.getIdProyecto()));
        }

        if (!notificaciones.isEmpty()) {
            notificacionService.registrarTodas(notificaciones);
        }
    }

    private void publicarReemplazo(MiembroProyecto miembro) {
        if (eventPublisher == null
                || miembro == null
                || miembro.getProyecto() == null
                || miembro.getProyecto().getEstado() != EstadoProyecto.CONFIRMADO
                || miembro.getIdMiembro() == null
                || miembro.getPerfil() == null
                || miembro.getPerfil().getIdPerfil() == null) {
            return;
        }

        eventPublisher.publishEvent(new ReemplazoNecesarioEvent(
                miembro.getProyecto().getIdProyecto(),
                miembro.getIdMiembro(),
                miembro.getPerfil().getIdPerfil(),
                OrigenReemplazo.BAJA_VOLUNTARIA));
    }
}
