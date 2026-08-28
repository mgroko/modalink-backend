package org.mgroko.backend.auth;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.auth.dto.AuthResponse;
import org.mgroko.backend.auth.dto.LoginRequest;
import org.mgroko.backend.auth.dto.RegistroRequest;
import org.mgroko.backend.auth.dto.UsuarioResponse;
import org.mgroko.backend.auth.exception.CorreoDuplicadoException;
import org.mgroko.backend.auth.exception.CredencialesInvalidasException;
import org.mgroko.backend.auth.exception.DniDuplicadoException;
import org.mgroko.backend.auth.exception.EdadInvalidaException;
import org.mgroko.backend.auth.exception.GeneroNoEncontradoException;
import org.mgroko.backend.auth.exception.RolGlobalNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioDeshabilitadoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.GeneroRepository;
import org.mgroko.backend.repositorio.RolGlobalRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Tests unitarios de AuthService.
 *
 * Estrategia: mockeamos TODAS las dependencias externas (repositorios,
 * encoder) para probar únicamente la lógica que vive dentro de AuthService,
 * sin tocar una base de datos real. Cada método de test cubre una única
 * rama del if/else de registrar() o login().
 */

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolGlobalRepository rolGlobalRepository;

    @Mock
    private GeneroRepository generoRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegistroRequest registroValido;

    @BeforeEach
    void setUp() {
        // Se recrea antes de CADA test, así ninguno contamina a otro.
        registroValido = new RegistroRequest(
                "Maria",
                "Flores",
                "12345678",
                LocalDate.of(1981, Month.JUNE, 24), 
                "maria.flores@test.com",
                "mujer",
                "password123"
        );
    }

    // ---------------------------------------------------------------
    // registrar()
    // ---------------------------------------------------------------

    @Test
    void registrar_correoDuplicado_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.existsByCorreo(registroValido.correo())).thenReturn(true);

        // Act + Assert
        assertThrows(CorreoDuplicadoException.class, () -> authService.registrar(registroValido));

        // Verificamos que, al fallar temprano, NUNCA se intenta guardar nada.
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_dniDuplicado_lanzaExcepcion() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(usuarioRepository.existsByDni(registroValido.dni())).thenReturn(true);

        assertThrows(DniDuplicadoException.class, () -> authService.registrar(registroValido));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_menorDeEdad_lanzaExcepcion() {
        RegistroRequest menorDeEdad = new RegistroRequest(
                "Ana", "Gomez", "87654321",
                LocalDate.now().minusYears(10), // 10 años: inválido
                "ana@test.com", "mujer", "password123"
        );
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(usuarioRepository.existsByDni(anyString())).thenReturn(false);

        assertThrows(EdadInvalidaException.class, () -> authService.registrar(menorDeEdad));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_rolGlobalNoEncontrado_lanzaExcepcion() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(usuarioRepository.existsByDni(anyString())).thenReturn(false);
        when(rolGlobalRepository.findByNombre("Usuario")).thenReturn(Optional.empty());

        assertThrows(RolGlobalNoEncontradoException.class, () -> authService.registrar(registroValido));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_generoInexistente_lanzaExcepcion() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(usuarioRepository.existsByDni(anyString())).thenReturn(false);
        when(rolGlobalRepository.findByNombre("Usuario")).thenReturn(Optional.of(mock(RolGlobal.class)));
        when(generoRepository.findByCodigo(registroValido.genero())).thenReturn(Optional.empty());

        assertThrows(GeneroNoEncontradoException.class, () -> authService.registrar(registroValido));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_datosValidos_creaUsuarioCorrectamente() {
        // Arrange: todos los checks pasan
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(usuarioRepository.existsByDni(anyString())).thenReturn(false);

        RolGlobal rolUsuario = mock(RolGlobal.class);
        when(rolUsuario.getNombre()).thenReturn("Usuario");
        when(rolGlobalRepository.findByNombre("Usuario")).thenReturn(Optional.of(rolUsuario));

        Genero generoMujer = mock(Genero.class);
        when(generoMujer.getCodigo()).thenReturn("mujer");
        when(generoRepository.findByCodigo("mujer")).thenReturn(Optional.of(generoMujer));

        when(passwordEncoder.encode(registroValido.password())).thenReturn("hash-simulado");

        // save() devuelve el mismo objeto que le pasaron (simula la persistencia)
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        UsuarioResponse response = authService.registrar(registroValido);

        // Assert: los datos del response son los que esperábamos
        assertEquals("Maria", response.nombre());
        assertEquals("Flores", response.apellido());
        assertEquals("maria.flores@test.com", response.correo());
        assertEquals("Usuario", response.rolGlobal());
        assertEquals("mujer", response.genero());

        // Verificamos que la password NUNCA se guarda en texto plano
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void registrar_normalizaCampos_antesDeGuardar() {
        // Los campos de texto se guardan trimmeados y el correo en minúsculas,
        // tal como indica la política de normalización del registro.
        RegistroRequest conRuido = new RegistroRequest(
                "  Maria  ", " Flores ", " 12345678 ",
                LocalDate.of(1981, Month.JUNE, 24),
                "  Maria.Flores@Test.COM ", "mujer", "password123"
        );
        when(usuarioRepository.existsByCorreo("maria.flores@test.com")).thenReturn(false);
        when(usuarioRepository.existsByDni("12345678")).thenReturn(false);
        when(rolGlobalRepository.findByNombre("Usuario"))
                .thenReturn(Optional.of(mock(RolGlobal.class)));
        Genero generoMujer = mock(Genero.class);
        when(generoRepository.findByCodigo("mujer")).thenReturn(Optional.of(generoMujer));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-simulado");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        authService.registrar(conRuido);

        // Los checks de duplicados también usan los valores normalizados
        verify(usuarioRepository).existsByCorreo("maria.flores@test.com");
        verify(usuarioRepository).existsByDni("12345678");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals("Maria", guardado.getNombre());
        assertEquals("Flores", guardado.getApellido());
        assertEquals("12345678", guardado.getDni());
        assertEquals("maria.flores@test.com", guardado.getCorreo());
    }

    @Test
    void registrar_generoNoCoincideExacto_lanzaExcepcion() {
        // Decisión de diseño: el código de género es case-sensitive y sin espacios
        // (match exacto contra la tabla genero). "MUJER" no es "mujer".
        RegistroRequest mayusculas = new RegistroRequest(
                "Maria", "Flores", "12345678",
                LocalDate.of(1981, Month.JUNE, 24),
                "maria.flores@test.com", "MUJER", "password123"
        );
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(usuarioRepository.existsByDni(anyString())).thenReturn(false);
        when(rolGlobalRepository.findByNombre("Usuario"))
                .thenReturn(Optional.of(mock(RolGlobal.class)));
        when(generoRepository.findByCodigo("MUJER")).thenReturn(Optional.empty());

        assertThrows(GeneroNoEncontradoException.class, () -> authService.registrar(mayusculas));
        verify(usuarioRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // login()
    // ---------------------------------------------------------------

    @Test
    void login_correoNoExiste_lanzaExcepcion() {
        LoginRequest request = new LoginRequest("noexiste@test.com", "cualquierPassword");
        when(usuarioRepository.findByCorreo(request.correo())).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
    }

    @Test
    void login_normalizaCorreo_antesDeBuscar() {
        // El correo se busca trimmeado y en minúsculas, tal como quedó guardado
        // al registrarse. Si el servicio buscara con el valor crudo del request,
        // este test falla (el stub no coincidiría).
        LoginRequest request = new LoginRequest("  Maria.Flores@Test.COM ", "password123");
        when(usuarioRepository.findByCorreo("maria.flores@test.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));

        ArgumentCaptor<String> correoBuscado = ArgumentCaptor.forClass(String.class);
        verify(usuarioRepository).findByCorreo(correoBuscado.capture());
        assertEquals("maria.flores@test.com", correoBuscado.getValue());
    }

    @Test
    void login_passwordHashNulo_lanzaExcepcion() {
        // Caso real: usuario registrado con Google, sin password local
        LoginRequest request = new LoginRequest("maria.flores@test.com", "cualquierPassword");
        Usuario usuarioSinPassword = Usuario.builder()
                .correo("maria.flores@test.com")
                .passwordHash(null)
                .build();
        when(usuarioRepository.findByCorreo(request.correo())).thenReturn(Optional.of(usuarioSinPassword));

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
    }

    @Test
    void login_passwordIncorrecta_lanzaExcepcion() {
        LoginRequest request = new LoginRequest("maria.flores@test.com", "passwordMala");
        Usuario usuario = Usuario.builder()
                .correo("maria.flores@test.com")
                .passwordHash("hash-guardado")
                .build();

        when(usuarioRepository.findByCorreo(request.correo())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("passwordMala", "hash-guardado")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
    }

    @Test
    void login_credencialesValidas_devuelveAuthResponse() {
        LoginRequest request = new LoginRequest("maria.flores@test.com", "password123");

        RolGlobal rolUsuario = mock(RolGlobal.class);
        when(rolUsuario.getNombre()).thenReturn("Usuario");

        Usuario usuario = Usuario.builder()
                .nombre("Maria")
                .correo("maria.flores@test.com")
                .passwordHash("hash-guardado")
                .rolGlobal(rolUsuario)
                .build();

        when(usuarioRepository.findByCorreo(request.correo())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password123", "hash-guardado")).thenReturn(true);

        AuthResponse response = authService.login(request);

        assertEquals("maria.flores@test.com", response.usuario().correo());
        assertEquals("Usuario", response.usuario().rolGlobal());
    }

    @Test
    void login_usuarioDeshabilitado_lanzaExcepcion() {
        LoginRequest request = new LoginRequest("maria.flores@test.com", "password123");

        RolGlobal rolUsuario = mock(RolGlobal.class);

        Usuario usuarioDeshabilitado = Usuario.builder()
                .nombre("Maria")
                .correo("maria.flores@test.com")
                .passwordHash("hash-guardado")
                .rolGlobal(rolUsuario)
                .estado(EstadoUsuario.Deshabilitado)
                .build();

        when(usuarioRepository.findByCorreo(request.correo())).thenReturn(Optional.of(usuarioDeshabilitado));
        when(passwordEncoder.matches("password123", "hash-guardado")).thenReturn(true);

        assertThrows(UsuarioDeshabilitadoException.class, () -> authService.login(request));
    }

    @Test
    void obtenerUsuarioActual_usuarioNoExiste_lanzaExcepcion() {
        Long idInexistente = 999L;
        when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> authService.obtenerUsuarioActual(idInexistente));
    }

    @Test
    void login_usuarioPendienteBaja_permiteLogin() {
        LoginRequest request = new LoginRequest("maria.flores@test.com", "password123");

        RolGlobal rolUsuario = mock(RolGlobal.class);
        when(rolUsuario.getNombre()).thenReturn("Usuario");

        Usuario usuario = Usuario.builder()
                .nombre("Maria")
                .correo("maria.flores@test.com")
                .passwordHash("hash-guardado")
                .estado(EstadoUsuario.PendienteBaja)
                .rolGlobal(rolUsuario)
                .build();

        when(usuarioRepository.findByCorreo(request.correo())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password123", "hash-guardado")).thenReturn(true);

        AuthResponse response = authService.login(request);

        assertEquals("maria.flores@test.com", response.usuario().correo());
        assertEquals("Usuario", response.usuario().rolGlobal());
    }

}
