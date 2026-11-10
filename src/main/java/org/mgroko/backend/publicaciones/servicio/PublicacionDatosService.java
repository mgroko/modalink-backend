package org.mgroko.backend.publicaciones.servicio;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.exception.LimiteImagenesPublicacionExcedidoException;
import org.mgroko.backend.publicaciones.exception.ProyectoNoAsociableException;
import org.mgroko.backend.publicaciones.exception.PublicacionInvalidaException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Reglas compartidas entre UC-19 (alta) y UC-21 (modificación) para que ambos
 * flujos validen el título, la descripción, el proyecto, la ubicación y las
 * imágenes de la misma forma.
 */
@Service
public class PublicacionDatosService {

    public static final int LIMITE_IMAGENES = 10;
    public static final String ESTADO_IMAGEN_ACTIVA = "Activa";

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;
    private final UbicacionService ubicacionService;

    public PublicacionDatosService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            ProyectoVisibilidadService proyectoVisibilidadService,
            UbicacionService ubicacionService) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
        this.ubicacionService = ubicacionService;
    }

    public Perfil validarPerfilActivo(Long idUsuario, Long idPerfilActivo) {
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

    public String normalizarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new PublicacionInvalidaException("El título de la publicación es obligatorio.");
        }
        String normalizado = titulo.trim();
        if (normalizado.length() > 150) {
            throw new PublicacionInvalidaException("El título no puede superar los 150 caracteres.");
        }
        return normalizado;
    }

    public String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        String normalizada = descripcion.trim();
        if (normalizada.length() > 1000) {
            throw new PublicacionInvalidaException("La descripción no puede superar los 1000 caracteres.");
        }
        return normalizada;
    }

    public Proyecto resolverProyecto(Long idProyecto, Perfil perfil) {
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

    public Ubicacion resolverUbicacion(String localidadId, String provinciaId) {
        if (localidadId == null || localidadId.isBlank()) {
            return null;
        }
        return ubicacionService.obtenerOCrear(localidadId, provinciaId);
    }

    /**
     * UC-19: el alta exige al menos una imagen.
     */
    public List<MultipartFile> validarArchivosObligatorios(List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            throw new ArchivoVacioException("Debés adjuntar al menos una imagen.");
        }
        if (archivos.size() > LIMITE_IMAGENES) {
            throw new LimiteImagenesPublicacionExcedidoException(LIMITE_IMAGENES);
        }
        validarArchivosNoVacios(archivos);
        return archivos;
    }

    /**
     * UC-21: las imágenes nuevas son opcionales; si vienen, se validan una a
     * una. La cantidad total final se valida en el servicio de modificación.
     */
    public List<MultipartFile> validarArchivosNuevos(List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            return List.of();
        }
        if (archivos.size() > LIMITE_IMAGENES) {
            throw new LimiteImagenesPublicacionExcedidoException(LIMITE_IMAGENES);
        }
        validarArchivosNoVacios(archivos);
        return new ArrayList<>(archivos);
    }

    private void validarArchivosNoVacios(List<MultipartFile> archivos) {
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                throw new ArchivoVacioException("El archivo de imagen no puede estar vacío.");
            }
        }
    }
}
