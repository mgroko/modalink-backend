package org.mgroko.backend.reemplazos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;
import org.mgroko.backend.reemplazos.evento.ReemplazoNecesarioEvent;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.mgroko.backend.reemplazos.servicio.modelo.CandidatoEvaluado;
import org.mgroko.backend.reemplazos.servicio.modelo.RequisitoReemplazo;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Construye los procesos de reemplazo a partir de una baja o eliminación.
 */
@Service
public class GenerarReemplazosService {

    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final CatalogoRequisitoService catalogoRequisitoService;
    private final CandidatoMatchingService candidatoMatchingService;
    private final ReemplazoRepository reemplazoRepository;
    private final ReemplazoCandidatoRepository reemplazoCandidatoRepository;
    private final ReemplazoNotificacionService notificacionService;
    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock reloj;
    private final int limiteCandidatos;

    public GenerarReemplazosService(
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            CupoRequerimientoService cupoRequerimientoService,
            CatalogoRequisitoService catalogoRequisitoService,
            CandidatoMatchingService candidatoMatchingService,
            ReemplazoRepository reemplazoRepository,
            ReemplazoCandidatoRepository reemplazoCandidatoRepository,
            ReemplazoNotificacionService notificacionService,
            PerfilRepository perfilRepository,
            UsuarioRepository usuarioRepository,
            Clock reloj,
            @Value("${reemplazos.max-candidatos:20}") int limiteCandidatos) {
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.catalogoRequisitoService = catalogoRequisitoService;
        this.candidatoMatchingService = candidatoMatchingService;
        this.reemplazoRepository = reemplazoRepository;
        this.reemplazoCandidatoRepository = reemplazoCandidatoRepository;
        this.notificacionService = notificacionService;
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.reloj = reloj;
        this.limiteCandidatos = limiteCandidatos > 0 ? limiteCandidatos : 20;
    }

    @Transactional
    public void generar(ReemplazoNecesarioEvent evento) {
        if (evento == null || evento.idProyecto() == null
                || evento.idMiembroBaja() == null) {
            return;
        }

        Proyecto proyecto = proyectoRepository.findById(evento.idProyecto()).orElse(null);
        if (proyecto == null || proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            return;
        }

        MiembroProyecto miembro = miembroProyectoRepository
                .findByIdMiembroAndProyecto(evento.idMiembroBaja(), evento.idProyecto())
                .orElse(null);

        if (miembro == null
                || miembro.getEstadoParticipacion() == EstadoParticipacion.ACTIVO
                || miembro.getPerfil() == null) {
            return;
        }

        generarGeneral(proyecto, miembro, evento);
        generarActividades(proyecto, miembro, evento);
    }

    private void generarGeneral(
            Proyecto proyecto,
            MiembroProyecto miembro,
            ReemplazoNecesarioEvent evento) {

        RequerimientoGralProyecto requerimientoBaja = miembro.getRequerimientoGral();
        Long idRequerimiento = requerimientoBaja != null
                ? (requerimientoBaja.getFechaArchivado() == null
                        ? requerimientoBaja.getIdRequerimientoGral()
                        : requerimientoBaja.getIdRequerimientoOrigen())
                : null;

        if (idRequerimiento == null
                || reemplazoRepository
                        .existsByMiembroBaja_IdMiembroAndRequerimientoGral_IdRequerimientoGral(
                                miembro.getIdMiembro(), idRequerimiento)) {
            return;
        }

        RequerimientoGralProyecto requerimiento = requerimientoGralProyectoRepository
                .findByIdConProyecto(idRequerimiento)
                .orElse(null);
        if (requerimiento == null
                || cupoRequerimientoService.calcularGeneral(requerimiento).satisfecho()) {
            return;
        }

        Reemplazo reemplazo = Reemplazo.builder()
                .tipo(TipoReemplazo.GENERAL)
                .proyecto(proyecto)
                .requerimientoGral(requerimiento)
                .miembroBaja(miembro)
                .perfilBaja(miembro.getPerfil())
                .origen(evento.origen())
                .estado(EstadoReemplazo.PENDIENTE_CONFIRMACION_DIRECTOR)
                .fechaCreacion(LocalDateTime.now(reloj))
                .build();

        crearCandidatos(reemplazo, evento.idProyecto());
    }

    private void generarActividades(
            Proyecto proyecto,
            MiembroProyecto miembro,
            ReemplazoNecesarioEvent evento) {

        List<AsignacionActividad> asignaciones = asignacionActividadRepository
                .findByMiembroConActividadYRequerimiento(miembro.getIdMiembro());

        Set<Long> procesadas = new HashSet<>();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        for (AsignacionActividad asignacion : asignaciones) {
            Actividad actividad = asignacion.getActividad();
            RequerimientoActividad requerimientoBaja = asignacion.getRequerimientoActividad();

            if (actividad == null || requerimientoBaja == null) {
                continue;
            }
            if (actividad.getEstado() == EstadoActividad.FINALIZADA
                    || actividad.getEstado() == EstadoActividad.CANCELADA) {
                continue;
            }
            if (actividad.getFechaHoraInicio() == null
                    || actividad.getFechaHoraFin() == null
                    || !actividad.getFechaHoraFin().isAfter(ahora)) {
                continue;
            }

            Long idRequerimiento = requerimientoBaja.getFechaArchivado() == null
                    ? requerimientoBaja.getIdRequerimiento()
                    : requerimientoBaja.getIdRequerimientoOrigen();

            if (idRequerimiento == null || !procesadas.add(idRequerimiento)) {
                continue;
            }
            if (reemplazoRepository
                    .existsByMiembroBaja_IdMiembroAndRequerimientoActividad_IdRequerimiento(
                            miembro.getIdMiembro(), idRequerimiento)) {
                continue;
            }

            RequerimientoActividad requerimiento = requerimientoActividadRepository
                    .findByIdConActividadYProyecto(idRequerimiento)
                    .orElse(null);
            if (requerimiento == null
                    || cupoRequerimientoService.calcularActividad(requerimiento).satisfecho()) {
                continue;
            }

            Reemplazo reemplazo = Reemplazo.builder()
                    .tipo(TipoReemplazo.ACTIVIDAD)
                    .proyecto(proyecto)
                    .actividad(requerimiento.getActividad())
                    .requerimientoActividad(requerimiento)
                    .miembroBaja(miembro)
                    .perfilBaja(miembro.getPerfil())
                    .origen(evento.origen())
                    .estado(EstadoReemplazo.PENDIENTE_CONFIRMACION_DIRECTOR)
                    .fechaCreacion(LocalDateTime.now(reloj))
                    .build();

            crearCandidatos(reemplazo, evento.idProyecto());
        }
    }

