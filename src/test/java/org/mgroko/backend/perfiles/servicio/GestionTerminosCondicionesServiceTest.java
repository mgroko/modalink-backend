package org.mgroko.backend.perfiles.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.modelo.PerfilTycId;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.ActualizarTerminosCondicionesRequest;
import org.mgroko.backend.perfiles.dto.AceptacionTerminosRequest;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesResponse;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.exception.TerminosCondicionesException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PerfilTycRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class GestionTerminosCondicionesServiceTest {

    private static final String PLANTILLA = "Plantilla de prueba";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private PerfilTycRepository perfilTycRepository;

    @Mock
    private ConfiguracionSistemaService configuracionSistemaService;

    @Mock
    private EntityManager entityManager;

    private GestionTerminosCondicionesService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC);
        service = new GestionTerminosCondicionesService(
                usuarioRepository,
                perfilRepository,
                perfilTycRepository,
                configuracionSistemaService,
                entityManager,
                reloj);
    }

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Activo)
                .build();
    }

    private Perfil perfil(EstadoPerfil estado) {
        return Perfil.builder()
                .idPerfil(5L)
                .nombreArtistico("Perfil")
                .biografia("Bio")
                .estado(estado)
                .usuario(usuarioActivo())
                .build();
    }

    private PerfilTyc version(Long idTyc, String texto) {
        return PerfilTyc.builder()
                .id(new PerfilTycId(5L, idTyc))
                .perfil(perfil(EstadoPerfil.Activo))
                .descripcion(texto)
                .fechaInicio(LocalDateTime.of(2026, 11, 1, 10, 0))
                .build();
    }

    private void usuarioActivoEnRepo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
    }

    // ------------------------------------------------------------- obtener

    @Test
    void obtener_sinVersion_devuelvePlantilla() {
        usuarioActivoEnRepo();
        when(perfilRepository.findById(5L)).thenReturn(Optional.of(perfil(EstadoPerfil.Activo)));
        when(perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(5L))
                .thenReturn(Optional.empty());
        when(configuracionSistemaService.obtenerPlantillaTerminosCondiciones())
                .thenReturn(PLANTILLA);

        TerminosCondicionesResponse response = service.obtener(1L, 5L);

        assertTrue(response.esPlantilla());
        assertNull(response.idTyc());
        assertEquals(PLANTILLA, response.descripcion());
        assertEquals(5L, response.idPerfil());
    }

    @Test
    void obtener_conVersion_devuelveVigente() {
        usuarioActivoEnRepo();
        when(perfilRepository.findById(5L)).thenReturn(Optional.of(perfil(EstadoPerfil.Activo)));
        when(perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(5L))
                .thenReturn(Optional.of(version(7L, "Texto vigente")));

        TerminosCondicionesResponse response = service.obtener(1L, 5L);

        assertFalse(response.esPlantilla());
        assertEquals(7L, response.idTyc());
        assertEquals("Texto vigente", response.descripcion());
    }

    @Test
    void obtener_perfilAjenoInactivo_lanza404() {
        usuarioActivoEnRepo();
        Perfil perfil = perfil(EstadoPerfil.PendienteBaja);
        perfil.getUsuario().setIdUsuario(99L);
        when(perfilRepository.findById(5L)).thenReturn(Optional.of(perfil));

        assertThrows(PerfilNoEncontradoException.class,
                () -> service.obtener(1L, 5L));
    }

    @Test
    void obtener_usuarioInactivo_lanza401() {
        Usuario usuario = usuarioActivo();
        usuario.setEstado(EstadoUsuario.Baja);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioNoEncontradoException.class,
                () -> service.obtener(1L, 5L));
    }

    // ------------------------------------------------------------ actualizar

    @Test
    void actualizar_creaPrimeraVersion() {
        usuarioActivoEnRepo();
        Perfil perfil = perfil(EstadoPerfil.Activo);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuarioForUpdate(5L, 1L))
                .thenReturn(Optional.of(perfil));
        when(perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(5L))
                .thenReturn(Optional.empty());
        when(perfilTycRepository.nextIdTyc()).thenReturn(10L);

        TerminosCondicionesResponse response = service.actualizar(
                1L, 5L, new ActualizarTerminosCondicionesRequest("  Nuevo texto  "));

        assertEquals(10L, response.idTyc());
        assertEquals("Nuevo texto", response.descripcion());
        assertFalse(response.esPlantilla());
        assertNotNull(response.fechaInicio());
    }

    @Test
    void actualizar_cierraVersionAnteriorYCreaNueva() {
        usuarioActivoEnRepo();
        Perfil perfil = perfil(EstadoPerfil.Activo);
        PerfilTyc vigente = version(7L, "Texto viejo");
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuarioForUpdate(5L, 1L))
                .thenReturn(Optional.of(perfil));
        when(perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(5L))
                .thenReturn(Optional.of(vigente));
        when(perfilTycRepository.saveAndFlush(any(PerfilTyc.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(perfilTycRepository.nextIdTyc()).thenReturn(8L);

        TerminosCondicionesResponse response = service.actualizar(
                1L, 5L, new ActualizarTerminosCondicionesRequest("Texto nuevo"));

        assertNotNull(vigente.getFechaFin());
        assertEquals(8L, response.idTyc());
        assertEquals("Texto nuevo", response.descripcion());
        verify(perfilTycRepository).saveAndFlush(vigente);
    }

    @Test
    void actualizar_mismoTexto_noCreaVersion() {
        usuarioActivoEnRepo();
        Perfil perfil = perfil(EstadoPerfil.Activo);
        PerfilTyc vigente = version(7L, "Texto vigente");
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuarioForUpdate(5L, 1L))
                .thenReturn(Optional.of(perfil));
        when(perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(5L))
                .thenReturn(Optional.of(vigente));

        TerminosCondicionesResponse response = service.actualizar(
                1L, 5L, new ActualizarTerminosCondicionesRequest("  Texto vigente  "));

        assertEquals(7L, response.idTyc());
        verify(perfilTycRepository, never()).saveAndFlush(any(PerfilTyc.class));
    }

    @Test
    void actualizar_perfilPendienteBaja_lanza409() {
        usuarioActivoEnRepo();
        Perfil perfil = perfil(EstadoPerfil.PendienteBaja);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuarioForUpdate(5L, 1L))
                .thenReturn(Optional.of(perfil));

        assertThrows(PerfilEnBajaException.class,
                () -> service.actualizar(1L, 5L,
                        new ActualizarTerminosCondicionesRequest("Texto")));
    }

    @Test
    void actualizar_perfilAjeno_lanza404() {
        usuarioActivoEnRepo();
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuarioForUpdate(5L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(PerfilNoEncontradoException.class,
                () -> service.actualizar(1L, 5L,
                        new ActualizarTerminosCondicionesRequest("Texto")));
    }

    // ----------------------------------------------------------- aceptación

    @Test
    void validarAceptacion_sinTyc_devuelveNull() {
        when(perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(5L))
                .thenReturn(Optional.empty());

        assertNull(service.validarAceptacion(5L, null));
    }

    @Test
    void validarAceptacion_sinAceptacion_lanzaNoAceptados() {
        when(perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(5L))
                .thenReturn(Optional.of(version(7L, "Texto")));

        TerminosCondicionesException ex = assertThrows(
                TerminosCondicionesException.class,
                () -> service.validarAceptacion(5L, null));

        assertEquals(TerminosCondicionesException.NO_ACEPTADOS, ex.getCodigo());
        assertEquals(7L, ex.getIdTycVigente());
    }

    @Test
    void validarAceptacion_versionDesactualizada_lanzaDesactualizados() {
        when(perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(5L))
                .thenReturn(Optional.of(version(7L, "Texto")));

        TerminosCondicionesException ex = assertThrows(
                TerminosCondicionesException.class,
                () -> service.validarAceptacion(5L,
                        new AceptacionTerminosRequest(6L, true)));

        assertEquals(TerminosCondicionesException.DESACTUALIZADOS, ex.getCodigo());
        assertEquals(7L, ex.getIdTycVigente());
    }

    @Test
    void validarAceptacion_valida_devuelveIdTyc() {
        when(perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(5L))
                .thenReturn(Optional.of(version(7L, "Texto")));

        assertEquals(7L, service.validarAceptacion(5L,
                new AceptacionTerminosRequest(7L, true)));
    }

    @Test
    void validarAceptacion_idTycSinVigente_lanzaNoVigentes() {
        when(perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(5L))
                .thenReturn(Optional.empty());

        TerminosCondicionesException ex = assertThrows(
                TerminosCondicionesException.class,
                () -> service.validarAceptacion(5L,
                        new AceptacionTerminosRequest(7L, true)));

        assertEquals(TerminosCondicionesException.NO_VIGENTES, ex.getCodigo());
    }

    @Test
    void obtenerVigentes_indexaPorPerfil() {
        PerfilTyc tyc = version(7L, "Texto");
        when(perfilTycRepository.findVigentesByPerfilIds(List.of(5L)))
                .thenReturn(List.of(tyc));

        var resultado = service.obtenerVigentes(List.of(5L));

        assertEquals(1, resultado.size());
        assertEquals(7L, resultado.get(5L).getIdTyc());
    }
}
