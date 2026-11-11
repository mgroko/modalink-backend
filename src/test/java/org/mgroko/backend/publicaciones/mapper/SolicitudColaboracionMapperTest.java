package org.mgroko.backend.publicaciones.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.publicaciones.dto.DireccionSolicitudColaboracion;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionBusquedaResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionGestionadaResponse;
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

    @Test
    void toBusquedaResponse_mapeaDireccionFlagsYPortada() {
        Perfil remitente = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
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
                .imagenes(List.of(Imagen.builder()
                        .idImagen(10L)
                        .url("/uploads/publicaciones/portada.webp")
                        .build()))
                .build();
        SolicitudColaboracion solicitud = SolicitudColaboracion.builder()
                .idSolicitudColaboracion(50L)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.of(2026, 11, 10, 20, 30))
                .publicacion(publicacion)
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .build();

        SolicitudColaboracionBusquedaResponse recibida =
                SolicitudColaboracionMapper.toBusquedaResponse(solicitud, 8L);
        SolicitudColaboracionBusquedaResponse enviada =
                SolicitudColaboracionMapper.toBusquedaResponse(solicitud, 7L);

        assertEquals(DireccionSolicitudColaboracion.RECIBIDA, recibida.direccion());
        assertTrue(recibida.puedeAceptar());
        assertTrue(recibida.puedeRechazar());
        assertFalse(recibida.puedeEliminar());
        assertEquals(null, recibida.fechaRespuesta());
        assertEquals("/uploads/publicaciones/portada.webp", recibida.publicacion().portadaUrl());
        assertEquals("ACTIVA", recibida.publicacion().estado());

        assertEquals(DireccionSolicitudColaboracion.ENVIADA, enviada.direccion());
        assertFalse(enviada.puedeAceptar());
        assertFalse(enviada.puedeRechazar());
        assertTrue(enviada.puedeEliminar());
    }

    @Test
    void toGestionadaResponse_mapeaEstadoFechaYColaboradorAgregado() {
        Perfil remitente = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .build();
        Publicacion publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .perfil(remitente)
                .build();
        SolicitudColaboracion solicitud = SolicitudColaboracion.builder()
                .idSolicitudColaboracion(50L)
                .estado(EstadoSolicitud.ACEPTADA)
                .fechaEnvio(LocalDateTime.of(2026, 11, 10, 20, 0))
                .fechaRespuesta(LocalDateTime.of(2026, 11, 10, 21, 0))
                .publicacion(publicacion)
                .perfilRemitente(remitente)
                .build();

        SolicitudColaboracionGestionadaResponse response =
                SolicitudColaboracionMapper.toGestionadaResponse(solicitud, true);

        assertEquals(50L, response.idSolicitudColaboracion());
        assertEquals(EstadoSolicitud.ACEPTADA, response.estado());
        assertEquals(LocalDateTime.of(2026, 11, 10, 21, 0), response.fechaRespuesta());
        assertEquals(99L, response.idPublicacion());
        assertEquals("Editorial Otoño", response.tituloPublicacion());
        assertTrue(response.colaboradorAgregado());
    }
}
