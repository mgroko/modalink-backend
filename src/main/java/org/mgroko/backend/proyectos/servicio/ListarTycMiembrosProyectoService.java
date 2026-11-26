package org.mgroko.backend.proyectos.servicio;

import java.util.List;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.MiembroTycProyectoResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lista los términos y condiciones aceptados por cada miembro activo del
 * proyecto.
 *
 * <p>Está disponible para cualquier miembro activo con permiso
 * {@code VER_PROYECTO}. Muestra la versión histórica aceptada al incorporar
 * al miembro, no su versión vigente actual.</p>
 */
@Service
public class ListarTycMiembrosProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ProyectoSecurityService proyectoSecurityService;

    public ListarTycMiembrosProyectoService(
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            ProyectoSecurityService proyectoSecurityService) {
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
    }

    @Transactional(readOnly = true)
    public List<MiembroTycProyectoResponse> listar(
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

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto,
                idPerfilActivo,
                ProyectoSecurityService.PERMISO_VER_PROYECTO);

        miembroProyectoRepository
                .findMiembroActivoConPermisos(idProyecto, idPerfilActivo)
                .orElseThrow(() -> new AccesoDenegadoProyectoException(
                        "No tienes el permiso 'VER_PROYECTO' en este proyecto o no eres un miembro activo."));

        return miembroProyectoRepository
                .listarTycAceptadosDeMiembros(proyecto.getIdProyecto())
                .stream()
                .map(proyeccion -> new MiembroTycProyectoResponse(
                        proyeccion.getIdMiembro(),
                        proyeccion.getIdPerfil(),
                        proyeccion.getNombreArtistico(),
                        proyeccion.getNombreProfesion(),
                        proyeccion.getIdTycAceptado(),
                        proyeccion.getNumeroVersion(),
                        proyeccion.getDescripcion(),
                        proyeccion.getFechaInicio(),
                        proyeccion.getFechaFin()))
                .toList();
    }
}
