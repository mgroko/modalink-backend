package org.mgroko.backend.invitaciones.mapper;

import org.mgroko.backend.invitaciones.dto.DireccionInvitacion;
import org.mgroko.backend.invitaciones.dto.InvitacionBusquedaResponse;
import org.mgroko.backend.invitaciones.dto.InvitacionResponse;
import org.mgroko.backend.invitaciones.dto.PerfilInvitacionResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;

public final class InvitacionMapper {

    private InvitacionMapper() {
    }

    public static InvitacionResponse toResponse(InvitacionGral invitacion) {
        return new InvitacionResponse(
                invitacion.getIdInvitacionGral(),
                TipoInvitacion.GENERAL,
                invitacion.getEstado(),
                invitacion.getFechaEnvio(),
                invitacion.getProyecto().getIdProyecto(),
                invitacion.getRequerimientoGral() != null
                        ? invitacion.getRequerimientoGral().getIdRequerimientoGral()
                        : null,
                null,
                invitacion.getPerfilRemitente().getIdPerfil(),
                invitacion.getPerfilDestinatario().getIdPerfil(),
                invitacion.getMensaje(),
                invitacion.getIdTycAceptado());
    }

    public static InvitacionResponse toResponse(InvitacionActividad invitacion) {
        RequerimientoActividad requerimiento = invitacion.getRequerimiento();
        Long idProyecto = requerimiento.getActividad()
                .getPlanificacion()
                .getProyecto()
                .getIdProyecto();

        return new InvitacionResponse(
                invitacion.getIdInvitacion(),
                TipoInvitacion.ACTIVIDAD,
                invitacion.getEstado(),
                invitacion.getFechaEnvio(),
                idProyecto,
                null,
                requerimiento.getIdRequerimiento(),
                invitacion.getPerfilRemitente().getIdPerfil(),
                invitacion.getPerfilDestinatario().getIdPerfil(),
                invitacion.getMensaje(),
                invitacion.getIdTycAceptado());
    }

    public static InvitacionBusquedaResponse toBusquedaResponse(
            InvitacionGral invitacion,
            Long idPerfilActivo) {

        boolean enviada = esRemitente(invitacion.getPerfilRemitente(), idPerfilActivo);
        Long idProyecto = invitacion.getProyecto().getIdProyecto();

        return new InvitacionBusquedaResponse(
                invitacion.getIdInvitacionGral(),
                TipoInvitacion.GENERAL,
                direccion(enviada),
                invitacion.getEstado(),
                invitacion.getFechaEnvio(),
                invitacion.getFechaRechazo(),
                idProyecto,
                invitacion.getProyecto().getNombre(),
                invitacion.getRequerimientoGral() != null
                        ? invitacion.getRequerimientoGral().getIdRequerimientoGral()
                        : null,
                null,
                null,
                null,
                toPerfilResponse(invitacion.getPerfilRemitente()),
                toPerfilResponse(invitacion.getPerfilDestinatario()),
                invitacion.getMensaje(),
                puedeEliminar(invitacion.getEstado(), enviada),
                invitacion.getIdTycAceptado());
    }

    public static InvitacionBusquedaResponse toBusquedaResponse(
            InvitacionActividad invitacion,
            Long idPerfilActivo) {

        RequerimientoActividad requerimiento = invitacion.getRequerimiento();
        var actividad = requerimiento.getActividad();
        var proyecto = actividad.getPlanificacion().getProyecto();
        boolean enviada = esRemitente(invitacion.getPerfilRemitente(), idPerfilActivo);

        return new InvitacionBusquedaResponse(
                invitacion.getIdInvitacion(),
                TipoInvitacion.ACTIVIDAD,
                direccion(enviada),
                invitacion.getEstado(),
                invitacion.getFechaEnvio(),
                invitacion.getFechaRechazo(),
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                null,
                requerimiento.getIdRequerimiento(),
                actividad.getIdActividad(),
                actividad.getNombre(),
                toPerfilResponse(invitacion.getPerfilRemitente()),
                toPerfilResponse(invitacion.getPerfilDestinatario()),
                invitacion.getMensaje(),
                puedeEliminar(invitacion.getEstado(), enviada),
                invitacion.getIdTycAceptado());
    }

    private static boolean esRemitente(Perfil perfil, Long idPerfilActivo) {
        return perfil != null
                && perfil.getIdPerfil() != null
                && perfil.getIdPerfil().equals(idPerfilActivo);
    }

    private static DireccionInvitacion direccion(boolean enviada) {
        return enviada ? DireccionInvitacion.ENVIADA : DireccionInvitacion.RECIBIDA;
    }

    private static boolean puedeEliminar(EstadoSolicitud estado, boolean enviada) {
        return enviada && estado == EstadoSolicitud.PENDIENTE;
    }

    private static PerfilInvitacionResponse toPerfilResponse(Perfil perfil) {
        if (perfil == null) {
            return null;
        }

        return new PerfilInvitacionResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getUsuario() != null ? perfil.getUsuario().getNombre() : null,
                perfil.getUsuario() != null ? perfil.getUsuario().getApellido() : null,
                perfil.getProfesion() != null ? perfil.getProfesion().getIdProfesion() : null,
                perfil.getProfesion() != null ? perfil.getProfesion().getNombre() : null);
    }
}
