package org.mgroko.backend.publicaciones.servicio;

import java.util.LinkedHashSet;

import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionInvalidaException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Permite etiquetar un perfil propio como colaborador de una publicación
 * propia, sin solicitud ni conexión entre los perfiles.
 *
 * <p>Es el caso de un usuario con varios perfiles profesionales (por ejemplo,
 * modelo y maquilladora) que quiere reconocer su propio trabajo en una
 * publicación.</p>
 */
@Service
public class AgregarColaboradorPropioService {

    private final PerfilActivoService perfilActivoService;
    private final PublicacionRepository publicacionRepository;
    private final PerfilRepository perfilRepository;
    private final ColaboradorPublicacionOrigenService colaboradorOrigenService;
    private final ColaboracionAutomaticaLifecycleService colaboracionLifecycleService;

    public AgregarColaboradorPropioService(
            PerfilActivoService perfilActivoService,
            PublicacionRepository publicacionRepository,
            PerfilRepository perfilRepository,
            ColaboradorPublicacionOrigenService colaboradorOrigenService,
            ColaboracionAutomaticaLifecycleService colaboracionLifecycleService) {
        this.perfilActivoService = perfilActivoService;
        this.publicacionRepository = publicacionRepository;
        this.perfilRepository = perfilRepository;
        this.colaboradorOrigenService = colaboradorOrigenService;
        this.colaboracionLifecycleService = colaboracionLifecycleService;
    }

    @Transactional
    @AuditarOperacion
    public void agregar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPublicacion,
            Long idPerfil) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        Publicacion publicacion = publicacionRepository
                .findByIdPublicacionAndPerfilIdPerfil(
                        idPublicacion, perfilActivo.getIdPerfil())
                .orElseThrow(() -> new PublicacionNoEncontradaException(idPublicacion));

        Perfil destino = perfilRepository.findById(idPerfil)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (destino.getUsuario() == null
                || destino.getUsuario().getEstado() != EstadoUsuario.ACTIVO
                || destino.getEstado() != EstadoPerfil.ACTIVO
                || !destino.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new SolicitudColaboracionInvalidaException(
                    "El perfil indicado no pertenece al usuario autenticado o no está activo.",
                    "PERFIL_PROPIO_INVALIDO");
        }

        if (perfilActivo.getIdPerfil().equals(destino.getIdPerfil())) {
            throw new SolicitudColaboracionInvalidaException(
                    "No se puede agregar el mismo perfil como colaborador.",
                    "PERFIL_PROPIO_INVALIDO");
        }

        if (!publicacionRepository.existsColaborador(
                idPublicacion, destino.getIdPerfil())) {
            if (publicacion.getColaboradores() == null) {
                publicacion.setColaboradores(new LinkedHashSet<>());
            }
            publicacion.getColaboradores().add(destino);
            publicacionRepository.saveAndFlush(publicacion);
        }

        colaboradorOrigenService.registrarManual(
                idPublicacion, destino.getIdPerfil());
        colaboracionLifecycleService.cancelarPendientesEntrePerfil(
                idPublicacion, destino.getIdPerfil());
    }
}
