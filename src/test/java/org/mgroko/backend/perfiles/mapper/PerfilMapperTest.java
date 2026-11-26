package org.mgroko.backend.perfiles.mapper;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.PerfilBusquedaResponse;
import org.mgroko.backend.perfiles.dto.PerfilDetalleResponse;
import org.mgroko.backend.perfiles.dto.PerfilResponse;

class PerfilMapperTest {

    @Test
    void toResponse_remapeaCamposCompletos() {
        Profesion profesion = Profesion.builder().idProfesion(2L).codigo("MODELO").nombre("modelo").build();

        CaracteristicaTecnica altura = CaracteristicaTecnica.builder()
                .idCaracteristica(11L).codigo("altura").profesion(profesion).build();
        CaracteristicaTecnica ojos = CaracteristicaTecnica.builder()
                .idCaracteristica(12L).codigo("color_ojos").profesion(profesion).build();

        CaracteristicaPerfil cpAltura = CaracteristicaPerfil.builder()
                .idCaracteristicaPerfil(1L)
                .caracteristicaTecnica(altura)
                .valor("175")
                .build();
        CaracteristicaPerfil cpOjos = CaracteristicaPerfil.builder()
                .idCaracteristicaPerfil(2L)
                .caracteristicaTecnica(ojos)
                .valor("verdes")
                .build();

        Set<CaracteristicaPerfil> caracteristicas = new LinkedHashSet<>();
        caracteristicas.add(cpOjos);
        caracteristicas.add(cpAltura);

        Perfil perfil = Perfil.builder()
                .idPerfil(5L)
                .nombreArtistico("Luna")
                .biografia("Modelo profesional.")
                .estado(EstadoPerfil.Activo)
                .profesion(profesion)
                .caracteristicas(caracteristicas)
                .build();

        PerfilResponse response = PerfilMapper.toResponse(perfil);

        assertEquals(5L, response.idPerfil());
        assertEquals("Luna", response.nombreArtistico());
        assertEquals("Modelo profesional.", response.biografia());
        assertEquals("Activo", response.estado());
        assertEquals("modelo", response.profesion());
        assertEquals(2, response.caracteristicas().size());
        // Ordenadas alfabéticamente por código: altura antes que color_ojos
        assertEquals("altura", response.caracteristicas().get(0).codigo());
        assertEquals("175", response.caracteristicas().get(0).valor());
        assertEquals("color_ojos", response.caracteristicas().get(1).codigo());
        assertEquals("verdes", response.caracteristicas().get(1).valor());
    }

    @Test
    void toResponse_sinCaracteristicas_devuelveListaVacia() {
        Profesion profesion = Profesion.builder().idProfesion(2L).codigo("MODELO").nombre("modelo").build();

        Perfil perfil = Perfil.builder()
                .idPerfil(6L)
                .nombreArtistico("Tatiana")
                .biografia("Otra biografía.")
                .estado(EstadoPerfil.Activo)
                .profesion(profesion)
                .build();

        PerfilResponse response = PerfilMapper.toResponse(perfil);

        assertTrue(response.caracteristicas().isEmpty());
        assertNull(response.fechaLimite());
    }

    @Test
    void toResponse_perfilPendienteBaja_calculaFechaLimiteConDiasBaja() {
        Profesion profesion = Profesion.builder().idProfesion(2L).codigo("MODELO").nombre("modelo").build();
        LocalDateTime fechaSolicitud = LocalDateTime.of(2026, 9, 20, 10, 0);

        Perfil perfil = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna")
                .biografia("Modelo profesional.")
                .estado(EstadoPerfil.PendienteBaja)
                .fechaSolicitudBaja(fechaSolicitud)
                .profesion(profesion)
                .build();

        PerfilResponse response = PerfilMapper.toResponse(perfil, 45);

        assertEquals("PendienteBaja", response.estado());
        assertEquals(fechaSolicitud, response.fechaSolicitudBaja());
        assertEquals(fechaSolicitud.plusDays(45), response.fechaLimite());
    }

    @Test
    void toResponse_perfilActivo_noTieneFechaLimiteAunqueSePaseDiasBaja() {
        Profesion profesion = Profesion.builder().idProfesion(2L).codigo("MODELO").nombre("modelo").build();

        Perfil perfil = Perfil.builder()
                .idPerfil(8L)
                .nombreArtistico("Luna")
                .biografia("Modelo profesional.")
                .estado(EstadoPerfil.Activo)
                .profesion(profesion)
                .build();

        PerfilResponse response = PerfilMapper.toResponse(perfil, 30);

        assertNull(response.fechaLimite());
    }

    private Perfil perfilConGenero(String codigoGenero, Profesion profesion) {
        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .nombre("Maria")
                .apellido("Gomez")
                .genero(Genero.builder().idGenero(1L).codigo(codigoGenero).build())
                .build();

        return Perfil.builder()
                .idPerfil(20L)
                .nombreArtistico("Luna")
                .biografia("Bio")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
    }

    private Profesion fotografo() {
        return Profesion.builder()
                .idProfesion(1L)
                .codigo("FOTOGRAFO")
                .nombre("Fotógrafo")
                .nombreFemenino("Fotógrafa")
                .build();
    }

    @Test
    void toResponse_mujer_devuelveProfesionFemenina() {
        PerfilResponse response = PerfilMapper.toResponse(perfilConGenero("MUJER", fotografo()));

        assertEquals("Fotógrafa", response.profesion());
    }

    @Test
    void toResponse_noBinario_devuelveProfesionCanonica() {
        PerfilResponse response = PerfilMapper.toResponse(perfilConGenero("NO_BINARIO", fotografo()));

        assertEquals("Fotógrafo", response.profesion());
    }

    @Test
    void toBusquedaResponse_mujer_devuelveProfesionFemenina() {
        PerfilBusquedaResponse response = PerfilMapper.toBusquedaResponse(
                perfilConGenero("MUJER", fotografo()));

        assertEquals("Fotógrafa", response.profesion());
        assertEquals("MUJER", response.genero());
        assertEquals(1L, response.idProfesion());
    }

    @Test
    void toDetalleResponse_mujer_devuelveProfesionFemenina() {
        PerfilDetalleResponse response = PerfilMapper.toDetalleResponse(
                perfilConGenero("MUJER", fotografo()), false);

        assertEquals("Fotógrafa", response.profesion());
        assertEquals("MUJER", response.genero());
        assertEquals(1L, response.idProfesion());
    }

    @Test
    void toResponse_sinDiasBaja_noCalculaFechaLimite() {
        Profesion profesion = Profesion.builder().idProfesion(2L).codigo("MODELO").nombre("modelo").build();

        Perfil perfil = Perfil.builder()
                .idPerfil(9L)
                .nombreArtistico("Luna")
                .biografia("Modelo profesional.")
                .estado(EstadoPerfil.PendienteBaja)
                .fechaSolicitudBaja(LocalDateTime.of(2026, 9, 20, 10, 0))
                .profesion(profesion)
                .build();

        assertNull(PerfilMapper.toResponse(perfil).fechaLimite());
    }
}