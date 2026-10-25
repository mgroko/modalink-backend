package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.PermisoProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;
import org.mgroko.backend.proyectos.dto.ContextoConsultaProyecto;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.dto.RequerimientoGralResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.mapper.ProyectoMapper;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ObjetivoRepository objetivoRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final MoodboardRepository moodboardRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;

    public ObtenerProyectoService(
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PlanificacionRepository planificacionRepository,
            ObjetivoRepository objetivoRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository,
            PostulacionGralRepository postulacionGralRepository,
            MoodboardRepository moodboardRepository,
            ProyectoVisibilidadService proyectoVisibilidadService) {
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoGralCaractValorRepository = requerimientoGralCaractValorRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.moodboardRepository = moodboardRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
    }

    /**
     * Devuelve el detalle del proyecto para el dashboard.
     *
     * Reglas de visibilidad:
     * - Miembro activo: ve cualquier proyecto.
     * - Exmiembro: ve el detalle de proyectos CANCELADOS (historial).
     * - No miembro: solo ve proyectos PUBLICO que no estén en BORRADOR ni CANCELADO.
     */
    @Transactional(readOnly = true)
    public ProyectoResponse obtener(Long idUsuario, Long idPerfilActivo, Long idProyecto) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        // Una sola consulta: membresía activa + rol + permisos del consultante
        Optional<MiembroProyecto> miembroActivo = miembroProyectoRepository
                .findMiembroActivoConPermisos(idProyecto, idPerfilActivo);

        boolean exMiembroDeProyectoCancelado = miembroActivo.isEmpty()
                && proyectoVisibilidadService.esExMiembroDeProyectoCancelado(proyecto, idPerfilActivo);

        if (miembroActivo.isEmpty() && !exMiembroDeProyectoCancelado
                && !proyectoVisibilidadService.esVisibleParaNoMiembro(proyecto)) {
            throw new AccesoDenegadoProyectoException(
                    "No tienes acceso a este proyecto.");
        }

        Perfil director = buscarDirector(idProyecto);

        LocalDate fechaEntrega = planificacionRepository.findByProyectoIdProyecto(idProyecto)
                .map(Planificacion::getFechaEntrega)
                .orElse(null);

        List<Objetivo> objetivos = objetivoRepository.findByProyectoIdProyecto(idProyecto);

        Long idProfesionPerfil = perfil.getProfesion() != null
                ? perfil.getProfesion().getIdProfesion()
                : null;
        boolean profesionDelPerfilCoincide = false;

        List<RequerimientoGralResponse> requerimientos = new ArrayList<>();
        for (RequerimientoGralProyecto rg : requerimientoGralProyectoRepository.findByProyectoIdProyecto(idProyecto)) {
            requerimientos.add(armarRequerimientoResponse(rg));
            if (idProfesionPerfil != null && rg.getProfesion() != null
                    && idProfesionPerfil.equals(rg.getProfesion().getIdProfesion())) {
                profesionDelPerfilCoincide = true;
            }
        }

        MoodboardResponse moodboardResponse = moodboardRepository.findByProyectoIdProyecto(idProyecto)
                .map(ProyectoMapper::toMoodboardResponse)
                .orElse(null);

        boolean elegibleParaPostularse = proyecto.getEstado() != EstadoProyecto.CANCELADO
                && miembroActivo.isEmpty()
                && Boolean.TRUE.equals(proyecto.getAceptaPostulacionGral())
                && profesionDelPerfilCoincide;

        ContextoConsultaProyecto contexto = armarContexto(miembroActivo, elegibleParaPostularse);

        return ProyectoMapper.toResponse(proyecto, director, fechaEntrega, objetivos, requerimientos,
                moodboardResponse, contexto);
    }

    /**
     * Contexto del consultante: rol y permisos si es miembro activo; si no es
     * miembro, indica la elegibilidad para postularse (profesión coincidente).
     */
    private ContextoConsultaProyecto armarContexto(Optional<MiembroProyecto> miembroActivo,
            boolean elegibleParaPostularse) {
        if (miembroActivo.isEmpty()) {
            return new ContextoConsultaProyecto(null, Boolean.FALSE, List.of(), elegibleParaPostularse);
        }

        MiembroProyecto miembro = miembroActivo.get();
        RolProyecto rol = miembro.getRolProyecto();
        List<String> permisos = rol != null && rol.getPermisos() != null
                ? rol.getPermisos().stream()
                        .map(PermisoProyecto::getNombre)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList()
                : List.of();

        return new ContextoConsultaProyecto(
                RolProyectoUsuario.desde(rol),
                Boolean.TRUE,
                permisos,
                Boolean.FALSE);
    }

    private Perfil buscarDirector(Long idProyecto) {
        return miembroProyectoRepository.findByProyectoIdProyecto(idProyecto).stream()
                .filter(m -> m.getEstadoParticipacion() == EstadoParticipacion.ACTIVO)
                .filter(m -> m.getRolProyecto() != null
                        && RolProyectoUsuario.desde(m.getRolProyecto()) == RolProyectoUsuario.DIRECTOR)
                .map(MiembroProyecto::getPerfil)
                .findFirst()
                .orElse(null);
    }

    private RequerimientoGralResponse armarRequerimientoResponse(RequerimientoGralProyecto rg) {
        List<CaracteristicaRequerimientoResponse> caracts = new ArrayList<>();
        for (RequerimientoGralCaract caract : rg.getCaracteristicas()) {
            CaracteristicaTecnica ct = caract.getCaracteristica();
            List<Long> valores = requerimientoGralCaractValorRepository
                    .findByIdReqGralCaract(caract.getIdReqGralCaract()).stream()
                    .map(RequerimientoGralCaractValor::getIdValor)
                    .sorted()
                    .toList();
            caracts.add(new CaracteristicaRequerimientoResponse(
                    ct.getIdCaracteristica(),
                    ct.getCodigo(),
                    ct.getTipoDato(),
                    caract.getValorMin(),
                    caract.getValorMax(),
                    valores));
        }

        List<Long> habilidades = rg.getHabilidades().stream()
                .map(Habilidad::getIdHabilidad)
                .sorted()
                .toList();

        boolean satisfecho = postulacionGralRepository
                .existsByRequerimientoGralIdRequerimientoGralAndEstado(
                        rg.getIdRequerimientoGral(), EstadoSolicitud.ACEPTADA);

        return new RequerimientoGralResponse(
                rg.getIdRequerimientoGral(),
                rg.getCantidad(),
                rg.getDescripcion(),
                rg.getProfesion() != null ? rg.getProfesion().getIdProfesion() : null,
                rg.getProfesion() != null ? rg.getProfesion().getNombre() : null,
                caracts,
                habilidades,
                satisfecho);
    }
}
