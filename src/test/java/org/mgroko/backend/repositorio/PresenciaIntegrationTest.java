package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.presencia.dto.PresenciaPerfilResponse;
import org.mgroko.backend.presencia.servicio.PresenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Presencia por perfil contra PostgreSQL real.
 */
@SpringBootTest
@Transactional
class PresenciaIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private ProfesionRepository profesionRepository;
    @Autowired
    private RolGlobalRepository rolGlobalRepository;
    @Autowired
    private GeneroRepository generoRepository;
    @Autowired
    private PresenciaService presenciaService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PerfilCreado perfilA;
    private PerfilCreado perfilB;

    @BeforeEach
    void setUp() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        perfilA = crearPerfil("A", rol, genero);
        perfilB = crearPerfil("B", rol, genero);
    }

    @Test
    void heartbeat_actualizaSoloElPerfilActivoYsnapshotLoRefleja() {
        presenciaService.registrarActividad(perfilA.idUsuario(), perfilA.idPerfil());

        LocalDateTime ultimaActividad = jdbcTemplate.queryForObject("""
                SELECT ultima_actividad FROM perfil WHERE id_perfil = ?
                """, LocalDateTime.class, perfilA.idPerfil());
        assertNotNull(ultimaActividad);

        List<PresenciaPerfilResponse> snapshot = presenciaService.snapshot(
                List.of(perfilA.idPerfil(), perfilB.idPerfil(), 999_999L));

        assertEquals(3, snapshot.size());
        assertTrue(snapshot.get(0).enLinea());
        assertFalse(snapshot.get(1).enLinea());
        assertFalse(snapshot.get(2).enLinea());
    }

    @Test
    void snapshot_usuarioInactivoQuedaDesconectado() {
        presenciaService.registrarActividad(perfilA.idUsuario(), perfilA.idPerfil());

        jdbcTemplate.update("""
                UPDATE usuario SET estado = 'PENDIENTE_BAJA' WHERE id_usuario = ?
                """, perfilA.idUsuario());

        List<PresenciaPerfilResponse> snapshot = presenciaService.snapshot(
                List.of(perfilA.idPerfil()));

        assertFalse(snapshot.get(0).enLinea());
    }

    @Test
    void heartbeat_noGeneraAuditoria() {
        Integer antes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auditoria
                WHERE entidad_afectada = 'perfil' AND id_afectado = ?
                """, Integer.class, perfilA.idPerfil());

        presenciaService.registrarActividad(perfilA.idUsuario(), perfilA.idPerfil());

        Integer despues = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auditoria
                WHERE entidad_afectada = 'perfil' AND id_afectado = ?
                """, Integer.class, perfilA.idPerfil());

        assertEquals(antes, despues);
    }

    @Test
    void estaEnLinea_fueraDelUmbral_esFalso() {
        presenciaService.registrarActividad(perfilA.idUsuario(), perfilA.idPerfil());

        Perfil perfil = perfilRepository.findById(perfilA.idPerfil()).orElseThrow();
        LocalDateTime ahora = LocalDateTime.now().plusSeconds(121);

        assertFalse(presenciaService.estaEnLinea(perfil, ahora));
    }

    private PerfilCreado crearPerfil(String etiqueta, RolGlobal rol, Genero genero) {
        int n = SECUENCIA.incrementAndGet();
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Presencia " + etiqueta)
                .apellido("Prueba")
                .dni(String.format("%08d", 94_000_000 + n))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("presencia." + etiqueta + n + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = profesionRepository.saveAndFlush(Profesion.builder()
                .codigo("PRESENCIA_" + etiqueta + n)
                .nombre("Profesion presencia " + etiqueta + n)
                .build());

        Perfil perfil = perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico("Perfil presencia " + etiqueta)
                .biografia("Perfil de prueba de presencia")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .profesion(profesion)
                .build());

        return new PerfilCreado(usuario.getIdUsuario(), perfil.getIdPerfil());
    }

    private record PerfilCreado(Long idUsuario, Long idPerfil) {
    }
}
