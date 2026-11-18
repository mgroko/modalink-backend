package org.mgroko.backend.admin.mapper;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.admin.dto.AdminProyectoDetalleResponse;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.dto.ObjetivoResponse;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;

/**
 * Arma la vista administrativa completa de un proyecto.
 */
public final class AdminProyectoDetalleMapper {

    private AdminProyectoDetalleMapper() {
    }

    public static AdminProyectoDetalleResponse toDetalle(
            Proyecto proyecto,
            Perfil director,
            LocalDate fechaEntregaEstipulada,
            List<Objetivo> objetivos,
            List<RequerimientoGralProyecto> requerimientos,
            MoodboardResponse moodboard,
            List<MiembroProyecto> miembros,
            List<AdminProyectoDetalleResponse.ContratoResumen> contratos,
            long totalContratos) {

        return new AdminProyectoDetalleResponse(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getDescripcion(),
                proyecto.getEstado() != null ? proyecto.getEstado().name() : null,
                proyecto.getPrivacidad() != null ? proyecto.getPrivacidad().name() : null,
                proyecto.getAceptaPostulacionGral(),
                proyecto.getFechaInicio(),
                fechaEntregaEstipulada,
                proyecto.getFechaFinalizacion(),
                proyecto.getFechaCancelacion(),
                proyecto.getMotivoCancelacion(),
                Boolean.TRUE.equals(proyecto.getFinalizacionPrematura()),
                proyecto.getUbicacion() != null
                        ? UbicacionMapper.toResponse(proyecto.getUbicacion())
                        : null,
                toDirector(director),
                objetivos == null
                        ? List.of()
                        : objetivos.stream()
                                .map(o -> new ObjetivoResponse(
                                        o.getIdObjetivo(), o.getNombre(), o.getDescripcion()))
                                .toList(),
                requerimientos == null
                        ? List.of()
                        : requerimientos.stream()
                                .map(AdminProyectoDetalleMapper::toRequerimiento)
                                .toList(),
                moodboard,
                miembros == null
                        ? List.of()
                        : miembros.stream()
                                .map(AdminProyectoDetalleMapper::toMiembro)
                                .toList(),
                contratos != null ? contratos : List.of(),
                totalContratos);
    }

    private static AdminProyectoDetalleResponse.DirectorResponse toDirector(Perfil director) {
        if (director == null) {
            return null;
        }
        return new AdminProyectoDetalleResponse.DirectorResponse(
                director.getIdPerfil(),
                director.getNombreArtistico(),
                nombreCompleto(director));
    }

    private static AdminProyectoDetalleResponse.RequerimientoResponse toRequerimiento(
            RequerimientoGralProyecto requerimiento) {

        List<AdminProyectoDetalleResponse.CaracteristicaResponse> caracteristicas =
                requerimiento.getCaracteristicas() == null
                        ? List.of()
                        : requerimiento.getCaracteristicas().stream()
                                .sorted(Comparator.comparing(
                                        c -> c.getCaracteristica().getCodigo()))
                                .map(AdminProyectoDetalleMapper::toCaracteristica)
                                .toList();

        List<String> habilidades = requerimiento.getHabilidades() == null
                ? List.of()
                : requerimiento.getHabilidades().stream()
                        .map(org.mgroko.backend.modelo.Habilidad::getNombre)
                        .sorted()
                        .toList();

        return new AdminProyectoDetalleResponse.RequerimientoResponse(
                requerimiento.getIdRequerimientoGral(),
                requerimiento.getCantidad(),
                requerimiento.getDescripcion(),
                requerimiento.getProfesion() != null
                        ? requerimiento.getProfesion().getIdProfesion()
                        : null,
                requerimiento.getProfesion() != null
                        ? requerimiento.getProfesion().getNombre()
                        : null,
                caracteristicas,
                habilidades);
    }

    private static AdminProyectoDetalleResponse.CaracteristicaResponse toCaracteristica(
            RequerimientoGralCaract caracteristica) {
        return new AdminProyectoDetalleResponse.CaracteristicaResponse(
                caracteristica.getCaracteristica().getIdCaracteristica(),
                caracteristica.getCaracteristica().getCodigo(),
                caracteristica.getCaracteristica().getTipoDato(),
                caracteristica.getValorMin(),
                caracteristica.getValorMax());
    }

    private static AdminProyectoDetalleResponse.MiembroResponse toMiembro(
            MiembroProyecto miembro) {
        Perfil perfil = miembro.getPerfil();
        return new AdminProyectoDetalleResponse.MiembroResponse(
                miembro.getIdMiembro(),
                perfil != null ? perfil.getIdPerfil() : null,
                perfil != null ? perfil.getNombreArtistico() : null,
                nombreCompleto(perfil),
                perfil != null && perfil.getProfesion() != null
                        ? perfil.getProfesion().getNombre()
                        : null,
                miembro.getRolProyecto() != null
                        ? miembro.getRolProyecto().getNombre()
                        : null,
                miembro.getEstadoParticipacion() != null
                        ? miembro.getEstadoParticipacion().name()
                        : null,
                miembro.getFechaAlta(),
                miembro.getFechaBaja(),
                miembro.getMotivoBaja());
    }

    public static String nombreCompleto(Perfil perfil) {
        if (perfil == null) {
            return null;
        }
        Usuario usuario = perfil.getUsuario();
        if (usuario != null) {
            String nombre = (usuario.getNombre() != null ? usuario.getNombre() : "")
                    + " " + (usuario.getApellido() != null ? usuario.getApellido() : "");
            String limpio = nombre.trim();
            if (!limpio.isBlank()) {
                return limpio;
            }
        }
        return perfil.getNombreArtistico();
    }
}
