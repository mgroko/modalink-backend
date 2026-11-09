package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.conexiones.dto.BuscarConexionesFiltro;
import org.mgroko.backend.conexiones.dto.ConexionResponse;
import org.mgroko.backend.conexiones.dto.EnviarSolicitudConexionRequest;
import org.mgroko.backend.conexiones.dto.SolicitudConexionGestionadaResponse;
import org.mgroko.backend.conexiones.dto.SolicitudConexionResponse;
import org.mgroko.backend.conexiones.exception.ConexionNoActivaException;
import org.mgroko.backend.conexiones.exception.ConexionNoEncontradaException;
import org.mgroko.backend.conexiones.servicio.BuscarConexionService;
import org.mgroko.backend.conexiones.servicio.EliminarConexionService;
import org.mgroko.backend.conexiones.servicio.EnviarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.GestionarSolicitudConexionService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-44 y UC-44 bis contra PostgreSQL real.
 */
@SpringBootTest
@Transactional
class ConexionIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private BuscarConexionService buscarConexionService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PerfilCreado perfilA;
    private PerfilCreado perfilB;
    private PerfilCreado perfilC;
    private Long idConexionAB;

    @BeforeEach
    void setUp() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        perfilA = crearPerfil("A", "Ana", "Pérez", "Ana Moda", "Modelo", rol, genero);
        perfilB = crearPerfil("B", "Bruno", "Gómez", "Bruno Estilo", "Fotógrafo", rol, genero);
        perfilC = crearPerfil("C", "Carla", "Núñez", "Carla Diseño", "Diseñadora", rol, genero);

        idConexionAB = crearConexion(perfilA, perfilB);
    }

    @Test
    void eliminarConexion_actualizaEstadoNotificaYAudita() {
        eliminarConexionService.eliminar(
                perfilA.idUsuario(), perfilA.idPerfil(), idConexionAB);

        var conexion = conexion(idConexionAB);
        assertEquals("ELIMINADA", conexion.estado());
        assertNotNull(conexion.fechaFin());

        assertEquals(1, contarNotificaciones(
                perfilA.idUsuario(),
                NotificacionService.TIPO_CONEXION_ELIMINADA,
                idConexionAB));
        assertEquals(1, contarNotificaciones(
                perfilB.idUsuario(),
                NotificacionService.TIPO_CONEXION_ELIMINADA,
                idConexionAB));
        assertEquals(1, contarAuditoria("CONEXION_ELIMINADA", idConexionAB));

        String estadoSolicitud = jdbcTemplate.queryForObject("""
                SELECT estado FROM solicitud_conexion
                WHERE id_solicitud_conexion = (
                    SELECT id_solicitud_conexion FROM conexion WHERE id_conexion = ?
                )
                """, String.class, idConexionAB);
        assertEquals(EstadoSolicitud.ACEPTADA.name(), estadoSolicitud);
    }

    @Test
    void eliminarConexion_contraparteUsuarioPendienteBaja_noNotificaContraparte() {
        Usuario usuarioB = usuarioRepository.findById(perfilB.idUsuario()).orElseThrow();
        usuarioB.setEstado(EstadoUsuario.PendienteBaja);
        usuarioRepository.saveAndFlush(usuarioB);

        eliminarConexionService.eliminar(
                perfilA.idUsuario(), perfilA.idPerfil(), idConexionAB);

        assertEquals(1, contarNotificaciones(
                perfilA.idUsuario(),
                NotificacionService.TIPO_CONEXION_ELIMINADA,
                idConexionAB));
        assertEquals(0, contarNotificaciones(
                perfilB.idUsuario(),
                NotificacionService.TIPO_CONEXION_ELIMINADA,
                idConexionAB));
    }

    @Test
    void eliminarConexion_contrapartePerfilInactivo_permiteLaBaja() {
        Perfil perfil = perfilRepository.findById(perfilB.idPerfil()).orElseThrow();
        perfil.setEstado(EstadoPerfil.PendienteBaja);
        perfilRepository.saveAndFlush(perfil);

        eliminarConexionService.eliminar(
                perfilA.idUsuario(), perfilA.idPerfil(), idConexionAB);

        assertEquals("ELIMINADA", conexion(idConexionAB).estado());
    }

    @Test
    void eliminarConexion_yaEliminada_lanzaNoActiva() {
        eliminarConexionService.eliminar(
                perfilA.idUsuario(), perfilA.idPerfil(), idConexionAB);

        assertThrows(ConexionNoActivaException.class, () ->
                eliminarConexionService.eliminar(
                        perfilA.idUsuario(), perfilA.idPerfil(), idConexionAB));
    }

    @Test
    void eliminarConexion_ajena_lanzaNoEncontrada() {
        assertThrows(ConexionNoEncontradaException.class, () ->
                eliminarConexionService.eliminar(
                        perfilC.idUsuario(), perfilC.idPerfil(), idConexionAB));
    }

    @Test
    void listarRed_devuelveSoloActivasYContraparte() {
        PaginaResponse<ConexionResponse> respuesta = buscarConexionService.buscar(
                perfilA.idUsuario(),
                perfilA.idPerfil(),
                null,
                0,
                20);

        assertEquals(1, respuesta.totalElementos());
        assertEquals(idConexionAB, respuesta.contenido().get(0).idConexion());
        assertEquals(perfilB.idPerfil(), respuesta.contenido().get(0).perfil().idPerfil());

        eliminarConexionService.eliminar(
                perfilA.idUsuario(), perfilA.idPerfil(), idConexionAB);

        PaginaResponse<ConexionResponse> despues = buscarConexionService.buscar(
                perfilA.idUsuario(), perfilA.idPerfil(), null, 0, 20);
        assertTrue(despues.contenido().isEmpty());
    }

    @Test
    void listarRed_filtraPorTextoYProfesion() {
        crearConexion(perfilA, perfilC);

        PaginaResponse<ConexionResponse> porTexto = buscarConexionService.buscar(
                perfilA.idUsuario(),
                perfilA.idPerfil(),
                new BuscarConexionesFiltro("Bruno", null, null, null, null),
                0,
                20);

        assertEquals(1, porTexto.totalElementos());
        assertEquals(perfilB.idPerfil(), porTexto.contenido().get(0).perfil().idPerfil());

        PaginaResponse<ConexionResponse> porProfesion = buscarConexionService.buscar(
                perfilA.idUsuario(),
                perfilA.idPerfil(),
                new BuscarConexionesFiltro(null, null, null, null, perfilC.idProfesion()),
                0,
                20);

        assertEquals(1, porProfesion.totalElementos());
        assertEquals(perfilC.idPerfil(), porProfesion.contenido().get(0).perfil().idPerfil());
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

    private ConexionEstado conexion(Long idConexion) {
        return jdbcTemplate.queryForObject("""
                SELECT estado, fecha_fin_conexion
                FROM conexion
                WHERE id_conexion = ?
                """, (rs, rowNum) -> new ConexionEstado(
                rs.getString("estado"),
                rs.getTimestamp("fecha_fin_conexion") != null
                        ? rs.getTimestamp("fecha_fin_conexion").toLocalDateTime()
                        : null),
                idConexion);
    }

    private int contarNotificaciones(
            Long idUsuarioDestino,
            String tipo,
            Long entidadId) {

        Integer cantidad = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = ?
                  AND entidad_id = ?
                """, Integer.class, idUsuarioDestino, tipo, entidadId);
        return cantidad != null ? cantidad : 0;
    }

    private int contarAuditoria(String tipoEvento, Long idAfectado) {
        Integer cantidad = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auditoria
                WHERE tipo_evento = ?
                  AND id_afectado = ?
                """, Integer.class, tipoEvento, idAfectado);
        return cantidad != null ? cantidad : 0;
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
                .dni(String.format("%08d", 92_000_000 + n))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("uc44." + etiqueta + n + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = profesionRepository.saveAndFlush(Profesion.builder()
                .codigo("UC44_" + etiqueta + "_" + n)
                .nombre(nombreProfesion + " " + etiqueta + " " + n)
                .build());

        Perfil perfil = perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de prueba UC-44")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .profesion(profesion)
                .build());

        return new PerfilCreado(
                usuario.getIdUsuario(),
                perfil.getIdPerfil(),
                profesion.getIdProfesion());
    }

    private record PerfilCreado(Long idUsuario, Long idPerfil, Long idProfesion) {
    }

    private record ConexionEstado(String estado, java.time.LocalDateTime fechaFin) {
    }
}
