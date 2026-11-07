package org.mgroko.backend.perfiles.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.ubicacion.dto.CiudadResponse;

public record PerfilDetalleResponse(
        Long idPerfil,
        String nombreArtistico,
        String biografia,
        String estado,
        LocalDateTime fechaSolicitudBaja,
        Long idProfesion,
        String profesion,
        Long idImagen,
        String fotoUrl,
        Long idUsuario,
        String nombreUsuario,
        String apellidoUsuario,
        String genero,
        CiudadResponse ciudad,
        List<String> habilidades,
        List<CaracteristicaResponse> caracteristicas,
        boolean esPropietario,
        LocalDateTime fechaLimite,
        TerminosCondicionesResponse terminosCondiciones
) {

    /**
     * Constructor histórico para consumidores que aún no leen los términos y
     * condiciones del perfil.
     */
    public PerfilDetalleResponse(
            Long idPerfil,
            String nombreArtistico,
            String biografia,
            String estado,
            LocalDateTime fechaSolicitudBaja,
            Long idProfesion,
            String profesion,
            Long idImagen,
            String fotoUrl,
            Long idUsuario,
            String nombreUsuario,
            String apellidoUsuario,
            String genero,
            CiudadResponse ciudad,
            List<String> habilidades,
            List<CaracteristicaResponse> caracteristicas,
            boolean esPropietario,
            LocalDateTime fechaLimite) {
        this(
                idPerfil,
                nombreArtistico,
                biografia,
                estado,
                fechaSolicitudBaja,
                idProfesion,
                profesion,
                idImagen,
                fotoUrl,
                idUsuario,
                nombreUsuario,
                apellidoUsuario,
                genero,
                ciudad,
                habilidades,
                caracteristicas,
                esPropietario,
                fechaLimite,
                null);
    }
}
