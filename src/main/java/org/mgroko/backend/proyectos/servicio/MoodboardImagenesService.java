package org.mgroko.backend.proyectos.servicio;

import java.util.List;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.exception.LimiteImagenesMoodboardExcedidoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.mapper.ProyectoMapper;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.storage.servicio.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * UC-24: subida de imágenes del moodboard una vez creado el proyecto
 * (flujo del wizard: el front llama a POST /proyectos y, enseguida, a este endpoint).
 */
@Service
public class MoodboardImagenesService {

    public static final int LIMITE_IMAGENES = 10;
    public static final String PERMISO_MODIFICAR_PROYECTO = "MODIFICAR_PROYECTO";
    public static final String ESTADO_IMAGEN_ACTIVA = "Activa";

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final MoodboardRepository moodboardRepository;
    private final ImagenRepository imagenRepository;
    private final StorageService storageService;
    private final BorradoArchivosDiferido borradoArchivosDiferido;
    private final ProyectoSecurityService proyectoSecurityService;

    public MoodboardImagenesService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            MoodboardRepository moodboardRepository,
            ImagenRepository imagenRepository,
            StorageService storageService,
            BorradoArchivosDiferido borradoArchivosDiferido,
            ProyectoSecurityService proyectoSecurityService) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.moodboardRepository = moodboardRepository;
        this.imagenRepository = imagenRepository;
        this.storageService = storageService;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
        this.proyectoSecurityService = proyectoSecurityService;
    }

    /**
     * Sube de 1 a {@value #LIMITE_IMAGENES} imágenes al moodboard del proyecto.
     * Crea el moodboard si aún no existe (el proyecto puede haberse creado sin él).
     */
    @Transactional
    public MoodboardResponse subirImagenes(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            List<MultipartFile> archivos) {

        validarPerfilActivo(idUsuario, idPerfilActivo);

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(idProyecto, idPerfilActivo, PERMISO_MODIFICAR_PROYECTO);

        if (archivos == null || archivos.isEmpty()) {
            throw new ArchivoVacioException("Debes adjuntar al menos una imagen para el moodboard.");
        }

        Moodboard moodboard = moodboardRepository.findByProyectoIdProyecto(idProyecto)
                .orElseGet(() -> moodboardRepository.save(
                        Moodboard.builder().proyecto(proyecto).build()));

        int imagenesExistentes = moodboard.getImagenes() != null ? moodboard.getImagenes().size() : 0;
        if (imagenesExistentes + archivos.size() > LIMITE_IMAGENES) {
            throw new LimiteImagenesMoodboardExcedidoException(LIMITE_IMAGENES);
        }

        for (MultipartFile archivo : archivos) {
            ArchivoAlmacenado almacenado = storageService.almacenarImagenMoodboard(archivo);

            // El archivo se borra solo si la transacción no llega a commit
            // (mismo patrón que FotoPerfilService).
            borradoArchivosDiferido.registrarBorrados(almacenado.nombreArchivo(), null);

            Imagen imagen = imagenRepository.save(Imagen.builder()
                    .nombreArchivo(almacenado.nombreArchivo())
                    .url(almacenado.url())
                    .tipoImagen(almacenado.tipoMime())
                    .tamanoBytes((int) almacenado.tamanoBytes())
                    .estado(ESTADO_IMAGEN_ACTIVA)
                    .build());

            moodboard.getImagenes().add(imagen);
        }

        Moodboard guardado = moodboardRepository.save(moodboard);
        return ProyectoMapper.toMoodboardResponse(guardado);
    }

    private void validarPerfilActivo(Long idUsuario, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }
    }
}
