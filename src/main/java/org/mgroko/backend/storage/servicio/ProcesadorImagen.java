package org.mgroko.backend.storage.servicio;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.mgroko.backend.repositorio.ConfiguracionSistemaRepository;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.exception.DimensionesImagenExcedidasException;
import org.mgroko.backend.storage.exception.ErrorAlmacenamientoException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
import org.springframework.stereotype.Service;

import com.luciad.imageio.webp.WebPWriteParam;

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
     * <p>
     * Este modo recorta al centro y se usa para fotos de perfil y moodboard.
     *
     * @param bytes bytes originales del archivo subido
     * @return los mismos bytes codificados en WebP
     * @throws ArchivoVacioException               si no hay bytes que procesar
     * @throws DimensionesImagenExcedidasException si supera el lado o los píxeles permitidos
     * @throws FormatoImagenInvalidoException      si el tamaño, los magic bytes o la decodificación fallan
     * @throws IllegalStateException               si falta un parámetro en configuracion_sistema
     */
    public byte[] procesar(byte[] bytes) {
        return procesar(bytes, false);
    }

    /**
     * Procesa una imagen de publicación respetando su relación de aspecto. No
     * recorta: el lado más largo queda limitado por
     * {@code IMAGEN_PUBLICACION_LADO_SALIDA_PX}.
     *
     * @param bytes bytes originales del archivo subido
     * @return los mismos bytes codificados en WebP
     * @throws ArchivoVacioException               si no hay bytes que procesar
     * @throws DimensionesImagenExcedidasException si supera el lado o los píxeles permitidos
     * @throws FormatoImagenInvalidoException      si el tamaño, los magic bytes o la decodificación fallan
     * @throws IllegalStateException               si falta un parámetro en configuracion_sistema
     */
    public byte[] procesarParaPublicacion(byte[] bytes) {
        return procesar(bytes, true);
    }

    private byte[] procesar(byte[] bytes, boolean paraPublicacion) {
        if (bytes == null || bytes.length == 0) {
            throw new ArchivoVacioException("El archivo de imagen no puede estar vacío.");
        }

        ParametrosImagen parametros = leerParametros(paraPublicacion);

        if (bytes.length > parametros.maxTamanoBytes()) {
            throw new FormatoImagenInvalidoException("El archivo supera el tamaño máximo permitido de "
                    + parametros.maxTamanoBytes() + " bytes.");
        }

        validarMagicBytes(bytes);

        Dimensiones dimensiones = validarDimensiones(bytes, parametros);

        int ladoSalida = paraPublicacion
                ? parametros.ladoSalidaPublicacionPx()
                : parametros.ladoSalidaPx();

        boolean aplicarExif = tieneExif(
                bytes,
                parametros.exifHabilitado(),
                parametros.exifScanMaxBytes());

        BufferedImage reescalada = decodificarYReescalar(
                bytes,
                ladoSalida,
                !paraPublicacion,
                aplicarExif,
                dimensiones,
                parametros.subsamplingHabilitado(),
                parametros.subsamplingUmbral());

        return codificarWebP(
                reescalada,
                parametros.calidadWebp(),
                parametros.metodoWebp());
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

    private Dimensiones validarDimensiones(byte[] bytes, ParametrosImagen parametros) {
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

                return new Dimensiones(ancho, alto);
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
    private BufferedImage decodificarYReescalar(
            byte[] bytes,
            int ladoSalidaPx,
            boolean recortarCuadrado,
            boolean aplicarExif,
            Dimensiones dimensiones,
            boolean subsamplingHabilitado,
            int subsamplingUmbral) {

        // El subsampling solo se usa cuando no hay EXIF (para no perder la
        // orientación), está habilitado y la imagen tiene al menos el umbral
        // configurado respecto del tamaño objetivo. El factor se limita para
        // que el resultado nunca quede por debajo del tamaño final.
        if (subsamplingHabilitado && !aplicarExif) {
            int factor = factorSubsampling(dimensiones, ladoSalidaPx, subsamplingUmbral);
            if (factor >= Math.max(2, subsamplingUmbral)) {
                BufferedImage submuestreada = leerConSubsampling(bytes, factor);
                if (submuestreada != null
                        && submuestreada.getWidth() >= ladoSalidaPx
                        && submuestreada.getHeight() >= ladoSalidaPx) {
                    return redimensionar(submuestreada, ladoSalidaPx, recortarCuadrado);
                }
            }
        }

        try {
            var builder = Thumbnails.of(new ByteArrayInputStream(bytes))
                    .useExifOrientation(aplicarExif)
                    .size(ladoSalidaPx, ladoSalidaPx);

            if (recortarCuadrado) {
                builder.crop(Positions.CENTER);
            }

            return builder.asBufferedImage();
        } catch (IOException e) {
            throw new FormatoImagenInvalidoException("No se pudo decodificar el archivo como una imagen válida.");
        }
    }

    private int factorSubsampling(
            Dimensiones dimensiones,
            int ladoSalidaPx,
            int subsamplingUmbral) {
        int menor = Math.min(dimensiones.ancho(), dimensiones.alto());
        if (ladoSalidaPx <= 0 || menor < (long) ladoSalidaPx * subsamplingUmbral) {
            return 1;
        }

        int factor = Math.min(
                dimensiones.ancho() / ladoSalidaPx,
                dimensiones.alto() / ladoSalidaPx);
        return Math.max(1, factor);
    }

    private BufferedImage leerConSubsampling(byte[] bytes, int factor) {
        try (ImageInputStream flujo = ImageIO.createImageInputStream(
                new ByteArrayInputStream(bytes))) {
            if (flujo == null) {
                return null;
            }

            Iterator<ImageReader> lectores = ImageIO.getImageReaders(flujo);
            if (!lectores.hasNext()) {
                return null;
            }

            ImageReader lector = lectores.next();
            try {
                lector.setInput(flujo, true, true);
                ImageReadParam parametrosLectura = lector.getDefaultReadParam();
                parametrosLectura.setSourceSubsampling(factor, factor, 0, 0);
                return lector.read(0, parametrosLectura);
            } finally {
                lector.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private BufferedImage redimensionar(
            BufferedImage imagen,
            int ladoSalidaPx,
            boolean recortarCuadrado) {
        try {
            var builder = Thumbnails.of(imagen).size(ladoSalidaPx, ladoSalidaPx);
            if (recortarCuadrado) {
                builder.crop(Positions.CENTER);
            }
            return builder.asBufferedImage();
        } catch (IOException e) {
            throw new FormatoImagenInvalidoException(
                    "No se pudo redimensionar la imagen.");
        }
    }

    private byte[] codificarWebP(BufferedImage imagen, int calidadWebp, int metodoWebp) {
        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByMIMEType(MIME_SALIDA);
        if (!escritores.hasNext()) {
            throw new ErrorAlmacenamientoException("No hay ningún codificador WebP disponible.", null);
        }

        ImageWriter escritor = escritores.next();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ImageOutputStream salida = new MemoryCacheImageOutputStream(buffer)) {
            escritor.setOutput(salida);

            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionType(COMPRESION_LOSSY);
            parametros.setCompressionQuality(calidadWebp / (float) PORCENTAJE_CALIDAD);

            if (parametros instanceof WebPWriteParam webpParametros) {
                // 0 = más rápido, 6 = mejor compresión. El valor se valida al
                // leerlo desde configuracion_sistema.
                webpParametros.setMethod(Math.max(0, Math.min(6, metodoWebp)));
            }

            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } catch (IOException e) {
            throw new ErrorAlmacenamientoException("Error al codificar la imagen en formato WebP.", e);
        } finally {
            escritor.dispose();
        }

        return buffer.toByteArray();
    }

    private ParametrosImagen leerParametros(boolean paraPublicacion) {
        List<String> claves = new ArrayList<>(List.of(
                ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_LADO_PX,
                ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_MEGAPIXELES,
                ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_TAMANO_BYTES,
                ConfiguracionSistemaService.CLAVE_IMAGEN_LADO_SALIDA_PX,
                ConfiguracionSistemaService.CLAVE_IMAGEN_CALIDAD_WEBP,
                ConfiguracionSistemaService.CLAVE_IMAGEN_SUBSAMPLING_HABILITADO,
                ConfiguracionSistemaService.CLAVE_IMAGEN_SUBSAMPLING_UMBRAL,
                ConfiguracionSistemaService.CLAVE_IMAGEN_EXIF_HABILITADO,
                ConfiguracionSistemaService.CLAVE_IMAGEN_EXIF_SCAN_MAX_BYTES,
                ConfiguracionSistemaService.CLAVE_IMAGEN_WEBP_METHOD));
        if (paraPublicacion) {
            claves.add(ConfiguracionSistemaService.CLAVE_IMAGEN_PUBLICACION_LADO_SALIDA_PX);
        }

        Map<String, String> valores = new HashMap<>();
        for (ConfiguracionSistema configuracion : configuracionSistemaRepository
                .findByClaveIn(claves)) {
            valores.put(configuracion.getClave(), configuracion.getValor());
        }

        int maxLadoPx = leerEnteroPositivo(valores,
                ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_LADO_PX);
        int megapixeles = leerEnteroPositivo(valores,
                ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_MEGAPIXELES);
        int maxTamanoBytes = leerEnteroPositivo(valores,
                ConfiguracionSistemaService.CLAVE_IMAGEN_MAX_TAMANO_BYTES);
        int ladoSalidaPx = leerEnteroPositivo(valores,
                ConfiguracionSistemaService.CLAVE_IMAGEN_LADO_SALIDA_PX);
        int ladoSalidaPublicacionPx = paraPublicacion
                ? leerEnteroPositivo(valores,
                        ConfiguracionSistemaService.CLAVE_IMAGEN_PUBLICACION_LADO_SALIDA_PX)
                : 0;
        int calidadWebp = leerEnteroEnRango(valores,
                ConfiguracionSistemaService.CLAVE_IMAGEN_CALIDAD_WEBP,
                0, PORCENTAJE_CALIDAD);
        boolean subsamplingHabilitado = leerBooleano(valores,
                ConfiguracionSistemaService.CLAVE_IMAGEN_SUBSAMPLING_HABILITADO);
        int subsamplingUmbral = leerEnteroEnRango(valores,
                ConfiguracionSistemaService.CLAVE_IMAGEN_SUBSAMPLING_UMBRAL,
                1, 10);
        boolean exifHabilitado = leerBooleano(valores,
                ConfiguracionSistemaService.CLAVE_IMAGEN_EXIF_HABILITADO);
        int exifScanMaxBytes = leerEnteroEnRango(valores,
                ConfiguracionSistemaService.CLAVE_IMAGEN_EXIF_SCAN_MAX_BYTES,
                1024, 1048576);
        int metodoWebp = leerEnteroEnRango(valores,
                ConfiguracionSistemaService.CLAVE_IMAGEN_WEBP_METHOD,
                0, 6);

        return new ParametrosImagen(
                maxLadoPx,
                megapixeles * PIXELES_POR_MEGAPIXEL,
                maxTamanoBytes,
                ladoSalidaPx,
                ladoSalidaPublicacionPx,
                calidadWebp,
                subsamplingHabilitado,
                subsamplingUmbral,
                exifHabilitado,
                exifScanMaxBytes,
                metodoWebp);
    }

    private int leerEnteroPositivo(Map<String, String> valores, String clave) {
        return leerEnteroEnRango(valores, clave, 1, Integer.MAX_VALUE);
    }

    private int leerEnteroEnRango(
            Map<String, String> valores,
            String clave,
            int minimo,
            int maximo) {
        String valor = valores.get(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Falta el valor de '" + clave + "' en configuracion_sistema.");
        }

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

    private boolean leerBooleano(Map<String, String> valores, String clave) {
        String valor = valores.get(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Falta el valor de '" + clave + "' en configuracion_sistema.");
        }

        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalizado)) {
            return true;
        }
        if ("false".equals(normalizado)) {
            return false;
        }
        throw new IllegalStateException("El valor de '" + clave
                + "' en configuracion_sistema debe ser true o false: " + valor);
    }

    /**
     * Detecta si un JPEG contiene un bloque APP1/EXIF. Evita pagar el costo de
     * {@code useExifOrientation(true)} en PNG, WebP y JPEG sin EXIF.
     */
    private static boolean tieneExif(
            byte[] bytes,
            boolean exifHabilitado,
            int maxBytesEscaneo) {
        if (!exifHabilitado || !empiezaCon(bytes, MAGIC_JPEG)) {
            return false;
        }

        int limite = Math.min(bytes.length - 10, maxBytesEscaneo);
        for (int i = 2; i <= limite; i++) {
            if ((bytes[i] & 0xFF) == 0xFF
                    && (bytes[i + 1] & 0xFF) == 0xE1
                    && bytes[i + 4] == 'E'
                    && bytes[i + 5] == 'x'
                    && bytes[i + 6] == 'i'
                    && bytes[i + 7] == 'f'
                    && bytes[i + 8] == 0
                    && bytes[i + 9] == 0) {
                return true;
            }
        }
        return false;
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

    private record ParametrosImagen(
            int maxLadoPx,
            long maxPixeles,
            int maxTamanoBytes,
            int ladoSalidaPx,
            int ladoSalidaPublicacionPx,
            int calidadWebp,
            boolean subsamplingHabilitado,
            int subsamplingUmbral,
            boolean exifHabilitado,
            int exifScanMaxBytes,
            int metodoWebp) {
    }

    private record Dimensiones(int ancho, int alto) {
    }
}
