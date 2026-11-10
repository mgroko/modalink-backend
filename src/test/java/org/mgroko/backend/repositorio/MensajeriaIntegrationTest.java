package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.conexiones.dto.EnviarSolicitudConexionRequest;
import org.mgroko.backend.conexiones.dto.SolicitudConexionGestionadaResponse;
import org.mgroko.backend.conexiones.dto.SolicitudConexionResponse;
import org.mgroko.backend.conexiones.servicio.EliminarConexionService;
import org.mgroko.backend.conexiones.servicio.EnviarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.GestionarSolicitudConexionService;
import org.mgroko.backend.mensajeria.dto.ConversacionResponse;
import org.mgroko.backend.mensajeria.dto.MensajeResponse;
import org.mgroko.backend.mensajeria.exception.ConversacionNoEncontradaException;
import org.mgroko.backend.mensajeria.exception.ConversacionNoPermitidaException;
import org.mgroko.backend.mensajeria.exception.MensajeNoPermitidoException;
import org.mgroko.backend.mensajeria.servicio.MensajeriaService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-42 contra PostgreSQL real: tablas, triggers, notificación única y lectura.
 */
@SpringBootTest
@Transactional
class MensajeriaIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private EnviarSolicitudConexionService enviarSolicitudConexionService;
    @Autowired
    private GestionarSolicitudConexionService gestionarSolicitudConexionService;
    @Autowired
    private EliminarConexionService eliminarConexionService;
    @Autowired
    private MensajeriaService mensajeriaService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PerfilCreado perfilA;
    private PerfilCreado perfilB;
    private PerfilCreado perfilC;
    private Long idConexionAB;
    private Long idConversacion;

    @BeforeEach
    void setUp() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        perfilA = crearPerfil("A", "Ana", "Pérez", "Ana Moda", "Modelo", rol, genero);
        perfilB = crearPerfil("B", "Bruno", "Gómez", "Bruno Estilo", "Fotógrafo", rol, genero);
        perfilC = crearPerfil("C", "Carla", "Núñez", "Carla Diseño", "Diseñadora", rol, genero);

        idConexionAB = crearConexion(perfilA, perfilB);
        idConversacion = mensajeriaService.crearConversacion(
                perfilA.idUsuario(), perfilA.idPerfil(), perfilB.idPerfil())
                .conversacion().idConversacion();
    }

    @Test
    void crearConversacion_sinConexionActiva_lanzaNoPermitida() {
        assertThrows(ConversacionNoPermitidaException.class, () ->
                mensajeriaService.crearConversacion(
                        perfilA.idUsuario(), perfilA.idPerfil(), perfilC.idPerfil()));
    }

    @Test
    void enviarMensaje_creaMensajeYNotificacionUnica() {
        MensajeResponse primero = mensajeriaService.enviarMensaje(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, "Hola Bruno");

        assertEquals("Hola Bruno", primero.contenido());
        assertTrue(primero.esMio());
        assertFalse(primero.leido());

        MensajeResponse segundo = mensajeriaService.enviarMensaje(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, "Otro mensaje");

        assertEquals(idConversacion, segundo.idConversacion());

        Integer mensajes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM mensaje WHERE id_conversacion = ?
                """, Integer.class, idConversacion);
        assertEquals(2, mensajes);

        Integer notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'MENSAJE_PRIVADO_RECIBIDO'
                  AND entidad_tipo = 'CONVERSACION'
                  AND entidad_id = ?
                """, Integer.class, perfilB.idUsuario(), idConversacion);
        assertEquals(1, notificaciones);

        Integer perfilContexto = jdbcTemplate.queryForObject("""
                SELECT id_perfil_contexto FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'MENSAJE_PRIVADO_RECIBIDO'
                  AND entidad_id = ?
                """, Integer.class, perfilB.idUsuario(), idConversacion);
        assertEquals(perfilB.idPerfil().intValue(), perfilContexto);

        assertEquals(1, mensajeriaService.resumen(
                perfilB.idUsuario(), perfilB.idPerfil()).totalConversaciones());
        assertEquals(1, mensajeriaService.resumen(
                perfilB.idUsuario(), perfilB.idPerfil()).conversacionesNoLeidas());

        ConversacionResponse conversacion = mensajeriaService.listarConversaciones(
                perfilB.idUsuario(), perfilB.idPerfil(), 0, 20)
                .contenido().get(0);
        assertTrue(conversacion.tieneNoLeidos());
        assertTrue(conversacion.puedeEnviar());
    }

    @Test
    void marcarLeido_marcaMensajesNotificacionYResumen() {
        mensajeriaService.enviarMensaje(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, "Hola");

        mensajeriaService.marcarLeido(
                perfilB.idUsuario(), perfilB.idPerfil(), idConversacion, null);

        Boolean leido = jdbcTemplate.queryForObject("""
                SELECT leido FROM mensaje WHERE id_conversacion = ?
                """, Boolean.class, idConversacion);
        assertTrue(leido);

        assertNotNull(jdbcTemplate.queryForObject("""
                SELECT fecha_lectura FROM mensaje WHERE id_conversacion = ?
                """, java.time.LocalDateTime.class, idConversacion));

        Boolean notificacionLeida = jdbcTemplate.queryForObject("""
                SELECT leida FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'MENSAJE_PRIVADO_RECIBIDO'
                  AND entidad_id = ?
                """, Boolean.class, perfilB.idUsuario(), idConversacion);
        assertTrue(notificacionLeida);

        assertEquals(0, mensajeriaService.resumen(
                perfilB.idUsuario(), perfilB.idPerfil()).conversacionesNoLeidas());

        ConversacionResponse conversacion = mensajeriaService.listarConversaciones(
                perfilB.idUsuario(), perfilB.idPerfil(), 0, 20)
                .contenido().get(0);
        assertFalse(conversacion.tieneNoLeidos());

        List<MensajeResponse> mensajes = mensajeriaService.listarMensajes(
                perfilB.idUsuario(), perfilB.idPerfil(), idConversacion, 0, 20)
                .contenido();
        assertTrue(mensajes.get(0).leido());

        List<MensajeResponse> desdeEmisor = mensajeriaService.listarMensajes(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, 0, 20)
                .contenido();
        assertTrue(desdeEmisor.get(0).leido());
    }

    @Test
    void marcarLeido_hastaIdMensaje_noMarcaMensajesPosteriores() {
        MensajeResponse primero = mensajeriaService.enviarMensaje(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, "Uno");
        MensajeResponse segundo = mensajeriaService.enviarMensaje(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, "Dos");
        MensajeResponse tercero = mensajeriaService.enviarMensaje(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, "Tres");

        mensajeriaService.marcarLeido(
                perfilB.idUsuario(),
                perfilB.idPerfil(),
                idConversacion,
                segundo.idMensaje());

        assertEquals(Boolean.TRUE, leerEstado(primero.idMensaje()));
        assertEquals(Boolean.TRUE, leerEstado(segundo.idMensaje()));
        assertEquals(Boolean.FALSE, leerEstado(tercero.idMensaje()));

        assertEquals(1, mensajeriaService.resumen(
                perfilB.idUsuario(), perfilB.idPerfil()).conversacionesNoLeidas());

        mensajeriaService.enviarMensaje(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, "Cuatro");

        // La conversación ya tenía no leídos: no se crea otra notificación.
        assertEquals(1, contarNotificacionesNoLeidas());

        mensajeriaService.marcarLeido(
                perfilB.idUsuario(), perfilB.idPerfil(), idConversacion, null);

        mensajeriaService.enviarMensaje(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, "Cinco");

        assertEquals(1, contarNotificacionesNoLeidas());
    }

    private Integer contarNotificacionesNoLeidas() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'MENSAJE_PRIVADO_RECIBIDO'
                  AND entidad_id = ?
                  AND leida = false
                """, Integer.class, perfilB.idUsuario(), idConversacion);
    }

    private Boolean leerEstado(Long idMensaje) {
        return jdbcTemplate.queryForObject("""
                SELECT leido FROM mensaje WHERE id_mensaje = ?
                """, Boolean.class, idMensaje);
    }

    @Test
    void conexionEliminada_historialVisibleYEnvioBloqueado() {
        mensajeriaService.enviarMensaje(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, "Hola");

        eliminarConexionService.eliminar(
                perfilA.idUsuario(), perfilA.idPerfil(), idConexionAB);

        ConversacionResponse conversacion = mensajeriaService.listarConversaciones(
                perfilB.idUsuario(), perfilB.idPerfil(), 0, 20)
                .contenido().get(0);
        assertFalse(conversacion.puedeEnviar());

        assertEquals(1, mensajeriaService.listarMensajes(
                perfilB.idUsuario(), perfilB.idPerfil(), idConversacion, 0, 20)
                .contenido().size());

        assertThrows(MensajeNoPermitidoException.class, () ->
                mensajeriaService.enviarMensaje(
                        perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, "Otro"));

        assertThrows(MensajeNoPermitidoException.class, () ->
                mensajeriaService.notificarEscribiendo(
                        perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, true));

        mensajeriaService.marcarLeido(
                perfilB.idUsuario(), perfilB.idPerfil(), idConversacion, null);
    }

    @Test
    void obtenerConversacion_devuelveLaContraparte() {
        ConversacionResponse conversacion = mensajeriaService.obtenerConversacion(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion);

        assertEquals(idConversacion, conversacion.idConversacion());
        assertEquals(perfilB.idPerfil(), conversacion.perfil().idPerfil());
        assertTrue(conversacion.puedeEnviar());
    }

    @Test
    void obtenerConversacion_tercero_lanzaNoEncontrada() {
        assertThrows(ConversacionNoEncontradaException.class, () ->
                mensajeriaService.obtenerConversacion(
                        perfilC.idUsuario(), perfilC.idPerfil(), idConversacion));
    }

    @Test
    void notificarEscribiendo_conConexionActiva_noFalla() {
        mensajeriaService.notificarEscribiendo(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, true);
        mensajeriaService.notificarEscribiendo(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, false);
    }

    @Test
    void terceroNoParticipa_lanzaNoEncontrada() {
        assertThrows(ConversacionNoEncontradaException.class, () ->
                mensajeriaService.listarMensajes(
                        perfilC.idUsuario(), perfilC.idPerfil(), idConversacion, 0, 20));
    }

    @Test
    void mensaje_noGeneraAuditoria() {
        Integer antes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auditoria
                WHERE entidad_afectada = 'mensaje'
                """, Integer.class);

        mensajeriaService.enviarMensaje(
                perfilA.idUsuario(), perfilA.idPerfil(), idConversacion, "Hola");

        Integer despues = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auditoria
                WHERE entidad_afectada = 'mensaje'
                """, Integer.class);

        assertEquals(antes, despues);
    }

    private Long crearConexion(PerfilCreado remitente, PerfilCreado destinatario) {
        SolicitudConexionResponse solicitud = enviarSolicitudConexionService.enviar(
                remitente.idUsuario(),
                remitente.idPerfil(),
                new EnviarSolicitudConexionRequest(destinatario.idPerfil()));

        SolicitudConexionGestionadaResponse aceptada =
                gestionarSolicitudConexionService.aceptar(
                        destinatario.idUsuario(),
                        destinatario.idPerfil(),
                        solicitud.idSolicitudConexion());

        return aceptada.idConexion();
    }

    private PerfilCreado crearPerfil(
            String etiqueta,
            String nombre,
            String apellido,
            String nombreArtistico,
            String nombreProfesion,
            RolGlobal rol,
            Genero genero) {

        int n = SECUENCIA.incrementAndGet();
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .dni(String.format("%08d", 95_000_000 + n))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("uc42." + etiqueta + n + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = profesionRepository.saveAndFlush(Profesion.builder()
                .codigo("UC42_" + etiqueta + "_" + n)
                .nombre(nombreProfesion + " " + etiqueta + " " + n)
                .build());

        Perfil perfil = perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de prueba UC-42")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .profesion(profesion)
                .build());

        return new PerfilCreado(
                usuario.getIdUsuario(), perfil.getIdPerfil(), profesion.getIdProfesion());
    }

    private record PerfilCreado(Long idUsuario, Long idPerfil, Long idProfesion) {
    }
}
