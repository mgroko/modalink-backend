package org.mgroko.backend.publicaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;

import org.mgroko.backend.publicaciones.dto.MeGustaResponse;
import org.mgroko.backend.repositorio.MeGustaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-66: alta y baja del Me gusta de un perfil sobre una publicación.
 *
 * <p>El alta es idempotente: si el perfil ya dio Me gusta, no se duplica la
 * fila ni se vuelve a notificar.</p>
 */
@Service
public class GestionarMeGustaPublicacionService {

    private final PublicacionInteraccionService publicacionInteraccionService;
    private final MeGustaRepository meGustaRepository;
    private final PublicacionInteraccionNotificacionService notificacionService;
    private final Clock reloj;

    public GestionarMeGustaPublicacionService(
            PublicacionInteraccionService publicacionInteraccionService,
            MeGustaRepository meGustaRepository,
            PublicacionInteraccionNotificacionService notificacionService,
            Clock reloj) {
        this.publicacionInteraccionService = publicacionInteraccionService;
        this.meGustaRepository = meGustaRepository;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
    }

    @Transactional
    public MeGustaResponse dar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPublicacion) {

        PublicacionInteraccionService.ContextoInteraccionPublicacion contexto =
                publicacionInteraccionService.resolver(
                        idUsuario, idPerfilActivo, idPublicacion);

        int insertadas = meGustaRepository.insertarSiNoExiste(
                idPublicacion,
                contexto.perfilActivo().getIdPerfil(),
                LocalDateTime.now(reloj));

        if (insertadas == 1) {
            notificacionService.notificarMeGusta(
                    contexto.publicacion(), contexto.perfilActivo());
        }

        return new MeGustaResponse(
                idPublicacion,
                true,
                cantidadMeGusta(idPublicacion));
    }

    @Transactional
    public MeGustaResponse quitar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPublicacion) {

        PublicacionInteraccionService.ContextoInteraccionPublicacion contexto =
                publicacionInteraccionService.resolver(
                        idUsuario, idPerfilActivo, idPublicacion);

        meGustaRepository.eliminar(
                idPublicacion, contexto.perfilActivo().getIdPerfil());

        return new MeGustaResponse(
                idPublicacion,
                false,
                cantidadMeGusta(idPublicacion));
    }

    private int cantidadMeGusta(Long idPublicacion) {
        return (int) meGustaRepository.contarPorPublicacion(idPublicacion);
    }
}
