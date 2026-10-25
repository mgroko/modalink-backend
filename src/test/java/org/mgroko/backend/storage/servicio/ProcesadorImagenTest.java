package org.mgroko.backend.storage.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mgroko.backend.admin.servicio.ConfiguracionSistemaService.CLAVE_IMAGEN_CALIDAD_WEBP;
import static org.mgroko.backend.admin.servicio.ConfiguracionSistemaService.CLAVE_IMAGEN_LADO_SALIDA_PX;
import static org.mgroko.backend.admin.servicio.ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_LADO_PX;
import static org.mgroko.backend.admin.servicio.ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_MEGAPIXELES;
import static org.mgroko.backend.admin.servicio.ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_TAMANO_BYTES;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.mgroko.backend.repositorio.ConfiguracionSistemaRepository;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.exception.DimensionesImagenExcedidasException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
import org.springframework.mock.web.MockMultipartFile;

class ProcesadorImagenTest {

    private static final int LADO_SALIDA = 320;

    private final Map<String, String> valoresPorDefecto = new HashMap<>();

    @BeforeEach
    void setUp() {
        valoresPorDefecto.put(CLAVE_IMAGEN_MAX_LADO_PX, "8000");
        valoresPorDefecto.put(CLAVE_IMAGEN_MAX_MEGAPIXELES, "24");
        valoresPorDefecto.put(CLAVE_IMAGEN_MAX_TAMANO_BYTES, "10485760");
        valoresPorDefecto.put(CLAVE_IMAGEN_LADO_SALIDA_PX, String.valueOf(LADO_SALIDA));
        valoresPorDefecto.put(CLAVE_IMAGEN_CALIDAD_WEBP, "85");
    }

    @Test
    void jpeg_generaSalidaWebPConElLadoDelParametro() throws IOException {
        byte[] salida = procesador().procesar(jpeg(1200, 800, Color.BLUE));

        assertEquals("RIFF", texto(salida, 0, 4));
        assertEquals("WEBP", texto(salida, 8, 4));

        BufferedImage leida = ImageIO.read(new ByteArrayInputStream(salida));
        assertNotNull(leida);
        assertEquals(LADO_SALIDA, leida.getWidth());
        assertEquals(LADO_SALIDA, leida.getHeight());
    }

    @Test
    void pngConTransparencia_conservaElAlfaCero() throws IOException {
        BufferedImage png = new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 800; y++) {
            for (int x = 0; x < 800; x++) {
                png.setRGB(x, y, x < 400 ? 0x00000000 : 0xFFFF0000);
            }
        }

        byte[] salida = procesador().procesar(aImagen(png, "png"));

