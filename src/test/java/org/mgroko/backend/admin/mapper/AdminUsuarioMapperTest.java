package org.mgroko.backend.admin.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;

class AdminUsuarioMapperTest {

    @Test
    void toResponse_remapeaCamposCompletos() {
        LocalDateTime fechaBaja = LocalDateTime.of(2026, 8, 1, 10, 30);
        LocalDateTime fechaHasta = LocalDateTime.of(2026, 9, 1, 10, 30);

        Usuario usuario = Usuario.builder()
                .idUsuario(2L)
                .nombre("Maria")
                .apellido("Flores")
                .correo("maria@test.com")
                .dni("12345678")
                .estado(EstadoUsuario.Deshabilitado)
                .fechaNacimiento(LocalDate.of(1990, 6, 15))
                .fechaSolicitudBaja(fechaBaja)
                .motivoDeshabilitacion("Incumplimiento de normas")
                .fechaHastaDeshabilitacion(fechaHasta)
                .rolGlobal(RolGlobal.builder().nombre("USUARIO").build())
                .genero(Genero.builder().codigo("mujer").build())
                .build();

        var response = AdminUsuarioMapper.toResponse(usuario);

        assertEquals(2L, response.idUsuario());
        assertEquals("Maria", response.nombre());
        assertEquals("Flores", response.apellido());
        assertEquals("maria@test.com", response.correo());
        assertEquals("12345678", response.dni());
        assertEquals("Deshabilitado", response.estado());
        assertEquals("USUARIO", response.rolGlobal());
        assertEquals(LocalDate.of(1990, 6, 15), response.fechaNacimiento());
        assertEquals(fechaBaja, response.fechaSolicitudBaja());
        assertEquals("Incumplimiento de normas", response.motivoDeshabilitacion());
        assertEquals(fechaHasta, response.fechaHastaDeshabilitacion());
        assertEquals("mujer", response.genero().getCodigo());
    }

    @Test
    void toResponse_camposOpcionalesNulos_devuelveNull() {
        Usuario usuario = Usuario.builder()
                .idUsuario(3L)
                .nombre("Juan")
                .apellido("Perez")
                .correo("juan@test.com")
                .dni("99999999")
                .estado(EstadoUsuario.Deshabilitado)
                .rolGlobal(RolGlobal.builder().nombre("PREMIUM").build())
                .build();

        var response = AdminUsuarioMapper.toResponse(usuario);

        assertEquals("Deshabilitado", response.estado());
        assertNull(response.fechaNacimiento());
        assertNull(response.fechaSolicitudBaja());
        assertNull(response.motivoDeshabilitacion());
        assertNull(response.fechaHastaDeshabilitacion());
        assertNull(response.genero());
    }
}
