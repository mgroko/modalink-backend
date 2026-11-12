package org.mgroko.backend.reportes.servicio;

import java.time.Clock;
import java.time.LocalDateTime;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Reporte;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.reportes.dto.CrearReportePublicacionRequest;
import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;
import org.mgroko.backend.reportes.evento.ReportePublicacionCreadoEvent;
import org.mgroko.backend.reportes.exception.AutoreportePublicacionException;
import org.mgroko.backend.reportes.exception.DescripcionReporteInvalidaException;
import org.mgroko.backend.reportes.exception.DescripcionReporteObligatoriaException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.reportes.exception.ReportePendienteExistenteException;
import org.mgroko.backend.reportes.mapper.MotivoReportePublicacionMapper;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.ReporteRepository;
import org.mgroko.backend.security.PerfilAccessPolicy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-69: registra un reporte de una publicación.
 *
 * <p>Solo las publicaciones activas de perfiles y usuarios activos son
 * reportables. El autor debe tener un perfil activo propio y no puede ser
 * administrador ni el dueño de la publicación. La alerta al administrador se
 * publica como evento y se genera después del commit.</p>
 */
@Service
public class ReportarPublicacionService {

    private static final int DESCRIPCION_MAX_LENGTH = 200;

    private final PerfilRepository perfilRepository;
    private final PublicacionRepository publicacionRepository;
    private final ReporteRepository reporteRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock reloj;

    public ReportarPublicacionService(
            PerfilRepository perfilRepository,
            PublicacionRepository publicacionRepository,
            ReporteRepository reporteRepository,
            ApplicationEventPublisher eventPublisher,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.publicacionRepository = publicacionRepository;
        this.reporteRepository = reporteRepository;
        this.eventPublisher = eventPublisher;
        this.reloj = reloj;
    }

    @Transactional
    public void reportar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPublicacion,
            CrearReportePublicacionRequest request) {

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

        if (PerfilAccessPolicy.esAdministrador(usuarioAutor)) {
            throw new ReporteNoPermitidoException(
                    "El administrador no puede reportar publicaciones.");
        }

        Publicacion publicacion = publicacionRepository.findByIdPublicacion(idPublicacion)
                .orElseThrow(() -> new PublicacionNoEncontradaException(idPublicacion));

        if (publicacion.getEstado() != EstadoPublicacion.ACTIVA) {
            throw new PublicacionNoEncontradaException(idPublicacion);
        }

        Perfil perfilPublicacion = publicacion.getPerfil();
        if (perfilPublicacion == null
                || perfilPublicacion.getEstado() != EstadoPerfil.ACTIVO
                || perfilPublicacion.getUsuario() == null
                || perfilPublicacion.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new PublicacionNoEncontradaException(idPublicacion);
        }

        if (perfilPublicacion.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new AutoreportePublicacionException();
        }

        String descripcion = normalizarDescripcion(request.descripcion());
        validarDescripcion(request.motivo(), descripcion);

        if (reporteRepository.existsPendiente(
                idPerfilActivo, EntidadReporte.PUBLICACION, idPublicacion)) {
            throw new ReportePendienteExistenteException();
        }

        Reporte reporte = Reporte.builder()
                .entidadTipo(EntidadReporte.PUBLICACION)
                .entidadId(idPublicacion)
                .usuarioAutor(usuarioAutor)
                .perfilAutor(autor)
                .perfilReportado(null)
                .motivo(MotivoReportePublicacionMapper.aPersistido(request.motivo()))
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

        eventPublisher.publishEvent(new ReportePublicacionCreadoEvent(
                reporte.getIdReporte(),
                publicacion.getIdPublicacion(),
                publicacion.getTitulo(),
                perfilPublicacion.getIdPerfil(),
                perfilPublicacion.getNombreArtistico(),
                usuarioAutor.getIdUsuario(),
                nombreCompleto(usuarioAutor),
                autor.getIdPerfil(),
                autor.getNombreArtistico(),
                request.motivo(),
                reporte.getDescripcion()));
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String limpia = descripcion.trim();
        return limpia.isEmpty() ? null : limpia;
    }

    private void validarDescripcion(
            MotivoReportePublicacion motivo,
            String descripcion) {

        if (motivo == MotivoReportePublicacion.OTRO && descripcion == null) {
            throw new DescripcionReporteObligatoriaException();
        }
        if (descripcion != null && descripcion.length() > DESCRIPCION_MAX_LENGTH) {
            throw new DescripcionReporteInvalidaException(
                    "La descripción no puede superar los "
                            + DESCRIPCION_MAX_LENGTH + " caracteres.");
        }
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
        return (nombre + " " + apellido).trim();
    }
}
