package org.mgroko.backend.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;

class ProfesionNombreResolverTest {

    private Profesion fotografo() {
        return Profesion.builder()
                .idProfesion(1L)
                .codigo("FOTOGRAFO")
                .nombre("Fotógrafo")
                .nombreFemenino("Fotógrafa")
                .build();
    }

    private Perfil perfilConGenero(String codigoGenero) {
        Genero genero = codigoGenero != null
                ? Genero.builder().idGenero(1L).codigo(codigoGenero).build()
                : null;
        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .nombre("Maria")
                .apellido("Gomez")
                .genero(genero)
                .build();
        return Perfil.builder()
                .idPerfil(1L)
                .nombreArtistico("Luna")
                .biografia("Bio")
                .usuario(usuario)
                .profesion(fotografo())
                .build();
    }

    @Test
    void mostrar_mujer_devuelveNombreFemenino() {
        assertEquals("Fotógrafa", ProfesionNombreResolver.mostrar(perfilConGenero("MUJER")));
    }

    @Test
    void mostrar_mujerEnMinusculas_devuelveNombreFemenino() {
        assertEquals("Fotógrafa", ProfesionNombreResolver.mostrar(perfilConGenero("mujer")));
    }

    @Test
    void mostrar_hombre_devuelveNombreCanonico() {
        assertEquals("Fotógrafo", ProfesionNombreResolver.mostrar(perfilConGenero("HOMBRE")));
    }

    @Test
    void mostrar_noBinario_devuelveNombreCanonico() {
        assertEquals("Fotógrafo", ProfesionNombreResolver.mostrar(perfilConGenero("NO_BINARIO")));
    }

    @Test
    void mostrar_noDecirlo_devuelveNombreCanonico() {
        assertEquals("Fotógrafo", ProfesionNombreResolver.mostrar(perfilConGenero("NO_DECIRLO")));
    }

    @Test
    void mostrar_sinGenero_devuelveNombreCanonico() {
        assertEquals("Fotógrafo", ProfesionNombreResolver.mostrar(perfilConGenero(null)));
    }

    @Test
    void mostrar_perfilSinUsuario_devuelveNombreCanonico() {
        Perfil perfil = Perfil.builder()
                .idPerfil(2L)
                .nombreArtistico("Sol")
                .biografia("Bio")
                .profesion(fotografo())
                .build();

        assertEquals("Fotógrafo", ProfesionNombreResolver.mostrar(perfil));
    }

    @Test
    void mostrar_nombreFemeninoAusente_devuelveNombreCanonico() {
        Profesion profesion = Profesion.builder()
                .codigo("FOTOGRAFO")
                .nombre("Fotógrafo")
                .build();

        assertEquals("Fotógrafo",
                ProfesionNombreResolver.mostrar(profesion, Genero.builder().codigo("MUJER").build()));
    }

    @Test
    void mostrar_nombreFemeninoEnBlanco_devuelveNombreCanonico() {
        Profesion profesion = Profesion.builder()
                .codigo("FOTOGRAFO")
                .nombre("Fotógrafo")
                .nombreFemenino("   ")
                .build();

        assertEquals("Fotógrafo",
                ProfesionNombreResolver.mostrar(profesion, Genero.builder().codigo("MUJER").build()));
    }

    @Test
    void mostrar_profesionInvariable_devuelveMismoNombre() {
        Profesion modelo = Profesion.builder()
                .codigo("MODELO")
                .nombre("Modelo")
                .nombreFemenino("Modelo")
                .build();

        assertEquals("Modelo",
                ProfesionNombreResolver.mostrar(modelo, Genero.builder().codigo("MUJER").build()));
    }

    @Test
    void mostrar_profesionNula_devuelveNull() {
        assertNull(ProfesionNombreResolver.mostrar(null, Genero.builder().codigo("MUJER").build()));
    }

    @Test
    void mostrar_perfilNulo_devuelveNull() {
        assertNull(ProfesionNombreResolver.mostrar((Perfil) null));
    }
}
