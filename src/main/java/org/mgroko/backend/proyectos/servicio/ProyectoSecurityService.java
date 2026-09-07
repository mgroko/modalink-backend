package org.mgroko.backend.proyectos.servicio;

import java.util.Optional;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.PermisoProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("proyectoSecurity")
public class ProyectoSecurityService {

    public static final String ROL_DIRECTOR = "Director";

    private final MiembroProyectoRepository miembroProyectoRepository;

    public ProyectoSecurityService(MiembroProyectoRepository miembroProyectoRepository) {
        this.miembroProyectoRepository = miembroProyectoRepository;
    }

    /**
     * Verifica si el perfil es miembro activo del proyecto y posee el permiso especificado.
     */
    @Transactional(readOnly = true)
    public boolean tienePermiso(Long idProyecto, Long idPerfil, String permiso) {
        if (idProyecto == null || idPerfil == null || permiso == null) {
            return false;
        }

        Optional<MiembroProyecto> miembroOpt = miembroProyectoRepository
                .findMiembroActivoConPermisos(idProyecto, idPerfil);

        if (miembroOpt.isEmpty()) {
            return false;
        }

        RolProyecto rol = miembroOpt.get().getRolProyecto();
        if (rol == null || rol.getPermisos() == null) {
            return false;
        }

        return rol.getPermisos().stream()
                .map(PermisoProyecto::getNombre)
                .anyMatch(p -> p.equalsIgnoreCase(permiso));
    }

    /**
     * Verifica si el perfil es Director activo del proyecto.
     */
    @Transactional(readOnly = true)
    public boolean esDirector(Long idProyecto, Long idPerfil) {
        if (idProyecto == null || idPerfil == null) {
            return false;
        }

        Optional<MiembroProyecto> miembroOpt = miembroProyectoRepository
                .findMiembroActivoConPermisos(idProyecto, idPerfil);

        if (miembroOpt.isEmpty()) {
            return false;
        }

        RolProyecto rol = miembroOpt.get().getRolProyecto();
        return rol != null && ROL_DIRECTOR.equalsIgnoreCase(rol.getNombre());
    }

    /**
     * Valida programáticamente el permiso en el proyecto, lanzando AccesoDenegadoProyectoException si no lo tiene.
     */
    @Transactional(readOnly = true)
    public void validarPermiso(Long idProyecto, Long idPerfil, String permiso) {
        if (!tienePermiso(idProyecto, idPerfil, permiso)) {
            throw new AccesoDenegadoProyectoException(
                    "No tienes el permiso '" + permiso + "' en este proyecto o no eres un miembro activo.");
        }
    }

    /**
     * Valida programáticamente que el perfil sea Director activo del proyecto.
     */
    @Transactional(readOnly = true)
    public void validarDirector(Long idProyecto, Long idPerfil) {
        if (!esDirector(idProyecto, idPerfil)) {
            throw new AccesoDenegadoProyectoException(
                    "Debes ser Director activo del proyecto para realizar esta acción.");
        }
    }
}
