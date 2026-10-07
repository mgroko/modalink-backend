package org.mgroko.backend.storage.servicio;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.mgroko.backend.repositorio.ConfiguracionSistemaRepository;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.exception.DimensionesImagenExcedidasException;
import org.mgroko.backend.storage.exception.ErrorAlmacenamientoException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
import org.springframework.stereotype.Service;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

/**
 * Valida, reescala y codifica imágenes en memoria, sin tocar el disco.
 * <p>
 * Todos los límites operativos (tamaño máximo en bytes, lado máximo, megapíxeles
 * máximos, lado de salida y calidad WebP) se leen de {@code configuracion_sistema};
 * si una clave falta o su valor no es válido se lanza un error explícito, nunca se
 * usa un valor por defecto en código.
 * <p>
 * Orden de validación: vacío → tamaño máximo → magic bytes → dimensiones del
 * header → decodificación real.
 */
@Service
public class ProcesadorImagen {

    /** MIME del formato con el que se codifica toda imagen procesada. */
    public static final String MIME_SALIDA = "image/webp";

    /** Extensión de archivo del formato con el que se codifica toda imagen procesada. */
    public static final String EXTENSION_SALIDA = "webp";

    private static final byte[] MAGIC_JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
    private static final byte[] MAGIC_PNG = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final byte[] RIFF = { 'R', 'I', 'F', 'F' };
    private static final byte[] WEBP = { 'W', 'E', 'B', 'P' };

    private static final String COMPRESION_LOSSY = "Lossy";
    private static final int PORCENTAJE_CALIDAD = 100;
    private static final long PIXELES_POR_MEGAPIXEL = 1_000_000L;
    /** Cabecera WEBP mínima: "RIFF" (4) + tamaño (4) + "WEBP" (4) = 12 bytes. */
    private static final int MINIMO_BYTES_WEBP = 12;
    /** Desplazamiento del identificador "WEBP" dentro de la cabecera RIFF. */
    private static final int DESPLAZAMIENTO_WEBP = 8;

    private final ConfiguracionSistemaRepository configuracionSistemaRepository;

    public ProcesadorImagen(ConfiguracionSistemaRepository configuracionSistemaRepository) {
        this.configuracionSistemaRepository = configuracionSistemaRepository;
    }

    /**
     * Valida los bytes recibidos, los reescala al lado configurado (respetando la
     * orientación EXIF) y los devuelve codificados en WebP lossy con la calidad
     * configurada.
     *
     * @param bytes bytes originales del archivo subido
     * @return los mismos bytes codificados en WebP
     * @throws ArchivoVacioException               si no hay bytes que procesar
     * @throws DimensionesImagenExcedidasException si supera el lado o los píxeles permitidos
     * @throws FormatoImagenInvalidoException      si el tamaño, los magic bytes o la decodificación fallan
     * @throws IllegalStateException               si falta un parámetro en configuracion_sistema
     */
    public byte[] procesar(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ArchivoVacioException("El archivo de imagen no puede estar vacío.");
        }

        ParametrosImagen parametros = leerParametros();

        if (bytes.length > parametros.maxTamanoBytes()) {
            throw new FormatoImagenInvalidoException("El archivo supera el tamaño máximo permitido de "
                    + parametros.maxTamanoBytes() + " bytes.");
        }

        validarMagicBytes(bytes);

        validarDimensiones(bytes, parametros);

        BufferedImage reescalada = decodificarYReescalar(bytes, parametros.ladoSalidaPx());

