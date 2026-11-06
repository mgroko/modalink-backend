package org.mgroko.backend.invitaciones.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.invitaciones.dto.BuscarInvitacionesFiltro;
import org.mgroko.backend.invitaciones.dto.DireccionInvitacion;
import org.mgroko.backend.invitaciones.dto.InvitacionBusquedaResponse;
import org.mgroko.backend.invitaciones.dto.PerfilInvitacionResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
import org.mgroko.backend.invitaciones.exception.RangoFechasInvitacionInvalidoException;
import org.mgroko.backend.invitaciones.mapper.InvitacionMapper;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-30: búsqueda de invitaciones recibidas o enviadas por el perfil activo.
 *
 * <p>La consulta se resuelve por tipo y los resultados se unen, ordenan y
 * paginan en memoria, siguiendo el patrón de UC-27. El alcance está acotado al
 * perfil activo: no se exige membresía ni visibilidad del proyecto.</p>
 */
@Service
public class BuscarInvitacionService {

    private final InvitacionGralRepository invitacionGralRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;

    public BuscarInvitacionService(
            InvitacionGralRepository invitacionGralRepository,
            InvitacionActividadRepository invitacionActividadRepository) {
        this.invitacionGralRepository = invitacionGralRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<InvitacionBusquedaResponse> buscar(
            Long idPerfilActivo,
            BuscarInvitacionesFiltro filtro,
            int page,
            int size,
            boolean todos) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        validarFechas(filtro);

        List<InvitacionBusquedaResponse> invitaciones = new ArrayList<>();

        if (incluyeTipo(filtro, TipoInvitacion.GENERAL)) {
            invitacionGralRepository.buscarPorPerfil(idPerfilActivo).stream()
                    .map(invitacion -> InvitacionMapper.toBusquedaResponse(
                            invitacion, idPerfilActivo))
                    .forEach(invitaciones::add);
        }

        if (incluyeTipo(filtro, TipoInvitacion.ACTIVIDAD)) {
            invitacionActividadRepository.buscarPorPerfil(idPerfilActivo).stream()
                    .map(invitacion -> InvitacionMapper.toBusquedaResponse(
                            invitacion, idPerfilActivo))
                    .forEach(invitaciones::add);
        }

        List<InvitacionBusquedaResponse> filtradas = invitaciones.stream()
                .filter(invitacion -> coincideDireccion(invitacion, filtro))
                .filter(invitacion -> coincideEstado(invitacion, filtro))
                .filter(invitacion -> coincideProyecto(invitacion, filtro))
                .filter(invitacion -> coincideFechas(invitacion, filtro))
                .filter(invitacion -> coincidePerfil(invitacion, filtro, idPerfilActivo))
                .sorted(comparador())
                .toList();

        return paginar(filtradas, page, size, todos);
    }

    private void validarFechas(BuscarInvitacionesFiltro filtro) {
        if (filtro == null || filtro.fechaDesde() == null || filtro.fechaHasta() == null) {
            return;
        }
        if (filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new RangoFechasInvitacionInvalidoException();
        }
    }

    private boolean incluyeTipo(BuscarInvitacionesFiltro filtro, TipoInvitacion tipo) {
        return filtro == null || filtro.tipo() == null || filtro.tipo() == tipo;
    }

    private boolean coincideDireccion(
            InvitacionBusquedaResponse invitacion,
            BuscarInvitacionesFiltro filtro) {
        return filtro == null || filtro.direccion() == null
                || filtro.direccion() == invitacion.direccion();
    }

    private boolean coincideEstado(
            InvitacionBusquedaResponse invitacion,
            BuscarInvitacionesFiltro filtro) {
        return filtro == null || filtro.estado() == null
                || filtro.estado() == invitacion.estado();
    }

