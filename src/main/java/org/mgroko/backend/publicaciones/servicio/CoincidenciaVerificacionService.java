package org.mgroko.backend.publicaciones.servicio;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.opencv_core.DMatchVector;
import org.bytedeco.opencv.opencv_core.DMatchVectorVector;
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

import static org.bytedeco.opencv.global.opencv_calib3d.RANSAC;
import static org.bytedeco.opencv.global.opencv_calib3d.findHomography;
import static org.bytedeco.opencv.global.opencv_core.CV_32FC2;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_core.NORM_HAMMING;
import static org.bytedeco.opencv.global.opencv_core.countNonZero;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;

/**
 * Verifica candidatos perceptuales usando ORB con ratio test y validación
 * geométrica por homografía (RANSAC), y pHash sobre múltiples recortes como
 * fallback.
 */
@Service
public class CoincidenciaVerificacionService {

    private static final Logger LOG =
            LoggerFactory.getLogger(CoincidenciaVerificacionService.class);

    private static final float RATIO_TEST = 0.75f;
    private static final double RANSAC_REPROYECCION = 3.0;
    private static final int RANSAC_ITERACIONES = 2000;
    private static final double RANSAC_CONFIANZA = 0.995;

    private final HuellaImagenService huellaImagenService;
    private final int orbMinKeypoints;
    private final int orbMinMatches;
    private final int orbMaxDistancia;
    private final int orbMinInliers;
    private final int multicropDistanciaMaxima;
    private final int multicropGrid;
    private final double[] multicropEscalas;

    public CoincidenciaVerificacionService(
            HuellaImagenService huellaImagenService,
            @Value("${app.colaboracion-automatica.orb-min-keypoints:50}") int orbMinKeypoints,
            @Value("${app.colaboracion-automatica.orb-min-matches:50}") int orbMinMatches,
            @Value("${app.colaboracion-automatica.orb-max-distancia:60}") int orbMaxDistancia,
            @Value("${app.colaboracion-automatica.orb-min-inliers:10}") int orbMinInliers,
            @Value("${app.colaboracion-automatica.multicrop-distancia-maxima:8}") int multicropDistanciaMaxima,
            @Value("${app.colaboracion-automatica.multicrop-grid:3}") int multicropGrid,
            @Value("${app.colaboracion-automatica.multicrop-escalas:90,80,70}") String multicropEscalas) {
        this.huellaImagenService = huellaImagenService;
        this.orbMinKeypoints = orbMinKeypoints;
        this.orbMinMatches = orbMinMatches;
        this.orbMaxDistancia = orbMaxDistancia;
        this.orbMinInliers = orbMinInliers;
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

            int matchesBuenos = contarMatchesBuenos(descriptorsA, descriptorsB);
            if (matchesBuenos < orbMinMatches) {
                return new ResultadoOrb(false, matchesBuenos, 0);
            }

            List<int[]> paresRatio = matchesConRatio(descriptorsA, descriptorsB);
            int inliers = contarInliersRansac(keypointsA, keypointsB, paresRatio);

            return new ResultadoOrb(
                    inliers >= orbMinInliers,
                    matchesBuenos,
                    inliers);
        }
    }

    /**
     * Pre-filtro de rendimiento: matches cruzados con distancia Hamming
     * aceptable. No alcanza para aceptar la coincidencia; después se exige
     * consistencia geométrica.
     */
    private int contarMatchesBuenos(Mat descriptorsA, Mat descriptorsB) {
        try (BFMatcher matcher = BFMatcher.create(NORM_HAMMING, true);
                DMatchVector matches = new DMatchVector()) {

            matcher.match(descriptorsA, descriptorsB, matches);

            int matchesBuenos = 0;
            for (long i = 0; i < matches.size(); i++) {
                if ((int) matches.get(i).distance() <= orbMaxDistancia) {
                    matchesBuenos++;
                }
            }
            return matchesBuenos;
        }
    }

    /**
     * Ratio test de Lowe: conserva el mejor match solo si es claramente mejor
     * que el segundo. Reduce outliers antes de RANSAC y lo hace barato.
     */
    private List<int[]> matchesConRatio(Mat descriptorsA, Mat descriptorsB) {
        List<int[]> pares = new ArrayList<>();

        try (BFMatcher matcher = BFMatcher.create(NORM_HAMMING, false);
                DMatchVectorVector knn = new DMatchVectorVector()) {

            matcher.knnMatch(descriptorsA, descriptorsB, knn, 2);

            for (long i = 0; i < knn.size(); i++) {
                DMatchVector vecinos = knn.get(i);
                if (vecinos.size() < 2) {
                    continue;
                }

                var mejor = vecinos.get(0);
                var segundo = vecinos.get(1);

                if (mejor.distance() <= orbMaxDistancia
                        && mejor.distance() < RATIO_TEST * segundo.distance()) {
                    pares.add(new int[] {
                            mejor.queryIdx(), mejor.trainIdx() });
                }
            }
        }

        return pares;
    }

    /**
     * Cuenta inliers geométricos reales: puntos que la homografía estimada por
     * RANSAC puede explicar.
     */
    private int contarInliersRansac(
            KeyPointVector keypointsA,
            KeyPointVector keypointsB,
            List<int[]> pares) {

        if (pares.size() < 4) {
            return 0;
        }

        try (Mat origen = new Mat(pares.size(), 1, CV_32FC2);
                Mat destino = new Mat(pares.size(), 1, CV_32FC2);
                Mat mascara = new Mat()) {

            FloatIndexer idxOrigen = origen.createIndexer();
            FloatIndexer idxDestino = destino.createIndexer();

            for (int i = 0; i < pares.size(); i++) {
                int[] par = pares.get(i);
                var puntoOrigen = keypointsA.get(par[0]).pt();
                var puntoDestino = keypointsB.get(par[1]).pt();
                idxOrigen.put(i, 0, puntoOrigen.x(), puntoOrigen.y());
                idxDestino.put(i, 0, puntoDestino.x(), puntoDestino.y());
            }

            idxOrigen.release();
            idxDestino.release();

            try (Mat homografia = findHomography(
                    origen,
                    destino,
                    RANSAC,
                    RANSAC_REPROYECCION,
                    mascara,
                    RANSAC_ITERACIONES,
                    RANSAC_CONFIANZA)) {

                if (homografia == null || homografia.empty()) {
                    return 0;
                }
                return countNonZero(mascara);
            }
        }
    }

    private Integer distanciaMulticrop(Mat imagenA, Mat imagenB) {
        List<Mat> recortesA = recortes(imagenA);
        List<Mat> recortesB = recortes(imagenB);

        try {
            List<Long> hashesA = hashes(recortesA);
            List<Long> hashesB = hashes(recortesB);

            int minima = Integer.MAX_VALUE;
            for (Long hashA : hashesA) {
                if (hashA == null) {
                    continue;
                }
                for (Long hashB : hashesB) {
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

    private List<Long> hashes(List<Mat> recortes) {
        List<Long> hashes = new ArrayList<>(recortes.size());
        for (Mat recorte : recortes) {
            hashes.add(huellaImagenService.phash(recorte));
        }
        return hashes;
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
