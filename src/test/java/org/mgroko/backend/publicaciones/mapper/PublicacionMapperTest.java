package org.mgroko.backend.publicaciones.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.OrigenColaboracion;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;

class PublicacionMapperTest {

    private Perfil perfil(Long id, String nombre, EstadoPerfil estado) {
        return Perfil.builder()
                .idPerfil(id)
                .nombreArtistico(nombre)
                .estado(estado)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .build();
    }

    private Publicacion publicacion(List<Imagen> imagenes) {
        return Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .descripcion("Descripción")
                .perfil(perfil(7L, "Luna Diseños", EstadoPerfil.Activo))
                .imagenes(new ArrayList<>(imagenes))
                .build();
    }

    @Test
    void toResponse_omiteProyectoNoVisible() {
        Publicacion publicacion = publicacion(List.of());
        publicacion.setProyecto(Proyecto.builder()
                .idProyecto(25L)
                .nombre("Privado")
                .estado(EstadoProyecto.CONFIRMADO)
                .build());

        PublicacionResponse response = PublicacionMapper.toResponse(publicacion, false);

        assertNull(response.proyecto());
    }

    @Test
    void toResponsePropia_incluyeProyectoNoVisibleConBanderaFalse() {
        Publicacion publicacion = publicacion(List.of());
        publicacion.setProyecto(Proyecto.builder()
                .idProyecto(25L)
                .nombre("Privado")
                .estado(EstadoProyecto.CONFIRMADO)
                .build());

        PublicacionResponse response = PublicacionMapper.toResponsePropia(publicacion, false);

        assertNotNull(response.proyecto());
        assertEquals(25L, response.proyecto().idProyecto());
        assertFalse(response.proyecto().puedeVerProyecto());
    }

    @Test
    void toResponse_mapeaColaboradoresActivosYOrdenaImagenes() {
        Perfil activo = perfil(8L, "Sol", EstadoPerfil.Activo);
        Perfil inactivo = perfil(9L, "Baja", EstadoPerfil.Baja);

        Publicacion publicacion = publicacion(List.of(
                Imagen.builder().idImagen(10L).url("/a.webp").nombreArchivo("a.webp").build(),
                Imagen.builder().idImagen(11L).url("/b.webp").nombreArchivo("b.webp").build()));
        publicacion.setColaboradores(new LinkedHashSet<>(List.of(activo, inactivo)));

        PublicacionResponse response = PublicacionMapper.toResponse(publicacion, true);

        assertEquals(2, response.imagenes().size());
        assertEquals(10L, response.imagenes().get(0).idImagen());
        assertEquals(0, response.imagenes().get(0).orden());
        assertEquals(11L, response.imagenes().get(1).idImagen());
        assertEquals(1, response.imagenes().get(1).orden());
        assertEquals(1, response.colaboradores().size());
        assertEquals(8L, response.colaboradores().get(0).idPerfil());
    }

    @Test
    void toResponse_incluyeOrigenDeColaborador() {
        Perfil colaborador = perfil(8L, "Sol", EstadoPerfil.Activo);
        Publicacion publicacion = publicacion(List.of());
        publicacion.setColaboradores(new LinkedHashSet<>(List.of(colaborador)));

        PublicacionResponse response = PublicacionMapper.toResponse(
                publicacion,
                true,
                org.mgroko.backend.publicaciones.dto.ResumenInteraccionPublicacion.vacio(),
                Map.of(8L, OrigenColaboracion.AUTOMATICA));

        assertEquals("AUTOMATICA", response.colaboradores().get(0).origen());
    }
}
