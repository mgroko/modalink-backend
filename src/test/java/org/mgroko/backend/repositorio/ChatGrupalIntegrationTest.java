package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.mensajeria.dto.ChatGrupalResponse;
import org.mgroko.backend.mensajeria.dto.MensajeGrupalResponse;
import org.mgroko.backend.mensajeria.exception.MensajeNoPermitidoException;
import org.mgroko.backend.mensajeria.servicio.ChatGrupalService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-41 contra PostgreSQL real: chat por proyecto, notificaciones y triggers.
 */
@SpringBootTest
@Transactional
class ChatGrupalIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private RolProyectoRepository rolProyectoRepository;
    @Autowired
    private ProyectoRepository proyectoRepository;
    @Autowired
    private MiembroProyectoRepository miembroProyectoRepository;
    @Autowired
    private ChatGrupalService chatGrupalService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PerfilCreado director;
    private PerfilCreado miembro;
    private PerfilCreado ajeno;
    private Proyecto proyecto;

    @BeforeEach
    void setUp() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        RolProyecto rolDirector = rolProyectoRepository.findByCodigo("DIRECTOR").orElseThrow();
        RolProyecto rolMiembro = rolProyectoRepository.findByCodigo("MIEMBRO").orElseThrow();

        director = crearPerfil("DIR", "Ana", "Pérez", "Ana Moda", rol, genero);
        miembro = crearPerfil("MIE", "Bruno", "Gómez", "Bruno Estilo", rol, genero);
        ajeno = crearPerfil("AJE", "Carla", "Núñez", "Carla Diseño", rol, genero);

        proyecto = proyectoRepository.saveAndFlush(Proyecto.builder()
                .nombre("Proyecto UC41 " + SECUENCIA.incrementAndGet())
                .descripcion("Proyecto de integración UC-41")
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .build());

        miembroProyectoRepository.saveAndFlush(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(director.perfil())
                .rolProyecto(rolDirector)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());

        miembroProyectoRepository.saveAndFlush(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(miembro.perfil())
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());
    }

    @Test
    void enviarMensaje_persisteYNotificaAlRestoDeParticipantes() {
        MensajeGrupalResponse response = chatGrupalService.enviarMensaje(
                director.idUsuario(), director.perfil().getIdPerfil(),
                proyecto.getIdProyecto(), "Hola equipo");

        assertEquals("Hola equipo", response.contenido());
        assertTrue(response.esMio());
        assertEquals(proyecto.getIdProyecto(), response.idProyecto());

        Integer mensajes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM mensaje_proyecto WHERE id_proyecto = ?
                """, Integer.class, proyecto.getIdProyecto());
        assertEquals(1, mensajes);

        Integer notificacionesMiembro = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'MENSAJE_GRUPAL_RECIBIDO'
                  AND entidad_tipo = 'PROYECTO'
                  AND entidad_id = ?
                """, Integer.class, miembro.idUsuario(), proyecto.getIdProyecto());
        assertEquals(1, notificacionesMiembro);

        Integer notificacionesDirector = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'MENSAJE_GRUPAL_RECIBIDO'
                  AND entidad_id = ?
                """, Integer.class, director.idUsuario(), proyecto.getIdProyecto());
        assertEquals(0, notificacionesDirector);
    }

    @Test
    void segundoMensajeSinLeer_noDuplicaNotificacion() {
        chatGrupalService.enviarMensaje(
                director.idUsuario(), director.perfil().getIdPerfil(),
                proyecto.getIdProyecto(), "Uno");
        chatGrupalService.enviarMensaje(
                director.idUsuario(), director.perfil().getIdPerfil(),
                proyecto.getIdProyecto(), "Dos");

        Integer notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'MENSAJE_GRUPAL_RECIBIDO'
                  AND entidad_id = ?
                  AND leida = false
                """, Integer.class, miembro.idUsuario(), proyecto.getIdProyecto());
        assertEquals(1, notificaciones);

        chatGrupalService.marcarLeido(
                miembro.idUsuario(), miembro.perfil().getIdPerfil(),
                proyecto.getIdProyecto());

        Boolean leida = jdbcTemplate.queryForObject("""
                SELECT leida FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'MENSAJE_GRUPAL_RECIBIDO'
                  AND entidad_id = ?
                """, Boolean.class, miembro.idUsuario(), proyecto.getIdProyecto());
        assertTrue(leida);

        chatGrupalService.enviarMensaje(
                director.idUsuario(), director.perfil().getIdPerfil(),
                proyecto.getIdProyecto(), "Tres");

        Integer nuevas = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'MENSAJE_GRUPAL_RECIBIDO'
                  AND entidad_id = ?
                  AND leida = false
                """, Integer.class, miembro.idUsuario(), proyecto.getIdProyecto());
        assertEquals(1, nuevas);
    }

    @Test
    void proyectoFinalizado_historialVisibleYEnvioBloqueado() {
        chatGrupalService.enviarMensaje(
                director.idUsuario(), director.perfil().getIdPerfil(),
                proyecto.getIdProyecto(), "Hola");

        proyecto.setEstado(EstadoProyecto.FINALIZADO);
        proyectoRepository.saveAndFlush(proyecto);

        ChatGrupalResponse chat = chatGrupalService.obtenerChat(
                miembro.idUsuario(), miembro.perfil().getIdPerfil(),
                proyecto.getIdProyecto());
        assertFalse(chat.puedeEnviar());

        assertEquals(1, chatGrupalService.listarMensajes(
                miembro.idUsuario(), miembro.perfil().getIdPerfil(),
                proyecto.getIdProyecto(), 0, 20).contenido().size());

        assertThrows(MensajeNoPermitidoException.class, () ->
                chatGrupalService.enviarMensaje(
                        miembro.idUsuario(), miembro.perfil().getIdPerfil(),
                        proyecto.getIdProyecto(), "Otro"));
    }

    @Test
    void perfilAjeno_noPuedeAbrirElChat() {
        assertThrows(AccesoDenegadoProyectoException.class, () ->
                chatGrupalService.obtenerChat(
                        ajeno.idUsuario(), ajeno.perfil().getIdPerfil(),
                        proyecto.getIdProyecto()));
    }

    @Test
    void trigger_rechazaMensajeDePerfilNoParticipante() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update("""
                        INSERT INTO mensaje_proyecto
                            (id_proyecto, id_perfil_emisor, contenido, fecha_envio)
                        VALUES (?, ?, ?, ?)
                        """,
                        proyecto.getIdProyecto(),
                        ajeno.perfil().getIdPerfil(),
                        "Intruso",
                        LocalDateTime.now()));
    }

    @Test
    void trigger_rechazaContenidoVacio() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update("""
                        INSERT INTO mensaje_proyecto
                            (id_proyecto, id_perfil_emisor, contenido, fecha_envio)
                        VALUES (?, ?, ?, ?)
                        """,
                        proyecto.getIdProyecto(),
                        director.perfil().getIdPerfil(),
                        "   ",
                        LocalDateTime.now()));
    }

    @Test
    void trigger_rechazaModificacion() {
        Long idMensaje = enviarMensajeYBuscarId();

        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "UPDATE mensaje_proyecto SET contenido = ? WHERE id_mensaje_proyecto = ?",
                        "Modificado",
                        idMensaje));
    }

    @Test
    void trigger_rechazaBorrado() {
        Long idMensaje = enviarMensajeYBuscarId();

        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "DELETE FROM mensaje_proyecto WHERE id_mensaje_proyecto = ?",
                        idMensaje));
    }

    private Long enviarMensajeYBuscarId() {
        chatGrupalService.enviarMensaje(
                director.idUsuario(), director.perfil().getIdPerfil(),
                proyecto.getIdProyecto(), "Hola");

        return jdbcTemplate.queryForObject("""
                SELECT id_mensaje_proyecto FROM mensaje_proyecto
                WHERE id_proyecto = ?
                """, Long.class, proyecto.getIdProyecto());
    }

    private PerfilCreado crearPerfil(
            String etiqueta,
            String nombre,
            String apellido,
            String nombreArtistico,
            RolGlobal rol,
            Genero genero) {

        int n = SECUENCIA.incrementAndGet();
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .dni(String.format("%08d", 94_000_000 + n))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("uc41." + etiqueta + n + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = profesionRepository.saveAndFlush(Profesion.builder()
                .codigo("UC41_" + etiqueta + "_" + n)
                .nombre("Profesión " + etiqueta + " " + n)
                .build());

        Perfil perfil = perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de prueba UC-41")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .profesion(profesion)
                .build());

        return new PerfilCreado(usuario.getIdUsuario(), perfil);
    }

    private record PerfilCreado(Long idUsuario, Perfil perfil) {
    }
}
