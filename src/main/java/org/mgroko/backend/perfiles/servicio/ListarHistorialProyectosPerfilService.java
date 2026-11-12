package org.mgroko.backend.perfiles.servicio;

import java.util.List;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.dto.HistorialProyectoPerfilResponse;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historial de proyectos de un perfil: proyectos finalizados y cancelados en
 * los que el perfil mantiene una membresía activa.
 */
@Service
public class ListarHistorialProyectosPerfilService {

    private static final int TAMANIO_DEFAULT = 10;
    private static final int TAMANIO_MAXIMO = 50;
    private static final List<EstadoProyecto> ESTADOS_HISTORIAL =
            List.of(EstadoProyecto.FINALIZADO, EstadoProyecto.CANCELADO);

    private final VerPerfilService verPerfilService;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;

    public ListarHistorialProyectosPerfilService(
            VerPerfilService verPerfilService,
            MiembroProyectoRepository miembroProyectoRepository,
            ProyectoVisibilidadService proyectoVisibilidadService) {
        this.verPerfilService = verPerfilService;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<HistorialProyectoPerfilResponse> listar(
            Long idPerfil,
            Long idUsuarioAutenticado,
            Long idPerfilActivo,
            int page,
            int size) {

        verPerfilService.obtenerPerfilVisible(idPerfil, idUsuarioAutenticado);

        int pagina = Math.max(0, page);
        int tamanio = size <= 0 ? TAMANIO_DEFAULT : Math.min(size, TAMANIO_MAXIMO);
        Pageable pageable = PageRequest.of(pagina, tamanio);

        Page<MiembroProyecto> historial = miembroProyectoRepository
                .buscarHistorialProyectosDePerfil(
                        idPerfil,
                        ESTADOS_HISTORIAL,
                        EstadoParticipacion.ACTIVO,
                        pageable);

        Page<HistorialProyectoPerfilResponse> respuesta = historial.map(
                miembro -> toResponse(miembro, idPerfilActivo));

        return PaginaResponse.fromPage(respuesta);
    }

    private HistorialProyectoPerfilResponse toResponse(
            MiembroProyecto miembro,
            Long idPerfilActivo) {

        Proyecto proyecto = miembro.getProyecto();
        DatosRol rol = resolverRol(miembro);

        return new HistorialProyectoPerfilResponse(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getEstado() != null ? proyecto.getEstado().getNombre() : null,
                rol.nombre(),
                rol.codigo(),
                proyecto.getFechaFinalizacion(),
                proyecto.getFechaCancelacion(),
                proyectoVisibilidadService.puedeVer(proyecto, idPerfilActivo));
    }

    private DatosRol resolverRol(MiembroProyecto miembro) {
        RequerimientoGralProyecto requerimiento = miembro.getRequerimientoGral();
        if (requerimiento != null && requerimiento.getProfesion() != null) {
            return new DatosRol(
                    requerimiento.getProfesion().getNombre(),
                    requerimiento.getProfesion().getCodigo());
        }

        if (RolProyectoUsuario.desde(miembro.getRolProyecto()) == RolProyectoUsuario.DIRECTOR) {
            return new DatosRol("Director", "DIRECTOR");
        }

        Perfil perfil = miembro.getPerfil();
        if (perfil != null && perfil.getProfesion() != null) {
            return new DatosRol(
                    perfil.getProfesion().getNombre(),
                    perfil.getProfesion().getCodigo());
        }

        return new DatosRol(null, null);
    }

    private record DatosRol(String nombre, String codigo) {
    }
}
