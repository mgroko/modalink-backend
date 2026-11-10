package org.mgroko.backend.publicaciones.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.CrearPublicacionRequest;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.exception.LimiteImagenesPublicacionExcedidoException;
import org.mgroko.backend.publicaciones.exception.ProyectoNoAsociableException;
import org.mgroko.backend.publicaciones.exception.PublicacionInvalidaException;
import org.mgroko.backend.publicaciones.mapper.PublicacionMapper;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.storage.servicio.StorageService;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CrearPublicacionService {

    public static final int LIMITE_IMAGENES = 10;
    public static final String ESTADO_IMAGEN_ACTIVA = "Activa";

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final PublicacionRepository publicacionRepository;
    private final ImagenRepository imagenRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;
    private final UbicacionService ubicacionService;
    private final StorageService storageService;
    private final BorradoArchivosDiferido borradoArchivosDiferido;

    public CrearPublicacionService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            PublicacionRepository publicacionRepository,
            ImagenRepository imagenRepository,
            ProyectoVisibilidadService proyectoVisibilidadService,
            UbicacionService ubicacionService,
            StorageService storageService,
            BorradoArchivosDiferido borradoArchivosDiferido) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.publicacionRepository = publicacionRepository;
        this.imagenRepository = imagenRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
        this.ubicacionService = ubicacionService;
        this.storageService = storageService;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
    }

    @Transactional
    public PublicacionResponse crear(Long idUsuario, Long idPerfilActivo, CrearPublicacionRequest request) {
        Perfil perfil = validarPerfil(idUsuario, idPerfilActivo);

        String titulo = normalizarTitulo(request.getTitulo());
        String descripcion = normalizarDescripcion(request.getDescripcion());
        List<MultipartFile> archivos = validarArchivos(request.getArchivos());

        Proyecto proyecto = resolverProyecto(request.getIdProyecto(), perfil);
        Ubicacion ubicacion = resolverUbicacion(request.getLocalidadId(), request.getProvinciaId());

        Publicacion publicacion = Publicacion.builder()
                .titulo(titulo)
                .descripcion(descripcion)
                .fechaPublicacion(LocalDateTime.now())
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .proyecto(proyecto)
                .ubicacion(ubicacion)
                .build();

        publicacion = publicacionRepository.save(publicacion);

        for (MultipartFile archivo : archivos) {
            ArchivoAlmacenado almacenado = storageService.almacenarImagenPublicacion(archivo);

            // Si la transacción no commitea, el archivo nuevo se borra.
            borradoArchivosDiferido.registrarBorrados(almacenado.nombreArchivo(), null);

            Imagen imagen = imagenRepository.save(Imagen.builder()
                    .nombreArchivo(almacenado.nombreArchivo())
                    .url(almacenado.url())
                    .tipoImagen(almacenado.tipoMime())
                    .tamanoBytes((int) almacenado.tamanoBytes())
                    .estado(ESTADO_IMAGEN_ACTIVA)
                    .build());

            publicacion.getImagenes().add(imagen);
        }

        publicacion = publicacionRepository.save(publicacion);

        boolean puedeVerProyecto = proyecto != null
                && proyectoVisibilidadService.puedeVer(proyecto, perfil.getIdPerfil());

        return PublicacionMapper.toResponse(publicacion, puedeVerProyecto);
    }

    private Perfil validarPerfil(Long idUsuario, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        return perfil;
    }

    private String normalizarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new PublicacionInvalidaException("El título de la publicación es obligatorio.");
        }
        String normalizado = titulo.trim();
        if (normalizado.length() > 150) {
            throw new PublicacionInvalidaException("El título no puede superar los 150 caracteres.");
        }
        return normalizado;
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        String normalizada = descripcion.trim();
        if (normalizada.length() > 1000) {
            throw new PublicacionInvalidaException("La descripción no puede superar los 1000 caracteres.");
        }
        return normalizada;
    }

    private List<MultipartFile> validarArchivos(List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            throw new ArchivoVacioException("Debés adjuntar al menos una imagen.");
        }
        if (archivos.size() > LIMITE_IMAGENES) {
            throw new LimiteImagenesPublicacionExcedidoException(LIMITE_IMAGENES);
        }
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                throw new ArchivoVacioException("El archivo de imagen no puede estar vacío.");
            }
        }
        return archivos;
    }

    private Proyecto resolverProyecto(Long idProyecto, Perfil perfil) {
        if (idProyecto == null) {
            return null;
        }

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        if (proyecto.getEstado() != EstadoProyecto.CONFIRMADO
                && proyecto.getEstado() != EstadoProyecto.FINALIZADO) {
            throw new ProyectoNoAsociableException(
                    "Solo se pueden asociar publicaciones a proyectos confirmados o finalizados.");
        }

        if (!proyectoVisibilidadService.esMiembroActivo(idProyecto, perfil.getIdPerfil())) {
            throw new ProyectoNoAsociableException(
                    "El perfil debe ser miembro activo del proyecto para asociar una publicación.");
        }

        return proyecto;
    }

    private Ubicacion resolverUbicacion(String localidadId, String provinciaId) {
        if (localidadId == null || localidadId.isBlank()) {
            return null;
        }
        return ubicacionService.obtenerOCrear(localidadId, provinciaId);
    }
}
