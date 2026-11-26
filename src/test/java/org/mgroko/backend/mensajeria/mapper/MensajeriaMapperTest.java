package org.mgroko.backend.mensajeria.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.mensajeria.dto.PerfilMensajeResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;

class MensajeriaMapperTest {

    @Test
    void toPerfilMensaje_mujer_devuelveProfesionFemenina() {
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

        PerfilMensajeResponse response = MensajeriaMapper.toPerfilMensaje(perfil);

        assertEquals("Fotógrafa", response.nombreProfesion());
        assertEquals(1L, response.idProfesion());
    }

    @Test
    void toPerfilMensaje_noDecirlo_devuelveProfesionCanonica() {
        Perfil perfil = Perfil.builder()
                .idPerfil(9L)
                .nombreArtistico("Alex")
                .usuario(Usuario.builder()
                        .idUsuario(3L)
                        .nombre("Alex")
                        .apellido("Gomez")
                        .genero(Genero.builder().codigo("NO_DECIRLO").build())
                        .build())
                .profesion(Profesion.builder()
                        .idProfesion(1L)
                        .codigo("FOTOGRAFO")
                        .nombre("Fotógrafo")
                        .nombreFemenino("Fotógrafa")
                        .build())
                .build();

        assertEquals("Fotógrafo",
                MensajeriaMapper.toPerfilMensaje(perfil).nombreProfesion());
    }
}
