package org.mgroko.backend.proyectos.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfImagenResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resuelveWebpDentroDeUploads() throws Exception {
        Path moodboard = tempDir.resolve("moodboard");
        Files.createDirectories(moodboard);
        Path archivo = moodboard.resolve("test.webp");

        BufferedImage imagen = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = imagen.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 32, 24);
        graphics.dispose();

        boolean escrito = ImageIO.write(imagen, "webp", archivo.toFile());
        Assumptions.assumeTrue(escrito, "El plugin WebP no está disponible en este entorno");

        PdfImagenResolver resolver = new PdfImagenResolver(tempDir.toString(), 1200);

        assertThat(resolver.resolver("/uploads/moodboard/test.webp")).isPresent();
    }

    @Test
    void rechazaRutasFueraDeUploadsYArchivosInexistentes() {
        PdfImagenResolver resolver = new PdfImagenResolver(tempDir.toString(), 1200);

        assertThat(resolver.resolver("/uploads/../secreto.webp")).isEmpty();
        assertThat(resolver.resolver("/uploads/moodboard/no-existe.webp")).isEmpty();
        assertThat(resolver.resolver("https://ejemplo.com/imagen.webp")).isEmpty();
        assertThat(resolver.resolver(null)).isEmpty();
    }
}
