package org.mgroko.backend.storage.servicio;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_img_hash.PHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;

/**
 * Calcula huellas de imágenes para detectar duplicados exactos y perceptuales.
 *
 * <p>El SHA-256 se calcula sobre los bytes indicados. El pHash se calcula con
 * OpenCV (módulo {@code img_hash}) y devuelve 8 bytes (64 bits). La distancia
 * entre dos pHash es la distancia de Hamming, entre 0 y 64.</p>
 */
@Service
public class HuellaImagenService {

    private static final Logger LOG = LoggerFactory.getLogger(HuellaImagenService.class);
    private static final int BYTES_PHASH = 8;

    public static final short PHASH_VERSION = 1;

    public String sha256(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible.", ex);
        }
    }

    /**
     * Calcula el pHash de los bytes de una imagen.
     *
     * @return los 64 bits empaquetados en un long, o {@code null} si la imagen
     *         no se pudo decodificar.
     */
    public Long phash(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try (BytePointer entrada = new BytePointer(bytes);
                Mat buffer = new Mat(1, bytes.length, CV_8UC1, entrada);
                Mat imagen = imdecode(buffer, IMREAD_COLOR)) {

            if (imagen.empty()) {
                LOG.warn("No se pudo decodificar la imagen para calcular pHash.");
                return null;
            }

            return phash(imagen);
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo calcular el pHash de la imagen.", ex);
            return null;
        }
    }

    /**
     * Calcula el pHash de una imagen ya decodificada.
     */
    public Long phash(Mat imagen) {
        if (imagen == null || imagen.empty()) {
            return null;
        }

        try (Mat hash = new Mat(); PHash phash = PHash.create()) {
            phash.compute(imagen, hash);

            if (hash.total() != BYTES_PHASH) {
                LOG.warn("OpenCV devolvió un pHash de tamaño inesperado: {}", hash.total());
                return null;
            }

            byte[] resultado = new byte[BYTES_PHASH];
            hash.data().get(resultado);
            return ByteBuffer.wrap(resultado).getLong();
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo calcular el pHash de la imagen.", ex);
            return null;
        }
    }

    public int distanciaHamming(Long hashA, Long hashB) {
        if (hashA == null || hashB == null) {
            return Integer.MAX_VALUE;
        }
        return Long.bitCount(hashA ^ hashB);
    }
}