        BufferedImage leida = ImageIO.read(new ByteArrayInputStream(salida));
        assertNotNull(leida);
        int alfa = (leida.getRGB(LADO_SALIDA / 4, LADO_SALIDA / 2) >>> 24) & 0xFF;
        assertEquals(0, alfa, "El píxel transparente debe conservar alfa 0");
    }

    @Test
    void webpDeEntrada_esAceptado() throws IOException {
        BufferedImage original = new BufferedImage(700, 500, BufferedImage.TYPE_INT_ARGB);
        rellenar(original, new Color(0, 128, 0));

        byte[] salida = procesador().procesar(aWebp(original));

        assertEquals("RIFF", texto(salida, 0, 4));
        assertEquals("WEBP", texto(salida, 8, 4));
        BufferedImage leida = ImageIO.read(new ByteArrayInputStream(salida));
        assertNotNull(leida);
        assertEquals(LADO_SALIDA, leida.getWidth());
        assertEquals(LADO_SALIDA, leida.getHeight());
    }

    @Test
    void bytesFalsosConContentTypeDeImagen_seRechazan() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "falso.png", "image/png", "esto no es una imagen".getBytes(StandardCharsets.UTF_8));

        assertThrows(FormatoImagenInvalidoException.class,
                () -> procesador().procesar(archivo.getBytes()));

        byte[] corto = { (byte) 0x89, 'P', 'N', 'G' };
        assertThrows(FormatoImagenInvalidoException.class,
                () -> procesador().procesar(corto));

        byte[] cortoSinFirma = { 0x01, 0x02, 0x03, 0x04 };
        assertThrows(FormatoImagenInvalidoException.class,
                () -> procesador().procesar(cortoSinFirma));
    }

    @Test
    void archivoQueSuperaElTamanoMaximo_seRechazaAntesDeDecodificar() throws IOException {
        Map<String, String> conLimiteBajo = new HashMap<>(valoresPorDefecto);
        conLimiteBajo.put(CLAVE_IMAGEN_MAX_TAMANO_BYTES, "1024");

        FormatoImagenInvalidoException excepcion = assertThrows(FormatoImagenInvalidoException.class,
                () -> procesadorCon(conLimiteBajo).procesar(jpeg(1200, 800, Color.BLUE)));
        assertTrue(excepcion.getMessage().contains("1024"),
                "El mensaje debe indicar el límite configurado: " + excepcion.getMessage());
    }

    @Test
    void imagenConHeaderValidoPeroRasterCorrupto_seRechaza() throws IOException {
        byte[] png = aImagen(new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB), "png");
        for (int i = png.length * 3 / 5; i < png.length; i++) {
            png[i] = 0;
        }

        assertThrows(FormatoImagenInvalidoException.class, () -> procesador().procesar(png));
    }

    @Test
    void archivoVacio_seRechaza() {
        assertThrows(ArchivoVacioException.class, () -> procesador().procesar(new byte[0]));
        assertThrows(ArchivoVacioException.class, () -> procesador().procesar(null));
    }

    @Test
    void imagenQueSuperaElLimite_seRechazaAntesDeDecodificar() throws IOException {
        BufferedImage grande = new BufferedImage(9000, 100, BufferedImage.TYPE_INT_RGB);
        rellenar(grande, Color.RED);

        byte[] png = aImagen(grande, "png");
        // El header sigue declarando 9000x100, pero el raster queda corrupto: si la
        // decodificación ocurriera primero, la falla sería de formato y no de dimensiones.
        for (int i = png.length * 3 / 5; i < png.length; i++) {
            png[i] = 0;
        }

        assertThrows(DimensionesImagenExcedidasException.class, () -> procesador().procesar(png));

        BufferedImage alta = new BufferedImage(100, 9000, BufferedImage.TYPE_INT_RGB);
        rellenar(alta, Color.RED);
        assertThrows(DimensionesImagenExcedidasException.class, () -> procesador().procesar(aImagen(alta, "jpg")));
    }

    @Test
    void jpegConOrientacionExifDistintaDeUno_seProcesaOrientado() throws IOException {
        BufferedImage original = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = original.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 600, 800);
        g.setColor(Color.BLUE);
        g.fillRect(600, 0, 600, 800);
        g.dispose();

        byte[] jpegOrientado = conOrientacionExif(aImagen(original, "jpg"), 6);

        BufferedImage leida = ImageIO.read(new ByteArrayInputStream(procesador().procesar(jpegOrientado)));
        assertNotNull(leida);

        int superior = leida.getRGB(LADO_SALIDA / 2, LADO_SALIDA / 4);
        int inferior = leida.getRGB(LADO_SALIDA / 2, 3 * LADO_SALIDA / 4);
        assertTrue(esRojo(superior), "La mitad superior debería ser roja, salió #" + Integer.toHexString(superior));
        assertTrue(esAzul(inferior), "La mitad inferior debería ser azul, salió #" + Integer.toHexString(inferior));
    }

    @Test
    void entradasCuadradaYMuyApaisada_noSeDeforman() throws IOException {
        BufferedImage cuadrada = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        pintarFondoBlanco(cuadrada);
        dibujarCirculoRojo(cuadrada, 250, 250, 300);

        BufferedImage apaisada = new BufferedImage(1600, 400, BufferedImage.TYPE_INT_RGB);
        pintarFondoBlanco(apaisada);
        dibujarCirculoRojo(apaisada, 650, 50, 300);

        assertNoDeforma(cuadrada);
        assertNoDeforma(apaisada);
    }

    @Test
    void parametroFaltante_lanzaErrorExplicito() throws IOException {
        Map<String, String> incompleta = new HashMap<>(valoresPorDefecto);
        incompleta.remove(CLAVE_IMAGEN_LADO_SALIDA_PX);

        ProcesadorImagen procesadorSinLado = procesadorCon(incompleta);

        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> procesadorSinLado.procesar(jpeg(400, 300, Color.GREEN)));
        assertTrue(excepcion.getMessage().contains(CLAVE_IMAGEN_LADO_SALIDA_PX),
                "El mensaje debe nombrar la clave faltante: " + excepcion.getMessage());
    }

    @Test
    void parametroInvalido_lanzaErrorExplicitoQueNombraLaClave() {
        assertErrorExplicito(CLAVE_IMAGEN_MAX_LADO_PX, "   ");
        assertErrorExplicito(CLAVE_IMAGEN_MAX_LADO_PX, null);
        assertErrorExplicito(CLAVE_IMAGEN_CALIDAD_WEBP, "muyAlta");
        assertErrorExplicito(CLAVE_IMAGEN_CALIDAD_WEBP, "150");
        assertErrorExplicito(CLAVE_IMAGEN_CALIDAD_WEBP, "-1");
    }

    @Test
    void cambiarUnParametroCambiaElComportamiento() throws IOException {
        byte[] jpeg = jpeg(1600, 900, Color.GREEN);

        assertNotNull(procesador().procesar(jpeg));

        Map<String, String> conLimiteBajo = new HashMap<>(valoresPorDefecto);
        conLimiteBajo.put(CLAVE_IMAGEN_MAX_MEGAPIXELES, "1");

        assertThrows(DimensionesImagenExcedidasException.class,
                () -> procesadorCon(conLimiteBajo).procesar(jpeg));
    }

    private ProcesadorImagen procesador() {
        return procesadorCon(new HashMap<>(valoresPorDefecto));
    }

    private void assertErrorExplicito(String clave, String valor) {
        Map<String, String> configurada = new HashMap<>(valoresPorDefecto);
        configurada.put(clave, valor);

        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> procesadorCon(configurada).procesar(jpeg(400, 300, Color.GREEN)));
        assertTrue(excepcion.getMessage().contains(clave),
                "El mensaje debe nombrar la clave '" + clave + "': " + excepcion.getMessage());
    }

    private ProcesadorImagen procesadorCon(Map<String, String> valores) {
        ConfiguracionSistemaRepository repositorio = mock(ConfiguracionSistemaRepository.class);
        when(repositorio.findByClave(anyString())).thenAnswer(invocacion -> {
            String clave = invocacion.getArgument(0);
            String valor = valores.get(clave);
            if (valor == null) {
                return Optional.empty();
            }
            return Optional.of(ConfiguracionSistema.builder().clave(clave).valor(valor).build());
        });
        return new ProcesadorImagen(repositorio);
    }

    private void assertNoDeforma(BufferedImage original) throws IOException {
        BufferedImage leida = ImageIO.read(
                new ByteArrayInputStream(procesador().procesar(aImagen(original, "jpg"))));
        assertNotNull(leida);
        assertEquals(LADO_SALIDA, leida.getWidth());
        assertEquals(LADO_SALIDA, leida.getHeight());

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int y = 0; y < leida.getHeight(); y++) {
            for (int x = 0; x < leida.getWidth(); x++) {
                if (esRojo(leida.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        assertTrue(maxX > minX && maxY > minY, "No se encontró el círculo rojo en la salida");
        double aspecto = (double) (maxX - minX + 1) / (maxY - minY + 1);
        assertTrue(aspecto > 0.8 && aspecto < 1.25,
                "El círculo salió deformado, relación de aspecto = " + aspecto);
    }

    private static byte[] jpeg(int ancho, int alto, Color color) throws IOException {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        rellenar(imagen, color);
        return aImagen(imagen, "jpg");
    }

    private static byte[] aImagen(BufferedImage imagen, String formato) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(imagen, formato, salida), "No hay escritor para el formato " + formato);
        return salida.toByteArray();
    }

    private static byte[] aWebp(BufferedImage imagen) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ImageOutputStream flujo = ImageIO.createImageOutputStream(salida)) {
            assertNotNull(flujo);
            Iterator<ImageWriter> escritores = ImageIO.getImageWritersByMIMEType("image/webp");
            assertTrue(escritores.hasNext(), "No hay escritor WebP disponible");
            ImageWriter escritor = escritores.next();
            escritor.setOutput(flujo);
            escritor.write(new javax.imageio.IIOImage(imagen, null, null));
            escritor.dispose();
        }
        return salida.toByteArray();
    }

    /**
     * Inserta un segmento APP1 EXIF con el tag Orientation en un JPEG generado con ImageIO.
     */
    private static byte[] conOrientacionExif(byte[] jpeg, int orientacion) {
        byte[] cabeceraExif = { 'E', 'x', 'i', 'f', 0, 0 };
        byte[] ifd = {
                // TIFF little-endian: magia 42 e IFD0 en el offset 8
                0x49, 0x49, 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00,
                // una entrada
                0x01, 0x00,
                // tag 0x0112 (Orientation), tipo SHORT, cantidad 1, valor
                0x12, 0x01, 0x03, 0x00, 0x01, 0x00, 0x00, 0x00,
                (byte) (orientacion & 0xFF), 0x00, 0x00, 0x00,
                // sin IFD siguiente
                0x00, 0x00, 0x00, 0x00 };

        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(cabeceraExif, 0, cabeceraExif.length);
        payload.write(ifd, 0, ifd.length);
        byte[] datos = payload.toByteArray();

        ByteArrayOutputStream resultado = new ByteArrayOutputStream();
        resultado.write(jpeg, 0, 2);
        int longitud = datos.length + 2;
        resultado.write(0xFF);
        resultado.write(0xE1);
        resultado.write((longitud >> 8) & 0xFF);
        resultado.write(longitud & 0xFF);
        resultado.write(datos, 0, datos.length);
        resultado.write(jpeg, 2, jpeg.length - 2);
        return resultado.toByteArray();
    }

    private static void rellenar(BufferedImage imagen, Color color) {
        Graphics2D g = imagen.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, imagen.getWidth(), imagen.getHeight());
        g.dispose();
    }

    private static void pintarFondoBlanco(BufferedImage imagen) {
        rellenar(imagen, Color.WHITE);
    }

    private static void dibujarCirculoRojo(BufferedImage imagen, int x, int y, int diametro) {
        Graphics2D g = imagen.createGraphics();
        g.setColor(Color.RED);
        g.fillOval(x, y, diametro, diametro);
        g.dispose();
    }

    private static boolean esRojo(int rgb) {
        return ((rgb >> 16) & 0xFF) > 150 && ((rgb >> 8) & 0xFF) < 100 && (rgb & 0xFF) < 100;
    }

    private static boolean esAzul(int rgb) {
        return (rgb & 0xFF) > 150 && ((rgb >> 16) & 0xFF) < 100 && ((rgb >> 8) & 0xFF) < 100;
    }

    private static String texto(byte[] bytes, int desde, int cantidad) {
        return new String(bytes, desde, cantidad, StandardCharsets.US_ASCII);
    }
}
