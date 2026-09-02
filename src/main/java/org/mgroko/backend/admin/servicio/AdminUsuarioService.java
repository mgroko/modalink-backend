package org.mgroko.backend.admin.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.admin.dto.AdminUsuarioResponse;
import org.mgroko.backend.admin.dto.DeshabilitarUsuarioRequest;
import org.mgroko.backend.admin.exception.AutoDeshabilitacionException;
import org.mgroko.backend.admin.exception.UsuarioAdminNoEncontradoException;
import org.mgroko.backend.admin.exception.UsuarioEnBajaException;
import org.mgroko.backend.admin.mapper.AdminUsuarioMapper;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUsuarioService {

    private final UsuarioRepository usuarioRepository;

    public AdminUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(AdminUsuarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUsuarioResponse obtenerDetalle(Long idUsuario) {
        Usuario usuario = buscarOFallar(idUsuario);
        return AdminUsuarioMapper.toResponse(usuario);
    }

    @Transactional
    public AdminUsuarioResponse habilitar(Long idUsuario) {
        Usuario usuario = buscarOFallar(idUsuario);
        rechazarSiEstaEnBaja(usuario);
        usuario.setEstado(EstadoUsuario.Activo);
        usuario.setMotivoDeshabilitacion(null);
        usuario.setFechaHastaDeshabilitacion(null);
        return AdminUsuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    /**
     * Deshabilita un usuario (UC-04). El motivo es obligatorio y la
     * duración opcional: si {@code duracionDias} se indica, la cuenta se
     * reactiva automáticamente al vencer; si no, la deshabilitación es
     * indefinida y solo se revierte con {@code /habilitar}.
     */
    @Transactional
    public AdminUsuarioResponse deshabilitar(Long idUsuario, Long idUsuarioSolicitante, DeshabilitarUsuarioRequest request) {
        if (idUsuario.equals(idUsuarioSolicitante)) {
            throw new AutoDeshabilitacionException("No podés deshabilitar tu propia cuenta.");
        }
        Usuario usuario = buscarOFallar(idUsuario);
        rechazarSiEstaEnBaja(usuario);
        usuario.setEstado(EstadoUsuario.Deshabilitado);
        usuario.setMotivoDeshabilitacion(request.motivo());
        usuario.setFechaHastaDeshabilitacion(
                request.duracionDias() != null
                        ? LocalDateTime.now().plusDays(request.duracionDias())
                        : null);
        return AdminUsuarioMapper.toResponse(usuarioRepository.save(usuario));
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