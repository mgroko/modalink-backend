package org.mgroko.backend.publicaciones.servicio;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ejecuta la detección automática de colaboraciones en un hilo aparte, después
 * de que la transacción de subida confirma.
 *
 * <p>Recarga la publicación y las imágenes por identificador para no depender
 * de entidades JPA de otra transacción ni de asociaciones perezosas.</p>
 */
@Service
public class DetectarColaboracionAutomaticaAsyncService {

    private static final Logger LOG =
            LoggerFactory.getLogger(DetectarColaboracionAutomaticaAsyncService.class);

    private final PublicacionRepository publicacionRepository;
    private final ImagenRepository imagenRepository;
    private final DetectarColaboracionAutomaticaService detectorService;

    public DetectarColaboracionAutomaticaAsyncService(
            PublicacionRepository publicacionRepository,
            ImagenRepository imagenRepository,
            DetectarColaboracionAutomaticaService detectorService) {
        this.publicacionRepository = publicacionRepository;
        this.imagenRepository = imagenRepository;
        this.detectorService = detectorService;
    }

    @Async("colaboracionAutomaticaExecutor")
    @Transactional
    public void procesar(Long idPublicacion, List<Long> idsImagenes) {
        if (idPublicacion == null || idsImagenes == null || idsImagenes.isEmpty()) {
            return;
        }

        try {
            Publicacion publicacion = publicacionRepository.findById(idPublicacion)
                    .orElse(null);
            if (publicacion == null) {
                LOG.debug("La publicación {} ya no existe; se omite la detección.",
                        idPublicacion);
                return;
            }

            Set<Long> idsDeLaPublicacion = publicacion.getImagenes().stream()
                    .map(Imagen::getIdImagen)
                    .collect(Collectors.toSet());

            List<Imagen> imagenes = imagenRepository.findAllById(idsImagenes).stream()
                    .filter(imagen -> idsDeLaPublicacion.contains(imagen.getIdImagen()))
                    .toList();
            if (imagenes.isEmpty()) {
                LOG.debug("No quedan imágenes de la publicación {}; se omite la detección.",
                        idPublicacion);
                return;
            }

            detectorService.detectar(publicacion, imagenes);
        } catch (RuntimeException ex) {
            // La subida ya confirmó: un fallo de detección no debe propagarse.
            LOG.warn("No se pudo ejecutar la detección automática para la publicación {}.",
                    idPublicacion, ex);
        }
    }
}
