package org.mgroko.backend.storage.servicio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.dto.ArchivoEnDisco;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.exception.ErrorAlmacenamientoException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Persiste en disco local las fotos de perfil, las im&aacute;genes del moodboard
 * y las im&aacute;genes de publicaciones, ya convertidas por
 * {@link ProcesadorImagen}. Cada tipo de imagen vive en su propio
 * subdirectorio dentro de {@code uploads}:
 * <ul>
 * <li>{@code perfiles/}</li>
 * <li>{@code moodboard/}</li>
 * <li>{@code publicaciones/}</li>
 * <li>{@code polaroids/}</li>
 * </ul>
 *
 * <p>El borrado por nombre busca en todos los directorios gestionados para
 * mantener la compatibilidad con archivos hist&oacute;ricos del moodboard que
 * fueron guardados en {@code perfiles/} antes de esta separaci&oacute;n.</p>
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
    private static final String PREFIJO_NOMBRE_MOODBOARD = "moodboard_";
    private static final String PREFIJO_NOMBRE_PUBLICACION = "publicacion_";
    private static final String PREFIJO_NOMBRE_POLAROID = "polaroid_";

    private final Path perfilesPath;
    private final Path moodboardPath;
    private final Path publicacionesPath;
    private final Path polaroidsPath;
    private final String uploadDir;
    private final String perfilesSubdir;
    private final String moodboardSubdir;
    private final String publicacionesSubdir;
    private final String polaroidsSubdir;
    private final ProcesadorImagen procesadorImagen;
    private final HuellaImagenService huellaImagenService;

    public LocalStorageService(
            @Value("${app.storage.upload-dir:uploads}") String uploadDir,
            @Value("${app.storage.perfiles-dir:perfiles}") String perfilesSubdir,
            @Value("${app.storage.moodboard-dir:moodboard}") String moodboardSubdir,
            @Value("${app.storage.publicaciones-dir:publicaciones}") String publicacionesSubdir,
            @Value("${app.storage.polaroids-dir:polaroids}") String polaroidsSubdir,
            ProcesadorImagen procesadorImagen,
            HuellaImagenService huellaImagenService) {
        this.uploadDir = uploadDir;
        this.perfilesSubdir = perfilesSubdir;
        this.moodboardSubdir = moodboardSubdir;
        this.publicacionesSubdir = publicacionesSubdir;
        this.polaroidsSubdir = polaroidsSubdir;
        this.procesadorImagen = procesadorImagen;
        this.huellaImagenService = huellaImagenService;

        this.perfilesPath = prepararDirectorio(uploadDir, perfilesSubdir);
        this.moodboardPath = prepararDirectorio(uploadDir, moodboardSubdir);
        this.publicacionesPath = prepararDirectorio(uploadDir, publicacionesSubdir);
        this.polaroidsPath = prepararDirectorio(uploadDir, polaroidsSubdir);
    }

    @Override
    public ArchivoAlmacenado almacenarFotoPerfil(MultipartFile archivo) {
        return almacenar(perfilesSubdir, perfilesPath, PREFIJO_NOMBRE_PERFIL, archivo, false);
    }

    @Override
    public ArchivoAlmacenado almacenarImagenMoodboard(MultipartFile archivo) {
        return almacenar(moodboardSubdir, moodboardPath, PREFIJO_NOMBRE_MOODBOARD, archivo, false);
    }

    @Override
    public ArchivoAlmacenado almacenarImagenPublicacion(MultipartFile archivo) {
        return almacenar(publicacionesSubdir, publicacionesPath, PREFIJO_NOMBRE_PUBLICACION, archivo, true);
    }

    @Override
    public ArchivoAlmacenado almacenarImagenPolaroid(MultipartFile archivo) {
        // Las polaroids tampoco se recortan: se conserva la relación de aspecto.
        return almacenar(polaroidsSubdir, polaroidsPath, PREFIJO_NOMBRE_POLAROID, archivo, true);
    }

    private ArchivoAlmacenado almacenar(
            String subdirectorio,
            Path directorio,
            String prefijoNombre,
            MultipartFile archivo,
            boolean paraPublicacion) {

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

        byte[] bytesConvertidos = paraPublicacion
                ? procesadorImagen.procesarParaPublicacion(bytes)
                : procesadorImagen.procesar(bytes);

        String nombreArchivoGenerado = prefijoNombre + UUID.randomUUID()
                + "." + ProcesadorImagen.EXTENSION_SALIDA;
        Path destino = directorio.resolve(nombreArchivoGenerado);

        try {
            Files.write(destino, bytesConvertidos, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException("Error al escribir la imagen optimizada en disco.", e);
        }

        String urlRelativa = "/" + uploadDir + "/" + subdirectorio + "/" + nombreArchivoGenerado;

        return new ArchivoAlmacenado(
                nombreArchivoGenerado,
                urlRelativa,
                ProcesadorImagen.MIME_SALIDA,
                bytesConvertidos.length,
                huellaImagenService.sha256(bytes),
                huellaImagenService.sha256(bytesConvertidos),
                huellaImagenService.phash(bytesConvertidos)
        );
    }

    /**
     * Busca el archivo por nombre en todos los directorios gestionados. Se
     * mantiene el nombre hist&oacute;rico del m&eacute;todo para no romper a sus
     * consumidores, pero el alcance ya no es solo {@code perfiles/}.
     */
    @Override
    public byte[] leerArchivo(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return null;
        }

        String nombreSimple = Paths.get(nombreArchivo).getFileName().toString();
        for (Path directorio : List.of(perfilesPath, moodboardPath, publicacionesPath, polaroidsPath)) {
            Path archivoPath = directorio.resolve(nombreSimple).normalize();
            if (!archivoPath.startsWith(directorio) || !Files.isRegularFile(archivoPath)) {
                continue;
            }
            try {
                return Files.readAllBytes(archivoPath);
            } catch (IOException e) {
                throw new ErrorAlmacenamientoException(
                        "No se pudo leer el archivo de almacenamiento: " + archivoPath, e);
            }
        }
        return null;
    }

    @Override
    public void eliminarFotoPerfil(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return;
        }

        String nombreSimple = Paths.get(nombreArchivo).getFileName().toString();
        for (Path directorio : List.of(perfilesPath, moodboardPath, publicacionesPath, polaroidsPath)) {
            Path archivoPath = directorio.resolve(nombreSimple).normalize();
            if (!archivoPath.startsWith(directorio) || !Files.exists(archivoPath)) {
                continue;
            }
            borrarArchivo(archivoPath);
        }
    }

    @Override
    public void eliminarArchivo(String subdirectorio, String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return;
        }

        Path directorio = directorioPorSubdirectorio(subdirectorio);
        Path archivoPath = directorio.resolve(Paths.get(nombreArchivo).getFileName().toString()).normalize();
        if (!archivoPath.startsWith(directorio)) {
            throw new ErrorAlmacenamientoException(
                    "La ruta del archivo no pertenece al directorio de almacenamiento: " + archivoPath, null);
        }

        borrarArchivo(archivoPath);
    }

    @Override
    public Map<String, Instant> listarFotografiasDePerfil() {
        Map<String, Instant> fotos = new LinkedHashMap<>();
        for (ArchivoEnDisco archivo : listarDirectorio(perfilesSubdir, perfilesPath)) {
            fotos.put(archivo.nombreArchivo(), archivo.fechaModificacion());
        }
        return fotos;
    }

    @Override
    public List<ArchivoEnDisco> listarArchivos() {
        List<ArchivoEnDisco> archivos = new ArrayList<>();
        archivos.addAll(listarDirectorio(perfilesSubdir, perfilesPath));
        archivos.addAll(listarDirectorio(moodboardSubdir, moodboardPath));
        archivos.addAll(listarDirectorio(publicacionesSubdir, publicacionesPath));
        archivos.addAll(listarDirectorio(polaroidsSubdir, polaroidsPath));
        archivos.sort(Comparator
                .comparing(ArchivoEnDisco::subdirectorio)
                .thenComparing(ArchivoEnDisco::nombreArchivo));
        return archivos;
    }

    private List<ArchivoEnDisco> listarDirectorio(String subdirectorio, Path directorio) {
        if (!Files.isDirectory(directorio)) {
            return List.of();
        }

        List<ArchivoEnDisco> archivos = new ArrayList<>();
        try (Stream<Path> entradas = Files.list(directorio)) {
            for (Path ruta : entradas.filter(Files::isRegularFile).toList()) {
                // Mismo criterio que eliminarFotoPerfil: solo el nombre simple del
                // archivo, nunca rutas con separadores ni "..".
                String nombre = Paths.get(ruta.getFileName().toString()).getFileName().toString();
                if (!directorio.resolve(nombre).normalize().equals(ruta.normalize())) {
                    continue;
                }
                try {
                    archivos.add(new ArchivoEnDisco(
                            subdirectorio,
                            nombre,
                            Files.getLastModifiedTime(ruta).toInstant()));
                } catch (IOException e) {
                    LOG.warn("No se pudo leer la fecha de modificación de '{}'; no entra en la limpieza.", nombre, e);
                }
            }
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException(
                    "No se pudo listar el directorio de almacenamiento: " + directorio, e);
        }
        return archivos;
    }

    private void borrarArchivo(Path archivoPath) {
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

    private Path directorioPorSubdirectorio(String subdirectorio) {
        if (perfilesSubdir.equals(subdirectorio)) {
            return perfilesPath;
        }
        if (moodboardSubdir.equals(subdirectorio)) {
            return moodboardPath;
        }
        if (publicacionesSubdir.equals(subdirectorio)) {
            return publicacionesPath;
        }
        if (polaroidsSubdir.equals(subdirectorio)) {
            return polaroidsPath;
        }
        throw new ErrorAlmacenamientoException(
                "Directorio de almacenamiento desconocido: " + subdirectorio, null);
    }

    private static Path prepararDirectorio(String uploadDir, String subdirectorio) {
        Path directorio = Paths.get(uploadDir, subdirectorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException(
                    "No se pudo crear el directorio de subida: " + directorio, e);
        }
        return directorio;
    }

    private static String formatosAdmitidos() {
        return CONTENT_TYPES_PERMITIDOS.stream()
                .sorted()
                .map(tipo -> tipo.substring(PREFIJO_CONTENT_TYPE.length()).toUpperCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
    }
}
