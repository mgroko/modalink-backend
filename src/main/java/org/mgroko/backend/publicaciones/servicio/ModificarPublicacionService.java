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
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.OrigenColaboracion;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.ModificarPublicacionRequest;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ResumenInteraccionPublicacion;
import org.mgroko.backend.publicaciones.exception.LimiteImagenesPublicacionExcedidoException;
import org.mgroko.backend.publicaciones.exception.PublicacionInvalidaException;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.mapper.PublicacionMapper;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.storage.servicio.HuellaImagenService;
import org.mgroko.backend.storage.servicio.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;

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
    private final EntityManager entityManager;
    private final PublicacionInteraccionResumenService interaccionResumenService;
    private final DetectarColaboracionAutomaticaService detectorColaboracionService;
    private final ColaboracionAutomaticaLifecycleService colaboracionLifecycleService;
    private final ColaboradorPublicacionOrigenService colaboradorOrigenService;

    public ModificarPublicacionService(
            PublicacionRepository publicacionRepository,
            ImagenRepository imagenRepository,
            PublicacionDatosService publicacionDatosService,
            ProyectoVisibilidadService proyectoVisibilidadService,
            StorageService storageService,
            BorradoArchivosDiferido borradoArchivosDiferido,
            EntityManager entityManager,
            PublicacionInteraccionResumenService interaccionResumenService,
            DetectarColaboracionAutomaticaService detectorColaboracionService,
            ColaboracionAutomaticaLifecycleService colaboracionLifecycleService,
            ColaboradorPublicacionOrigenService colaboradorOrigenService) {
        this.publicacionRepository = publicacionRepository;
        this.imagenRepository = imagenRepository;
        this.publicacionDatosService = publicacionDatosService;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
        this.storageService = storageService;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
        this.entityManager = entityManager;
        this.interaccionResumenService = interaccionResumenService;
        this.detectorColaboracionService = detectorColaboracionService;
        this.colaboracionLifecycleService = colaboracionLifecycleService;
        this.colaboradorOrigenService = colaboradorOrigenService;
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

        if (publicacion.getEstado() != EstadoPublicacion.ACTIVA) {
            throw new PublicacionNoEncontradaException(idPublicacion);
        }

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

        // Se persisten los datos escalares antes de tocar las imágenes.
        publicacion = publicacionRepository.saveAndFlush(publicacion);

        reconciliarImagenes(publicacion, imagenesConservadas, archivosNuevos);

        boolean puedeVerProyecto = publicacion.getProyecto() != null
                && proyectoVisibilidadService.puedeVer(
                        publicacion.getProyecto(), perfil.getIdPerfil());

        ResumenInteraccionPublicacion resumen =
                interaccionResumenService.resumirUna(idPublicacion, perfil.getIdPerfil());

        Map<Long, OrigenColaboracion> origenes = colaboradorOrigenService
                .obtenerOrigenes(List.of(publicacion.getIdPublicacion()))
                .getOrDefault(publicacion.getIdPublicacion(), Map.of());

        return PublicacionMapper.toResponsePropia(
                publicacion, puedeVerProyecto, resumen, origenes);
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

        // Si no cambió el conjunto ni el orden, no hace falta reescribir la
        // tabla de unión.
        if (archivosNuevos.isEmpty() && aEliminar.isEmpty() && actuales.equals(conservadas)) {
            return;
        }

        Long idPublicacion = publicacion.getIdPublicacion();
        int orden = 0;

        // Se actualiza solo la columna orden. Hibernate, en cambio, reordena
        // una lista @OrderColumn actualizando id_imagen por posición, lo que
        // viola la PK si se intercambian dos imágenes.
        for (Imagen imagen : conservadas) {
            publicacionRepository.actualizarOrdenImagenPublicacion(
                    idPublicacion, imagen.getIdImagen(), orden++);
        }

        for (Imagen imagen : aEliminar) {
            colaboracionLifecycleService.cancelarPendientesPorImagen(imagen.getIdImagen());
            publicacionRepository.eliminarImagenPublicacion(
                    idPublicacion, imagen.getIdImagen());
            borradoArchivosDiferido.registrarBorrados(null, imagen.getNombreArchivo());
            imagenRepository.eliminarFisicamente(imagen.getIdImagen());
        }

        List<Imagen> nuevas = new ArrayList<>();
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
                    .sha256Original(almacenado.sha256Original())
                    .sha256Contenido(almacenado.sha256Contenido())
                    .phash(almacenado.phash())
                    .phashVersion(HuellaImagenService.PHASH_VERSION)
                    .build());

            publicacionRepository.insertarImagenPublicacion(
                    idPublicacion, imagen.getIdImagen(), orden++);
            nuevas.add(imagen);
        }

        // Se fuerza el flush de los cambios escalares y de las imágenes
        // eliminadas antes de modificar la colección en memoria.
        entityManager.flush();

        // La colección se actualiza solo para la respuesta. El contexto se
        // limpia sin volver a flushear para que Hibernate no intente reordenar
        // la lista @OrderColumn.
        if (publicacion.getImagenes() == null) {
            publicacion.setImagenes(new ArrayList<>());
        }
        publicacion.getImagenes().clear();
        publicacion.getImagenes().addAll(conservadas);
        publicacion.getImagenes().addAll(nuevas);

        // Inicializa asociaciones perezosas que el mapper necesita después
        // del clear.
        if (publicacion.getColaboradores() != null) {
            publicacion.getColaboradores().size();
        }

        detectorColaboracionService.detectar(publicacion, nuevas);

        entityManager.clear();
    }
}
