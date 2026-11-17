package org.mgroko.backend.admin.servicio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mgroko.backend.admin.dto.AdminProyectoDetalleResponse;
import org.mgroko.backend.admin.dto.AdminProyectoList;
import org.mgroko.backend.admin.mapper.AdminProyectoDetalleMapper;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.mapper.ProyectoMapper;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas administrativas de proyectos. Solo lectura.
 */
@Service
@Transactional(readOnly = true)
public class AdminProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ObjetivoRepository objetivoRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final MoodboardRepository moodboardRepository;
    private final ContratoRepository contratoRepository;
    private final FirmaContratoRepository firmaContratoRepository;

    public AdminProyectoService(
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PlanificacionRepository planificacionRepository,
            ObjetivoRepository objetivoRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            MoodboardRepository moodboardRepository,
            ContratoRepository contratoRepository,
            FirmaContratoRepository firmaContratoRepository) {
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.moodboardRepository = moodboardRepository;
        this.contratoRepository = contratoRepository;
        this.firmaContratoRepository = firmaContratoRepository;
    }

    public PaginaResponse<AdminProyectoList> listarProyectos(
            EstadoProyecto[] estado, int page, int size) {

        if (page < 0) {
            throw new IllegalArgumentException("page debe ser mayor o igual a cero.");
        }
        validarSize(size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "fechaInicio"));

        Page<Proyecto> proyectos;
        if (estado != null && estado.length > 0) {
            proyectos = proyectoRepository.findByEstadoIn(List.of(estado), pageable);
        } else {
            proyectos = proyectoRepository.findAll(pageable);
        }

        return PaginaResponse.fromPage(proyectos.map(this::toProyectoList));
    }

    public AdminProyectoDetalleResponse obtenerDetalle(Long idProyecto) {
        Proyecto proyecto = proyectoRepository.findByIdConUbicacion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Perfil director = miembroProyectoRepository
                .buscarDirectoresDeProyectos(List.of(idProyecto))
                .stream()
                .map(MiembroProyecto::getPerfil)
                .findFirst()
                .orElse(null);

        LocalDate fechaEntrega = planificacionRepository
                .findByProyectoIdProyecto(idProyecto)
                .map(Planificacion::getFechaEntrega)
                .orElse(null);

        List<Objetivo> objetivos =
                objetivoRepository.findByProyectoIdProyecto(idProyecto);

        List<RequerimientoGralProyecto> requerimientos =
                requerimientoGralProyectoRepository.findByProyectoIdProyecto(idProyecto);

        MoodboardResponse moodboard = moodboardRepository
                .findByProyectoIdProyecto(idProyecto)
                .map(ProyectoMapper::toMoodboardResponse)
                .orElse(null);

        List<MiembroProyecto> miembros =
                miembroProyectoRepository.buscarTodosDeProyectoConPerfilProfesionYRol(idProyecto);

        List<Contrato> contratosEntidad =
                contratoRepository.findByProyectoIdProyectoConTipo(idProyecto);

        Map<Long, List<FirmaContrato>> firmasPorContrato = contratosEntidad.isEmpty()
                ? Map.of()
                : firmaContratoRepository.findConPartesPorContratos(
                        contratosEntidad.stream().map(Contrato::getIdContrato).toList())
                        .stream()
                        .collect(Collectors.groupingBy(FirmaContrato::getIdContrato));

        List<AdminProyectoDetalleResponse.ContratoResumen> contratos =
                contratosEntidad.stream()
                        .map(c -> toContratoResumen(
                                c,
                                firmasPorContrato.getOrDefault(c.getIdContrato(), List.of())))
                        .toList();

        return AdminProyectoDetalleMapper.toDetalle(
                proyecto,
                director,
                fechaEntrega,
                objetivos,
                requerimientos,
                moodboard,
                miembros,
                contratos,
                contratos.size());
    }

    private AdminProyectoList toProyectoList(Proyecto proyecto) {
        return new AdminProyectoList(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getFechaInicio(),
                proyecto.getEstado(),
                proyecto.getPrivacidad() != null
                        ? proyecto.getPrivacidad().name()
                        : null);
    }

    private AdminProyectoDetalleResponse.ContratoResumen toContratoResumen(
            Contrato contrato,
            List<FirmaContrato> firmas) {
        int firmados = (int) firmas.stream()
                .filter(f -> f.getEstadoFirma() == EstadoFirmaContrato.FIRMADO)
                .count();
        int totalPartes = (int) firmas.stream()
                .filter(f -> f.getEstadoFirma() == EstadoFirmaContrato.PENDIENTE
                        || f.getEstadoFirma() == EstadoFirmaContrato.FIRMADO)
                .count();
        return new AdminProyectoDetalleResponse.ContratoResumen(
                contrato.getIdContrato(),
                contrato.getNombre(),
                contrato.getEstado() != null ? contrato.getEstado().name() : null,
                contrato.getVersionActual(),
                contrato.getFechaCreacion(),
                contrato.getFechaEmision(),
                firmados,
                totalPartes);
    }

    private void validarSize(int size) {
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size debe estar entre 1 y 100.");
        }
    }
}
