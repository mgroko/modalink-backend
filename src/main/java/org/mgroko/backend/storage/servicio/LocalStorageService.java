package org.mgroko.backend.storage.servicio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.exception.ErrorAlmacenamientoException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.Thumbnails;

@Service
public class LocalStorageService implements StorageService {

    public static final int TARGET_WIDTH = 600;
    public static final int TARGET_HEIGHT = 600;
    public static final double TARGET_OUTPUT_QUALITY = 0.85;

    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final Path perfilesPath;
    private final String uploadDir;
    private final String perfilesSubdir;

    public LocalStorageService(
            @Value("${app.storage.upload-dir:uploads}") String uploadDir,
            @Value("${app.storage.perfiles-dir:perfiles}") String perfilesSubdir) {
        this.uploadDir = uploadDir;
        this.perfilesSubdir = perfilesSubdir;
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
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new FormatoImagenInvalidoException("El formato de archivo no está permitido. Formatos admitidos: JPEG, PNG, WEBP.");
        }

        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException("Error al leer los bytes del archivo.", e);
        }

        // Validación de magic bytes / lectura con ImageIO para descartar binarios corruptos o falsos
        try {
            if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
                throw new FormatoImagenInvalidoException("El archivo subido no es una imagen válida.");
            }
        } catch (IOException e) {
            throw new FormatoImagenInvalidoException("No se pudo interpretar el archivo como una imagen válida.");
        }

        // Redimensionamiento y optimización a 600x600 px
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            Thumbnails.of(new ByteArrayInputStream(bytes))
                    .size(TARGET_WIDTH, TARGET_HEIGHT)
                    .crop(net.coobird.thumbnailator.geometry.Positions.CENTER)
                    .outputFormat("jpg")
                    .outputQuality(TARGET_OUTPUT_QUALITY)
                    .toOutputStream(outputStream);
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException("Error al procesar y redimensionar la imagen.", e);
        }

        byte[] optimizedBytes = outputStream.toByteArray();
        String nombreArchivoGenerado = "perfil_" + UUID.randomUUID() + ".jpg";
        Path destino = this.perfilesPath.resolve(nombreArchivoGenerado);

        try {
            Files.write(destino, optimizedBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException("Error al escribir la imagen optimizada en disco.", e);
        }

        String urlRelativa = "/" + uploadDir + "/" + perfilesSubdir + "/" + nombreArchivoGenerado;

        return new ArchivoAlmacenado(
                nombreArchivoGenerado,
                urlRelativa,
                "image/jpeg",
                optimizedBytes.length
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
            // Se loguea pero no debe frenar la transacción principal si el archivo ya no estaba
            System.err.println("No se pudo eliminar el archivo físico: " + archivoPath + " - " + e.getMessage());
        }
    }
}
