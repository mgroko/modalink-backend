package org.mgroko.backend.perfiles.servicio;

import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
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
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
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
    private final ConfiguracionSistemaService configuracionSistemaService;
    private final BorradoArchivosDiferido borradoArchivosDiferido;

    public FotoPerfilService(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ImagenRepository imagenRepository,
            StorageService storageService,
            ConfiguracionSistemaService configuracionSistemaService,
            BorradoArchivosDiferido borradoArchivosDiferido) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.imagenRepository = imagenRepository;
        this.storageService = storageService;
        this.configuracionSistemaService = configuracionSistemaService;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
    }

    @Transactional
    public PerfilResponse subirFoto(Long idUsuario, Long idPerfil, MultipartFile archivo) {
        Perfil perfil = obtenerPerfilValido(idUsuario, idPerfil);

        // 1. Se escribe el archivo nuevo ANTES de tocar la BD: si la transacción no
        // llega a commit, el archivo recién escrito se borra en afterCompletion.
        ArchivoAlmacenado archivoAlmacenado = storageService.almacenarFotoPerfil(archivo);

        Imagen imagenAnterior = perfil.getImagen();
        String nombreArchivoAnterior = imagenAnterior != null ? imagenAnterior.getNombreArchivo() : null;

        // 2. El archivo viejo se borra recién después del commit (nunca dentro de la transacción)
        borradoArchivosDiferido.registrarBorrados(archivoAlmacenado.nombreArchivo(), nombreArchivoAnterior);

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

        // 5. Desvincular la imagen previa (solo BD: su archivo se borra tras el commit)
        if (imagenAnterior != null) {
            imagenRepository.delete(imagenAnterior);
        }

        return PerfilMapper.toResponse(perfilGuardado, configuracionSistemaService.obtenerDiasBaja());
    }

    @Transactional
    public PerfilResponse eliminarFoto(Long idUsuario, Long idPerfil) {
        Perfil perfil = obtenerPerfilValido(idUsuario, idPerfil);

        Imagen imagenActual = perfil.getImagen();
        if (imagenActual != null) {
            String nombreArchivoActual = imagenActual.getNombreArchivo();

            perfil.setImagen(null);
            perfilRepository.save(perfil);
            imagenRepository.delete(imagenActual);

            // El archivo se borra recién después del commit (si no hay commit, se conserva)
            borradoArchivosDiferido.registrarBorrados(null, nombreArchivoActual);
        }

        return PerfilMapper.toResponse(perfil, configuracionSistemaService.obtenerDiasBaja());
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
