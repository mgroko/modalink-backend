package org.mgroko.backend.storage.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
import org.springframework.mock.web.MockMultipartFile;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService(tempDir.toString(), "perfiles");
    }

    @Test
    void almacenarFotoPerfil_valida_optimizaYRedimensionaA600x600() throws IOException {
        // Generar una imagen en memoria de 1200x800 px (alta resolución)
        BufferedImage imagenOriginal = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagenOriginal.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 1200, 800);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(imagenOriginal, "jpg", baos);
        byte[] bytesOriginales = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "foto_original.jpg",
                "image/jpeg",
                bytesOriginales
        );

        ArchivoAlmacenado resultado = storageService.almacenarFotoPerfil(file);

        assertNotNull(resultado);
        assertNotNull(resultado.nombreArchivo());
        assertTrue(resultado.nombreArchivo().startsWith("perfil_"));
        assertTrue(resultado.nombreArchivo().endsWith(".jpg"));
        assertEquals("image/jpeg", resultado.tipoMime());

        // Verificar archivo en disco
        Path archivoEnDisco = tempDir.resolve("perfiles").resolve(resultado.nombreArchivo());
        assertTrue(Files.exists(archivoEnDisco));

        // Verificar dimensiones en disco: exactamente 600x600 px
        byte[] bytesGuardados = Files.readAllBytes(archivoEnDisco);
        BufferedImage imagenOptimizada = ImageIO.read(new ByteArrayInputStream(bytesGuardados));
        assertEquals(600, imagenOptimizada.getWidth());
        assertEquals(600, imagenOptimizada.getHeight());
    }

    @Test
    void almacenarFotoPerfil_archivoVacio_lanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "vacio.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThrows(ArchivoVacioException.class, () -> storageService.almacenarFotoPerfil(file));
    }

    @Test
    void almacenarFotoPerfil_formatoNoPermitido_lanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "documento.pdf",
                "application/pdf",
                "contenido dummy".getBytes()
        );

        assertThrows(FormatoImagenInvalidoException.class, () -> storageService.almacenarFotoPerfil(file));
    }

    @Test
    void almacenarFotoPerfil_bytesCorruptosConMimeImage_lanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "falso.png",
                "image/png",
                "esto no es una imagen".getBytes()
        );

        assertThrows(FormatoImagenInvalidoException.class, () -> storageService.almacenarFotoPerfil(file));
    }

    @Test
    void eliminarFotoPerfil_eliminaArchivoEnDisco() throws IOException {
        Path perfilesDir = tempDir.resolve("perfiles");
        Path archivoPrueba = perfilesDir.resolve("perfil_test.jpg");
        Files.write(archivoPrueba, "dummy".getBytes());
        assertTrue(Files.exists(archivoPrueba));

        storageService.eliminarFotoPerfil("perfil_test.jpg");

        assertTrue(Files.notExists(archivoPrueba));
    }
}
