package org.mgroko.backend.admin.servicio;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.mgroko.backend.admin.dto.AdminUsuarioResponse;
import org.mgroko.backend.admin.dto.DeshabilitarUsuarioRequest;
import org.mgroko.backend.admin.exception.AutoDeshabilitacionException;
import org.mgroko.backend.admin.exception.UsuarioAdminNoEncontradoException;
import org.mgroko.backend.admin.exception.UsuarioEnBajaException;
import org.mgroko.backend.admin.mapper.AdminUsuarioMapper;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

@Service
public class AdminUsuarioService {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public AdminUsuarioService(
            UsuarioRepository usuarioRepository,
            NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public List<AdminUsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(AdminUsuarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public org.mgroko.backend.common.dto.PaginaResponse<AdminUsuarioResponse> buscar(
            org.mgroko.backend.admin.dto.BuscarUsuariosAdminFiltro filtro,
            int page,
            int size,
            boolean todos) {

        org.springframework.data.domain.Pageable pageable;
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "apellido", "nombre");

        if (todos || size <= 0) {
            pageable = org.springframework.data.domain.Pageable.unpaged(sort);
        } else {
            int paginaAjustada = Math.max(0, page);
            pageable = org.springframework.data.domain.PageRequest.of(paginaAjustada, size, sort);
        }

        org.springframework.data.jpa.domain.Specification<Usuario> spec =
                org.mgroko.backend.admin.especificacion.UsuarioSpecifications.conFiltros(filtro);
        org.springframework.data.domain.Page<Usuario> resultado = usuarioRepository.findAll(spec, pageable);

        org.springframework.data.domain.Page<AdminUsuarioResponse> paginaDto =
                resultado.map(AdminUsuarioMapper::toResponse);

        return org.mgroko.backend.common.dto.PaginaResponse.fromPage(paginaDto);
    }

    @Transactional(readOnly = true)
    public AdminUsuarioResponse obtenerDetalle(Long idUsuario) {
        Usuario usuario = buscarOFallar(idUsuario);
        return AdminUsuarioMapper.toResponse(usuario);
    }

    @Transactional
    @AuditarOperacion
    public AdminUsuarioResponse habilitar(Long idUsuario) {
        Usuario usuario = buscarOFallar(idUsuario);
        rechazarSiEstaEnBaja(usuario);
        boolean cambiaEstado = usuario.getEstado() != EstadoUsuario.Activo;
        usuario.setEstado(EstadoUsuario.Activo);
        usuario.setMotivoDeshabilitacion(null);
        usuario.setFechaHastaDeshabilitacion(null);
        Usuario guardado = usuarioRepository.save(usuario);
        if (cambiaEstado) {
            notificarCambioCuenta(
                    guardado,
                    NotificacionService.TIPO_USUARIO_HABILITADO,
                    "Tu cuenta fue habilitada nuevamente. Ya podés volver a ingresar a ModaLink.");
        }
        return AdminUsuarioMapper.toResponse(guardado);
    }

    /**
     * Deshabilita un usuario (UC-04). El motivo es obligatorio y la
     * duración opcional: si {@code duracionDias} se indica, la cuenta se
     * reactiva automáticamente al vencer; si no, la deshabilitación es
     * indefinida y solo se revierte con {@code /habilitar}.
     */
    @Transactional
    @AuditarOperacion
    public AdminUsuarioResponse deshabilitar(Long idUsuario, Long idUsuarioSolicitante, DeshabilitarUsuarioRequest request) {
        if (idUsuario.equals(idUsuarioSolicitante)) {
            throw new AutoDeshabilitacionException("No podés deshabilitar tu propia cuenta.");
        }
        Usuario usuario = buscarOFallar(idUsuario);
        rechazarSiEstaEnBaja(usuario);
        boolean cambiaEstado = usuario.getEstado() != EstadoUsuario.Deshabilitado;
        usuario.setEstado(EstadoUsuario.Deshabilitado);
        usuario.setMotivoDeshabilitacion(request.motivo());
        usuario.setFechaHastaDeshabilitacion(
                request.duracionDias() != null
                        ? LocalDateTime.now().plusDays(request.duracionDias())
                        : null);
        Usuario guardado = usuarioRepository.save(usuario);
        if (cambiaEstado) {
            notificarCambioCuenta(
                    guardado,
                    NotificacionService.TIPO_USUARIO_DESHABILITADO,
                    mensajeDeshabilitacion(guardado));
        }
        return AdminUsuarioMapper.toResponse(guardado);
    }

    private void notificarCambioCuenta(Usuario usuario, String tipo, String mensaje) {
        notificacionService.registrarTodas(List.of(
                notificacionService.crear(
                        usuario,
                        tipo,
                        mensaje,
                        NotificacionService.ENTIDAD_USUARIO,
                        usuario.getIdUsuario())));
    }

    private String mensajeDeshabilitacion(Usuario usuario) {
        String motivo = usuario.getMotivoDeshabilitacion() == null
                ? ""
                : usuario.getMotivoDeshabilitacion().trim();
        StringBuilder mensaje = new StringBuilder(
                "Tu cuenta fue deshabilitada. Motivo: ").append(motivo).append('.');
        if (usuario.getFechaHastaDeshabilitacion() != null) {
            mensaje.append(" La deshabilitación finaliza el ")
                    .append(usuario.getFechaHastaDeshabilitacion().format(FORMATO_FECHA))
                    .append('.');
        }
        return mensaje.toString();
    }

    private void rechazarSiEstaEnBaja(Usuario usuario) {
        if (usuario.getEstado() == EstadoUsuario.Baja) {
            throw new UsuarioEnBajaException(
                    "El usuario solicitó la baja de su cuenta; no puede habilitarse ni deshabilitarse desde el panel administrativo.");
        }
    }
    private Usuario buscarOFallar(Long idUsuario) {
    return usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new UsuarioAdminNoEncontradoException("Usuario no encontrado."));
}
}