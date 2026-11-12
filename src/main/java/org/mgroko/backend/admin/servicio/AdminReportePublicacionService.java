package org.mgroko.backend.admin.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.mgroko.backend.admin.dto.ActualizarEstadoReporteRequest;
import org.mgroko.backend.admin.dto.AdminPublicacionResponse;
import org.mgroko.backend.admin.dto.AdminReportePublicacionDetalleResponse;
import org.mgroko.backend.admin.dto.AdminReportePublicacionResumenResponse;
import org.mgroko.backend.admin.dto.BuscarReportesPublicacionFiltro;
import org.mgroko.backend.admin.exception.UsuarioAdminNoEncontradoException;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Reporte;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.reportes.especificacion.ReporteSpecifications;
import org.mgroko.backend.reportes.exception.EstadoReporteInvalidoException;
import org.mgroko.backend.reportes.exception.RangoFechasReporteInvalidoException;
import org.mgroko.backend.reportes.exception.ReporteNoEncontradoException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.reportes.mapper.MotivoReportePublicacionMapper;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.ReporteRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.PerfilAccessPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-69: bandeja de revisión de reportes de publicación para administradores.
 */
@Service
public class AdminReportePublicacionService {

    private final ReporteRepository reporteRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdminPublicacionService adminPublicacionService;
    private final Clock reloj;

