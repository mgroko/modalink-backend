package org.mgroko.backend.perfiles.dto;

import java.util.List;

import org.mgroko.backend.ubicacion.dto.CiudadResponse;

public record PerfilBusquedaResponse(
        Long idPerfil,
        String nombreArtistico,
        String biografia,
        String estado,
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
        boolean enLinea
) {

    /**
     * Constructor histórico para consumidores que todavía no leen presencia.
     */
    public PerfilBusquedaResponse(
            Long idPerfil,
            String nombreArtistico,
            String biografia,
            String estado,
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
            List<CaracteristicaResponse> caracteristicas) {
        this(
                idPerfil,
                nombreArtistico,
                biografia,
                estado,
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
                false);
    }
}
