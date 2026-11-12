package org.mgroko.backend.publicaciones.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.CrearPublicacionRequest;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
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

@Service
public class CrearPublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final ImagenRepository imagenRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;
    private final PublicacionDatosService publicacionDatosService;
    private final StorageService storageService;
    private final BorradoArchivosDiferido borradoArchivosDiferido;
    private final DetectarColaboracionAutomaticaService detectorColaboracionService;

    public CrearPublicacionService(
            PublicacionRepository publicacionRepository,
            ImagenRepository imagenRepository,
            ProyectoVisibilidadService proyectoVisibilidadService,
            PublicacionDatosService publicacionDatosService,
            StorageService storageService,
            BorradoArchivosDiferido borradoArchivosDiferido,
            DetectarColaboracionAutomaticaService detectorColaboracionService) {
        this.publicacionRepository = publicacionRepository;
        this.imagenRepository = imagenRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
        this.publicacionDatosService = publicacionDatosService;
        this.storageService = storageService;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
        this.detectorColaboracionService = detectorColaboracionService;
    }

    @Transactional
    public PublicacionResponse crear(Long idUsuario, Long idPerfilActivo, CrearPublicacionRequest request) {
        Perfil perfil = publicacionDatosService.validarPerfilActivo(idUsuario, idPerfilActivo);

        String titulo = publicacionDatosService.normalizarTitulo(request.getTitulo());
        String descripcion = publicacionDatosService.normalizarDescripcion(request.getDescripcion());
        List<MultipartFile> archivos = publicacionDatosService.validarArchivosObligatorios(request.getArchivos());

        Proyecto proyecto = publicacionDatosService.resolverProyecto(request.getIdProyecto(), perfil);
        Ubicacion ubicacion = publicacionDatosService.resolverUbicacion(
                request.getLocalidadId(), request.getProvinciaId());

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
                    .estado(PublicacionDatosService.ESTADO_IMAGEN_ACTIVA)
                    .sha256Original(almacenado.sha256Original())
                    .sha256Contenido(almacenado.sha256Contenido())
                    .phash(almacenado.phash())
                    .phashVersion(HuellaImagenService.PHASH_VERSION)
                    .build());

            publicacion.getImagenes().add(imagen);
        }

        publicacion = publicacionRepository.save(publicacion);

        detectorColaboracionService.detectar(
                publicacion, new java.util.ArrayList<>(publicacion.getImagenes()));

        boolean puedeVerProyecto = proyecto != null
                && proyectoVisibilidadService.puedeVer(proyecto, perfil.getIdPerfil());

        return PublicacionMapper.toResponse(publicacion, puedeVerProyecto);
    }
}
