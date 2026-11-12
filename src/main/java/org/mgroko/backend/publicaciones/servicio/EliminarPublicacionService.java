package org.mgroko.backend.publicaciones.servicio;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * UC-20 - Baja de publicación.
 *
 * <p>El dueño elimina la publicación de forma física y desaparece para todos
 * los perfiles. Un colaborador solo se quita la etiqueta de su propio perfil:
 * la publicación, sus imágenes, archivos, solicitudes y notificaciones se
 * conservan.</p>
 */
@Service
public class EliminarPublicacionService {

    private final PublicacionDatosService publicacionDatosService;
    private final PublicacionRepository publicacionRepository;
    private final ImagenRepository imagenRepository;
    private final SolicitudColaboracionRepository solicitudColaboracionRepository;
    private final NotificacionService notificacionService;
    private final BorradoArchivosDiferido borradoArchivosDiferido;
    private final ColaboracionAutomaticaLifecycleService colaboracionLifecycleService;
    private final ColaboradorPublicacionOrigenService colaboradorOrigenService;
    private final EntityManager entityManager;

    public EliminarPublicacionService(
            PublicacionDatosService publicacionDatosService,
            PublicacionRepository publicacionRepository,
            ImagenRepository imagenRepository,
            SolicitudColaboracionRepository solicitudColaboracionRepository,
            NotificacionService notificacionService,
            BorradoArchivosDiferido borradoArchivosDiferido,
            ColaboracionAutomaticaLifecycleService colaboracionLifecycleService,
            ColaboradorPublicacionOrigenService colaboradorOrigenService,
            EntityManager entityManager) {
        this.publicacionDatosService = publicacionDatosService;
        this.publicacionRepository = publicacionRepository;
        this.imagenRepository = imagenRepository;
        this.solicitudColaboracionRepository = solicitudColaboracionRepository;
        this.notificacionService = notificacionService;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
        this.colaboracionLifecycleService = colaboracionLifecycleService;
        this.colaboradorOrigenService = colaboradorOrigenService;
        this.entityManager = entityManager;
    }

    @Transactional
    public void eliminar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPublicacion) {

        Perfil perfil = publicacionDatosService.validarPerfilActivo(
                idUsuario, idPerfilActivo);

        Publicacion publicacion = publicacionRepository
                .findByIdPublicacionForUpdate(idPublicacion)
                .orElseThrow(() -> new PublicacionNoEncontradaException(idPublicacion));

        if (esDuenio(publicacion, perfil)) {
            eliminarFisicamente(publicacion);
            return;
        }

        if (!esColaborador(publicacion, perfil)) {
            throw new PublicacionNoEncontradaException(idPublicacion);
        }

        boolean origenAutomatico = colaboradorOrigenService
                .buscar(idPublicacion, perfil.getIdPerfil())
                .map(origen -> origen.getOrigen()
                        == org.mgroko.backend.modelo.enums.OrigenColaboracion.AUTOMATICA
                        || origen.getOrigen()
                        == org.mgroko.backend.modelo.enums.OrigenColaboracion.AMBOS)
                .orElse(false);

        if (origenAutomatico) {
            colaboracionLifecycleService.disolverPorEtiqueta(
                    idPublicacion, perfil.getIdPerfil());
        }

        publicacionRepository.eliminarColaboradorPublicacion(
                idPublicacion, perfil.getIdPerfil());
        colaboradorOrigenService.eliminar(idPublicacion, perfil.getIdPerfil());
    }

    private boolean esDuenio(Publicacion publicacion, Perfil perfil) {
        return publicacion.getPerfil() != null
                && perfil.getIdPerfil().equals(publicacion.getPerfil().getIdPerfil());
    }

    private boolean esColaborador(Publicacion publicacion, Perfil perfil) {
        return publicacion.getColaboradores() != null
                && publicacion.getColaboradores().stream()
                        .anyMatch(colaborador -> perfil.getIdPerfil()
                                .equals(colaborador.getIdPerfil()));
    }

    private void eliminarFisicamente(Publicacion publicacion) {
        Long idPublicacion = publicacion.getIdPublicacion();

        colaboracionLifecycleService.cancelarPendientesPorPublicacion(idPublicacion);

        List<Imagen> imagenes = publicacion.getImagenes() != null
                ? new ArrayList<>(publicacion.getImagenes())
                : List.of();

        publicacionRepository.eliminarImagenesPublicacion(idPublicacion);
        for (Imagen imagen : imagenes) {
            borradoArchivosDiferido.registrarBorrados(null, imagen.getNombreArchivo());
            imagenRepository.eliminarFisicamente(imagen.getIdImagen());
        }

        if (publicacion.getColaboradores() != null) {
            for (Perfil colaborador : publicacion.getColaboradores()) {
                colaboradorOrigenService.eliminar(
                        idPublicacion, colaborador.getIdPerfil());
            }
        }

        publicacionRepository.eliminarColaboradoresPublicacion(idPublicacion);
        solicitudColaboracionRepository.eliminarPorPublicacion(idPublicacion);
        publicacionRepository.eliminarMeGustaDePublicacion(idPublicacion);
        publicacionRepository.eliminarComentariosDePublicacion(idPublicacion);

        notificacionService.eliminarPorEntidad(
                NotificacionService.ENTIDAD_PUBLICACION,
                idPublicacion);

        publicacionRepository.eliminarFisicamente(idPublicacion);
        entityManager.clear();
    }
}
