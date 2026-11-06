package org.mgroko.backend.admin.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.admin.dto.ActualizarEstadoReporteRequest;
import org.mgroko.backend.admin.dto.AdminReportePerfilDetalleResponse;
import org.mgroko.backend.admin.dto.AdminReportePerfilResumenResponse;
import org.mgroko.backend.admin.dto.BuscarReportesPerfilFiltro;
import org.mgroko.backend.admin.exception.UsuarioAdminNoEncontradoException;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Reporte;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.reportes.especificacion.ReporteSpecifications;
import org.mgroko.backend.reportes.exception.EstadoReporteInvalidoException;
import org.mgroko.backend.reportes.exception.RangoFechasReporteInvalidoException;
import org.mgroko.backend.reportes.exception.ReporteNoEncontradoException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.repositorio.ReporteRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-15: bandeja de revisión de reportes de perfil para administradores.
 */
@Service
public class AdminReportePerfilService {

    private final ReporteRepository reporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock reloj;

    public AdminReportePerfilService(
            ReporteRepository reporteRepository,
            UsuarioRepository usuarioRepository,
            Clock reloj) {
        this.reporteRepository = reporteRepository;
        this.usuarioRepository = usuarioRepository;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AdminReportePerfilResumenResponse> buscar(
            BuscarReportesPerfilFiltro filtro,
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
                ReporteSpecifications.conFiltros(filtro), pageable);

        return PaginaResponse.fromPage(resultados.map(this::toResumen));
    }

    @Transactional(readOnly = true)
    public AdminReportePerfilDetalleResponse obtenerDetalle(Long idReporte) {
        return toDetalle(cargarReportePerfil(idReporte));
    }

    @Transactional
    public AdminReportePerfilDetalleResponse actualizarEstado(
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

        if (!esAdministrador(admin)) {
            throw new ReporteNoPermitidoException("Solo un administrador puede gestionar reportes.");
        }

        Reporte reporte = reporteRepository.findByIdParaActualizar(idReporte)
                .orElseThrow(ReporteNoEncontradoException::new);

        if (reporte.getEntidadTipo() != EntidadReporte.PERFIL) {
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

        return toDetalle(cargarReportePerfil(idReporte));
    }

    private Reporte cargarReportePerfil(Long idReporte) {
        Reporte reporte = reporteRepository.findDetalleById(idReporte)
                .orElseThrow(ReporteNoEncontradoException::new);

        if (reporte.getEntidadTipo() != EntidadReporte.PERFIL) {
            throw new ReporteNoEncontradoException();
        }
        return reporte;
    }

    private AdminReportePerfilResumenResponse toResumen(Reporte reporte) {
        Perfil reportado = reporte.getPerfilReportado();
        Usuario autor = reporte.getUsuarioAutor();
        Perfil perfilAutor = reporte.getPerfilAutor();

        return new AdminReportePerfilResumenResponse(
                reporte.getIdReporte(),
                reporte.getMotivo(),
                reporte.getMotivo().getDescripcion(),
                reporte.getDescripcion(),
                reporte.getEstado(),
                reporte.getFechaReporte(),
                reportado != null ? reportado.getIdPerfil() : null,
                reportado != null ? reportado.getNombreArtistico() : null,
                autor != null ? autor.getIdUsuario() : null,
                autor != null ? nombreCompleto(autor) : null,
                perfilAutor != null ? perfilAutor.getIdPerfil() : null,
                perfilAutor != null ? perfilAutor.getNombreArtistico() : null);
    }

    private AdminReportePerfilDetalleResponse toDetalle(Reporte reporte) {
        Perfil reportado = reporte.getPerfilReportado();
        Usuario usuarioReportado = reportado != null ? reportado.getUsuario() : null;
        Usuario autor = reporte.getUsuarioAutor();
        Perfil perfilAutor = reporte.getPerfilAutor();
        Usuario admin = reporte.getAdminRevisor();

        AdminReportePerfilDetalleResponse.PerfilReportadoResponse perfilReportado =
                reportado == null
                        ? null
                        : new AdminReportePerfilDetalleResponse.PerfilReportadoResponse(
                                reportado.getIdPerfil(),
                                reportado.getNombreArtistico(),
                                reportado.getProfesion() != null
                                        ? reportado.getProfesion().getNombre()
                                        : null,
                                reportado.getEstado() != null
                                        ? reportado.getEstado().name()
                                        : null);

        AdminReportePerfilDetalleResponse.UsuarioResponse usuarioReportadoResponse =
                usuarioReportado == null
                        ? null
                        : new AdminReportePerfilDetalleResponse.UsuarioResponse(
                                usuarioReportado.getIdUsuario(),
                                usuarioReportado.getNombre(),
                                usuarioReportado.getApellido(),
                                usuarioReportado.getCorreo(),
                                usuarioReportado.getDni(),
                                usuarioReportado.getEstado() != null
                                        ? usuarioReportado.getEstado().getNombre()
                                        : null);

        AdminReportePerfilDetalleResponse.AutorResponse autorResponse =
                autor == null
                        ? null
                        : new AdminReportePerfilDetalleResponse.AutorResponse(
                                autor.getIdUsuario(),
                                autor.getNombre(),
                                autor.getApellido(),
                                autor.getCorreo(),
                                autor.getDni(),
                                perfilAutor != null ? perfilAutor.getIdPerfil() : null,
                                perfilAutor != null ? perfilAutor.getNombreArtistico() : null);

        return new AdminReportePerfilDetalleResponse(
                reporte.getIdReporte(),
                reporte.getEntidadTipo(),
                reporte.getEntidadId(),
                reporte.getMotivo(),
                reporte.getMotivo().getDescripcion(),
                reporte.getDescripcion(),
                reporte.getEstado(),
                reporte.getFechaReporte(),
                perfilReportado,
                usuarioReportadoResponse,
                autorResponse,
                admin != null ? admin.getIdUsuario() : null,
                admin != null ? nombreCompleto(admin) : null,
                reporte.getFechaResolucion());
    }

    private boolean esAdministrador(Usuario usuario) {
        if (usuario.getRolGlobal() == null) {
            return false;
        }
        String codigo = usuario.getRolGlobal().getCodigo();
        if (codigo != null) {
            return "ADMINISTRADOR".equalsIgnoreCase(codigo);
        }
        String nombre = usuario.getRolGlobal().getNombre();
        return nombre != null && "Administrador".equalsIgnoreCase(nombre);
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
        return (nombre + " " + apellido).trim();
    }
}
