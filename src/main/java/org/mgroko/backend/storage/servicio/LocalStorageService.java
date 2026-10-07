package org.mgroko.backend.storage.servicio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.exception.ErrorAlmacenamientoException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Persiste en disco local las fotos de perfil ya convertidas por
 * {@link ProcesadorImagen}: este servicio solo valida el content type de entrada,
 * escribe los bytes resultantes y borra archivos.
 */
@Service
public class LocalStorageService implements StorageService {

    private static final Logger LOG = LoggerFactory.getLogger(LocalStorageService.class);

    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            ProcesadorImagen.MIME_SALIDA
    );

    private static final String PREFIJO_CONTENT_TYPE = "image/";
    private static final String PREFIJO_NOMBRE_PERFIL = "perfil_";

    private final Path perfilesPath;
    private final String uploadDir;
    private final String perfilesSubdir;
    private final ProcesadorImagen procesadorImagen;

    public LocalStorageService(
            @Value("${app.storage.upload-dir:uploads}") String uploadDir,
            @Value("${app.storage.perfiles-dir:perfiles}") String perfilesSubdir,
            ProcesadorImagen procesadorImagen) {
        this.uploadDir = uploadDir;
        this.perfilesSubdir = perfilesSubdir;
        this.procesadorImagen = procesadorImagen;
        this.perfilesPath = Paths.get(uploadDir, perfilesSubdir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.perfilesPath);
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException("No se pudo crear el directorio de subida: " + this.perfilesPath, e);
        }
    }

    @Override
    public ArchivoAlmacenado almacenarFotoPerfil(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoVacioException("El archivo de imagen no puede estar vacío.");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new FormatoImagenInvalidoException(
                    "El formato de archivo no está permitido. Formatos admitidos: " + formatosAdmitidos() + ".");
        }

        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException("Error al leer los bytes del archivo.", e);
        }

        byte[] bytesConvertidos = procesadorImagen.procesar(bytes);

        String nombreArchivoGenerado = PREFIJO_NOMBRE_PERFIL + UUID.randomUUID()
                + "." + ProcesadorImagen.EXTENSION_SALIDA;
        Path destino = this.perfilesPath.resolve(nombreArchivoGenerado);

        try {
            Files.write(destino, bytesConvertidos, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException("Error al escribir la imagen optimizada en disco.", e);
        }

        String urlRelativa = "/" + uploadDir + "/" + perfilesSubdir + "/" + nombreArchivoGenerado;

        return new ArchivoAlmacenado(
                nombreArchivoGenerado,
                urlRelativa,
                ProcesadorImagen.MIME_SALIDA,
                bytesConvertidos.length
        );
    }

    @Override
    public void eliminarFotoPerfil(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return;
        }

        // Prevenir path traversal
        Path archivoPath = this.perfilesPath.resolve(Paths.get(nombreArchivo).getFileName().toString()).normalize();

        try {
            Files.deleteIfExists(archivoPath);
        } catch (IOException e) {
            // No se traga el error: se registra con la ruta y se propaga; los borrados
            // programados por la transacción lo capturan y lo dejan para el job de
            // limpieza (Fase 5).
            LOG.warn("No se pudo eliminar el archivo físico: {}", archivoPath, e);
            throw new ErrorAlmacenamientoException("No se pudo eliminar el archivo físico: " + archivoPath, e);
        }
    }

    @Override
    public Map<String, Instant> listarFotografiasDePerfil() {
        if (!Files.isDirectory(this.perfilesPath)) {
            return Map.of();
        }

        Map<String, Instant> fotos = new LinkedHashMap<>();
        try (Stream<Path> entradas = Files.list(this.perfilesPath)) {
            for (Path ruta : entradas.filter(Files::isRegularFile).toList()) {
                // Mismo criterio que getFileName() de eliminarFotoPerfil: solo el
                // nombre simple del archivo, nunca rutas con separadores ni "..".
                String nombre = Paths.get(ruta.getFileName().toString()).getFileName().toString();
                if (!this.perfilesPath.resolve(nombre).normalize().equals(ruta.normalize())) {
                    continue;
                }
                try {
                    fotos.put(nombre, Files.getLastModifiedTime(ruta).toInstant());
                } catch (IOException e) {
                    LOG.warn("No se pudo leer la fecha de modificación de '{}'; no entra en la limpieza.", nombre, e);
                }
            }
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException(
                    "No se pudo listar el directorio de fotos de perfil: " + this.perfilesPath, e);
        }
        return fotos;
    }

    private static String formatosAdmitidos() {
        return CONTENT_TYPES_PERMITIDOS.stream()
                .sorted()
                .map(tipo -> tipo.substring(PREFIJO_CONTENT_TYPE.length()).toUpperCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
    }
}
