package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.enums.TipoCoincidenciaColaboracion;
import org.mgroko.backend.storage.servicio.HuellaImagenService;

class CoincidenciaVerificacionServiceTest {

    private HuellaImagenService huella;
    private CoincidenciaVerificacionService servicio;

    @BeforeEach
    void setUp() {
        huella = new HuellaImagenService();
        servicio = new CoincidenciaVerificacionService(
                huella,
                50,
                50,
                60,
                40,
                8,
                3,
                "90,80,70");
    }

    @Test
    void verificar_mismaImagen_aceptaPorOrb() throws Exception {
        byte[] imagen = imagen(320, 240, 1);

        CoincidenciaVerificacionService.Resultado resultado =
                servicio.verificar(imagen, imagen, 0);

        assertTrue(resultado.aceptada());
        assertEquals(TipoCoincidenciaColaboracion.PERCEPTUAL_ORB, resultado.tipo());
    }

    @Test
    void verificar_imagenRecortada_acepta() throws Exception {
        byte[] original = imagen(320, 240, 1);
        byte[] recortada = recorte(original, 20, 15, 280, 210);

        CoincidenciaVerificacionService.Resultado resultado =
                servicio.verificar(original, recortada, 17);

        assertTrue(resultado.aceptada());
    }

    @Test
    void verificar_imagenesDistintas_rechaza() throws Exception {
        byte[] a = imagenRuido(320, 240, 1);
        byte[] b = imagenRuido(320, 240, 2);

        CoincidenciaVerificacionService.Resultado resultado =
                servicio.verificar(a, b, 20);

        assertFalse(resultado.aceptada());
    }

    @Test
    void verificar_sinKeypointsUsaMultiRecorte() throws Exception {
        CoincidenciaVerificacionService soloMulticrop =
                new CoincidenciaVerificacionService(
                        huella, 100000, 50, 60, 40, 8, 3, "90,80,70");
        byte[] imagen = imagen(320, 240, 1);

        CoincidenciaVerificacionService.Resultado resultado =
                soloMulticrop.verificar(imagen, imagen, 17);

        assertTrue(resultado.aceptada());
        assertEquals(
                TipoCoincidenciaColaboracion.PERCEPTUAL_MULTICROP,
                resultado.tipo());
    }

    private byte[] imagen(int ancho, int alto, int semilla) throws Exception {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(new Color(20, 60, 120));
        g.fillRect(0, 0, ancho, alto);
        for (int i = 0; i < 80; i++) {
            int x = (i * 97 + semilla * 31) % (ancho - 80);
            int y = (i * 53 + semilla * 17) % (alto - 80);
            g.setColor(new Color(
                    (i * 37 + semilla * 11) % 255,
                    (i * 71 + semilla * 23) % 255,
                    (i * 19 + semilla * 43) % 255));
            g.fillRect(x, y, 60 + (i % 20), 50 + (i % 30));
        }
        g.setColor(Color.WHITE);
        g.drawString("MODALINK " + semilla, 40, 60);
        g.dispose();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private byte[] imagenRuido(int ancho, int alto, long semilla) throws Exception {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        java.util.Random random = new java.util.Random(semilla);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                imagen.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private byte[] recorte(byte[] original, int x, int y, int ancho, int alto)
            throws Exception {
        BufferedImage imagen = ImageIO.read(new java.io.ByteArrayInputStream(original));
        BufferedImage recorte = imagen.getSubimage(x, y, ancho, alto);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(recorte, "png", salida);
        return salida.toByteArray();
    }
}
