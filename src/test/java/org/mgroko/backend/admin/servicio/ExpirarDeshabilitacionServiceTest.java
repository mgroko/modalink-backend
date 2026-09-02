package org.mgroko.backend.admin.servicio;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpirarDeshabilitacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ExpirarDeshabilitacionService expirarDeshabilitacionService;

    private Usuario deshabilitadoVencido() {
        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .nombre("Maria")
                .apellido("Flores")
                .estado(EstadoUsuario.Deshabilitado)
                .build();
        usuario.setMotivoDeshabilitacion("Incumplimiento de normas");
        usuario.setFechaHastaDeshabilitacion(LocalDateTime.now().minusDays(1));
        return usuario;
    }

    @Test
    void reactivarVencidos_reactivaYLimpiaCampos() {
        Usuario usuario = deshabilitadoVencido();
        when(usuarioRepository.findByEstadoAndFechaHastaDeshabilitacionBefore(
                eq(EstadoUsuario.Deshabilitado), any()))
                .thenReturn(List.of(usuario));

        int cantidad = expirarDeshabilitacionService.reactivarVencidos();

        assertEquals(1, cantidad);
        ArgumentCaptor<List<Usuario>> captor = ArgumentCaptor.forClass(List.class);
        verify(usuarioRepository).saveAll(captor.capture());
        Usuario guardado = captor.getValue().get(0);
        assertEquals(EstadoUsuario.Activo, guardado.getEstado());
        assertNull(guardado.getMotivoDeshabilitacion());
        assertNull(guardado.getFechaHastaDeshabilitacion());
    }

    @Test
    void reactivarVencidos_sinVencidos_noGuarda() {
        when(usuarioRepository.findByEstadoAndFechaHastaDeshabilitacionBefore(
                eq(EstadoUsuario.Deshabilitado), any()))
                .thenReturn(List.of());

        int cantidad = expirarDeshabilitacionService.reactivarVencidos();

        assertEquals(0, cantidad);
        verify(usuarioRepository, never()).saveAll(any());
    }
}