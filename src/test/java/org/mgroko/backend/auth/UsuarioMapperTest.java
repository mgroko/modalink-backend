package org.mgroko.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.auth.dto.UsuarioResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UsuarioMapper es package-private, por eso este test vive en el mismo
 * paquete org.mgroko.backend.auth (no en un subpaquete de test aparte).
 * No hace falta Mockito para las dependencias porque no hay ninguna:
 * es una función pura de transformación.
 */
class UsuarioMapperTest {

    @Test
    void toResponse_conRolGlobal_mapeaTodosLosCampos() {
        RolGlobal rol = mock(RolGlobal.class);
        when(rol.getNombre()).thenReturn("Usuario");

        Genero genero = mock(Genero.class);
        when(genero.getCodigo()).thenReturn("mujer");

        Usuario usuario = Usuario.builder()
                .nombre("Maria")
                .apellido("Flores")
                .dni("12345678")
                .correo("maria.flores@test.com")
                .rolGlobal(rol)
                .genero(genero)
                .build();

        UsuarioResponse response = UsuarioMapper.toResponse(usuario);

        assertEquals("Maria", response.nombre());
        assertEquals("Flores", response.apellido());
        assertEquals("12345678", response.dni());
        assertEquals("maria.flores@test.com", response.correo());
        assertEquals("Usuario", response.rolGlobal());
        assertEquals("mujer", response.genero());
    }

    @Test
    void toResponse_sinRolGlobal_devuelveRolNull() {
        // Rama del operador ternario: usuario.getRolGlobal() != null ? ... : null
        Usuario usuario = Usuario.builder()
                .nombre("Ana")
                .apellido("Gomez")
                .dni("87654321")
                .correo("ana@test.com")
                .rolGlobal(null)
                .genero(null)
                .build();

        UsuarioResponse response = UsuarioMapper.toResponse(usuario);

        assertNull(response.rolGlobal());
        assertNull(response.genero());
        assertEquals("Ana", response.nombre());
    }
}