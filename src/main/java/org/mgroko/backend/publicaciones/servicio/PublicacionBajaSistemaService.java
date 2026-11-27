package org.mgroko.backend.publicaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.OrigenColaboracion;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.ColaboracionAutomaticaRepository;
import org.mgroko.backend.repositorio.ColaboradorPublicacionOrigenRepository;
import org.mgroko.backend.repositorio.ComentarioRepository;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.MeGustaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * Resuelve las publicaciones de un perfil que se da de baja definitivamente.
 *
 * <p>Las publicaciones independientes se eliminan fisicamente. Las
 * publicaciones con colaboradores se conservan; si el perfil eliminado era
 * colaborador, solo se quita su etiqueta. Los comentarios conservan el nombre
 * artistico como snapshot historico.</p>
 */
@Service
public class PublicacionBajaSistemaService {

    private final PublicacionRepository publicacionRepository;
    private final ImagenRepository imagenRepository;
    private final SolicitudColaboracionRepository solicitudColaboracionRepository;
    private final ComentarioRepository comentarioRepository;
    private final MeGustaRepository meGustaRepository;
    private final ColaboracionAutomaticaRepository colaboracionAutomaticaRepository;
    private final ColaboradorPublicacionOrigenRepository colaboradorOrigenRepository;
    private final ColaboradorPublicacionOrigenService colaboradorOrigenService;
    private final ColaboracionAutomaticaLifecycleService colaboracionLifecycleService;
    private final NotificacionService notificacionService;
    private final BorradoArchivosDiferido borradoArchivosDiferido;
    private final EntityManager entityManager;
    private final Clock reloj;

    public PublicacionBajaSistemaService(
            PublicacionRepository publicacionRepository,
            ImagenRepository imagenRepository,
            SolicitudColaboracionRepository solicitudColaboracionRepository,
            ComentarioRepository comentarioRepository,
            MeGustaRepository meGustaRepository,
            ColaboracionAutomaticaRepository colaboracionAutomaticaRepository,
            ColaboradorPublicacionOrigenRepository colaboradorOrigenRepository,
            ColaboradorPublicacionOrigenService colaboradorOrigenService,
            ColaboracionAutomaticaLifecycleService colaboracionLifecycleService,
            NotificacionService notificacionService,
            BorradoArchivosDiferido borradoArchivosDiferido,
            EntityManager entityManager,
            Clock reloj) {
        this.publicacionRepository = publicacionRepository;
        this.imagenRepository = imagenRepository;
        this.solicitudColaboracionRepository = solicitudColaboracionRepository;
        this.comentarioRepository = comentarioRepository;
        this.meGustaRepository = meGustaRepository;
        this.colaboracionAutomaticaRepository = colaboracionAutomaticaRepository;
        this.colaboradorOrigenRepository = colaboradorOrigenRepository;
        this.colaboradorOrigenService = colaboradorOrigenService;
        this.colaboracionLifecycleService = colaboracionLifecycleService;
        this.notificacionService = notificacionService;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
        this.entityManager = entityManager;
        this.reloj = reloj;
    }

    @Transactional
    public int procesarPerfil(Perfil perfil) {
        if (perfil == null || perfil.getIdPerfil() == null) {
            return 0;
        }

        Long idPerfil = perfil.getIdPerfil();
        String nombreArtistico = perfil.getNombreArtistico();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int eliminadas = 0;

        List<Long> publicacionesPropias =
                publicacionRepository.findIdsByPerfilIdPerfil(idPerfil);

        // Primera pasada: eliminar las publicaciones independientes.
        for (Long idPublicacion : publicacionesPropias) {
            Optional<Publicacion> publicacion = publicacionRepository
                    .findByIdPublicacionForUpdate(idPublicacion);
            if (publicacion.isEmpty()) {
                continue;
            }
            if (publicacionRepository.contarColaboradores(idPublicacion) == 0) {
                eliminarFisicamente(publicacion.get());
                eliminadas++;
            }
        }

        // Quitar la etiqueta del perfil eliminado de publicaciones ajenas.
        for (Long idPublicacion : publicacionRepository.findIdsDondeEsColaborador(idPerfil)) {
            quitarColaborador(idPublicacion, idPerfil);
        }

        // Si una publicacion propia quedo sin colaboradores tras quitar la
        // etiqueta, tambien corresponde eliminarla.
        for (Long idPublicacion : publicacionesPropias) {
            if (!publicacionRepository.existsById(idPublicacion)) {
                continue;
            }
            if (publicacionRepository.contarColaboradores(idPublicacion) == 0) {
                Optional<Publicacion> publicacion = publicacionRepository
                        .findByIdPublicacionForUpdate(idPublicacion);
                if (publicacion.isPresent()) {
                    eliminarFisicamente(publicacion.get());
                    eliminadas++;
                }
            }
        }

        if (nombreArtistico != null && !nombreArtistico.isBlank()) {
            comentarioRepository.fijarNombreArtisticoAutor(idPerfil, nombreArtistico);
        }

        meGustaRepository.eliminarPorPerfil(idPerfil);
        solicitudColaboracionRepository.rechazarPendientesDePerfil(idPerfil, ahora);
        colaboracionAutomaticaRepository.eliminarPorPerfil(idPerfil);
        colaboradorOrigenRepository.eliminarPorPerfil(idPerfil);

        return eliminadas;
    }

    private void quitarColaborador(Long idPublicacion, Long idPerfil) {
        boolean origenAutomatico = colaboradorOrigenService
                .buscar(idPublicacion, idPerfil)
                .map(origen -> origen.getOrigen() == OrigenColaboracion.AUTOMATICA
                        || origen.getOrigen() == OrigenColaboracion.AMBOS)
                .orElse(false);

        if (origenAutomatico) {
            colaboracionLifecycleService.disolverPorEtiqueta(idPublicacion, idPerfil);
        }

        publicacionRepository.eliminarColaboradorPublicacion(idPublicacion, idPerfil);
        colaboradorOrigenService.eliminar(idPublicacion, idPerfil);
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
        entityManager.detach(publicacion);
    }
}