        return codificarWebP(reescalada, parametros.calidadWebp());
    }

    private void validarMagicBytes(byte[] bytes) {
        if (empiezaCon(bytes, MAGIC_JPEG) || empiezaCon(bytes, MAGIC_PNG)) {
            return;
        }
        if (bytes.length >= MINIMO_BYTES_WEBP && empiezaCon(bytes, RIFF)
                && empiezaEn(bytes, WEBP, DESPLAZAMIENTO_WEBP)) {
            return;
        }
        throw new FormatoImagenInvalidoException(
                "El archivo no corresponde a una imagen JPEG, PNG o WEBP válida.");
    }

    private void validarDimensiones(byte[] bytes, ParametrosImagen parametros) {
        try (ImageInputStream flujo = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (flujo == null) {
                throw new FormatoImagenInvalidoException("No se pudo interpretar el archivo como una imagen válida.");
            }

            Iterator<ImageReader> lectores = ImageIO.getImageReaders(flujo);
            if (!lectores.hasNext()) {
                throw new FormatoImagenInvalidoException("No se pudo interpretar el archivo como una imagen válida.");
            }

            ImageReader lector = lectores.next();
            try {
                lector.setInput(flujo);
                int ancho;
                int alto;
                try {
                    ancho = lector.getWidth(0);
                    alto = lector.getHeight(0);
                } catch (IOException e) {
                    throw new FormatoImagenInvalidoException("No se pudieron leer las dimensiones de la imagen.");
                }

                if (ancho <= 0 || alto <= 0) {
                    throw new FormatoImagenInvalidoException("Las dimensiones de la imagen no son válidas.");
                }
                if (ancho > parametros.maxLadoPx() || alto > parametros.maxLadoPx()) {
                    throw new DimensionesImagenExcedidasException("La imagen mide " + ancho + "x" + alto
                            + " px y supera el máximo de " + parametros.maxLadoPx() + " px por lado.");
                }

                long pixeles = (long) ancho * (long) alto;
                if (pixeles > parametros.maxPixeles()) {
                    throw new DimensionesImagenExcedidasException("La imagen tiene " + pixeles
                            + " px y supera el máximo de " + parametros.maxPixeles() + " px.");
                }
            } finally {
                lector.dispose();
            }
        } catch (IOException e) {
            throw new FormatoImagenInvalidoException("No se pudo interpretar el archivo como una imagen válida.");
        }
    }

    /**
     * Decodifica la imagen desde sus bytes originales para que Thumbnailator aplique
     * la orientación EXIF antes de reescalar (si se decodifica antes con ImageIO y se
     * pasa el BufferedImage, la orientación se pierde).
     */
    private BufferedImage decodificarYReescalar(byte[] bytes, int ladoSalidaPx) {
        try {
            return Thumbnails.of(new ByteArrayInputStream(bytes))
                    .useExifOrientation(true)
                    .size(ladoSalidaPx, ladoSalidaPx)
                    .crop(Positions.CENTER)
                    .asBufferedImage();
        } catch (IOException e) {
            throw new FormatoImagenInvalidoException("No se pudo decodificar el archivo como una imagen válida.");
        }
    }

    private byte[] codificarWebP(BufferedImage imagen, int calidadWebp) {
        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByMIMEType(MIME_SALIDA);
        if (!escritores.hasNext()) {
            throw new ErrorAlmacenamientoException("No hay ningún codificador WebP disponible.", null);
        }

        ImageWriter escritor = escritores.next();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ImageOutputStream salida = ImageIO.createImageOutputStream(buffer)) {
            if (salida == null) {
                throw new ErrorAlmacenamientoException("No se pudo crear el flujo de salida de la imagen.", null);
            }
            escritor.setOutput(salida);

            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionType(COMPRESION_LOSSY);
            parametros.setCompressionQuality(calidadWebp / (float) PORCENTAJE_CALIDAD);

            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException("Error al codificar la imagen en formato WebP.", e);
        } finally {
            escritor.dispose();
        }

        return buffer.toByteArray();
    }

    private ParametrosImagen leerParametros() {
        int maxLadoPx = leerEnteroPositivo(ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_LADO_PX);
        int megapixeles = leerEnteroPositivo(ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_MEGAPIXELES);
        int maxTamanoBytes = leerEnteroPositivo(ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_TAMANO_BYTES);
        int ladoSalidaPx = leerEnteroPositivo(ConfiguracionSistemaService.CLAVE_IMAGEN_LADO_SALIDA_PX);
        int calidadWebp = leerEnteroEnRango(ConfiguracionSistemaService.CLAVE_IMAGEN_CALIDAD_WEBP,
                0, PORCENTAJE_CALIDAD);

        return new ParametrosImagen(maxLadoPx, megapixeles * PIXELES_POR_MEGAPIXEL,
                maxTamanoBytes, ladoSalidaPx, calidadWebp);
    }

    private int leerEnteroPositivo(String clave) {
        return leerEnteroEnRango(clave, 1, Integer.MAX_VALUE);
    }

    private int leerEnteroEnRango(String clave, int minimo, int maximo) {
        String valor = configuracionSistemaRepository.findByClave(clave)
                .map(ConfiguracionSistema::getValor)
                .filter(valorObtenido -> !valorObtenido.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el valor de '" + clave + "' en configuracion_sistema."));

        int numero;
        try {
            numero = Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "El valor de '" + clave + "' en configuracion_sistema no es un número entero: " + valor);
        }

        if (numero < minimo || numero > maximo) {
            throw new IllegalStateException("El valor de '" + clave + "' en configuracion_sistema debe estar entre "
                    + minimo + " y " + maximo + ": " + numero);
        }
        return numero;
    }

    private static boolean empiezaCon(byte[] bytes, byte[] prefijo) {
        return empiezaEn(bytes, prefijo, 0);
    }

    private static boolean empiezaEn(byte[] bytes, byte[] patron, int desplazamiento) {
        if (bytes.length < desplazamiento + patron.length) {
            return false;
        }
        for (int i = 0; i < patron.length; i++) {
            if (bytes[desplazamiento + i] != patron[i]) {
                return false;
            }
        }
        return true;
    }

    private record ParametrosImagen(int maxLadoPx, long maxPixeles, int maxTamanoBytes,
            int ladoSalidaPx, int calidadWebp) {
    }
}
