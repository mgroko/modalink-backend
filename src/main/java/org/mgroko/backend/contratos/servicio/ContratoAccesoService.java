package org.mgroko.backend.contratos.servicio;

import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validaciones comunes de acceso para UC-39: perfil activo, proyecto visible
 * y director activo del proyecto.
 */
@Service
public class ContratoAccesoService {

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final ProyectoSecurityService proyectoSecurityService;

    public ContratoAccesoService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            ProyectoSecurityService proyectoSecurityService) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
    }

    @Transactional(readOnly = true)
    public ContextoAcceso cargar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = proyectoRepository.findByIdConUbicacion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new ContratoNoGenerableException(
                    "PROYECTO_NO_APTO_PARA_CONTRATO",
                    "Solo se puede generar un contrato para un proyecto PUBLICADO o CONFIRMADO.");
        }

        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        return new ContextoAcceso(perfil, proyecto);
    }

    public record ContextoAcceso(Perfil perfil, Proyecto proyecto) {
    }
}