    /**
     * Vuelve a calcular candidatos para un proceso que quedó sin ellos.
     * No se usa para procesos que ya tienen invitaciones enviadas.
     */
    @Transactional
    public void regenerar(Reemplazo reemplazo) {
        if (reemplazo == null
                || reemplazo.getEstado() != EstadoReemplazo.SIN_CANDIDATOS) {
            return;
        }
        crearCandidatos(reemplazo, reemplazo.getProyecto().getIdProyecto());
    }

    private void crearCandidatos(
            Reemplazo reemplazo,
            Long idProyecto) {

        reemplazo = reemplazoRepository.saveAndFlush(reemplazo);
        RequisitoReemplazo requisito = reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD
                ? catalogoRequisitoService.cargarActividad(
                        reemplazo.getRequerimientoActividad().getIdRequerimiento())
                : catalogoRequisitoService.cargarGeneral(
                        reemplazo.getRequerimientoGral().getIdRequerimientoGral());

        List<CandidatoEvaluado> evaluados = candidatoMatchingService.buscar(
                reemplazo,
                requisito,
                limiteCandidatos);

        if (evaluados.isEmpty()) {
            reemplazo.setEstado(EstadoReemplazo.SIN_CANDIDATOS);
            reemplazoRepository.saveAndFlush(reemplazo);
            notificacionService.notificarDirectores(
                    reemplazo,
                    NotificacionService.TIPO_REEMPLAZO_SIN_CANDIDATOS,
                    mensajeSinCandidatos(reemplazo));
            return;
        }

        List<ReemplazoCandidato> candidatos = new ArrayList<>();
        int orden = 1;
        for (CandidatoEvaluado evaluado : evaluados) {
            candidatos.add(ReemplazoCandidato.builder()
                    .reemplazo(reemplazo)
                    .idProyecto(idProyecto)
                    .perfil(perfilRepository.getReferenceById(evaluado.perfil().idPerfil()))
                    .usuario(usuarioRepository.getReferenceById(evaluado.perfil().idUsuario()))
                    .estado(EstadoCandidatoReemplazo.PROPUESTO)
                    .ordenRanking(orden++)
                    .puntaje(evaluado.concordancia().puntaje())
                    .nivelUbicacion(evaluado.nivelUbicacion())
                    .esAlternativo(evaluado.concordancia().alternativo())
                    .detalleFaltantes(joinFaltantes(evaluado.concordancia().faltantes()))
                    .tipoDisponibilidad(evaluado.disponibilidad().tipo())
                    .inicioPropuesto(evaluado.disponibilidad().inicio())
                    .finPropuesto(evaluado.disponibilidad().fin())
                    .build());
        }

        reemplazoCandidatoRepository.saveAll(candidatos);
        reemplazo.setEstado(EstadoReemplazo.PENDIENTE_CONFIRMACION_DIRECTOR);
        reemplazoRepository.saveAndFlush(reemplazo);

        notificacionService.notificarDirectores(
                reemplazo,
                NotificacionService.TIPO_REEMPLAZO_CANDIDATOS_DISPONIBLES,
                mensajeConCandidatos(reemplazo, candidatos.size()));
    }

    private String mensajeConCandidatos(Reemplazo reemplazo, int cantidad) {
        return "Hay " + cantidad + " candidato(s) para cubrir la vacante de "
                + descripcionVacante(reemplazo)
                + " en el proyecto '" + reemplazo.getProyecto().getNombre() + "'.";
    }

    private String mensajeSinCandidatos(Reemplazo reemplazo) {
        return "No se encontraron candidatos disponibles para cubrir la vacante de "
                + descripcionVacante(reemplazo)
                + " en el proyecto '" + reemplazo.getProyecto().getNombre() + "'.";
    }

    private String descripcionVacante(Reemplazo reemplazo) {
        if (reemplazo.getTipo() == TipoReemplazo.GENERAL) {
            return "la profesión '"
                    + reemplazo.getRequerimientoGral().getProfesion().getNombre()
                    + "'";
        }
        return "la actividad '" + reemplazo.getActividad().getNombre()
                + "' (" + reemplazo.getRequerimientoActividad().getProfesion().getNombre()
                + ")";
    }

    private String joinFaltantes(List<String> faltantes) {
        if (faltantes == null || faltantes.isEmpty()) {
            return null;
        }
        String texto = faltantes.stream()
                .sorted(Comparator.naturalOrder())
                .distinct()
                .collect(Collectors.joining("\n"));
        return texto.length() > 1000 ? texto.substring(0, 1000) : texto;
    }
}
