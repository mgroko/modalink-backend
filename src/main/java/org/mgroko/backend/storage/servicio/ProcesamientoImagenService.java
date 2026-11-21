package org.mgroko.backend.storage.servicio;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.dto.ArchivoEnDisco;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Envuelve el almacenamiento de imágenes para que el procesamiento (decode,
 * resize, WebP, hashes) se ejecute fuera de la transacción de base de datos.
 *
 * <p>Los servicios de alta de publicación/perfil/moodboard/polaroid son
 * transaccionales. Al llamar a estos métodos a través del proxy de Spring, la
 * transacción se suspende, se libera la conexión JDBC y el trabajo intensivo de
 * CPU no mantiene ocupado el pool de Hikari.</p>
 */
@Service
@Primary
public class ProcesamientoImagenService implements StorageService {

    private final LocalStorageService localStorageService;

    public ProcesamientoImagenService(LocalStorageService localStorageService) {
        this.localStorageService = localStorageService;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ArchivoAlmacenado almacenarFotoPerfil(MultipartFile archivo) {
        return localStorageService.almacenarFotoPerfil(archivo);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ArchivoAlmacenado almacenarImagenMoodboard(MultipartFile archivo) {
        return localStorageService.almacenarImagenMoodboard(archivo);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ArchivoAlmacenado almacenarImagenPublicacion(MultipartFile archivo) {
        return localStorageService.almacenarImagenPublicacion(archivo);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ArchivoAlmacenado almacenarImagenPolaroid(MultipartFile archivo) {
        return localStorageService.almacenarImagenPolaroid(archivo);
    }

    @Override
    public void eliminarFotoPerfil(String nombreArchivo) {
        localStorageService.eliminarFotoPerfil(nombreArchivo);
    }

    @Override
    public byte[] leerArchivo(String nombreArchivo) {
        return localStorageService.leerArchivo(nombreArchivo);
    }

    @Override
    public void eliminarArchivo(String subdirectorio, String nombreArchivo) {
        localStorageService.eliminarArchivo(subdirectorio, nombreArchivo);
    }

    @Override
    public Map<String, Instant> listarFotografiasDePerfil() {
        return localStorageService.listarFotografiasDePerfil();
    }

    @Override
    public List<ArchivoEnDisco> listarArchivos() {
        return localStorageService.listarArchivos();
    }
}