    private boolean coincideProyecto(
            InvitacionBusquedaResponse invitacion,
            BuscarInvitacionesFiltro filtro) {
        if (filtro == null) {
            return true;
        }

        if (filtro.idProyecto() != null
                && !filtro.idProyecto().equals(invitacion.idProyecto())) {
            return false;
        }

        return contieneIgnoreCase(invitacion.nombreProyecto(), filtro.nombreProyecto());
    }

    private boolean coincideFechas(
            InvitacionBusquedaResponse invitacion,
            BuscarInvitacionesFiltro filtro) {
        if (filtro == null || invitacion.fechaEnvio() == null) {
            return true;
        }

        LocalDate fecha = invitacion.fechaEnvio().toLocalDate();
        if (filtro.fechaDesde() != null && fecha.isBefore(filtro.fechaDesde())) {
            return false;
        }
        if (filtro.fechaHasta() != null && fecha.isAfter(filtro.fechaHasta())) {
            return false;
        }
        return true;
    }

    private boolean coincidePerfil(
            InvitacionBusquedaResponse invitacion,
            BuscarInvitacionesFiltro filtro,
            Long idPerfilActivo) {

        if (filtro == null || filtro.nombrePerfil() == null
                || filtro.nombrePerfil().isBlank()) {
            return true;
        }

        if (filtro.direccion() == DireccionInvitacion.RECIBIDA) {
            return coincideNombre(invitacion.remitente(), filtro.nombrePerfil());
        }

        if (filtro.direccion() == DireccionInvitacion.ENVIADA) {
            return coincideNombre(invitacion.destinatario(), filtro.nombrePerfil());
        }

        // Sin dirección, el filtro se aplica a la contraparte. No se tiene en
        // cuenta el propio perfil activo porque siempre participa de la
        // invitación y haría que cualquier búsqueda devuelva todo.
        return coincideContraparte(invitacion.remitente(), idPerfilActivo, filtro.nombrePerfil())
                || coincideContraparte(invitacion.destinatario(), idPerfilActivo, filtro.nombrePerfil());
    }

    private boolean coincideContraparte(
            PerfilInvitacionResponse perfil,
            Long idPerfilActivo,
            String texto) {

        if (perfil == null || (idPerfilActivo != null
                && idPerfilActivo.equals(perfil.idPerfil()))) {
            return false;
        }
        return coincideNombre(perfil, texto);
    }

    private boolean coincideNombre(PerfilInvitacionResponse perfil, String texto) {
        if (perfil == null) {
            return false;
        }
        return contieneIgnoreCase(perfil.nombreArtistico(), texto)
                || contieneIgnoreCase(perfil.nombre(), texto)
                || contieneIgnoreCase(perfil.apellido(), texto);
    }

    private boolean contieneIgnoreCase(String valor, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        if (valor == null) {
            return false;
        }
        return valor.toLowerCase(Locale.ROOT)
                .contains(texto.trim().toLowerCase(Locale.ROOT));
    }

    private Comparator<InvitacionBusquedaResponse> comparador() {
        return Comparator
                .comparing(
                        InvitacionBusquedaResponse::fechaEnvio,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()))
                .thenComparing(
                        InvitacionBusquedaResponse::idInvitacion,
                        Comparator.nullsLast(Comparator.<Long>reverseOrder()))
                .thenComparing(
                        invitacion -> invitacion.tipo() != null
                                ? invitacion.tipo().name()
                                : "");
    }

    private PaginaResponse<InvitacionBusquedaResponse> paginar(
            List<InvitacionBusquedaResponse> invitaciones,
            int page,
            int size,
            boolean todos) {

        if (todos || size <= 0) {
            return PaginaResponse.fromPage(new PageImpl<>(invitaciones));
        }

        PageRequest pageable = PageRequest.of(Math.max(0, page), size);
        int desde = (int) Math.min(pageable.getOffset(), invitaciones.size());
        int hasta = Math.min(desde + size, invitaciones.size());

        return PaginaResponse.fromPage(new PageImpl<>(
                invitaciones.subList(desde, hasta), pageable, invitaciones.size()));
    }
}
