package org.mgroko.backend.storage.servicio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.mgroko.backend.repositorio.ConfiguracionSistemaRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.exception.ErrorAlmacenamientoException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
import org.springframework.mock.web.MockMultipartFile;

class LocalStorageServiceTest {

    private static final String CLAVE_MAX_LADO_PX = ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_LADO_PX;
    private static final String CLAVE_MAX_MEGAPIXELES = ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_MEGAPIXELES;
    private static final String CLAVE_MAX_TAMANO_BYTES = ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_TAMANO_BYTES;
    private static final String CLAVE_LADO_SALIDA_PX = ConfiguracionSistemaService.CLAVE_IMAGEN_LADO_SALIDA_PX;
    private static final String CLAVE_CALIDAD_WEBP = ConfiguracionSistemaService.CLAVE_IMAGEN_CALIDAD_WEBP;

    private static final Map<String, String> CONFIGURACION = Map.of(
            CLAVE_MAX_LADO_PX, "8000",
            CLAVE_MAX_MEGAPIXELES, "24",
            CLAVE_MAX_TAMANO_BYTES, "10485760",
            CLAVE_LADO_SALIDA_PX, "600",
            CLAVE_CALIDAD_WEBP, "85");

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        ConfiguracionSistemaRepository repositorio = mock(ConfiguracionSistemaRepository.class);
        when(repositorio.findByClave(anyString())).thenAnswer(invocacion -> {
            String clave = invocacion.getArgument(0);
            String valor = CONFIGURACION.get(clave);
            if (valor == null) {
                return Optional.empty();
            }
            return Optional.of(ConfiguracionSistema.builder().clave(clave).valor(valor).build());
        });

        storageService = new LocalStorageService(tempDir.toString(), "perfiles", new ProcesadorImagen(repositorio));
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
        assertTrue(resultado.nombreArchivo().endsWith(".webp"));
        assertEquals("image/webp", resultado.tipoMime());

        // Verificar archivo en disco
        Path archivoEnDisco = tempDir.resolve("perfiles").resolve(resultado.nombreArchivo());
        assertTrue(Files.exists(archivoEnDisco));
        assertEquals(Files.size(archivoEnDisco), resultado.tamanoBytes(),
                "El tamaño reportado debe ser el del archivo ya convertido");

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
    void almacenarFotoPerfil_archivoNulo_lanzaExcepcion() {
        assertThrows(ArchivoVacioException.class, () -> storageService.almacenarFotoPerfil(null));
    }

    @Test
    void almacenarFotoPerfil_sinContentType_lanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "foto.jpg",
                null,
                "contenido dummy".getBytes()
        );

        assertThrows(FormatoImagenInvalidoException.class, () -> storageService.almacenarFotoPerfil(file));
    }

    @Test
    void almacenarFotoPerfil_formatoNoPermitido_lanzaExcepcionConLosFormatosAdmitidos() {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "documento.pdf",
                "application/pdf",
                "contenido dummy".getBytes()
        );

        FormatoImagenInvalidoException excepcion = assertThrows(FormatoImagenInvalidoException.class,
                () -> storageService.almacenarFotoPerfil(file));
        assertTrue(excepcion.getMessage().contains("JPEG, PNG, WEBP"),
                "Debe anunciar los formatos realmente admitidos: " + excepcion.getMessage());
    }

    @Test
    void almacenarFotoPerfil_contentTypeJpg_noEsAdmitido() {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "foto.jpg",
                "image/jpg",
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

    @Test
    void eliminarFotoPerfil_nombreNuloOEnBlanco_noFalla() {
        assertDoesNotThrow(() -> storageService.eliminarFotoPerfil(null));
        assertDoesNotThrow(() -> storageService.eliminarFotoPerfil(""));
        assertDoesNotThrow(() -> storageService.eliminarFotoPerfil("   "));
    }

    @Test
    void eliminarFotoPerfil_archivoQueNoSePuedeBorrar_lanzaErrorYNoSeTraga() throws IOException {
        // Un directorio no vacío no se puede borrar: reproduce un borrado fallido
        // (en Windows ocurre si el archivo está abierto por otro proceso)
        Path bloqueado = tempDir.resolve("perfiles").resolve("perfil_bloqueado.webp");
        Files.createDirectory(bloqueado);
        Files.write(bloqueado.resolve("interno.bin"), "dummy".getBytes());

        ErrorAlmacenamientoException excepcion = assertThrows(ErrorAlmacenamientoException.class,
                () -> storageService.eliminarFotoPerfil("perfil_bloqueado.webp"));
        assertTrue(excepcion.getMessage().contains("perfil_bloqueado.webp"),
                "El error debe indicar la ruta: " + excepcion.getMessage());
        assertTrue(Files.exists(bloqueado));
    }

    @Test
    void almacenarFotoPerfil_noDejaStreamsAbiertos_yElArchivoSePuedeBorrar() throws IOException {
        ArchivoAlmacenado almacenado = storageService.almacenarFotoPerfil(new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", jpeg()));

        // En Windows el borrado falla si la conversión dejó algún stream abierto
        assertDoesNotThrow(() -> storageService.eliminarFotoPerfil(almacenado.nombreArchivo()));
        assertTrue(Files.notExists(tempDir.resolve("perfiles").resolve(almacenado.nombreArchivo())));
    }

    private static byte[] jpeg() throws IOException {
        BufferedImage imagen = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 800, 600);
        g.dispose();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(imagen, "jpg", salida), "No hay escritor JPEG disponible");
        return salida.toByteArray();
    }

    @Test
    void listarFotografiasDePerfil_soloArchivosDelDirectorioConSuFechaDeModificacion() throws IOException {
        Path perfilesDir = tempDir.resolve("perfiles");
        Path archivo = perfilesDir.resolve("perfil_vieja.webp");
        Files.write(archivo, "dummy".getBytes());
        Path subdirectorio = perfilesDir.resolve("subdirectorio");
        Files.createDirectory(subdirectorio);
        Files.write(subdirectorio.resolve("perfil_dentro.webp"), "dummy".getBytes());

        Map<String, Instant> fotografias = storageService.listarFotografiasDePerfil();

        assertEquals(Set.of("perfil_vieja.webp"), fotografias.keySet(),
                "Solo se listan archivos del directorio, nunca subdirectorios");
        assertNotNull(fotografias.get("perfil_vieja.webp"));
        assertTrue(fotografias.get("perfil_vieja.webp").isBefore(Instant.now()),
                "Debe devolver la última modificación real del archivo");
        assertTrue(Files.exists(subdirectorio.resolve("perfil_dentro.webp")),
                "El listado no debe tocar el contenido de los subdirectorios");
    }

    @Test
    void listarFotografiasDePerfil_directorioInexistente_retornaVacio() throws IOException {
        Files.delete(tempDir.resolve("perfiles"));

        assertTrue(storageService.listarFotografiasDePerfil().isEmpty());
    }

    @Test
    void listarFotografiasDePerfil_directorioVacio_retornaVacio() {
        assertTrue(storageService.listarFotografiasDePerfil().isEmpty());
    }
}
