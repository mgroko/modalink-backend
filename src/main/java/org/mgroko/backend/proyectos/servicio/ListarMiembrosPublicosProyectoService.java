package org.mgroko.backend.proyectos.servicio;

import java.util.List;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.MiembroPublicoTycProyectoResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Miembros visibles en la vista pública del proyecto, con la versión de T&C
 * aceptada al incorporarse.
 *
 * <p>No exige membresía: aplica la misma regla de visibilidad del proyecto que
 * el detalle público. No expone datos de contacto ni ids internos de usuario.</p>
 */
@Service
public class ListarMiembrosPublicosProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;

    public ListarMiembrosPublicosProyectoService(
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            ProyectoVisibilidadService proyectoVisibilidadService) {
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
    }

    @Transactional(readOnly = true)
    public List<MiembroPublicoTycProyectoResponse> listar(
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

        proyectoVisibilidadService.validarAcceso(proyecto, idPerfilActivo);

        return miembroProyectoRepository
                .listarMiembrosPublicosConTyc(proyecto.getIdProyecto())
                .stream()
                .map(proyeccion -> new MiembroPublicoTycProyectoResponse(
                        proyeccion.getIdPerfil(),
                        proyeccion.getNombreArtistico(),
                        proyeccion.getNombreProfesion(),
                        proyeccion.getCodigoRol(),
                        proyeccion.getIdTycAceptado(),
                        proyeccion.getDescripcion(),
                        proyeccion.getFechaInicio(),
                        proyeccion.getFechaFin()))
                .toList();
    }
}
