package org.mgroko.backend.auth.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.auth.dto.CompletarRegistroGoogleRequest;
import org.mgroko.backend.auth.exception.CuentaGoogleNoVerificadaException;
import org.mgroko.backend.auth.exception.DniDuplicadoException;
import org.mgroko.backend.auth.exception.EdadInvalidaException;
import org.mgroko.backend.auth.exception.IdentidadGoogleConflictoException;
import org.mgroko.backend.auth.servicio.AuthService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.UsuarioIdentidad;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.ProveedorAuth;
import org.mgroko.backend.repositorio.GeneroRepository;
import org.mgroko.backend.repositorio.RolGlobalRepository;
import org.mgroko.backend.repositorio.UsuarioIdentidadRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String SUB = "google-sub-123";
    private static final String CORREO = "persona@example.com";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioIdentidadRepository usuarioIdentidadRepository;
    @Mock
    private RolGlobalRepository rolGlobalRepository;
    @Mock
    private GeneroRepository generoRepository;
    @Mock
    private UbicacionService ubicacionService;
    @Mock
    private AuthService authService;
    @Mock
    private RegistroPendienteService registroPendienteService;

    @InjectMocks
    private GoogleAuthService googleAuthService;

    private AuthService.LoginResultado loginResultado;
    private GoogleUserData datosGoogle;

    @BeforeEach
    void setUp() {
        loginResultado = mock(AuthService.LoginResultado.class);
        datosGoogle = new GoogleUserData(SUB, CORREO, true, "Ana", "Gómez", null);
        lenient().when(authService.construirLoginResultado(any())).thenReturn(loginResultado);
        lenient().when(authService.reactivarSiPendienteBaja(any())).thenReturn(false);
    }

    @Test
    void autenticar_usuarioNuevo_devuelveRegistroPendiente() {
        when(usuarioIdentidadRepository.findByProveedorAndIdExterno(ProveedorAuth.GOOGLE, SUB))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(registroPendienteService.crearTicket(datosGoogle)).thenReturn("ticket-123");

        GoogleAuthResult resultado = googleAuthService.autenticar(datosGoogle);

        assertTrue(resultado.requiereCompletarRegistro());
        assertEquals("ticket-123", resultado.ticket());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void autenticar_identidadGoogleExistente_iniciaSesion() {
        Usuario usuario = usuario(1L, CORREO);
        UsuarioIdentidad identidad = UsuarioIdentidad.builder()
                .usuario(usuario)
                .proveedor(ProveedorAuth.GOOGLE)
                .idExterno(SUB)
                .correo(CORREO)
                .correoVerificado(true)
                .build();

        when(usuarioIdentidadRepository.findByProveedorAndIdExterno(ProveedorAuth.GOOGLE, SUB))
                .thenReturn(Optional.of(identidad));

        GoogleAuthResult resultado = googleAuthService.autenticar(datosGoogle);

        assertFalse(resultado.requiereCompletarRegistro());
        assertEquals(loginResultado, resultado.login());
        verify(usuarioRepository, never()).findByCorreo(anyString());
    }

    @Test
    void autenticar_correoLocalExistente_vinculaGoogleEIniciaSesion() {
        Usuario usuario = usuario(2L, CORREO);
        when(usuarioIdentidadRepository.findByProveedorAndIdExterno(ProveedorAuth.GOOGLE, SUB))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(usuarioIdentidadRepository.findByUsuario_IdUsuarioAndProveedor(2L, ProveedorAuth.GOOGLE))
                .thenReturn(Optional.empty());

        GoogleAuthResult resultado = googleAuthService.autenticar(datosGoogle);

        assertFalse(resultado.requiereCompletarRegistro());
        assertEquals(loginResultado, resultado.login());

        ArgumentCaptor<UsuarioIdentidad> captor = ArgumentCaptor.forClass(UsuarioIdentidad.class);
        verify(usuarioIdentidadRepository).save(captor.capture());
        assertEquals(ProveedorAuth.GOOGLE, captor.getValue().getProveedor());
        assertEquals(SUB, captor.getValue().getIdExterno());
        assertTrue(captor.getValue().isCorreoVerificado());
    }

    @Test
    void autenticar_emailNoVerificado_lanzaExcepcion() {
        GoogleUserData sinVerificar = new GoogleUserData(SUB, CORREO, false, "Ana", "Gómez", null);

        assertThrows(CuentaGoogleNoVerificadaException.class,
                () -> googleAuthService.autenticar(sinVerificar));
    }

    @Test
    void autenticar_googleYaVinculadoAOtroUsuario_lanzaConflicto() {
        Usuario usuarioDelCorreo = usuario(3L, CORREO);
        Usuario otroUsuario = usuario(4L, "otro@example.com");
        UsuarioIdentidad identidadDeOtro = UsuarioIdentidad.builder()
                .usuario(otroUsuario)
                .proveedor(ProveedorAuth.GOOGLE)
                .idExterno(SUB)
                .correo("otro@example.com")
                .correoVerificado(true)
                .build();

        // Primera consulta (autenticar): no hay identidad por sub.
        // Segunda consulta (vincularGoogle): el sub ya pertenece a otro usuario.
        when(usuarioIdentidadRepository.findByProveedorAndIdExterno(ProveedorAuth.GOOGLE, SUB))
                .thenReturn(Optional.empty(), Optional.of(identidadDeOtro));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuarioDelCorreo));

        assertThrows(IdentidadGoogleConflictoException.class,
                () -> googleAuthService.autenticar(datosGoogle));
    }

    @Test
    void completarRegistro_datosValidos_creaUsuarioSinPasswordYIdentidadGoogle() {
        when(registroPendienteService.leerTicket("ticket")).thenReturn(datosGoogle);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByDni("12345678")).thenReturn(false);

        RolGlobal rol = RolGlobal.builder().idRolGlobal(1L).nombre("Usuario").codigo("USUARIO").build();
        Genero genero = Genero.builder().idGenero(1L).codigo("NO_DECIRLO").nombre("Prefiero no decirlo").build();
        when(rolGlobalRepository.findByNombre("Usuario")).thenReturn(Optional.of(rol));
        when(generoRepository.findByCodigo("NO_DECIRLO")).thenReturn(Optional.of(genero));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        CompletarRegistroGoogleRequest request = new CompletarRegistroGoogleRequest(
                "Ana", "Gómez", "12345678", LocalDate.now().minusYears(25), "NO_DECIRLO", null);

        AuthService.LoginResultado resultado = googleAuthService.completarRegistro("ticket", request);

        assertEquals(loginResultado, resultado);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario guardado = usuarioCaptor.getValue();
        assertNull(guardado.getPasswordHash());
        assertEquals(CORREO, guardado.getCorreo());
        assertEquals("12345678", guardado.getDni());
        assertEquals(EstadoUsuario.ACTIVO, guardado.getEstado());

        ArgumentCaptor<UsuarioIdentidad> identidadCaptor = ArgumentCaptor.forClass(UsuarioIdentidad.class);
        verify(usuarioIdentidadRepository).save(identidadCaptor.capture());
        assertEquals(ProveedorAuth.GOOGLE, identidadCaptor.getValue().getProveedor());
        assertEquals(SUB, identidadCaptor.getValue().getIdExterno());
    }

    @Test
    void completarRegistro_dniDuplicado_lanzaExcepcion() {
        when(registroPendienteService.leerTicket("ticket")).thenReturn(datosGoogle);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByDni("12345678")).thenReturn(true);

        CompletarRegistroGoogleRequest request = new CompletarRegistroGoogleRequest(
                "Ana", "Gómez", "12345678", LocalDate.now().minusYears(25), "NO_DECIRLO", null);

        assertThrows(DniDuplicadoException.class,
                () -> googleAuthService.completarRegistro("ticket", request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void completarRegistro_menorDeEdad_lanzaExcepcion() {
        when(registroPendienteService.leerTicket("ticket")).thenReturn(datosGoogle);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByDni("12345678")).thenReturn(false);

        CompletarRegistroGoogleRequest request = new CompletarRegistroGoogleRequest(
                "Ana", "Gómez", "12345678", LocalDate.now().minusYears(17), "NO_DECIRLO", null);

        assertThrows(EdadInvalidaException.class,
                () -> googleAuthService.completarRegistro("ticket", request));
        verify(usuarioRepository, never()).save(any());
    }

    private Usuario usuario(Long id, String correo) {
        return Usuario.builder()
                .idUsuario(id)
                .correo(correo)
                .nombre("Ana")
                .apellido("Gómez")
                .dni("12345678")
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .estado(EstadoUsuario.Activo)
                .build();
    }
}
