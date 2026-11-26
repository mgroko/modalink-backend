package org.mgroko.backend.admin.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminPerfilDetalleResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;

class AdminPerfilDetalleMapperTest {

    @Test
    void toDetalle_mujer_devuelveProfesionFemenina() {
        Perfil perfil = Perfil.builder()
                .idPerfil(20L)
                .nombreArtistico("Sol")
                .biografia("Bio")
                .estado(EstadoPerfil.Activo)
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

        AdminPerfilDetalleResponse response = AdminPerfilDetalleMapper.toDetalle(
                perfil, null, null, List.of(), 0, List.of(), 0, List.of(), 0);

        assertEquals("Fotógrafa", response.profesion());
        assertEquals(1L, response.idProfesion());
    }
}
