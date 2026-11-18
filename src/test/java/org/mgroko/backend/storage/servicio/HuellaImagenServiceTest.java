package org.mgroko.backend.storage.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class HuellaImagenServiceTest {

    private final HuellaImagenService servicio = new HuellaImagenService();

    @Test
    void sha256_esDeterminista() {
        byte[] bytes = "modalink".getBytes();

        assertEquals(servicio.sha256(bytes), servicio.sha256(bytes));
        assertEquals(64, servicio.sha256(bytes).length());
    }

    @Test
    void phash_mismaImagen_distanciaCero() throws Exception {
        byte[] imagen = imagen(new Color(20, 80, 160), new Color(240, 180, 60));

        Long hashA = servicio.phash(imagen);
        Long hashB = servicio.phash(imagen);

        assertNotNull(hashA);
        assertEquals(0, servicio.distanciaHamming(hashA, hashB));
    }

    @Test
    void phash_imagenesDistintas_distanciaMayorACero() throws Exception {
        byte[] imagenA = imagen(new Color(20, 80, 160), new Color(240, 180, 60));
        byte[] imagenB = imagen(new Color(0, 0, 0), new Color(255, 255, 255));

        int distancia = servicio.distanciaHamming(
                servicio.phash(imagenA), servicio.phash(imagenB));

        assertTrue(distancia > 0);
    }

    private byte[] imagen(Color desde, Color hasta) throws Exception {
        BufferedImage imagen = new BufferedImage(160, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setPaint(new java.awt.GradientPaint(0, 0, desde, 160, 120, hasta));
        g.fillRect(0, 0, 160, 120);
        g.setColor(Color.WHITE);
        g.fillOval(30, 20, 80, 60);
        g.dispose();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }
}
