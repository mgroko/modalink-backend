package org.mgroko.backend.publicaciones.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionResponse;

class SolicitudColaboracionMapperTest {

    @Test
    void toResponse_mapeaSolicitudRemitenteYDestinatario() {
        Perfil remitente = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .imagen(Imagen.builder().idImagen(1L).url("/uploads/luna.webp").build())
                .build();
        Perfil destinatario = Perfil.builder()
                .idPerfil(8L)
                .nombreArtistico("Sol")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(4L).nombre("Fotógrafa").build())
                .build();
        Publicacion publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .perfil(remitente)
                .build();
        SolicitudColaboracion solicitud = SolicitudColaboracion.builder()
                .idSolicitudColaboracion(50L)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.of(2026, 11, 10, 20, 30))
                .publicacion(publicacion)
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .build();

        SolicitudColaboracionResponse response = SolicitudColaboracionMapper.toResponse(solicitud);

        assertEquals(50L, response.idSolicitudColaboracion());
        assertEquals(EstadoSolicitud.PENDIENTE, response.estado());
        assertEquals(LocalDateTime.of(2026, 11, 10, 20, 30), response.fechaEnvio());
        assertEquals(99L, response.idPublicacion());
        assertEquals("Editorial Otoño", response.tituloPublicacion());
        assertEquals(7L, response.remitente().idPerfil());
        assertEquals("Diseñadora", response.remitente().profesion());
        assertEquals(8L, response.destinatario().idPerfil());
        assertEquals("Fotógrafa", response.destinatario().profesion());
        assertEquals(null, response.destinatario().fotoUrl());
    }
}
