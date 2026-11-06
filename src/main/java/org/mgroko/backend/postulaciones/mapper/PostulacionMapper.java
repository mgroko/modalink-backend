package org.mgroko.backend.postulaciones.mapper;

import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.postulaciones.dto.PostulacionBusquedaResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;

public final class PostulacionMapper {

    private PostulacionMapper() {
    }

    public static PostulacionBusquedaResponse toBusquedaGeneral(
            PostulacionGral postulacion,
            CupoRequerimiento cupo,
            boolean aportaCupo,
            boolean puedeAceptar,
            boolean puedeRechazar) {

        return new PostulacionBusquedaResponse(
                postulacion.getIdPostulacionGral(),
                TipoPostulacion.GENERAL,
                postulacion.getEstado(),
                postulacion.getFechaPostulacion(),
                postulacion.getPerfil().getIdPerfil(),
                postulacion.getPerfil().getNombreArtistico(),
                postulacion.getPerfil().getProfesion() != null
                        ? postulacion.getPerfil().getProfesion().getIdProfesion()
                        : null,
                postulacion.getPerfil().getProfesion() != null
                        ? postulacion.getPerfil().getProfesion().getNombre()
                        : null,
                postulacion.getRequerimientoGral().getIdRequerimientoGral(),
                postulacion.getRequerimientoGral().getDescripcion(),
                null,
                null,
                cupo.cantidad(),
                cupo.cubiertos(),
                cupo.faltantes(),
                cupo.satisfecho(),
                aportaCupo,
                puedeAceptar,
                puedeRechazar);
    }

    public static PostulacionBusquedaResponse toBusquedaActividad(
            PostulacionActividad postulacion,
            CupoRequerimiento cupo,
            boolean aportaCupo,
            boolean puedeAceptar,
            boolean puedeRechazar) {

        return new PostulacionBusquedaResponse(
                postulacion.getIdPostulacion(),
                TipoPostulacion.ACTIVIDAD,
                postulacion.getEstado(),
                postulacion.getFechaPostulacion(),
                postulacion.getPerfil().getIdPerfil(),
                postulacion.getPerfil().getNombreArtistico(),
                postulacion.getPerfil().getProfesion() != null
                        ? postulacion.getPerfil().getProfesion().getIdProfesion()
                        : null,
                postulacion.getPerfil().getProfesion() != null
                        ? postulacion.getPerfil().getProfesion().getNombre()
                        : null,
                postulacion.getRequerimiento().getIdRequerimiento(),
                postulacion.getRequerimiento().getDescripcion(),
                postulacion.getRequerimiento().getActividad().getIdActividad(),
                postulacion.getRequerimiento().getActividad().getNombre(),
                cupo.cantidad(),
                cupo.cubiertos(),
                cupo.faltantes(),
                cupo.satisfecho(),
                aportaCupo,
                puedeAceptar,
                puedeRechazar);
    }
}
