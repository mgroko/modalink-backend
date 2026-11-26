package org.mgroko.backend.proyectos.pdf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.openpdf.text.Image;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resuelve imágenes del moodboard para incrustarlas en el PDF.
 *
 * <p>Las imágenes procesadas se guardan en WebP; OpenPDF no lo soporta de
 * forma nativa, así que se decodifican con ImageIO y se convierten a JPEG en
 * memoria. Solo se aceptan rutas relativas bajo {@code /uploads/}.</p>
 */
@Component
public class PdfImagenResolver {

    private static final String PREFIJO_UPLOADS = "/uploads/";

    private final Path uploadRoot;
    private final int maxLadoPx;

    public PdfImagenResolver(
            @Value("${app.storage.upload-dir:uploads}") String uploadDir,
            @Value("${app.informe-proyecto.max-imagen-px:1200}") int maxLadoPx) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxLadoPx = Math.max(200, maxLadoPx);
    }

    public Optional<Image> resolver(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }

        String limpia = url.trim();
        int query = limpia.indexOf('?');
        if (query >= 0) {
            limpia = limpia.substring(0, query);
        }
        if (!limpia.startsWith(PREFIJO_UPLOADS)) {
            return Optional.empty();
        }

        String relativa = limpia.substring(PREFIJO_UPLOADS.length());
        Path candidato = uploadRoot.resolve(relativa).normalize();
        if (!candidato.startsWith(uploadRoot) || !Files.isRegularFile(candidato)) {
            return Optional.empty();
        }

        try {
            BufferedImage original = ImageIO.read(
                    new ByteArrayInputStream(Files.readAllBytes(candidato)));
            if (original == null || original.getWidth() <= 0 || original.getHeight() <= 0) {
                return Optional.empty();
            }

            BufferedImage jpeg = convertirAJpeg(original);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            if (!ImageIO.write(jpeg, "jpg", salida)) {
                return Optional.empty();
            }
            return Optional.of(Image.getInstance(salida.toByteArray()));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private BufferedImage convertirAJpeg(BufferedImage original) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        double escala = Math.min(1.0, (double) maxLadoPx / Math.max(ancho, alto));
        int anchoDestino = Math.max(1, (int) Math.round(ancho * escala));
        int altoDestino = Math.max(1, (int) Math.round(alto * escala));

        BufferedImage rgb = new BufferedImage(
                anchoDestino,
                altoDestino,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, anchoDestino, altoDestino);
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(original, 0, 0, anchoDestino, altoDestino, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }
}
