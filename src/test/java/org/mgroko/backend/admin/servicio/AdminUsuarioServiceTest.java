package org.mgroko.backend.admin.servicio;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.AdminUsuarioResponse;
import org.mgroko.backend.admin.dto.DeshabilitarUsuarioRequest;
import org.mgroko.backend.admin.exception.AutoDeshabilitacionException;
import org.mgroko.backend.admin.exception.UsuarioAdminNoEncontradoException;
import org.mgroko.backend.admin.exception.UsuarioEnBajaException;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AdminUsuarioService adminUsuarioService;

    private Usuario usuarioConRol(EstadoUsuario estado) {
        return Usuario.builder()
                .idUsuario(2L)
                .nombre("Maria")
                .apellido("Flores")
                .correo("maria@test.com")
                .dni("12345678")
                .estado(estado)
                .rolGlobal(RolGlobal.builder().nombre("USUARIO").build())
                .genero(Genero.builder().codigo("mujer").build())
                .build();
    }

    private DeshabilitarUsuarioRequest deshabilitarRequest(String motivo, Integer duracionDias) {
        return new DeshabilitarUsuarioRequest(motivo, duracionDias);
    }

    @Test
    void habilitar_usuarioDeshabilitado_cambiaEstadoAActivo() {
        Usuario usuario = usuarioConRol(EstadoUsuario.Deshabilitado);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminUsuarioResponse response = adminUsuarioService.habilitar(2L);

        assertEquals("Activo", response.estado());
        assertEquals("Activo", usuario.getEstado().name());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void habilitar_usuarioYaActivo_mantieneEstadoActivo() {
        Usuario usuario = usuarioConRol(EstadoUsuario.Activo);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminUsuarioResponse response = adminUsuarioService.habilitar(2L);

        assertEquals("Activo", response.estado());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void habilitar_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioAdminNoEncontradoException.class,
                () -> adminUsuarioService.habilitar(999L));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void habilitar_usuarioEnBaja_lanzaExcepcion() {
        Usuario usuario = usuarioConRol(EstadoUsuario.Baja);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioEnBajaException.class,
                () -> adminUsuarioService.habilitar(2L));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deshabilitar_usuarioActivo_cambiaEstadoADeshabilitado() {
        Usuario usuario = usuarioConRol(EstadoUsuario.Activo);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminUsuarioResponse response =
                adminUsuarioService.deshabilitar(2L, 1L, deshabilitarRequest("Incumplimiento de normas", 7));

        assertEquals("Deshabilitado", response.estado());
        assertEquals("Incumplimiento de normas", response.motivoDeshabilitacion());
        assertNotNull(response.fechaHastaDeshabilitacion());
        assertEquals(EstadoUsuario.Deshabilitado, usuario.getEstado());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deshabilitar_conDuracion_calculaFechaHasta() {
        Usuario usuario = usuarioConRol(EstadoUsuario.Activo);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminUsuarioService.deshabilitar(2L, 1L, deshabilitarRequest("Motivo", 7));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertNotNull(guardado.getFechaHastaDeshabilitacion());
        assertTrue(guardado.getFechaHastaDeshabilitacion().isAfter(LocalDateTime.now()));
        assertTrue(guardado.getFechaHastaDeshabilitacion().isBefore(LocalDateTime.now().plusDays(7).plusSeconds(1)));
    }

    @Test
    void deshabilitar_sinDuracion_deshabilitacionIndefinida() {
        Usuario usuario = usuarioConRol(EstadoUsuario.Activo);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminUsuarioResponse response =
                adminUsuarioService.deshabilitar(2L, 1L, deshabilitarRequest("Motivo", null));

        assertEquals("Motivo", response.motivoDeshabilitacion());
        assertNull(response.fechaHastaDeshabilitacion());
    }

    @Test
    void deshabilitar_autoDeshabilitacion_lanzaExcepcion() {
        assertThrows(AutoDeshabilitacionException.class,
                () -> adminUsuarioService.deshabilitar(1L, 1L, deshabilitarRequest("Motivo", 7)));

        verify(usuarioRepository, never()).findById(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deshabilitar_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioAdminNoEncontradoException.class,
                () -> adminUsuarioService.deshabilitar(999L, 1L, deshabilitarRequest("Motivo", 7)));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deshabilitar_usuarioEnBaja_lanzaExcepcion() {
        Usuario usuario = usuarioConRol(EstadoUsuario.Baja);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioEnBajaException.class,
                () -> adminUsuarioService.deshabilitar(2L, 1L, deshabilitarRequest("Motivo", 7)));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void habilitar_usaIdUsuarioCorrecto() {
        Usuario usuario = usuarioConRol(EstadoUsuario.Deshabilitado);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminUsuarioService.habilitar(2L);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(EstadoUsuario.Activo, captor.getValue().getEstado());
    }

    @Test
    void habilitar_limpiaMotivoYDuracionDeLaDeshabilitacion() {
        Usuario usuario = usuarioConRol(EstadoUsuario.Deshabilitado);
        usuario.setMotivoDeshabilitacion("Incumplimiento de normas");
        usuario.setFechaHastaDeshabilitacion(LocalDateTime.now().plusDays(7));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminUsuarioService.habilitar(2L);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(EstadoUsuario.Activo, captor.getValue().getEstado());
        assertNull(captor.getValue().getMotivoDeshabilitacion());
        assertNull(captor.getValue().getFechaHastaDeshabilitacion());
    }

    @Test
    void deshabilitar_idSolicitanteDiferenteDeshabilitaOtroUsuario() {
        Usuario usuario = usuarioConRol(EstadoUsuario.Activo);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminUsuarioResponse response =
                adminUsuarioService.deshabilitar(2L, 1L, deshabilitarRequest("Motivo", null));

        assertEquals("Deshabilitado", response.estado());
        verify(usuarioRepository).save(usuario);
    }

    // UC-06 - Buscar usuario
    @Test
    void buscar_conPaginacionPorDefecto_devuelvePaginaResponse() {
        Usuario u1 = usuarioConRol(EstadoUsuario.Activo);
        Usuario u2 = usuarioConRol(EstadoUsuario.Deshabilitado);
        org.springframework.data.domain.PageImpl<Usuario> page =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(u1, u2),
                        org.springframework.data.domain.PageRequest.of(0, 20), 2);

        when(usuarioRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        org.mgroko.backend.admin.dto.BuscarUsuariosAdminFiltro filtro =
                new org.mgroko.backend.admin.dto.BuscarUsuariosAdminFiltro("Maria", null, null, null, null, null, null);

        org.mgroko.backend.common.dto.PaginaResponse<AdminUsuarioResponse> resultado =
                adminUsuarioService.buscar(filtro, 0, 20, false);

        assertNotNull(resultado);
        assertEquals(2, resultado.contenido().size());
        assertEquals(2, resultado.totalElementos());
        assertEquals(20, resultado.tamanoPagina());
        assertEquals(0, resultado.paginaActual());
    }

    @Test
    void buscar_conOpcionTodos_usaPageableUnpaged() {
        Usuario u1 = usuarioConRol(EstadoUsuario.Activo);
        org.springframework.data.domain.PageImpl<Usuario> page =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(u1));

        when(usuarioRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        org.mgroko.backend.admin.dto.BuscarUsuariosAdminFiltro filtro =
                new org.mgroko.backend.admin.dto.BuscarUsuariosAdminFiltro(null, null, null, null, null, null, null);

        org.mgroko.backend.common.dto.PaginaResponse<AdminUsuarioResponse> resultado =
                adminUsuarioService.buscar(filtro, 0, 0, true);

        ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(usuarioRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), captor.capture());

        assertTrue(captor.getValue().isUnpaged());
        assertEquals(1, resultado.contenido().size());
    }

    @Test
    void buscar_sinCoincidencias_devuelvePaginaVacia() {
        org.springframework.data.domain.PageImpl<Usuario> page =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(),
                        org.springframework.data.domain.PageRequest.of(0, 50), 0);

        when(usuarioRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        org.mgroko.backend.admin.dto.BuscarUsuariosAdminFiltro filtro =
                new org.mgroko.backend.admin.dto.BuscarUsuariosAdminFiltro("Inexistente", null, null, null, null, null, null);

        org.mgroko.backend.common.dto.PaginaResponse<AdminUsuarioResponse> resultado =
                adminUsuarioService.buscar(filtro, 0, 50, false);

        assertTrue(resultado.contenido().isEmpty());
        assertEquals(0, resultado.totalElementos());
        assertEquals(50, resultado.tamanoPagina());
    }
}
