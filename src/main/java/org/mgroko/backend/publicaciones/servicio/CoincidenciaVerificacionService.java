package org.mgroko.backend.publicaciones.servicio;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.DMatchVector;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_features2d.BFMatcher;
import org.bytedeco.opencv.opencv_features2d.ORB;
import org.mgroko.backend.modelo.enums.TipoCoincidenciaColaboracion;
import org.mgroko.backend.storage.servicio.HuellaImagenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_core.NORM_HAMMING;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;

/**
 * Verifica candidatos perceptuales usando ORB y, como fallback, pHash sobre
 * múltiples recortes.
 */
@Service
public class CoincidenciaVerificacionService {

    private static final Logger LOG =
            LoggerFactory.getLogger(CoincidenciaVerificacionService.class);

    private final HuellaImagenService huellaImagenService;
    private final int orbMinKeypoints;
    private final int orbMinMatches;
    private final int orbMaxDistancia;
    private final int orbMaxDistanciaInliers;
    private final int multicropDistanciaMaxima;
    private final int multicropGrid;
    private final double[] multicropEscalas;

    public CoincidenciaVerificacionService(
            HuellaImagenService huellaImagenService,
            @Value("${app.colaboracion-automatica.orb-min-keypoints:50}") int orbMinKeypoints,
            @Value("${app.colaboracion-automatica.orb-min-matches:50}") int orbMinMatches,
            @Value("${app.colaboracion-automatica.orb-max-distancia:60}") int orbMaxDistancia,
            @Value("${app.colaboracion-automatica.orb-max-distancia-inliers:40}") int orbMaxDistanciaInliers,
            @Value("${app.colaboracion-automatica.multicrop-distancia-maxima:8}") int multicropDistanciaMaxima,
            @Value("${app.colaboracion-automatica.multicrop-grid:3}") int multicropGrid,
            @Value("${app.colaboracion-automatica.multicrop-escalas:90,80,70}") String multicropEscalas) {
        this.huellaImagenService = huellaImagenService;
        this.orbMinKeypoints = orbMinKeypoints;
        this.orbMinMatches = orbMinMatches;
        this.orbMaxDistancia = orbMaxDistancia;
        this.orbMaxDistanciaInliers = orbMaxDistanciaInliers;
        this.multicropDistanciaMaxima = multicropDistanciaMaxima;
        this.multicropGrid = Math.max(1, multicropGrid);
        this.multicropEscalas = parseEscalas(multicropEscalas);
    }

    public Resultado verificar(byte[] imagenA, byte[] imagenB, int distanciaPhash) {
        try (Mat matA = decodificar(imagenA); Mat matB = decodificar(imagenB)) {
            if (matA == null || matB == null) {
                return new Resultado(false, null, distanciaPhash, null, 0, 0);
            }

            ResultadoOrb orb = verificarOrb(matA, matB);
            if (orb.aceptada()) {
                return new Resultado(
                        true,
                        TipoCoincidenciaColaboracion.PERCEPTUAL_ORB,
                        distanciaPhash,
                        null,
                        orb.matches(),
                        orb.inliers());
            }

            Integer distanciaMulticrop = distanciaMulticrop(matA, matB);
            if (distanciaMulticrop != null
                    && distanciaMulticrop <= multicropDistanciaMaxima) {
                return new Resultado(
                        true,
                        TipoCoincidenciaColaboracion.PERCEPTUAL_MULTICROP,
                        distanciaPhash,
                        distanciaMulticrop,
                        orb.matches(),
                        orb.inliers());
            }

            return new Resultado(
                    false, null, distanciaPhash, distanciaMulticrop,
                    orb.matches(), orb.inliers());
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo verificar la coincidencia de imágenes.", ex);
            return new Resultado(false, null, distanciaPhash, null, 0, 0);
        }
    }

