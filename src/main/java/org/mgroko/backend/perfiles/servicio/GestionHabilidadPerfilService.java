package org.mgroko.backend.perfiles.servicio;

import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.perfiles.exception.HabilidadNoAsociadaException;
import org.mgroko.backend.perfiles.exception.HabilidadYaAsociadaException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.mapper.HabilidadMapper;
import org.mgroko.backend.proyectos.exception.HabilidadNoEncontradaException;
import org.mgroko.backend.repositorio.HabilidadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.PerfilAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-47 — Gestionar habilidades desde el perfil.
 */
@Service
public class GestionHabilidadPerfilService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final HabilidadRepository habilidadRepository;

    public GestionHabilidadPerfilService(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            HabilidadRepository habilidadRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.habilidadRepository = habilidadRepository;
    }

    @Transactional(readOnly = true)
    public List<HabilidadResponse> listar(Long idUsuario, Long idPerfil) {
        Perfil perfil = buscarPerfilPropio(idUsuario, idPerfil);
        return perfil.getHabilidades().stream()
                .sorted(Comparator.comparing(Habilidad::getNombre,
                        String.CASE_INSENSITIVE_ORDER))
                .map(HabilidadMapper::toResponse)
                .toList();
    }

    @Transactional
    public HabilidadResponse agregar(Long idUsuario, Long idPerfil, Long idHabilidad) {
        Perfil perfil = buscarPerfilPropioParaModificar(idUsuario, idPerfil);
        Habilidad habilidad = buscarHabilidad(idHabilidad);

        boolean yaAsociada = perfil.getHabilidades().stream()
                .anyMatch(h -> h.getIdHabilidad().equals(idHabilidad));
        if (yaAsociada) {
            throw new HabilidadYaAsociadaException(
                    "El perfil ya tiene asociada la habilidad " + habilidad.getNombre() + ".");
        }

        perfil.getHabilidades().add(habilidad);
        perfilRepository.save(perfil);

        return HabilidadMapper.toResponse(habilidad);
    }

    @Transactional
    public void quitar(Long idUsuario, Long idPerfil, Long idHabilidad) {
        Perfil perfil = buscarPerfilPropioParaModificar(idUsuario, idPerfil);

        Habilidad habilidad = perfil.getHabilidades().stream()
                .filter(h -> h.getIdHabilidad().equals(idHabilidad))
                .findFirst()
                .orElseThrow(() -> new HabilidadNoAsociadaException(
                        "El perfil no tiene asociada la habilidad con id " + idHabilidad + "."));

        perfil.getHabilidades().remove(habilidad);
        perfilRepository.save(perfil);
    }

    private Perfil buscarPerfilPropio(Long idUsuario, Long idPerfil) {
        buscarUsuarioActivo(idUsuario);
        return perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfil, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);
    }

    private Perfil buscarPerfilPropioParaModificar(Long idUsuario, Long idPerfil) {
        Perfil perfil = buscarPerfilPropio(idUsuario, idPerfil);
        if (perfil.getEstado() == EstadoPerfil.Baja) {
            throw new PerfilEnBajaException("No se puede editar un perfil dado de baja.");
        }
        return perfil;
    }

    private Usuario buscarUsuarioActivo(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
        if (!usuario.getEstado().permiteAcceso()) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }
        PerfilAccessPolicy.validarNoAdministrador(usuario);
        return usuario;
    }

    private Habilidad buscarHabilidad(Long idHabilidad) {
        return habilidadRepository.findById(idHabilidad)
                .orElseThrow(() -> new HabilidadNoEncontradaException(idHabilidad));
    }
}
