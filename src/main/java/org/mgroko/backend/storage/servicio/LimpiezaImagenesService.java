package org.mgroko.backend.storage.servicio;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.servicio.EliminarImagenesPerfilService;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Job de reconciliaci&oacute;n de im&aacute;genes: en cada ciclo deja el directorio de
 * fotos y la tabla {@code imagen} &uacute;nicamente con lo que los perfiles que no
 * est&aacute;n dados de baja necesitan.
 * <p>
 * Los pasos son, todos con la gracia de
 * {@code IMAGEN_LIMPIEZA_GRACIA_MINUTOS} salvo donde no aplica:
 * <ol>
 * <li>filas de {@code imagen} sin ninguna referencia (mapeo de la Fase 0:
 * {@code perfil.foto_perfil}, {@code imagen_moodboard},
 * {@code imagen_publicacion} y {@code polaroid_perfil}) y antiguas &rarr; se
 * borra la fila y el archivo;</li>
 * <li>im&aacute;genes a&uacute;n referenciadas por perfiles en {@code BAJA} (borrados
 * anteriores que fallaron) &rarr; se eliminan con el m&eacute;todo dedicado;</li>
 * <li>archivos del directorio de perfiles sin fila en {@code imagen} y
 * antiguos &rarr; se borra el archivo.</li>
 * </ol>
 * Un fallo en un elemento se registra y no aborta el resto; al final se
 * registran los totales.
 */
@Service
public class LimpiezaImagenesService {

    private static final Logger LOG = LoggerFactory.getLogger(LimpiezaImagenesService.class);

    private final ImagenRepository imagenRepository;
    private final PerfilRepository perfilRepository;
    private final StorageService storageService;
    private final EliminarImagenesPerfilService eliminarImagenesPerfilService;
    private final ConfiguracionSistemaService configuracionSistemaService;

    public LimpiezaImagenesService(
            ImagenRepository imagenRepository,
            PerfilRepository perfilRepository,
            StorageService storageService,
            EliminarImagenesPerfilService eliminarImagenesPerfilService,
            ConfiguracionSistemaService configuracionSistemaService) {
        this.imagenRepository = imagenRepository;
        this.perfilRepository = perfilRepository;
        this.storageService = storageService;
        this.eliminarImagenesPerfilService = eliminarImagenesPerfilService;
        this.configuracionSistemaService = configuracionSistemaService;
    }

    /**
     * Ejecuta un ciclo de limpieza.
     *
     * @return totales de lo que se elimin&oacute; y de los fallos
     */
    public Resultado ejecutar() {
        int graciaMinutos = configuracionSistemaService.obtenerGraciaLimpiezaImagenesMinutos();
        LocalDateTime limiteAntiguedad = LocalDateTime.now().minusMinutes(graciaMinutos);

        Conteo huerfanas = borrarImagenesHuerfanas(limiteAntiguedad);
        Conteo perfilesBaja = borrarImagenesDePerfilesEnBaja();
        Conteo archivos = borrarArchivosSinFila(limiteAntiguedad);

        int errores = huerfanas.errores() + perfilesBaja.errores() + archivos.errores();
        int nombresConFila = imagenRepository.listarNombresArchivos().size();

        LOG.info("Limpieza de imágenes finalizada: {} archivo(s) huérfano(s) borrado(s), "
                        + "{} imagen(es) sin referencia, {} imagen(es) de perfil en baja, "
                        + "{} error(es). Quedan {} nombre(s) de archivo con fila.",
                archivos.borrados(), huerfanas.borrados(), perfilesBaja.borrados(),
                errores, nombresConFila);

        return new Resultado(archivos.borrados(), huerfanas.borrados(), perfilesBaja.borrados(), errores);
    }

    private Conteo borrarImagenesHuerfanas(LocalDateTime limiteAntiguedad) {
        int borradas = 0;
        int errores = 0;
        for (Imagen imagen : imagenRepository.buscarHuerfanas(limiteAntiguedad)) {
            try {
                imagenRepository.deleteById(imagen.getIdImagen());
                storageService.eliminarFotoPerfil(imagen.getNombreArchivo());
                borradas++;
            } catch (Exception e) {
                errores++;
                LOG.warn("No se pudo eliminar la imagen {} ({}); se continúa con el resto.",
                        imagen.getIdImagen(), imagen.getNombreArchivo(), e);
            }
        }
        return new Conteo(borradas, errores);
    }

    private Conteo borrarImagenesDePerfilesEnBaja() {
        int borradas = 0;
        int errores = 0;
        for (Long idPerfil : perfilRepository.buscarIdsConFoto(EstadoPerfil.Baja)) {
            try {
                eliminarImagenesPerfilService.eliminarImagenesDePerfil(idPerfil);
                borradas++;
            } catch (Exception e) {
                errores++;
                LOG.warn("No se pudieron eliminar las imágenes del perfil {} en baja; "
                        + "se continúa con el resto.", idPerfil, e);
            }
        }
        return new Conteo(borradas, errores);
    }

    private Conteo borrarArchivosSinFila(LocalDateTime limiteAntiguedad) {
        int borrados = 0;
        int errores = 0;
        Set<String> nombresConFila = new HashSet<>(imagenRepository.listarNombresArchivos());
        Map<String, Instant> fotosEnDisco = storageService.listarFotografiasDePerfil();
        Instant limiteArchivo = limiteAntiguedad.atZone(ZoneId.systemDefault()).toInstant();

        for (Map.Entry<String, Instant> foto : fotosEnDisco.entrySet()) {
            String nombre = foto.getKey();
            if (nombresConFila.contains(nombre) || foto.getValue().isAfter(limiteArchivo)) {
                continue;
            }
            try {
                storageService.eliminarFotoPerfil(nombre);
                borrados++;
            } catch (Exception e) {
                errores++;
                LOG.warn("No se pudo eliminar el archivo huérfano '{}'; se continúa con el resto.", nombre, e);
            }
        }
        return new Conteo(borrados, errores);
    }

    private record Conteo(int borrados, int errores) {
    }

    /**
     * Totales de un ciclo de limpieza.
     *
     * @param archivosHuerfanos    archivos sin fila y fuera de la gracia borrados
     * @param imagenesHuerfanas    filas sin ninguna referencia y fuera de la gracia
     * @param imagenesPerfilesBaja imágenes de perfiles en baja reintentadas
     * @param errores              elementos que no se pudieron eliminar
     */
    public record Resultado(
            int archivosHuerfanos,
            int imagenesHuerfanas,
            int imagenesPerfilesBaja,
            int errores) {
    }
}
