package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;
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
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerProyectoService {

    public static final String ROL_DIRECTOR = "Director";

    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ObjetivoRepository objetivoRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;
    private final MoodboardRepository moodboardRepository;

    public ObtenerProyectoService(
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PlanificacionRepository planificacionRepository,
            ObjetivoRepository objetivoRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository,
            MoodboardRepository moodboardRepository) {
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoGralCaractValorRepository = requerimientoGralCaractValorRepository;
        this.moodboardRepository = moodboardRepository;
    }

    /**
     * Devuelve el detalle del proyecto para el dashboard.
     *
     * Reglas de visibilidad:
     * - Miembro activo: ve cualquier proyecto.
     * - No miembro: solo ve proyectos PUBLICADO (o estados posteriores) con privacidad PUBLICO.
     * - Proyectos en Borrador solo son visibles para miembros activos.
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

        boolean esMiembroActivo = miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(
                        idProyecto, idPerfilActivo, EstadoParticipacion.ACTIVO);

        if (!esMiembroActivo && !esVisibleParaNoMiembro(proyecto)) {
            throw new AccesoDenegadoProyectoException(
                    "No tienes acceso a este proyecto.");
        }

        Perfil director = buscarDirector(idProyecto);

        LocalDate fechaFin = planificacionRepository.findByProyectoIdProyecto(idProyecto)
                .map(Planificacion::getFechaEntrega)
                .orElse(null);

        List<Objetivo> objetivos = objetivoRepository.findByProyectoIdProyecto(idProyecto);

        List<RequerimientoGralResponse> requerimientos = new ArrayList<>();
        for (RequerimientoGralProyecto rg : requerimientoGralProyectoRepository.findByProyectoIdProyecto(idProyecto)) {
            requerimientos.add(armarRequerimientoResponse(rg));
        }

        MoodboardResponse moodboardResponse = moodboardRepository.findByProyectoIdProyecto(idProyecto)
                .map(m -> new MoodboardResponse(m.getIdMoodboard(), m.getDescripcion(), m.getFechaCreacion()))
                .orElse(null);

        return ProyectoMapper.toResponse(proyecto, director, fechaFin, objetivos, requerimientos, moodboardResponse);
    }

    private boolean esVisibleParaNoMiembro(Proyecto proyecto) {
        boolean noBorrador = proyecto.getEstado() != null
                && proyecto.getEstado() != org.mgroko.backend.modelo.enums.EstadoProyecto.BORRADOR;
        return noBorrador && proyecto.getPrivacidad() == Privacidad.PUBLICO;
    }

    private Perfil buscarDirector(Long idProyecto) {
        return miembroProyectoRepository.findByProyectoIdProyecto(idProyecto).stream()
                .filter(m -> m.getEstadoParticipacion() == EstadoParticipacion.ACTIVO)
                .filter(m -> m.getRolProyecto() != null
                        && ROL_DIRECTOR.equalsIgnoreCase(m.getRolProyecto().getNombre()))
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

        return new RequerimientoGralResponse(
                rg.getIdRequerimientoGral(),
                rg.getCantidad(),
                rg.getDescripcion(),
                rg.getProfesion() != null ? rg.getProfesion().getIdProfesion() : null,
                rg.getProfesion() != null ? rg.getProfesion().getNombre() : null,
                caracts,
                habilidades);
    }
}