    public AdminReportePublicacionService(
            ReporteRepository reporteRepository,
            PublicacionRepository publicacionRepository,
            UsuarioRepository usuarioRepository,
            AdminPublicacionService adminPublicacionService,
            Clock reloj) {
        this.reporteRepository = reporteRepository;
        this.publicacionRepository = publicacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.adminPublicacionService = adminPublicacionService;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AdminReportePublicacionResumenResponse> buscar(
            BuscarReportesPublicacionFiltro filtro,
            int page,
            int size,
            boolean todos) {

        if (filtro != null
                && filtro.fechaDesde() != null
                && filtro.fechaHasta() != null
                && filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new RangoFechasReporteInvalidoException();
        }

        Sort sort = Sort.by(
                Sort.Order.desc("fechaReporte"),
                Sort.Order.desc("idReporte"));

        Pageable pageable = todos || size <= 0
                ? Pageable.unpaged(sort)
                : PageRequest.of(Math.max(0, page), size, sort);

        Page<Reporte> resultados = reporteRepository.findAll(
                ReporteSpecifications.conFiltrosPublicacion(filtro), pageable);

        Map<Long, Publicacion> publicaciones = cargarPublicaciones(resultados.getContent());

        return PaginaResponse.fromPage(resultados.map(reporte ->
                toResumen(reporte, publicaciones.get(reporte.getEntidadId()))));
    }

    @Transactional(readOnly = true)
    public AdminReportePublicacionDetalleResponse obtenerDetalle(Long idReporte) {
        Reporte reporte = cargarReportePublicacion(idReporte);
        return toDetalle(reporte, cargarPublicacion(reporte.getEntidadId()));
    }

    @Transactional
    @AuditarOperacion
    public AdminReportePublicacionDetalleResponse actualizarEstado(
            Long idReporte,
            Long idAdmin,
            ActualizarEstadoReporteRequest request) {

        if (request.estado() == null || request.estado() == EstadoReporte.PENDIENTE) {
            throw new EstadoReporteInvalidoException(
                    "Solo se puede pasar de PENDIENTE a RESUELTO o DESCARTADO.");
        }

        Usuario admin = usuarioRepository.findById(idAdmin)
                .orElseThrow(() -> new UsuarioAdminNoEncontradoException(
                        "Administrador no encontrado."));

        if (!PerfilAccessPolicy.esAdministrador(admin)) {
            throw new ReporteNoPermitidoException(
                    "Solo un administrador puede gestionar reportes.");
        }

        Reporte reporte = reporteRepository.findByIdParaActualizar(idReporte)
                .orElseThrow(ReporteNoEncontradoException::new);

        if (reporte.getEntidadTipo() != EntidadReporte.PUBLICACION) {
            throw new ReporteNoEncontradoException();
        }

        if (reporte.getEstado() != EstadoReporte.PENDIENTE) {
            throw new EstadoReporteInvalidoException(
                    "El reporte ya fue gestionado y no admite nuevas transiciones.");
        }

        reporte.setEstado(request.estado());
        reporte.setAdminRevisor(admin);
        reporte.setFechaResolucion(LocalDateTime.now(reloj));
        reporteRepository.saveAndFlush(reporte);

        return toDetalle(reporte, cargarPublicacion(reporte.getEntidadId()));
    }

    private Reporte cargarReportePublicacion(Long idReporte) {
        Reporte reporte = reporteRepository.findDetalleById(idReporte)
                .orElseThrow(ReporteNoEncontradoException::new);

        if (reporte.getEntidadTipo() != EntidadReporte.PUBLICACION) {
            throw new ReporteNoEncontradoException();
        }
        return reporte;
    }

    private Publicacion cargarPublicacion(Long idPublicacion) {
        if (idPublicacion == null) {
            return null;
        }
        return publicacionRepository.findByIdPublicacion(idPublicacion).orElse(null);
    }

    private Map<Long, Publicacion> cargarPublicaciones(List<Reporte> reportes) {
        List<Long> ids = reportes.stream()
                .map(Reporte::getEntidadId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        return publicacionRepository.findDetallePorIds(ids).stream()
                .collect(Collectors.toMap(Publicacion::getIdPublicacion, p -> p));
    }

    private AdminReportePublicacionResumenResponse toResumen(
            Reporte reporte,
            Publicacion publicacion) {

        Perfil perfilPublicacion = publicacion != null ? publicacion.getPerfil() : null;
        Usuario usuarioPublicacion = perfilPublicacion != null
                ? perfilPublicacion.getUsuario()
                : null;
        Usuario autor = reporte.getUsuarioAutor();
        Perfil perfilAutor = reporte.getPerfilAutor();

        return new AdminReportePublicacionResumenResponse(
                reporte.getIdReporte(),
                MotivoReportePublicacionMapper.desdePersistido(reporte.getMotivo()),
                MotivoReportePublicacionMapper.descripcionDe(reporte.getMotivo()),
                reporte.getDescripcion(),
                reporte.getEstado(),
                reporte.getFechaReporte(),
                publicacion == null,
                publicacion != null ? publicacion.getIdPublicacion() : reporte.getEntidadId(),
                publicacion != null ? publicacion.getTitulo() : null,
                publicacion != null ? publicacion.getEstado() : null,
                perfilPublicacion != null ? perfilPublicacion.getIdPerfil() : null,
                perfilPublicacion != null ? perfilPublicacion.getNombreArtistico() : null,
                autor != null ? autor.getIdUsuario() : null,
                autor != null ? nombreCompleto(autor) : null,
                perfilAutor != null ? perfilAutor.getIdPerfil() : null,
                perfilAutor != null ? perfilAutor.getNombreArtistico() : null);
    }

    private AdminReportePublicacionDetalleResponse toDetalle(
            Reporte reporte,
            Publicacion publicacion) {

        Usuario autor = reporte.getUsuarioAutor();
        Perfil perfilAutor = reporte.getPerfilAutor();
        Usuario admin = reporte.getAdminRevisor();

        AdminPublicacionResponse publicacionResponse =
                adminPublicacionService.toResponse(publicacion);

        AdminReportePublicacionDetalleResponse.AutorResponse autorResponse =
                autor == null
                        ? null
                        : new AdminReportePublicacionDetalleResponse.AutorResponse(
                                autor.getIdUsuario(),
                                autor.getNombre(),
                                autor.getApellido(),
                                autor.getCorreo(),
                                perfilAutor != null ? perfilAutor.getIdPerfil() : null,
                                perfilAutor != null
                                        ? perfilAutor.getNombreArtistico()
                                        : null);

        return new AdminReportePublicacionDetalleResponse(
                reporte.getIdReporte(),
                reporte.getEntidadTipo(),
                reporte.getEntidadId(),
                MotivoReportePublicacionMapper.desdePersistido(reporte.getMotivo()),
                MotivoReportePublicacionMapper.descripcionDe(reporte.getMotivo()),
                reporte.getMotivo(),
                reporte.getDescripcion(),
                reporte.getEstado(),
                reporte.getFechaReporte(),
                publicacion == null,
                publicacionResponse,
                autorResponse,
                admin != null ? admin.getIdUsuario() : null,
                admin != null ? nombreCompleto(admin) : null,
                reporte.getFechaResolucion());
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
        return (nombre + " " + apellido).trim();
    }
}
