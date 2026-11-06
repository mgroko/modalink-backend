package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.servicio.ExpirarDeshabilitacionService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.UsuarioIdentidad;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.ProveedorAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test de integración de UsuarioRepository contra PostgreSQL real.
 *
 * El contenedor se comparte vía {@link AbstractPostgresIntegrationTest}.
 * A diferencia de los tests de Genero y RolGlobal, no hay usuarios
 * sembrados: cada test persiste sus propios datos, por lo que la clase
 * se anota con @Transactional para que cada método haga rollback y no
 * contamine al resto (la BD es compartida entre todas las clases).
 *
 * Valida las consultas derivadas (findByCorreo, existsByCorreo,
 * existsByDni), su case-sensitivity, y las constraints únicas
 * uq_usuario_correo / uq_usuario_dni del esquema migrado.
 */
@SpringBootTest
@Transactional
class UsuarioRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String CORREO = "juan.perez@example.com";
    private static final String DNI = "12345678";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioIdentidadRepository usuarioIdentidadRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private ExpirarDeshabilitacionService expirarDeshabilitacionService;

    /**
     * Construye un usuario válido según las constraints del esquema
     * (V2 exige nombre/apellido >= 2 caracteres y mayoría de edad),
     * resolviendo el rol y el género desde los datos sembrados.
     */
    private Usuario construirUsuario(String correo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        Ubicacion ubicacion = ubicacionRepository.findAll().stream().findFirst().orElse(null);

        return Usuario.builder()
                .nombre("Juan")
                .apellido("Perez")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(20))
                .correo(correo)
                .rolGlobal(rol)
                .genero(genero)
                .ubicacion(ubicacion)
                .build();
    }

    private Usuario guardarUsuarioBase() {
        return usuarioRepository.saveAndFlush(construirUsuario(CORREO, DNI));
    }

    @Test
    void findByCorreo_usuarioPersistido_encontrado() {
        Usuario guardado = guardarUsuarioBase();

        Optional<Usuario> resultado = usuarioRepository.findByCorreo(CORREO);

        assertTrue(resultado.isPresent());
        assertNotNull(guardado.getIdUsuario());
        assertEquals(CORREO, resultado.get().getCorreo());
        assertEquals(DNI, resultado.get().getDni());
        assertEquals(guardado.getIdUsuario(), resultado.get().getIdUsuario());
    }

    @Test
    void findByIdConRolYPermisos_cargaRolYPermisosDelAdministrador() {
        Usuario admin = usuarioRepository.findByCorreo("admin@modalink.com").orElseThrow();

        Usuario cargado = usuarioRepository.findByIdConRolYPermisos(admin.getIdUsuario()).orElseThrow();

        assertNotNull(cargado.getRolGlobal());
        assertEquals("Administrador", cargado.getRolGlobal().getNombre());
        assertFalse(cargado.getRolGlobal().getPermisos().isEmpty());
    }

    @Test
    void findByCorreo_correoInexistente_devuelveVacio() {
        Optional<Usuario> resultado = usuarioRepository.findByCorreo("nadie@example.com");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void findByCorreo_esCaseSensitive() {
        guardarUsuarioBase();

        Optional<Usuario> resultado = usuarioRepository.findByCorreo(CORREO.toUpperCase());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void existsByCorreo_correoExistente_devuelveTrue() {
        guardarUsuarioBase();

        assertTrue(usuarioRepository.existsByCorreo(CORREO));
    }

    @Test
    void existsByCorreo_correoInexistente_devuelveFalse() {
        assertFalse(usuarioRepository.existsByCorreo("nadie@example.com"));
    }

    @Test
    void existsByDni_dniExistente_devuelveTrue() {
        guardarUsuarioBase();

        assertTrue(usuarioRepository.existsByDni(DNI));
    }

    @Test
    void existsByDni_dniInexistente_devuelveFalse() {
        assertFalse(usuarioRepository.existsByDni("99999999"));
    }

    @Test
    void save_sinPasswordHash_persiste() {
        // password_hash es nullable (registro solo vía Google)
        Usuario google = construirUsuario("google.user@example.com", "87654321");

        assertDoesNotThrow(() -> usuarioRepository.saveAndFlush(google));

        assertTrue(usuarioRepository.existsByCorreo("google.user@example.com"));
    }

    @Test
    void save_correoDuplicado_lanzaExcepcionDeIntegridad() {
        guardarUsuarioBase();

        Usuario duplicado = construirUsuario(CORREO, "87654321");
        assertThrows(DataIntegrityViolationException.class,
                () -> usuarioRepository.saveAndFlush(duplicado));
    }

    @Test
    void save_dniDuplicado_lanzaExcepcionDeIntegridad() {
        guardarUsuarioBase();

        Usuario duplicado = construirUsuario("otro.correo@example.com", DNI);
        assertThrows(DataIntegrityViolationException.class,
                () -> usuarioRepository.saveAndFlush(duplicado));
    }

    // ------------------------------------------------------------------
    // UC-04: motivo y duración de la deshabilitación (migración V18)
    // ------------------------------------------------------------------

    @Test
    void save_deshabilitadoConMotivoYDuracion_persiste() {
        Usuario usuario = guardarUsuarioBase();
        usuario.setEstado(EstadoUsuario.Deshabilitado);
        usuario.setMotivoDeshabilitacion("Incumplimiento de normas");
        usuario.setFechaHastaDeshabilitacion(LocalDateTime.now().plusDays(7));
        usuarioRepository.saveAndFlush(usuario);

        Optional<Usuario> resultado = usuarioRepository.findById(usuario.getIdUsuario());

        assertTrue(resultado.isPresent());
        assertEquals(EstadoUsuario.Deshabilitado, resultado.get().getEstado());
        assertEquals("Incumplimiento de normas", resultado.get().getMotivoDeshabilitacion());
        assertNotNull(resultado.get().getFechaHastaDeshabilitacion());
    }

    @Test
    void findByEstadoAndFechaHastaDeshabilitacionBefore_devuelveVencidos() {
        Usuario vencido = guardarUsuarioBase();
        vencido.setEstado(EstadoUsuario.Deshabilitado);
        vencido.setMotivoDeshabilitacion("Motivo");
        vencido.setFechaHastaDeshabilitacion(LocalDateTime.now().minusDays(1));
        usuarioRepository.saveAndFlush(vencido);

        List<Usuario> vencidos = usuarioRepository
                .findByEstadoAndFechaHastaDeshabilitacionBefore(EstadoUsuario.Deshabilitado, LocalDateTime.now());

        assertEquals(1, vencidos.size());
        assertEquals(vencido.getIdUsuario(), vencidos.get(0).getIdUsuario());
    }

    @Test
    void reactivarVencidos_vencida_reactivaLaCuentaContraPostgres() {
        Usuario vencido = guardarUsuarioBase();
        vencido.setEstado(EstadoUsuario.Deshabilitado);
        vencido.setMotivoDeshabilitacion("Motivo");
        vencido.setFechaHastaDeshabilitacion(LocalDateTime.now().minusDays(1));
        usuarioRepository.saveAndFlush(vencido);

        int cantidad = expirarDeshabilitacionService.reactivarVencidos();

        assertEquals(1, cantidad);
        Optional<Usuario> resultado = usuarioRepository.findById(vencido.getIdUsuario());
        assertTrue(resultado.isPresent());
        assertEquals(EstadoUsuario.Activo, resultado.get().getEstado());
        assertNull(resultado.get().getMotivoDeshabilitacion());
        assertNull(resultado.get().getFechaHastaDeshabilitacion());
    }

    // ------------------------------------------------------------------
    // UC-09: usuario_identidad
    // ------------------------------------------------------------------

    @Test
    void save_identidadLocal_seEncuentraPorUsuarioYProveedor() {
        Usuario usuario = guardarUsuarioBase();
        UsuarioIdentidad identidad = usuarioIdentidadRepository.saveAndFlush(
                UsuarioIdentidad.builder()
                        .usuario(usuario)
                        .proveedor(ProveedorAuth.LOCAL)
                        .correo(usuario.getCorreo())
                        .correoVerificado(false)
                        .build());

        assertNotNull(identidad.getIdIdentidad());
        assertTrue(usuarioIdentidadRepository
                .findByUsuario_IdUsuarioAndProveedor(usuario.getIdUsuario(), ProveedorAuth.LOCAL)
                .isPresent());
    }

    @Test
    void save_identidadGoogle_seEncuentraPorSub() {
        Usuario usuario = guardarUsuarioBase();
        usuarioIdentidadRepository.saveAndFlush(UsuarioIdentidad.builder()
                .usuario(usuario)
                .proveedor(ProveedorAuth.GOOGLE)
                .idExterno("google-sub-1")
                .correo(usuario.getCorreo())
                .correoVerificado(true)
                .build());

        assertTrue(usuarioIdentidadRepository
                .findByProveedorAndIdExterno(ProveedorAuth.GOOGLE, "google-sub-1")
                .isPresent());
    }

    @Test
    void save_dosIdentidadesGoogleConMismoSub_lanzaExcepcion() {
        Usuario primero = guardarUsuarioBase();
        Usuario segundo = usuarioRepository.saveAndFlush(
                construirUsuario("segundo@example.com", "87654321"));

        usuarioIdentidadRepository.saveAndFlush(UsuarioIdentidad.builder()
                .usuario(primero)
                .proveedor(ProveedorAuth.GOOGLE)
                .idExterno("google-sub-1")
                .correo(primero.getCorreo())
                .correoVerificado(true)
                .build());

        assertThrows(DataIntegrityViolationException.class,
                () -> usuarioIdentidadRepository.saveAndFlush(UsuarioIdentidad.builder()
                        .usuario(segundo)
                        .proveedor(ProveedorAuth.GOOGLE)
                        .idExterno("google-sub-1")
                        .correo(segundo.getCorreo())
                        .correoVerificado(true)
                        .build()));
    }

    @Test
    void save_dosIdentidadesMismoUsuarioYProveedor_lanzaExcepcion() {
        Usuario usuario = guardarUsuarioBase();
        usuarioIdentidadRepository.saveAndFlush(UsuarioIdentidad.builder()
                .usuario(usuario)
                .proveedor(ProveedorAuth.LOCAL)
                .correo(usuario.getCorreo())
                .correoVerificado(false)
                .build());

        assertThrows(DataIntegrityViolationException.class,
                () -> usuarioIdentidadRepository.saveAndFlush(UsuarioIdentidad.builder()
                        .usuario(usuario)
                        .proveedor(ProveedorAuth.LOCAL)
                        .correo(usuario.getCorreo())
                        .correoVerificado(false)
                        .build()));
    }
}
