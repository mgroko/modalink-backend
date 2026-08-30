package org.mgroko.backend.perfiles.servicio;

import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.EditarPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.exception.ImagenNoEncontradaException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.mapper.PerfilMapper;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditarPerfilService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ImagenRepository imagenRepository;
    private final CaracteristicaPerfilHelper caracteristicaPerfilHelper;

    public EditarPerfilService(UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ImagenRepository imagenRepository,
            CaracteristicaPerfilHelper caracteristicaPerfilHelper) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.imagenRepository = imagenRepository;
        this.caracteristicaPerfilHelper = caracteristicaPerfilHelper;
    }

    @Transactional
    public PerfilResponse editar(Long idUsuario, Long idPerfil, EditarPerfilRequest request) {
        Usuario usuario = buscarUsuarioActivo(idUsuario);

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfil, idUsuario)
                .orElseThrow(() -> new PerfilNoEncontradoException("Perfil no encontrado."));

        if (perfil.getEstado() == EstadoPerfil.Baja) {
            throw new PerfilEnBajaException("No se puede editar un perfil dado de baja.");
        }

        perfil.setNombreArtistico(request.nombreArtistico());
        perfil.setBiografia(request.biografia());
        perfil.setImagen(resolverImagen(request.idImagen()));

        perfil.getCaracteristicas().clear();
        perfilRepository.flush(); 

        perfil.getCaracteristicas().addAll(
                caracteristicaPerfilHelper.construir(perfil, perfil.getProfesion(), request.caracteristicas()));

        Perfil guardado = perfilRepository.save(perfil);
        return PerfilMapper.toResponse(guardado);
    }

    private Usuario buscarUsuarioActivo(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
        if (!usuario.getEstado().permiteAcceso()) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }
        return usuario;
    }

    private Imagen resolverImagen(Long idImagen) {
        if (idImagen == null) {
            return null;
        }
        return imagenRepository.findById(idImagen)
                .orElseThrow(() -> new ImagenNoEncontradaException("Imagen no encontrada: " + idImagen));
    }
}