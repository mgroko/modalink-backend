package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.ActividadHistorialResponse;
import org.mgroko.backend.proyectos.dto.AuditoriaBajaResponse;
import org.mgroko.backend.proyectos.dto.AutorBajaResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auditoría derivada de bajas de proyecto. No modifica datos.
 */
@Service
public class ListarAuditoriaBajasProyectoService {

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final ProyectoSecurityService proyectoSecurityService;

    public ListarAuditoriaBajasProyectoService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            ProyectoSecurityService proyectoSecurityService) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.proyectoSecurityService = proyectoSecurityService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaBajaResponse> listar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            String estado,
            Long idActividad,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int page,
            int size,
            boolean todos) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        List<MiembroProyecto> historicos = miembroProyectoRepository
                .findHistoricosDeProyectoParaAuditoria(idProyecto);

        if (historicos.isEmpty()) {
            Pageable pageable = construirPageable(page, size, todos);
            return PaginaResponse.fromPage(new PageImpl<>(List.of(), pageable, 0));
        }

        List<Long> idsMiembros = historicos.stream()
                .map(MiembroProyecto::getIdMiembro)
                .toList();

        Map<Long, List<AsignacionActividad>> asignacionesPorMiembro = new HashMap<>();
        for (AsignacionActividad asignacion
                : asignacionActividadRepository
                        .findHistoricasPorMiembrosConActividadYRequerimiento(idsMiembros)) {
            if (asignacion.getMiembro() == null || asignacion.getMiembro().getIdMiembro() == null) {
                continue;
            }
            asignacionesPorMiembro
                    .computeIfAbsent(asignacion.getMiembro().getIdMiembro(),
                            id -> new ArrayList<>())
                    .add(asignacion);
        }

        List<AuditoriaBajaResponse> filtrados = new ArrayList<>();
        for (MiembroProyecto miembro : historicos) {
            if (!coincideEstado(miembro, estado)
                    || !coincideFecha(miembro, fechaDesde, fechaHasta)) {
                continue;
            }

            List<AsignacionActividad> asignaciones = asignacionesPorMiembro
                    .getOrDefault(miembro.getIdMiembro(), List.of());
            List<ActividadHistorialResponse> actividades = asignaciones.stream()
                    .filter(asignacion -> idActividad == null
                            || (asignacion.getActividad() != null
                                    && idActividad.equals(
                                            asignacion.getActividad().getIdActividad())))
                    .map(this::toActividad)
                    .filter(java.util.Objects::nonNull)
                    .toList();

            if (idActividad != null && actividades.isEmpty()) {
                continue;
            }

            filtrados.add(toResponse(miembro, actividades));
        }

        Pageable pageable = construirPageable(page, size, todos);
        List<AuditoriaBajaResponse> contenido = pageable.isPaged()
                ? paginar(filtrados, pageable)
                : filtrados;

        return PaginaResponse.fromPage(
                new PageImpl<>(contenido, pageable, filtrados.size()));
    }

    private boolean coincideEstado(MiembroProyecto miembro, String estado) {
        return estado == null
                || estado.isBlank()
                || (miembro.getEstadoParticipacion() != null
                        && estado.equalsIgnoreCase(
                                miembro.getEstadoParticipacion().name()));
    }

    private boolean coincideFecha(
            MiembroProyecto miembro,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {

        if (miembro.getFechaBaja() == null) {
            return fechaDesde == null && fechaHasta == null;
        }
        LocalDate fecha = miembro.getFechaBaja().toLocalDate();
        return (fechaDesde == null || !fecha.isBefore(fechaDesde))
                && (fechaHasta == null || !fecha.isAfter(fechaHasta));
    }

    private ActividadHistorialResponse toActividad(AsignacionActividad asignacion) {
        if (asignacion.getActividad() == null) {
            return null;
        }

        RequerimientoActividad requerimiento = asignacion.getRequerimientoActividad();
        return new ActividadHistorialResponse(
                asignacion.getActividad().getIdActividad(),
                asignacion.getActividad().getNombre(),
                asignacion.getActividad().getEstado() != null
                        ? asignacion.getActividad().getEstado().name()
                        : null,
                requerimiento != null ? requerimiento.getIdRequerimiento() : null,
                requerimiento != null && requerimiento.getProfesion() != null
                        ? requerimiento.getProfesion().getNombre()
                        : null);
    }

    private AuditoriaBajaResponse toResponse(
            MiembroProyecto miembro,
            List<ActividadHistorialResponse> actividades) {

        Perfil perfil = miembro.getPerfil();
        Perfil autor = miembro.getPerfilAutorBaja();

        return new AuditoriaBajaResponse(
                miembro.getIdMiembro(),
                perfil != null ? perfil.getIdPerfil() : null,
                perfil != null && perfil.getUsuario() != null
                        ? perfil.getUsuario().getIdUsuario()
                        : null,
                perfil != null ? perfil.getNombreArtistico() : null,
                perfil != null && perfil.getUsuario() != null
                        ? perfil.getUsuario().getNombre()
                        : null,
                perfil != null && perfil.getUsuario() != null
                        ? perfil.getUsuario().getApellido()
                        : null,
                miembro.getRolProyecto() != null
                        ? RolProyectoUsuario.desde(miembro.getRolProyecto()).name()
                        : null,
                miembro.getEstadoParticipacion() != null
                        ? miembro.getEstadoParticipacion().name()
                        : null,
                miembro.getMotivoBaja(),
                miembro.getFechaAlta(),
                miembro.getFechaBaja(),
                autor != null
                        ? new AutorBajaResponse(
                                autor.getIdPerfil(),
                                autor.getUsuario() != null
                                        ? autor.getUsuario().getIdUsuario()
                                        : null,
                                autor.getNombreArtistico(),
                                autor.getUsuario() != null
                                        ? autor.getUsuario().getNombre()
                                        : null,
                                autor.getUsuario() != null
                                        ? autor.getUsuario().getApellido()
                                        : null)
                        : null,
                actividades);
    }

    private Pageable construirPageable(int page, int size, boolean todos) {
        if (todos || size <= 0) {
            return Pageable.unpaged();
        }
        return PageRequest.of(Math.max(0, page), size);
    }

    private List<AuditoriaBajaResponse> paginar(
            List<AuditoriaBajaResponse> elementos,
            Pageable pageable) {

        int desde = (int) pageable.getOffset();
        if (desde >= elementos.size()) {
            return List.of();
        }
        int hasta = Math.min(desde + pageable.getPageSize(), elementos.size());
        return elementos.subList(desde, hasta);
    }
}
