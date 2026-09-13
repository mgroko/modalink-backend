package org.mgroko.backend.perfiles.servicio;

import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.mapper.PerfilMapper;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.servicio.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FotoPerfilService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ImagenRepository imagenRepository;
    private final StorageService storageService;

    public FotoPerfilService(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ImagenRepository imagenRepository,
            StorageService storageService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.imagenRepository = imagenRepository;
        this.storageService = storageService;
    }

    @Transactional
    public PerfilResponse subirFoto(Long idUsuario, Long idPerfil, MultipartFile archivo) {
        Perfil perfil = obtenerPerfilValido(idUsuario, idPerfil);

        // 1. Procesar, redimensionar a 600x600 px y guardar físicamente
        ArchivoAlmacenado archivoAlmacenado = storageService.almacenarFotoPerfil(archivo);

        // 2. Si ya tenía una foto previa, desvincularla y limpiarla
        Imagen imagenAnterior = perfil.getImagen();

        // 3. Crear entidad Imagen en base de datos
        Imagen nuevaImagen = Imagen.builder()
                .nombreArchivo(archivoAlmacenado.nombreArchivo())
                .url(archivoAlmacenado.url())
                .tipoImagen(archivoAlmacenado.tipoMime())
                .tamanoBytes((int) archivoAlmacenado.tamanoBytes())
                .estado("Activa")
                .build();

        Imagen imagenGuardada = imagenRepository.save(nuevaImagen);

        // 4. Asignar nueva imagen al perfil
        perfil.setImagen(imagenGuardada);
        Perfil perfilGuardado = perfilRepository.save(perfil);

        // 5. Eliminar la imagen previa de la base de datos y de disco
        if (imagenAnterior != null) {
            imagenRepository.delete(imagenAnterior);
            storageService.eliminarFotoPerfil(imagenAnterior.getNombreArchivo());
        }

        return PerfilMapper.toResponse(perfilGuardado);
    }

    @Transactional
    public PerfilResponse eliminarFoto(Long idUsuario, Long idPerfil) {
        Perfil perfil = obtenerPerfilValido(idUsuario, idPerfil);

        Imagen imagenActual = perfil.getImagen();
        if (imagenActual != null) {
            perfil.setImagen(null);
            perfilRepository.save(perfil);

            imagenRepository.delete(imagenActual);
            storageService.eliminarFotoPerfil(imagenActual.getNombreArchivo());
        }

        return PerfilMapper.toResponse(perfil);
    }

    private Perfil obtenerPerfilValido(Long idUsuario, Long idPerfil) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        if (!usuario.getEstado().permiteAcceso()) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfil, idUsuario)
                .orElseThrow(() -> new PerfilNoEncontradoException("Perfil no encontrado."));

        if (perfil.getEstado() == EstadoPerfil.Baja) {
            throw new PerfilEnBajaException("No se puede gestionar la foto de un perfil dado de baja.");
        }

        return perfil;
    }
}