    private ResultadoOrb verificarOrb(Mat imagenA, Mat imagenB) {
        try (ORB orb = ORB.create();
                KeyPointVector keypointsA = new KeyPointVector();
                KeyPointVector keypointsB = new KeyPointVector();
                Mat descriptorsA = new Mat();
                Mat descriptorsB = new Mat();
                Mat mascara = new Mat()) {

            orb.detectAndCompute(imagenA, mascara, keypointsA, descriptorsA);
            orb.detectAndCompute(imagenB, mascara, keypointsB, descriptorsB);

            if (keypointsA.size() < orbMinKeypoints
                    || keypointsB.size() < orbMinKeypoints) {
                return new ResultadoOrb(false, 0, 0);
            }

            try (BFMatcher matcher = BFMatcher.create(NORM_HAMMING, true);
                    DMatchVector matches = new DMatchVector()) {

                matcher.match(descriptorsA, descriptorsB, matches);

                int matchesBuenos = 0;
                int inliers = 0;
                for (long i = 0; i < matches.size(); i++) {
                    int distancia = (int) matches.get(i).distance();
                    if (distancia <= orbMaxDistancia) {
                        matchesBuenos++;
                    }
                    if (distancia <= orbMaxDistanciaInliers) {
                        inliers++;
                    }
                }

                return new ResultadoOrb(
                        matchesBuenos >= orbMinMatches, matchesBuenos, inliers);
            }
        }
    }

    private Integer distanciaMulticrop(Mat imagenA, Mat imagenB) {
        List<Mat> recortesA = recortes(imagenA);
        List<Mat> recortesB = recortes(imagenB);

        try {
            int minima = Integer.MAX_VALUE;
            for (Mat recorteA : recortesA) {
                Long hashA = huellaImagenService.phash(recorteA);
                if (hashA == null) {
                    continue;
                }
                for (Mat recorteB : recortesB) {
                    Long hashB = huellaImagenService.phash(recorteB);
                    if (hashB == null) {
                        continue;
                    }
                    minima = Math.min(
                            minima, huellaImagenService.distanciaHamming(hashA, hashB));
                }
            }
            return minima == Integer.MAX_VALUE ? null : minima;
        } finally {
            recortesA.forEach(Mat::close);
            recortesB.forEach(Mat::close);
        }
    }

    private List<Mat> recortes(Mat imagen) {
        List<Mat> resultado = new ArrayList<>();
        int ancho = imagen.cols();
        int alto = imagen.rows();

        for (double escala : multicropEscalas) {
            int anchoRecorte = Math.max(1, (int) Math.round(ancho * escala / 100.0));
            int altoRecorte = Math.max(1, (int) Math.round(alto * escala / 100.0));
            int maxX = Math.max(0, ancho - anchoRecorte);
            int maxY = Math.max(0, alto - altoRecorte);
            int pasoX = multicropGrid <= 1 ? 0 : maxX / (multicropGrid - 1);
            int pasoY = multicropGrid <= 1 ? 0 : maxY / (multicropGrid - 1);

            for (int ix = 0; ix < multicropGrid; ix++) {
                for (int iy = 0; iy < multicropGrid; iy++) {
                    int x = Math.min(maxX, ix * pasoX);
                    int y = Math.min(maxY, iy * pasoY);
                    resultado.add(new Mat(imagen, new Rect(x, y, anchoRecorte, altoRecorte)));
                }
            }
        }

        return resultado;
    }

    private Mat decodificar(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try (BytePointer entrada = new BytePointer(bytes);
                Mat buffer = new Mat(1, bytes.length, CV_8UC1, entrada);
                Mat decodificada = imdecode(buffer, IMREAD_COLOR)) {

            if (decodificada.empty()) {
                return null;
            }
            return decodificada.clone();
        }
    }

    private double[] parseEscalas(String valor) {
        if (valor == null || valor.isBlank()) {
            return new double[] { 90, 80, 70 };
        }

        String[] partes = valor.split(",");
        List<Double> escalas = new ArrayList<>();
        for (String parte : partes) {
            try {
                double escala = Double.parseDouble(parte.trim());
                if (escala > 0 && escala <= 100) {
                    escalas.add(escala);
                }
            } catch (NumberFormatException ex) {
                LOG.warn("Escala de multi-recorte inválida: {}", parte);
            }
        }
        return escalas.isEmpty()
                ? new double[] { 90, 80, 70 }
                : escalas.stream().mapToDouble(Double::doubleValue).toArray();
    }

    public record Resultado(
            boolean aceptada,
            TipoCoincidenciaColaboracion tipo,
            Integer distanciaPhash,
            Integer distanciaMulticrop,
            Integer orbMatches,
            Integer orbInliers) {
    }

    private record ResultadoOrb(
            boolean aceptada,
            int matches,
            int inliers) {
    }
}
