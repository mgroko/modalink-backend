package org.mgroko.backend.publicaciones.servicio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.ModificarPublicacionRequest;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.exception.LimiteImagenesPublicacionExcedidoException;
import org.mgroko.backend.publicaciones.exception.PublicacionInvalidaException;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.mapper.PublicacionMapper;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.storage.servicio.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * UC-21: modifica una publicación del perfil activo.
 *
 * <p>La operación es atómica: los datos y las imágenes se actualizan en una
 * sola transacción. {@code imagenesConservadas} define qué imágenes existentes
 * sobreviven y en qué orden; las no incluidas se borran físicamente. Los
 * archivos nuevos se agregan al final y se eliminan si la transacción falla.</p>
 */
@Service
public class ModificarPublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final ImagenRepository imagenRepository;
    private final PublicacionDatosService publicacionDatosService;
    private final ProyectoVisibilidadService proyectoVisibilidadService;
    private final StorageService storageService;
    private final BorradoArchivosDiferido borradoArchivosDiferido;

    public ModificarPublicacionService(
            PublicacionRepository publicacionRepository,
            ImagenRepository imagenRepository,
            PublicacionDatosService publicacionDatosService,
            ProyectoVisibilidadService proyectoVisibilidadService,
            StorageService storageService,
            BorradoArchivosDiferido borradoArchivosDiferido) {
        this.publicacionRepository = publicacionRepository;
        this.imagenRepository = imagenRepository;
        this.publicacionDatosService = publicacionDatosService;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
        this.storageService = storageService;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
    }

    @Transactional
    public PublicacionResponse modificar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPublicacion,
            ModificarPublicacionRequest request) {

        Perfil perfil = publicacionDatosService.validarPerfilActivo(idUsuario, idPerfilActivo);

        Publicacion publicacion = publicacionRepository
                .findByIdPublicacionAndPerfilIdPerfil(idPublicacion, perfil.getIdPerfil())
                .orElseThrow(() -> new PublicacionNoEncontradaException(idPublicacion));

        String titulo = publicacionDatosService.normalizarTitulo(request.getTitulo());
        String descripcion = publicacionDatosService.normalizarDescripcion(request.getDescripcion());
        List<MultipartFile> archivosNuevos = publicacionDatosService.validarArchivosNuevos(
                request.getArchivos());
        List<Long> imagenesConservadas = validarImagenesConservadas(request.getImagenesConservadas());

        validarCantidadFinalDeImagenes(imagenesConservadas.size(), archivosNuevos.size());

        publicacion.setTitulo(titulo);
        publicacion.setDescripcion(descripcion);
        actualizarProyecto(publicacion, request.getIdProyecto(), perfil);
        publicacion.setUbicacion(publicacionDatosService.resolverUbicacion(
                request.getLocalidadId(), request.getProvinciaId()));

        reconciliarImagenes(publicacion, imagenesConservadas, archivosNuevos);

        Publicacion guardada = publicacionRepository.save(publicacion);

        boolean puedeVerProyecto = guardada.getProyecto() != null
                && proyectoVisibilidadService.puedeVer(
                        guardada.getProyecto(), perfil.getIdPerfil());

        return PublicacionMapper.toResponsePropia(guardada, puedeVerProyecto);
    }

    private void actualizarProyecto(Publicacion publicacion, Long idProyecto, Perfil perfil) {
        Long proyectoActual = publicacion.getProyecto() != null
                ? publicacion.getProyecto().getIdProyecto()
                : null;

        // Si el proyecto no cambia, se conserva sin revalidar membresía ni
        // estado: la publicación puede ser histórica y el autor ya no ser
        // miembro del proyecto.
        if (Objects.equals(proyectoActual, idProyecto)) {
            return;
        }

        if (idProyecto == null) {
            publicacion.setProyecto(null);
            return;
        }

        Proyecto proyecto = publicacionDatosService.resolverProyecto(idProyecto, perfil);
        publicacion.setProyecto(proyecto);
    }

    private List<Long> validarImagenesConservadas(List<Long> ids) {
        if (ids == null) {
            throw new PublicacionInvalidaException(
                    "Debés indicar las imágenes que se conservan.");
        }

        Set<Long> unicas = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null) {
                throw new PublicacionInvalidaException("La imagen indicada no es válida.");
            }
            unicas.add(id);
        }
        return new ArrayList<>(unicas);
    }

    private void validarCantidadFinalDeImagenes(int conservadas, int nuevas) {
        int total = conservadas + nuevas;
        if (total < 1) {
            throw new PublicacionInvalidaException(
                    "Debés conservar o adjuntar al menos una imagen.");
        }
        if (total > PublicacionDatosService.LIMITE_IMAGENES) {
            throw new LimiteImagenesPublicacionExcedidoException(
                    PublicacionDatosService.LIMITE_IMAGENES);
        }
    }

    private void reconciliarImagenes(
            Publicacion publicacion,
            List<Long> idsConservadas,
            List<MultipartFile> archivosNuevos) {

        List<Imagen> actuales = publicacion.getImagenes() != null
                ? new ArrayList<>(publicacion.getImagenes())
                : new ArrayList<>();

        Map<Long, Imagen> porId = new HashMap<>();
        for (Imagen imagen : actuales) {
            porId.put(imagen.getIdImagen(), imagen);
        }

        List<Imagen> conservadas = new ArrayList<>();
        for (Long id : idsConservadas) {
            Imagen imagen = porId.get(id);
            if (imagen == null) {
                throw new PublicacionInvalidaException(
                        "La imagen indicada no pertenece a la publicación.");
            }
            conservadas.add(imagen);
        }

        Set<Long> idsAConservar = new LinkedHashSet<>(idsConservadas);
        List<Imagen> aEliminar = actuales.stream()
                .filter(imagen -> !idsAConservar.contains(imagen.getIdImagen()))
                .toList();

        // Deja las conservadas en el orden solicitado y persiste la
        // desasociación antes de borrar las filas de imagen.
        if (publicacion.getImagenes() == null) {
            publicacion.setImagenes(new ArrayList<>());
        }
        publicacion.getImagenes().clear();
        publicacion.getImagenes().addAll(conservadas);
        publicacionRepository.saveAndFlush(publicacion);

        for (Imagen imagen : aEliminar) {
            borradoArchivosDiferido.registrarBorrados(null, imagen.getNombreArchivo());
            imagenRepository.delete(imagen);
        }

        for (MultipartFile archivo : archivosNuevos) {
            ArchivoAlmacenado almacenado = storageService.almacenarImagenPublicacion(archivo);

            // Si la transacción no commitea, el archivo nuevo se borra.
            borradoArchivosDiferido.registrarBorrados(almacenado.nombreArchivo(), null);

            Imagen imagen = imagenRepository.save(Imagen.builder()
                    .nombreArchivo(almacenado.nombreArchivo())
                    .url(almacenado.url())
                    .tipoImagen(almacenado.tipoMime())
                    .tamanoBytes((int) almacenado.tamanoBytes())
                    .estado(PublicacionDatosService.ESTADO_IMAGEN_ACTIVA)
                    .build());

            publicacion.getImagenes().add(imagen);
        }
    }
}
