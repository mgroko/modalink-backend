package org.mgroko.backend.conexiones.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.conexiones.dto.PerfilConexionResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;

class SolicitudConexionMapperTest {

    @Test
    void toPerfilResponse_mujer_devuelveProfesionFemenina() {
        Perfil perfil = Perfil.builder()
                .idPerfil(8L)
                .nombreArtistico("Sol")
                .usuario(Usuario.builder()
                        .idUsuario(2L)
                        .nombre("Sol")
                        .apellido("Perez")
                        .genero(Genero.builder().codigo("MUJER").build())
                        .build())
                .profesion(Profesion.builder()
                        .idProfesion(1L)
                        .codigo("FOTOGRAFO")
                        .nombre("Fotógrafo")
                        .nombreFemenino("Fotógrafa")
                        .build())
                .build();

        PerfilConexionResponse response = SolicitudConexionMapper.toPerfilResponse(perfil);

        assertEquals("Fotógrafa", response.nombreProfesion());
        assertEquals(1L, response.idProfesion());
    }

    @Test
    void toPerfilResponse_hombre_devuelveProfesionCanonica() {
        Perfil perfil = Perfil.builder()
                .idPerfil(9L)
                .nombreArtistico("Bruno")
                .usuario(Usuario.builder()
                        .idUsuario(3L)
                        .nombre("Bruno")
                        .apellido("Gomez")
                        .genero(Genero.builder().codigo("HOMBRE").build())
                        .build())
                .profesion(Profesion.builder()
                        .idProfesion(1L)
                        .codigo("FOTOGRAFO")
                        .nombre("Fotógrafo")
                        .nombreFemenino("Fotógrafa")
                        .build())
                .build();

        assertEquals("Fotógrafo",
                SolicitudConexionMapper.toPerfilResponse(perfil).nombreProfesion());
    }
}
