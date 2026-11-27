package org.mgroko.backend.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.perfiles.dto.CaracteristicaResponse;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesResponse;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;

/**
 * Detalle administrativo de un perfil. Incluye datos del usuario duenio y
 * listas relacionadas de proyectos, contratos y publicaciones.
 *
 * <p>{@code terminosCondiciones} solo incluye una versión configurada por el
 * perfil; es {@code null} cuando todavía no configuró sus términos, para que la
 * vista administrativa muestre el estado vacío en lugar de la plantilla base.</p>
 */
public record AdminPerfilDetalleResponse(
        Long idPerfil,
        String nombreArtistico,
        String biografia,
        String estado,
        LocalDateTime fechaSolicitudBaja,
        LocalDateTime fechaLimiteBaja,
        Long idProfesion,
        String profesion,
        Long idImagen,
        String fotoUrl,
        UsuarioAdmin usuario,
        UbicacionResponse ubicacion,
        List<String> habilidades,
        List<CaracteristicaResponse> caracteristicas,
        TerminosCondicionesResponse terminosCondiciones,
        RelacionadosResumen resumenRelacionados,
        List<ProyectoRelacionado> proyectos,
        List<ContratoRelacionado> contratos,
        List<PublicacionRelacionada> publicaciones
) {

    public record UsuarioAdmin(
            Long idUsuario,
            String nombre,
            String apellido,
            String correo,
            String dni,
            LocalDate fechaNacimiento,
            String genero,
            String estado,
            String rolGlobal,
            LocalDateTime fechaSolicitudBaja,
            String motivoDeshabilitacion,
            LocalDateTime fechaHastaDeshabilitacion
    ) {
    }

    public record RelacionadosResumen(
            long totalProyectos,
            long totalContratos,
            long totalPublicaciones
    ) {
    }

    public record ProyectoRelacionado(
            Long idProyecto,
            String nombre,
            String estadoProyecto,
            String rolProyecto,
            String estadoParticipacion,
            LocalDateTime fechaAlta,
            LocalDateTime fechaBaja
    ) {
    }

    public record ContratoRelacionado(
            Long idContrato,
            String nombre,
            String estado,
            Long idProyecto,
            String nombreProyecto,
            String rolParte,
            String estadoFirma,
            LocalDateTime fechaEmision
    ) {
    }

    public record PublicacionRelacionada(
            Long idPublicacion,
            String titulo,
            String estado,
            LocalDateTime fechaPublicacion,
            Long idProyecto,
            String nombreProyecto
    ) {
    }
}
