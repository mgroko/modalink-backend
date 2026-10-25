package org.mgroko.backend.admin.servicio;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.mgroko.backend.admin.dto.AdminDashboardResumen;
import org.mgroko.backend.admin.dto.AdminDashboardProvinciaRow;
import org.mgroko.backend.admin.dto.AdminProyectoList;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UsuarioRepository usuarioRepository;
    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;

    public AdminDashboardService(
            UsuarioRepository usuarioRepository,
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
    }

    public AdminDashboardResumen obtenerResumen(int size) {
        validarSize(size);
        Long totalUsuarios = countTotalUsuarios();
        Long proyectosConfirmados = countProyectosConfirmados();
        Long totalProyectos = proyectoRepository.countByEstadoNot(EstadoProyecto.BORRADOR);
        Double porcentajeConfirmados = calcularPorcentaje(totalProyectos, proyectosConfirmados);

        List<AdminDashboardResumen.AdminResumenProvincia> actividadProvincia =
                listarActividadProvincia();

        AdminDashboardResumen.AdminProfesionDemandada profesionMasDemandada =
                profesionMasDemandada();

        List<AdminDashboardResumen.AdminTopProvincia> topProvincias =
                topProvincias(actividadProvincia, size);

        List<AdminDashboardResumen.AdminUltimoProyecto> listaNuevosProyectos =
                ultimosProyectos(size);

        return new AdminDashboardResumen(
                totalUsuarios,
                proyectosConfirmados,
                porcentajeConfirmados,
                actividadProvincia,
                profesionMasDemandada,
                topProvincias,
                listaNuevosProyectos
        );
    }

    public PaginaResponse<AdminProyectoList> listarProyectos(
            EstadoProyecto[] estado, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page debe ser mayor o igual a cero.");
        }
        validarSize(size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "fechaInicio"));
        Page<Proyecto> proyectos;

        if (estado != null && estado.length > 0) {
            proyectos = proyectoRepository.findByEstadoIn(List.of(estado), pageRequest);
        } else {
            proyectos = proyectoRepository.findByEstadoNot(EstadoProyecto.BORRADOR, pageRequest);
        }

        return PaginaResponse.fromPage(proyectos.map(this::toProyectoList));
    }

    private Long countTotalUsuarios() {
        return usuarioRepository.count();
    }

    private Long countProyectosConfirmados() {
        return proyectoRepository.countByEstado(EstadoProyecto.CONFIRMADO);
    }

    private Double calcularPorcentaje(Long total, Long confirmados) {
        if (total == null || total == 0) return 0.0;
        return (double) confirmados / total * 100;
    }

    private List<AdminDashboardResumen.AdminResumenProvincia> listarActividadProvincia() {
        List<AdminDashboardProvinciaRow> perfiles = usuarioRepository
                .contarPerfilesActivosPorProvincia();
        List<AdminDashboardProvinciaRow> proyectos = proyectoRepository
                .contarProyectosNoBorradorPorProvincia();
        Map<Long, AdminDashboardProvinciaRow> provincias = new LinkedHashMap<>();
        Map<Long, Long> perfilesPorProvincia = perfiles
                .stream()
                .collect(Collectors.toMap(
                        AdminDashboardProvinciaRow::idProvincia,
                        AdminDashboardProvinciaRow::total));

        Map<Long, Long> proyectosPorProvincia = proyectos
                .stream()
                .collect(Collectors.toMap(
                        AdminDashboardProvinciaRow::idProvincia,
                        AdminDashboardProvinciaRow::total));

        perfiles
                .forEach(row -> provincias.put(row.idProvincia(), row));
        proyectos
                .forEach(row -> provincias.putIfAbsent(row.idProvincia(), row));

        return provincias.values().stream()
                .map(row -> {
                    long totalPerfiles = perfilesPorProvincia.getOrDefault(row.idProvincia(), 0L);
                    long totalProyectos = proyectosPorProvincia.getOrDefault(row.idProvincia(), 0L);
                    return new AdminDashboardResumen.AdminResumenProvincia(
                            row.idProvincia(),
                            row.nombreProvincia(),
                            totalPerfiles,
                            totalProyectos,
                            totalPerfiles + totalProyectos);
                })
                .sorted((a, b) -> {
                    int actividad = Long.compare(b.totalActividad(), a.totalActividad());
                    return actividad != 0
                            ? actividad
                            : a.nombreProvincia().compareToIgnoreCase(b.nombreProvincia());
                })
                .toList();
    }

    private AdminDashboardResumen.AdminProfesionDemandada profesionMasDemandada() {
        return perfilRepository.contarPerfilesActivosPorProfesion(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(row -> new AdminDashboardResumen.AdminProfesionDemandada(
                        row.idProfesion(), row.nombreProfesion(), row.totalPerfiles()))
                .orElse(null);
    }

    private List<AdminDashboardResumen.AdminTopProvincia> topProvincias(
            List<AdminDashboardResumen.AdminResumenProvincia> actividadProvincia,
            int size) {
        return actividadProvincia.stream()
                .limit(size)
                .map(row -> new AdminDashboardResumen.AdminTopProvincia(
                        row.idProvincia(), row.nombreProvincia(), row.totalActividad()))
                .toList();
    }

    private List<AdminDashboardResumen.AdminUltimoProyecto> ultimosProyectos(int size) {
        // Últimos proyectos excluyendo borrador, ordenados por fechaInicio DESC
        return proyectoRepository.findByEstadoNotOrderByFechaInicioDesc(
                        EstadoProyecto.BORRADOR,
                        PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "fechaInicio")))
                .stream()
                .map(this::toUltimoProyecto)
                .toList();
    }

    private void validarSize(int size) {
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size debe estar entre 1 y 100.");
        }
    }

    private AdminProyectoList toProyectoList(Proyecto proyecto) {
        return new AdminProyectoList(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getFechaInicio(),
                proyecto.getEstado(),
                proyecto.getPrivacidad() != null ? proyecto.getPrivacidad().name() : null);
    }

    private AdminDashboardResumen.AdminUltimoProyecto toUltimoProyecto(Proyecto proyecto) {
        return new AdminDashboardResumen.AdminUltimoProyecto(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getFechaInicio(),
                proyecto.getEstado());
    }
}