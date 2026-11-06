package org.mgroko.backend.reportes.servicio;

import java.time.Clock;
import java.time.LocalDateTime;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Reporte;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.reportes.dto.CrearReportePerfilRequest;
import org.mgroko.backend.reportes.evento.ReportePerfilCreadoEvent;
import org.mgroko.backend.reportes.exception.AutoreporteException;
import org.mgroko.backend.reportes.exception.DescripcionReporteInvalidaException;
import org.mgroko.backend.reportes.exception.DescripcionReporteObligatoriaException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.reportes.exception.ReportePendienteExistenteException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ReporteRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-15: registra un reporte de un perfil.
 *
 * <p>Solo los perfiles activos de usuarios activos son reportables. El autor
 * debe tener un perfil activo propio y no puede ser administrador. La alerta
 * al administrador se publica como evento y se genera después del commit.</p>
 */
@Service
public class ReportarPerfilService {

    private static final int DESCRIPCION_MAX_LENGTH = 200;
    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    private final PerfilRepository perfilRepository;
    private final ReporteRepository reporteRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock reloj;

    public ReportarPerfilService(
            PerfilRepository perfilRepository,
            ReporteRepository reporteRepository,
            ApplicationEventPublisher eventPublisher,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.reporteRepository = reporteRepository;
        this.eventPublisher = eventPublisher;
        this.reloj = reloj;
    }

    @Transactional
    public void reportar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPerfilReportado,
            CrearReportePerfilRequest request) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil autor = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (autor.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Usuario usuarioAutor = autor.getUsuario();
        if (usuarioAutor == null) {
            throw new PerfilNoEncontradoException();
        }

        if (esAdministrador(usuarioAutor)) {
            throw new ReporteNoPermitidoException("El administrador no puede reportar perfiles.");
        }

        Perfil reportado = perfilRepository.findById(idPerfilReportado)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (reportado.getEstado() != EstadoPerfil.ACTIVO
                || reportado.getUsuario() == null
                || reportado.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new PerfilNoEncontradoException();
        }

        if (reportado.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new AutoreporteException();
        }

        String descripcion = normalizarDescripcion(request.descripcion());
        validarDescripcion(request.motivo(), descripcion);

        if (reporteRepository.existsPendiente(
                idPerfilActivo, EntidadReporte.PERFIL, idPerfilReportado)) {
            throw new ReportePendienteExistenteException();
        }

        Reporte reporte = Reporte.builder()
                .entidadTipo(EntidadReporte.PERFIL)
                .entidadId(idPerfilReportado)
                .usuarioAutor(usuarioAutor)
                .perfilAutor(autor)
                .perfilReportado(reportado)
                .motivo(request.motivo())
                .descripcion(descripcion)
                .estado(EstadoReporte.PENDIENTE)
                .fechaReporte(LocalDateTime.now(reloj))
                .build();

        try {
            reporte = reporteRepository.saveAndFlush(reporte);
        } catch (DataIntegrityViolationException ex) {
            // Carrera contra el índice único parcial de reportes pendientes.
            throw new ReportePendienteExistenteException();
        }

        eventPublisher.publishEvent(new ReportePerfilCreadoEvent(
                reporte.getIdReporte(),
                reportado.getIdPerfil(),
                reportado.getNombreArtistico(),
                usuarioAutor.getIdUsuario(),
                nombreCompleto(usuarioAutor),
                autor.getIdPerfil(),
                autor.getNombreArtistico(),
                reporte.getMotivo(),
                reporte.getDescripcion()));
    }

    private boolean esAdministrador(Usuario usuario) {
        if (usuario.getRolGlobal() == null) {
            return false;
        }

        String codigo = usuario.getRolGlobal().getCodigo();
        if (codigo != null) {
            return ROL_ADMINISTRADOR.equalsIgnoreCase(codigo);
        }

        String nombre = usuario.getRolGlobal().getNombre();
        return nombre != null && "Administrador".equalsIgnoreCase(nombre);
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String limpia = descripcion.trim();
        return limpia.isEmpty() ? null : limpia;
    }

    private void validarDescripcion(MotivoReporte motivo, String descripcion) {
        if (motivo == MotivoReporte.OTRO && descripcion == null) {
            throw new DescripcionReporteObligatoriaException();
        }
        if (descripcion != null && descripcion.length() > DESCRIPCION_MAX_LENGTH) {
            throw new DescripcionReporteInvalidaException(
                    "La descripción no puede superar los " + DESCRIPCION_MAX_LENGTH + " caracteres.");
        }
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
        return (nombre + " " + apellido).trim();
    }
}
