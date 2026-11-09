package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.conexiones.dto.EnviarSolicitudConexionRequest;
import org.mgroko.backend.conexiones.dto.SolicitudConexionGestionadaResponse;
import org.mgroko.backend.conexiones.dto.SolicitudConexionResponse;
import org.mgroko.backend.conexiones.exception.ConexionYaExistenteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionDuplicadaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionInvalidaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPendienteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPermitidaException;
import org.mgroko.backend.conexiones.modelo.EstadoRelacionPerfil;
import org.mgroko.backend.conexiones.modelo.RelacionPerfil;
import org.mgroko.backend.conexiones.servicio.CancelarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.EnviarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.GestionarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.RelacionPerfilService;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoConexion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.dto.PerfilDetalleResponse;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-43 contra PostgreSQL real: alta de solicitud, duplicados en ambas
 * direcciones, notificación y estado de relación en el detalle del perfil.
 */
@SpringBootTest
@Transactional
class SolicitudConexionIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private SolicitudConexionRepository solicitudConexionRepository;

    @Autowired
    private ConexionRepository conexionRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private EnviarSolicitudConexionService enviarSolicitudConexionService;

    @Autowired
    private CancelarSolicitudConexionService cancelarSolicitudConexionService;

    @Autowired
    private GestionarSolicitudConexionService gestionarSolicitudConexionService;

    @Autowired
    private RelacionPerfilService relacionPerfilService;

    @Autowired
    private VerPerfilService verPerfilService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Usuario usuarioA;
    private Usuario usuarioB;
    private Perfil perfilA;
    private Perfil perfilB;

    @BeforeEach
    void setUp() {
        int n = SECUENCIA.incrementAndGet();
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        usuarioA = usuarioRepository.saveAndFlush(crearUsuario("A", n, rol, genero));
        usuarioB = usuarioRepository.saveAndFlush(crearUsuario("B", n, rol, genero));

        perfilA = perfilRepository.saveAndFlush(crearPerfil(usuarioA, "Perfil A", n, "A"));
        perfilB = perfilRepository.saveAndFlush(crearPerfil(usuarioB, "Perfil B", n, "B"));
    }

    @Test
    void enviarSolicitud_creaPendienteYNotificacion() {
        SolicitudConexionResponse response = enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(),
                perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        assertNotNull(response.idSolicitudConexion());
        assertEquals(EstadoSolicitud.PENDIENTE, response.estado());
        assertEquals(perfilA.getIdPerfil(), response.remitente().idPerfil());
        assertEquals(perfilB.getIdPerfil(), response.destinatario().idPerfil());

        SolicitudConexion solicitud = solicitudConexionRepository
                .findById(response.idSolicitudConexion())
                .orElseThrow();
        assertEquals(EstadoSolicitud.PENDIENTE, solicitud.getEstado());
        assertEquals(perfilA.getIdPerfil(), solicitud.getPerfilRemitente().getIdPerfil());

        long notificaciones = notificacionRepository
                .countByUsuarioDestino_IdUsuario(usuarioB.getIdUsuario());
        assertTrue(notificaciones >= 1);
        assertTrue(notificacionRepository.findAll().stream()
                .anyMatch(n -> NotificacionService.TIPO_SOLICITUD_CONEXION_RECIBIDA
                        .equals(n.getTipo())
                        && response.idSolicitudConexion().equals(n.getEntidadId())));
    }

    @Test
    void enviarSolicitud_duplicadaMismaDireccion_lanzaDuplicada() {
        enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        assertThrows(SolicitudConexionDuplicadaException.class, () ->
                enviarSolicitudConexionService.enviar(
                        usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                        new EnviarSolicitudConexionRequest(perfilB.getIdPerfil())));
    }

    @Test
    void enviarSolicitud_duplicadaDireccionInversa_lanzaDuplicada() {
        enviarSolicitudConexionService.enviar(
                usuarioB.getIdUsuario(), perfilB.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilA.getIdPerfil()));

        assertThrows(SolicitudConexionDuplicadaException.class, () ->
                enviarSolicitudConexionService.enviar(
                        usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                        new EnviarSolicitudConexionRequest(perfilB.getIdPerfil())));
    }

    @Test
    void enviarSolicitud_mismoPerfil_lanzaInvalida() {
        assertThrows(SolicitudConexionInvalidaException.class, () ->
                enviarSolicitudConexionService.enviar(
                        usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                        new EnviarSolicitudConexionRequest(perfilA.getIdPerfil())));
    }

    @Test
    void enviarSolicitud_dosPerfilesDelMismoUsuario_lanzaInvalida() {
        Perfil segundoPerfilA = perfilRepository.saveAndFlush(
                crearPerfil(usuarioA, "Perfil A2", SECUENCIA.incrementAndGet(), "A2"));

        assertThrows(SolicitudConexionInvalidaException.class, () ->
                enviarSolicitudConexionService.enviar(
                        usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                        new EnviarSolicitudConexionRequest(segundoPerfilA.getIdPerfil())));
    }

    @Test
    void enviarSolicitud_destinatarioInactivo_lanzaPerfilNoEncontrado() {
        perfilB.setEstado(EstadoPerfil.PendienteBaja);
        perfilRepository.saveAndFlush(perfilB);

        assertThrows(org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException.class, () ->
                enviarSolicitudConexionService.enviar(
                        usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                        new EnviarSolicitudConexionRequest(perfilB.getIdPerfil())));
    }

    @Test
    void enviarSolicitud_remitenteInactivo_lanzaPerfilEnBaja() {
        perfilA.setEstado(EstadoPerfil.PendienteBaja);
        perfilRepository.saveAndFlush(perfilA);

        assertThrows(PerfilEnBajaException.class, () ->
                enviarSolicitudConexionService.enviar(
                        usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                        new EnviarSolicitudConexionRequest(perfilB.getIdPerfil())));
    }

    @Test
    void enviarSolicitud_conConexionActiva_lanzaConexionYaExistente() {
        crearConexionAceptada();

        assertThrows(ConexionYaExistenteException.class, () ->
                enviarSolicitudConexionService.enviar(
                        usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                        new EnviarSolicitudConexionRequest(perfilB.getIdPerfil())));
    }

    @Test
    void relacionPerfil_reflejaSolicitudEnviadaYRecibida() {
        enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        RelacionPerfil desdeA = relacionPerfilService.obtener(
                perfilA.getIdPerfil(), perfilB.getIdPerfil());
        RelacionPerfil desdeB = relacionPerfilService.obtener(
                perfilB.getIdPerfil(), perfilA.getIdPerfil());

        assertEquals(EstadoRelacionPerfil.SOLICITUD_ENVIADA, desdeA.estado());
        assertEquals(EstadoRelacionPerfil.SOLICITUD_RECIBIDA, desdeB.estado());
        assertNotNull(desdeB.idSolicitudConexion());
    }

    @Test
    void detallePerfil_incluyeEstadoDeRelacion() {
        enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        PerfilDetalleResponse detalle = verPerfilService.obtenerDetalle(
                perfilB.getIdPerfil(), usuarioA.getIdUsuario(), perfilA.getIdPerfil());

        assertEquals("SOLICITUD_ENVIADA", detalle.estadoConexion());
        assertNotNull(detalle.idSolicitudConexion());
    }

    @Test
    void cancelarSolicitud_propiaPendiente_eliminaFilaYNotificacionYPermiteReenviar() {
        SolicitudConexionResponse creada = enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        assertEquals(1, contarNotificacionesDeSolicitud(creada.idSolicitudConexion()));

        cancelarSolicitudConexionService.cancelar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                creada.idSolicitudConexion());

        assertEquals(0, contarSolicitudes(creada.idSolicitudConexion()));
        assertEquals(0, contarNotificacionesDeSolicitud(creada.idSolicitudConexion()));

        SolicitudConexionResponse reenviada = enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        assertNotEquals(creada.idSolicitudConexion(), reenviada.idSolicitudConexion());
        assertEquals(EstadoSolicitud.PENDIENTE, reenviada.estado());
    }

    @Test
    void cancelarSolicitud_porElDestinatario_lanzaNoPermitida() {
        SolicitudConexionResponse creada = enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        assertThrows(SolicitudConexionNoPermitidaException.class, () ->
                cancelarSolicitudConexionService.cancelar(
                        usuarioB.getIdUsuario(), perfilB.getIdPerfil(),
                        creada.idSolicitudConexion()));
    }

    @Test
    void cancelarSolicitud_aceptada_lanzaNoPendiente() {
        SolicitudConexion aceptada = crearConexionAceptada();

        assertThrows(SolicitudConexionNoPendienteException.class, () ->
                cancelarSolicitudConexionService.cancelar(
                        usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                        aceptada.getIdSolicitudConexion()));
    }

    @Test
    void aceptarSolicitud_creaConexionNotificaYAudita() {
        SolicitudConexionResponse creada = enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        SolicitudConexionGestionadaResponse respuesta =
                gestionarSolicitudConexionService.aceptar(
                        usuarioB.getIdUsuario(),
                        perfilB.getIdPerfil(),
                        creada.idSolicitudConexion());

        assertEquals(EstadoSolicitud.ACEPTADA, respuesta.estado());
        assertNotNull(respuesta.idConexion());

        Conexion conexion = conexionRepository
                .findActivaEntre(perfilA.getIdPerfil(), perfilB.getIdPerfil())
                .orElseThrow();
        assertEquals(respuesta.idConexion(), conexion.getIdConexion());
        assertEquals(creada.idSolicitudConexion(),
                conexion.getSolicitudOrigen().getIdSolicitudConexion());
        assertNotNull(conexion.getFechaInicioConexion());

        assertEquals(1, contarNotificaciones(
                usuarioA.getIdUsuario(),
                NotificacionService.TIPO_SOLICITUD_CONEXION_ACEPTADA,
                conexion.getIdConexion()));
        assertEquals(1, contarNotificaciones(
                usuarioB.getIdUsuario(),
                NotificacionService.TIPO_SOLICITUD_CONEXION_ACEPTADA,
                conexion.getIdConexion()));
        assertEquals(1, contarNotificaciones(
                usuarioB.getIdUsuario(),
                NotificacionService.TIPO_SOLICITUD_CONEXION_RECIBIDA,
                creada.idSolicitudConexion()));
        assertEquals(1, contarAuditoria("CONEXION_CREADA", conexion.getIdConexion()));
    }

    @Test
    void rechazarSolicitud_soloNotificaAlRemitenteYNoAudita() {
        SolicitudConexionResponse creada = enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        SolicitudConexionGestionadaResponse respuesta =
                gestionarSolicitudConexionService.rechazar(
                        usuarioB.getIdUsuario(),
                        perfilB.getIdPerfil(),
                        creada.idSolicitudConexion());

        assertEquals(EstadoSolicitud.RECHAZADA, respuesta.estado());
        assertNull(respuesta.idConexion());
        assertTrue(conexionRepository
                .findActivaEntre(perfilA.getIdPerfil(), perfilB.getIdPerfil())
                .isEmpty());

        assertEquals(1, contarNotificaciones(
                usuarioA.getIdUsuario(),
                NotificacionService.TIPO_SOLICITUD_CONEXION_RECHAZADA,
                creada.idSolicitudConexion()));
        assertEquals(0, contarNotificaciones(
                usuarioB.getIdUsuario(),
                NotificacionService.TIPO_SOLICITUD_CONEXION_RECHAZADA,
                creada.idSolicitudConexion()));
        assertEquals(1, contarNotificaciones(
                usuarioB.getIdUsuario(),
                NotificacionService.TIPO_SOLICITUD_CONEXION_RECIBIDA,
                creada.idSolicitudConexion()));
        assertEquals(0, contarAuditoriaConexiones());
    }

    @Test
    void aceptarSolicitud_porElRemitente_lanzaNoPermitida() {
        SolicitudConexionResponse creada = enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        assertThrows(SolicitudConexionNoPermitidaException.class, () ->
                gestionarSolicitudConexionService.aceptar(
                        usuarioA.getIdUsuario(),
                        perfilA.getIdPerfil(),
                        creada.idSolicitudConexion()));
    }

    @Test
    void aceptarSolicitud_yaAceptada_lanzaNoPendiente() {
        SolicitudConexionResponse creada = enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        gestionarSolicitudConexionService.aceptar(
                usuarioB.getIdUsuario(), perfilB.getIdPerfil(),
                creada.idSolicitudConexion());

        assertThrows(SolicitudConexionNoPendienteException.class, () ->
                gestionarSolicitudConexionService.aceptar(
                        usuarioB.getIdUsuario(),
                        perfilB.getIdPerfil(),
                        creada.idSolicitudConexion()));
    }

    @Test
    void aceptarSolicitud_remitenteInactivo_lanzaPerfilEnBaja() {
        SolicitudConexionResponse creada = enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        perfilA.setEstado(EstadoPerfil.PendienteBaja);
        perfilRepository.saveAndFlush(perfilA);

        assertThrows(PerfilEnBajaException.class, () ->
                gestionarSolicitudConexionService.aceptar(
                        usuarioB.getIdUsuario(),
                        perfilB.getIdPerfil(),
                        creada.idSolicitudConexion()));
    }

    @Test
    void eliminarLogicamenteConexion_auditaBaja() {
        SolicitudConexionResponse creada = enviarSolicitudConexionService.enviar(
                usuarioA.getIdUsuario(), perfilA.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.getIdPerfil()));

        SolicitudConexionGestionadaResponse aceptada =
                gestionarSolicitudConexionService.aceptar(
                        usuarioB.getIdUsuario(),
                        perfilB.getIdPerfil(),
                        creada.idSolicitudConexion());

        Conexion conexion = conexionRepository
                .findById(aceptada.idConexion())
                .orElseThrow();
        conexion.setEstado(EstadoConexion.ELIMINADA);
        conexion.setFechaFinConexion(LocalDateTime.now());
        conexionRepository.saveAndFlush(conexion);

        assertEquals(1, contarAuditoria(
                "CONEXION_ELIMINADA", conexion.getIdConexion()));
    }

    @Test
    void baseDeDatos_rechazaSolicitudEntrePerfilesDelMismoUsuario() {
        Perfil segundoPerfilA = perfilRepository.saveAndFlush(
                crearPerfil(usuarioA, "Perfil A3", SECUENCIA.incrementAndGet(), "A3"));

        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update("""
                    INSERT INTO solicitud_conexion
                        (id_perfil_remitente, id_perfil_destinatario, estado)
                    VALUES (?, ?, 'PENDIENTE')
                    """,
                        perfilA.getIdPerfil(), segundoPerfilA.getIdPerfil()));
    }

    @Test
    void baseDeDatos_rechazaDosSolicitudesPendientesEnLaMismaDireccion() {
        jdbcTemplate.update("""
            INSERT INTO solicitud_conexion
                (id_perfil_remitente, id_perfil_destinatario, estado)
            VALUES (?, ?, 'PENDIENTE')
            """, perfilA.getIdPerfil(), perfilB.getIdPerfil());

        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update("""
                    INSERT INTO solicitud_conexion
                        (id_perfil_remitente, id_perfil_destinatario, estado)
                    VALUES (?, ?, 'PENDIENTE')
                    """, perfilA.getIdPerfil(), perfilB.getIdPerfil()));
    }

    @Test
    void baseDeDatos_rechazaDosSolicitudesPendientesEnDireccionInversa() {
        jdbcTemplate.update("""
            INSERT INTO solicitud_conexion
                (id_perfil_remitente, id_perfil_destinatario, estado)
            VALUES (?, ?, 'PENDIENTE')
            """, perfilA.getIdPerfil(), perfilB.getIdPerfil());

        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update("""
                    INSERT INTO solicitud_conexion
                        (id_perfil_remitente, id_perfil_destinatario, estado)
                    VALUES (?, ?, 'PENDIENTE')
                    """, perfilB.getIdPerfil(), perfilA.getIdPerfil()));
    }

    private int contarNotificaciones(
            Long idUsuarioDestino,
            String tipo,
            Long entidadId) {

        Integer cantidad = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = ?
                  AND entidad_id = ?
                """,
                Integer.class,
                idUsuarioDestino,
                tipo,
                entidadId);
        return cantidad != null ? cantidad : 0;
    }

    private int contarAuditoria(String tipoEvento, Long idAfectado) {
        Integer cantidad = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auditoria
                WHERE tipo_evento = ?
                  AND id_afectado = ?
                """,
                Integer.class,
                tipoEvento,
                idAfectado);
        return cantidad != null ? cantidad : 0;
    }

    private int contarAuditoriaConexiones() {
        Integer cantidad = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auditoria
                WHERE tipo_evento IN ('CONEXION_CREADA', 'CONEXION_ELIMINADA')
                """,
                Integer.class);
        return cantidad != null ? cantidad : 0;
    }

    private int contarSolicitudes(Long idSolicitudConexion) {
        Integer cantidad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM solicitud_conexion WHERE id_solicitud_conexion = ?",
                Integer.class,
                idSolicitudConexion);
        return cantidad != null ? cantidad : 0;
    }

    private int contarNotificacionesDeSolicitud(Long idSolicitudConexion) {
        Integer cantidad = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM notificacion
                WHERE tipo = ?
                  AND entidad_tipo = ?
                  AND entidad_id = ?
                """,
                Integer.class,
                NotificacionService.TIPO_SOLICITUD_CONEXION_RECIBIDA,
                NotificacionService.ENTIDAD_SOLICITUD_CONEXION,
                idSolicitudConexion);
        return cantidad != null ? cantidad : 0;
    }

    private SolicitudConexion crearConexionAceptada() {
        SolicitudConexion solicitud = solicitudConexionRepository.saveAndFlush(
                SolicitudConexion.builder()
                        .perfilRemitente(perfilA)
                        .perfilDestinatario(perfilB)
                        .estado(EstadoSolicitud.PENDIENTE)
                        .fechaEnvio(LocalDateTime.now())
                        .build());

        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudConexionRepository.saveAndFlush(solicitud);

        Long idMenor = Math.min(perfilA.getIdPerfil(), perfilB.getIdPerfil());
        Perfil perfilMenor = idMenor.equals(perfilA.getIdPerfil()) ? perfilA : perfilB;
        Perfil perfilMayor = idMenor.equals(perfilA.getIdPerfil()) ? perfilB : perfilA;

        conexionRepository.saveAndFlush(Conexion.builder()
                .perfil1(perfilMenor)
                .perfil2(perfilMayor)
                .solicitudOrigen(solicitud)
                .estado(EstadoConexion.ACTIVA)
                .fechaInicioConexion(LocalDateTime.now())
                .build());

        return solicitud;
    }

    private Usuario crearUsuario(String etiqueta, int n, RolGlobal rol, Genero genero) {
        int dni = n * 10 + ("A".equals(etiqueta) ? 1 : 2);

        return Usuario.builder()
                .nombre("Usuario " + etiqueta + n)
                .apellido("Conexion")
                .dni(String.format("%08d", dni))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("conexion-" + etiqueta + n + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build();
    }

    private Perfil crearPerfil(Usuario usuario, String nombre, int n, String sufijo) {
        Profesion profesion = profesionRepository.saveAndFlush(Profesion.builder()
                .codigo("PROF_CONEXION_" + sufijo + "_" + n)
                .nombre("Profesion Conexion " + sufijo + " " + n)
                .build());

        return Perfil.builder()
                .nombreArtistico(nombre)
                .biografia("Perfil de prueba de conexiones")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
    }
}
