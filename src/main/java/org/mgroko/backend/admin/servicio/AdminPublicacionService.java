package org.mgroko.backend.admin.servicio;

import java.util.List;

import org.mgroko.backend.admin.dto.AdminPublicacionResponse;
import org.mgroko.backend.admin.dto.ModerarPublicacionRequest;
import org.mgroko.backend.admin.exception.UsuarioAdminNoEncontradoException;
import org.mgroko.backend.admin.mapper.AdminPublicacionMapper;
import org.mgroko.backend.auditoria.dto.AuditoriaDetalleRequest;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.auditoria.servicio.AuditoriaEventoService;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.publicaciones.evento.PublicacionModeradaEvent;
import org.mgroko.backend.publicaciones.exception.EstadoPublicacionInvalidoException;
import org.mgroko.backend.publicaciones.exception.PublicacionInvalidaException;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.repositorio.ComentarioRepository;
import org.mgroko.backend.repositorio.MeGustaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.ReporteRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.PerfilAccessPolicy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-69: moderación administrativa de publicaciones reportadas.
 *
 * <p>La única transición permitida es {@code ACTIVA <-> OCULTA}. El motivo se
 * registra en auditoría y no se persiste en la publicación. La notificación al
 * dueño se publica como evento y se genera después del commit.</p>
 */
@Service
public class AdminPublicacionService {

    private static final int MOTIVO_MAX_LENGTH = 200;
    private static final String ENTIDAD_PUBLICACION = "publicacion";

    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final MeGustaRepository meGustaRepository;
    private final ComentarioRepository comentarioRepository;
    private final ReporteRepository reporteRepository;
    private final AuditoriaEventoService auditoriaEventoService;
    private final ApplicationEventPublisher eventPublisher;

    public AdminPublicacionService(
            PublicacionRepository publicacionRepository,
            UsuarioRepository usuarioRepository,
            MeGustaRepository meGustaRepository,
            ComentarioRepository comentarioRepository,
            ReporteRepository reporteRepository,
            AuditoriaEventoService auditoriaEventoService,
            ApplicationEventPublisher eventPublisher) {
        this.publicacionRepository = publicacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.meGustaRepository = meGustaRepository;
        this.comentarioRepository = comentarioRepository;
        this.reporteRepository = reporteRepository;
        this.auditoriaEventoService = auditoriaEventoService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @AuditarOperacion
    public AdminPublicacionResponse moderar(
            Long idPublicacion,
            Long idAdmin,
            ModerarPublicacionRequest request) {

        if (request.estado() == null) {
            throw new PublicacionInvalidaException("El estado es obligatorio.");
        }

        Usuario admin = usuarioRepository.findById(idAdmin)
                .orElseThrow(() -> new UsuarioAdminNoEncontradoException(
                        "Administrador no encontrado."));

        if (!PerfilAccessPolicy.esAdministrador(admin)) {
            throw new ReporteNoPermitidoException(
                    "Solo un administrador puede moderar publicaciones.");
        }

        String motivo = normalizarMotivo(request.motivo());
        if (request.estado() == EstadoPublicacion.OCULTA && motivo == null) {
            throw new PublicacionInvalidaException(
                    "El motivo es obligatorio al ocultar una publicación.");
        }

        Publicacion publicacion = publicacionRepository
                .findByIdPublicacionForUpdate(idPublicacion)
                .orElseThrow(() -> new PublicacionNoEncontradaException(idPublicacion));

        if (publicacion.getEstado() == request.estado()) {
            throw new EstadoPublicacionInvalidoException(
                    request.estado() == EstadoPublicacion.OCULTA
                            ? "La publicación ya se encuentra oculta."
                            : "La publicación ya se encuentra activa.");
        }

        EstadoPublicacion estadoAnterior = publicacion.getEstado();
        publicacion.setEstado(request.estado());
        publicacion = publicacionRepository.saveAndFlush(publicacion);

        String evento = request.estado() == EstadoPublicacion.OCULTA
                ? "PUBLICACION_OCULTADA"
                : "PUBLICACION_REACTIVADA";

        auditoriaEventoService.registrar(
                ENTIDAD_PUBLICACION,
                idPublicacion,
                evento,
                "UPDATE",
                List.of(
                        new AuditoriaDetalleRequest(
                                "estado",
                                estadoAnterior != null ? estadoAnterior.name() : null,
                                request.estado().name()),
                        new AuditoriaDetalleRequest("motivo", null, motivo)));

        Perfil perfil = publicacion.getPerfil();
        Usuario duenio = perfil != null ? perfil.getUsuario() : null;

        if (perfil != null && duenio != null) {
            eventPublisher.publishEvent(new PublicacionModeradaEvent(
                    publicacion.getIdPublicacion(),
                    publicacion.getTitulo(),
                    perfil.getIdPerfil(),
                    duenio.getIdUsuario(),
                    publicacion.getEstado()));
        }

        return toResponse(publicacion);
    }

    public AdminPublicacionResponse toResponse(Publicacion publicacion) {
        if (publicacion == null) {
            return null;
        }

        Long idPublicacion = publicacion.getIdPublicacion();
        long cantidadMeGusta = meGustaRepository.contarPorPublicacion(idPublicacion);
        long cantidadComentarios = comentarioRepository.contarPorPublicacion(idPublicacion);
        long cantidadReportes = reporteRepository.countByEntidadTipoAndEntidadId(
                EntidadReporte.PUBLICACION, idPublicacion);
        long cantidadReportesPendientes =
                reporteRepository.countByEntidadTipoAndEntidadIdAndEstado(
                        EntidadReporte.PUBLICACION,
                        idPublicacion,
                        EstadoReporte.PENDIENTE);

        return AdminPublicacionMapper.toResponse(
                publicacion,
                cantidadMeGusta,
                cantidadComentarios,
                cantidadReportes,
                cantidadReportesPendientes);
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null) {
            return null;
        }
        String limpio = motivo.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.length() > MOTIVO_MAX_LENGTH) {
            throw new PublicacionInvalidaException(
                    "El motivo no puede superar los " + MOTIVO_MAX_LENGTH + " caracteres.");
        }
        return limpio;
    }
}
