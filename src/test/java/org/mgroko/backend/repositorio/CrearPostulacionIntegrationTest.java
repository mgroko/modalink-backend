package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.dto.CrearRequerimientoActividadRequest;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.servicio.CrearActividadService;
import org.mgroko.backend.modelo.Agenda;
import org.mgroko.backend.modelo.BloqueoAgenda;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.dto.NotificacionResponse;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.postulaciones.dto.BuscarPostulacionesFiltro;
import org.mgroko.backend.postulaciones.dto.PostulacionBusquedaResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionGestionadaResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.exception.PostulacionNoPermitidaException;
import org.mgroko.backend.postulaciones.exception.PostulacionYaGestionadaException;
import org.mgroko.backend.postulaciones.servicio.AceptarPostulacionService;
import org.mgroko.backend.postulaciones.servicio.BuscarPostulacionesProyectoService;
import org.mgroko.backend.postulaciones.servicio.CrearPostulacionService;
import org.mgroko.backend.postulaciones.servicio.RechazarPostulacionService;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.dto.CancelarProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.servicio.CancelarProyectoService;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.PublicarProyectoService;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración de UC-26 contra PostgreSQL real: alta de postulaciones a
 * requerimientos generales y de actividad, reglas de negocio (proyecto
 * publicado, profesión, duplicados, disponibilidad), persistencia y
 * notificación a los directores activos.
 */
@SpringBootTest
@Transactional
class CrearPostulacionIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolGlobalRepository rolGlobalRepository;
    @Autowired
    private GeneroRepository generoRepository;
    @Autowired
    private ProfesionRepository profesionRepository;
    @Autowired
    private CrearPerfilService crearPerfilService;
    @Autowired
    private CrearProyectoService crearProyectoService;
    @Autowired
    private PublicarProyectoService publicarProyectoService;
    @Autowired
    private CancelarProyectoService cancelarProyectoService;
    @Autowired
    private CrearActividadService crearActividadService;
    @Autowired
    private CrearPostulacionService crearPostulacionService;
    @Autowired
    private BuscarPostulacionesProyectoService buscarPostulacionesProyectoService;
    @Autowired
    private AceptarPostulacionService aceptarPostulacionService;
    @Autowired
    private RechazarPostulacionService rechazarPostulacionService;
    @Autowired
    private ProyectoRepository proyectoRepository;
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private RolProyectoRepository rolProyectoRepository;
    @Autowired
    private MiembroProyectoRepository miembroProyectoRepository;
    @Autowired
    private PostulacionActividadRepository postulacionActividadRepository;
    @Autowired
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Autowired
    private AgendaRepository agendaRepository;
    @Autowired
    private BloqueoAgendaRepository bloqueoAgendaRepository;
    @Autowired
    private NotificacionService notificacionService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record PerfilCreado(Long idUsuario, Long idPerfil) {}

    // ---------------------------------------------------------------
    // Requerimiento general
    // ---------------------------------------------------------------

    @Test
    void general_publicado_persistePostulacionYNotificaAlDirector() {
        PerfilCreado director = crearPerfil("uc26.dir1@example.com", "88826001", "Directora UC26", "modelo");
        PerfilCreado postulante = crearPerfil("uc26.pos1@example.com", "88826002", "Postulante UC26", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC26 Uno");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        PostulacionResponse respuesta = crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        assertThat(respuesta.tipo()).isEqualTo(TipoPostulacion.GENERAL);
        assertThat(respuesta.estado().name()).isEqualTo("PENDIENTE");
        assertThat(respuesta.idProyecto()).isEqualTo(publicado.idProyecto());
        assertThat(respuesta.idRequerimiento()).isEqualTo(idRequerimiento);
        assertThat(respuesta.fechaPostulacion()).isNotNull();
        assertThat(respuesta.tentativa()).isFalse();

        Long postulaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM postulacion_gral
                WHERE id_perfil = ? AND id_requerimiento_gral = ? AND estado = 'PENDIENTE'
                """, Long.class, postulante.idPerfil(), idRequerimiento);
        assertThat(postulaciones).isEqualTo(1L);

        Long notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'POSTULACION_RECIBIDA'
                  AND entidad_tipo = 'POSTULACION_GRAL'
                  AND entidad_id = ?
                """, Long.class, director.idUsuario(), respuesta.idPostulacion());
        assertThat(notificaciones).isEqualTo(1L);
    }

    @Test
    void notificacionDePostulacionGral_incluyeIdProyecto() {
        PerfilCreado director = crearPerfil(
                "uc26.notif.dir1@example.com", "88826101", "Directora Notif Gral", "modelo");
        PerfilCreado postulante = crearPerfil(
                "uc26.notif.pos1@example.com", "88826102", "Postulante Notif Gral", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile Notif Gral");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        PaginaResponse<NotificacionResponse> pagina = notificacionService.listar(
                director.idUsuario(), director.idPerfil(), 0, 20);

        assertThat(pagina.contenido())
                .filteredOn(n -> NotificacionService.TIPO_POSTULACION_RECIBIDA.equals(n.tipo()))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.entidadTipo()).isEqualTo("POSTULACION_GRAL");
                    assertThat(n.idProyecto()).isEqualTo(publicado.idProyecto());
                });
    }

    @Test
    void notificacionDePostulacionActividad_incluyeIdProyecto() {
        PerfilCreado director = crearPerfil(
                "uc26.notif.dir2@example.com", "88826103", "Directora Notif Act", "modelo");
        PerfilCreado postulante = crearPerfil(
                "uc26.notif.pos2@example.com", "88826104", "Postulante Notif Act", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile Notif Actividad");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        confirmarInicioDelProyecto(publicado.idProyecto());

        crearPostulacionService.darDeAltaPostulacionActividad(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        PaginaResponse<NotificacionResponse> pagina = notificacionService.listar(
                director.idUsuario(), director.idPerfil(), 0, 20);

        assertThat(pagina.contenido())
                .filteredOn(n -> NotificacionService.TIPO_POSTULACION_RECIBIDA.equals(n.tipo()))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.entidadTipo()).isEqualTo("POSTULACION_ACTIVIDAD");
                    assertThat(n.idProyecto()).isEqualTo(publicado.idProyecto());
                });
    }

    @Test
    void buscarPostulaciones_devuelveLasDelProyectoConCupoYFiltros() {
        PerfilCreado director = crearPerfil("uc74.dir1@example.com", "88874001", "Directora UC74", "modelo");
        PerfilCreado postulanteA = crearPerfil("uc74.pos1@example.com", "88874002", "Postulante A UC74", "modelo");
        PerfilCreado postulanteB = crearPerfil("uc74.pos2@example.com", "88874003", "Postulante B UC74", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC74");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        crearPostulacionService.darDeAltaPostulacionGeneral(
                postulanteA.idUsuario(), postulanteA.idPerfil(),
                publicado.idProyecto(), idRequerimiento);
        crearPostulacionService.darDeAltaPostulacionGeneral(
                postulanteB.idUsuario(), postulanteB.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        PaginaResponse<PostulacionBusquedaResponse> pagina =
                buscarPostulacionesProyectoService.buscar(
                        director.idPerfil(),
                        publicado.idProyecto(),
                        new BuscarPostulacionesFiltro(null, null, null, null, null, null),
                        0, 20, false);

        assertThat(pagina.contenido()).hasSize(2);
        assertThat(pagina.contenido())
                .allSatisfy(postulacion -> {
                    assertThat(postulacion.tipo()).isEqualTo(TipoPostulacion.GENERAL);
                    assertThat(postulacion.estado().name()).isEqualTo("PENDIENTE");
                    assertThat(postulacion.idRequerimiento()).isEqualTo(idRequerimiento);
                    assertThat(postulacion.cubiertos()).isZero();
                    assertThat(postulacion.faltantes()).isEqualTo(1);
                    assertThat(postulacion.satisfecho()).isFalse();
                    assertThat(postulacion.puedeAceptar()).isTrue();
                    assertThat(postulacion.puedeRechazar()).isTrue();
                });

        PaginaResponse<PostulacionBusquedaResponse> filtrada =
                buscarPostulacionesProyectoService.buscar(
                        director.idPerfil(),
                        publicado.idProyecto(),
                        new BuscarPostulacionesFiltro(
                                null, TipoPostulacion.GENERAL,
                                "Postulante A", null, null, null),
                        0, 20, false);

        assertThat(filtrada.contenido()).hasSize(1);
        assertThat(filtrada.contenido().get(0).nombreArtistico())
                .isEqualTo("Postulante A UC74");
    }

    @Test
    void buscarPostulaciones_sinPermisoDeDirector_lanzaExcepcion() {
        PerfilCreado director = crearPerfil("uc74.dir2@example.com", "88874004", "Directora UC74 Dos", "modelo");
        PerfilCreado postulante = crearPerfil("uc74.pos3@example.com", "88874005", "Postulante UC74 Tres", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC74 Dos");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        assertThatThrownBy(() -> buscarPostulacionesProyectoService.buscar(
                postulante.idPerfil(),
                publicado.idProyecto(),
                new BuscarPostulacionesFiltro(null, null, null, null, null, null),
                0, 20, false))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void aceptarGeneral_creaMiembroYRechazaLasDemasPendientes() {
        PerfilCreado director = crearPerfil("uc34.dir1@example.com", "88834001", "Directora UC34", "modelo");
        PerfilCreado postulanteA = crearPerfil("uc34.pos1@example.com", "88834002", "Postulante A UC34", "modelo");
        PerfilCreado postulanteB = crearPerfil("uc34.pos2@example.com", "88834003", "Postulante B UC34", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC34 Aceptar");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        PostulacionResponse postulacionA = crearPostulacionService.darDeAltaPostulacionGeneral(
                postulanteA.idUsuario(), postulanteA.idPerfil(),
                publicado.idProyecto(), idRequerimiento);
        PostulacionResponse postulacionB = crearPostulacionService.darDeAltaPostulacionGeneral(
                postulanteB.idUsuario(), postulanteB.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        PostulacionGestionadaResponse respuesta = aceptarPostulacionService.aceptarGeneral(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacionA.idPostulacion());

        assertThat(respuesta.tipo()).isEqualTo(TipoPostulacion.GENERAL);
        assertThat(respuesta.estado()).isEqualTo(org.mgroko.backend.modelo.enums.EstadoSolicitud.ACEPTADA);
        assertThat(respuesta.idMiembro()).isNotNull();
        assertThat(respuesta.idAsignacionActividad()).isNull();

        Long miembros = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM miembros_proyecto
                WHERE id_proyecto = ? AND id_perfil = ?
                  AND id_requerimiento_gral = ? AND estado_participacion = 'ACTIVO'
                """, Long.class, publicado.idProyecto(), postulanteA.idPerfil(), idRequerimiento);
        assertThat(miembros).isEqualTo(1L);

        String estadoA = jdbcTemplate.queryForObject("""
                SELECT estado FROM postulacion_gral WHERE id_postulacion_gral = ?
                """, String.class, postulacionA.idPostulacion());
        assertThat(estadoA).isEqualTo("ACEPTADA");

        String estadoB = jdbcTemplate.queryForObject("""
                SELECT estado FROM postulacion_gral WHERE id_postulacion_gral = ?
                """, String.class, postulacionB.idPostulacion());
        assertThat(estadoB).isEqualTo("RECHAZADA");

        Long notificacionA = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'POSTULACION_ACEPTADA'
                  AND entidad_tipo = 'POSTULACION_GRAL' AND entidad_id = ?
                """, Long.class, postulanteA.idUsuario(), postulacionA.idPostulacion());
        assertThat(notificacionA).isEqualTo(1L);

        Long notificacionB = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'POSTULACION_RECHAZADA'
                  AND entidad_tipo = 'POSTULACION_GRAL' AND entidad_id = ?
                """, Long.class, postulanteB.idUsuario(), postulacionB.idPostulacion());
        assertThat(notificacionB).isEqualTo(1L);

        assertThatThrownBy(() -> aceptarPostulacionService.aceptarGeneral(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacionA.idPostulacion()))
                .isInstanceOf(PostulacionYaGestionadaException.class);
    }

    @Test
    void aceptarGeneral_sinSerDirector_lanzaAccesoDenegado() {
        PerfilCreado director = crearPerfil("uc34.dir2@example.com", "88834004", "Directora UC34 Dos", "modelo");
        PerfilCreado postulante = crearPerfil("uc34.pos3@example.com", "88834005", "Postulante UC34 Tres", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC34 Sin Permiso");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        PostulacionResponse postulacion = crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        assertThatThrownBy(() -> aceptarPostulacionService.aceptarGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion()))
                .isInstanceOf(AccesoDenegadoProyectoException.class);

        Long estado = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM miembros_proyecto
                WHERE id_proyecto = ? AND id_perfil = ? AND estado_participacion = 'ACTIVO'
                """, Long.class, publicado.idProyecto(), postulante.idPerfil());
        assertThat(estado).isZero();
    }

    @Test
    void aceptarGeneral_enProyectoConfirmado_permiteAceptar() {
        PerfilCreado director = crearPerfil("uc34.dir3@example.com", "88834006", "Directora UC34 Tres", "modelo");
        PerfilCreado postulante = crearPerfil("uc34.pos4@example.com", "88834007", "Postulante UC34 Cuatro", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC34 Confirmado");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        PostulacionResponse postulacion = crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        Proyecto proyecto = proyectoRepository.findById(publicado.idProyecto()).orElseThrow();
        proyecto.setEstado(EstadoProyecto.CONFIRMADO);
        proyecto.setFechaInicio(java.time.LocalDate.now());
        proyectoRepository.saveAndFlush(proyecto);

        PostulacionGestionadaResponse respuesta = aceptarPostulacionService.aceptarGeneral(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion());

        assertThat(respuesta.estado())
                .isEqualTo(org.mgroko.backend.modelo.enums.EstadoSolicitud.ACEPTADA);
        assertThat(respuesta.idMiembro()).isNotNull();
    }

    @Test
    void aceptarActividad_creaAsignacionYNotifica() {
        PerfilCreado director = crearPerfil("uc34.act.dir1@example.com", "88834010", "Directora Actividad UC34", "modelo");
        PerfilCreado postulante = crearPerfil("uc34.act.pos1@example.com", "88834011", "Postulante Actividad UC34", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC34 Actividad");
        Long idRequerimiento = requerimientoDeActividad(publicado);
        Long idActividad = idActividadDe(publicado.idProyecto());

        PostulacionResponse postulacion = crearPostulacionService.darDeAltaPostulacionActividad(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        PostulacionGestionadaResponse respuesta = aceptarPostulacionService.aceptarActividad(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion());

        assertThat(respuesta.tipo()).isEqualTo(TipoPostulacion.ACTIVIDAD);
        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(respuesta.idMiembro()).isNotNull();
        assertThat(respuesta.idAsignacionActividad()).isNotNull();

        Long asignaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM asignacion_actividad
                WHERE id_requerimiento_act = ? AND id_miembro = ? AND id_actividad = ?
                """, Long.class, idRequerimiento, respuesta.idMiembro(), idActividad);
        assertThat(asignaciones).isEqualTo(1L);

        Long miembros = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM miembros_proyecto
                WHERE id_proyecto = ? AND id_perfil = ?
                  AND estado_participacion = 'ACTIVO'
                  AND id_requerimiento_gral IS NULL
                """, Long.class, publicado.idProyecto(), postulante.idPerfil());
        assertThat(miembros).isEqualTo(1L);

        String estado = jdbcTemplate.queryForObject("""
                SELECT estado FROM postulacion_actividad WHERE id_postulacion = ?
                """, String.class, postulacion.idPostulacion());
        assertThat(estado).isEqualTo("ACEPTADA");

        Long notificacionAceptada = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'POSTULACION_ACEPTADA'
                  AND entidad_tipo = 'POSTULACION_ACTIVIDAD' AND entidad_id = ?
                """, Long.class, postulante.idUsuario(), postulacion.idPostulacion());
        assertThat(notificacionAceptada).isEqualTo(1L);

        Long notificacionAsignacion = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'ACTIVIDAD_ASIGNADA'
                  AND entidad_tipo = 'ACTIVIDAD' AND entidad_id = ?
                """, Long.class, postulante.idUsuario(), idActividad);
        assertThat(notificacionAsignacion).isEqualTo(1L);
    }

    @Test
    void aceptarActividad_llenaCupoYRechazaOtraPendiente() {
        PerfilCreado director = crearPerfil("uc34.act.dir5@example.com", "88834017", "Directora Actividad UC34 Cinco", "modelo");
        PerfilCreado postulanteA = crearPerfil("uc34.act.pos5@example.com", "88834018", "Postulante Actividad A UC34", "modelo");
        PerfilCreado postulanteB = crearPerfil("uc34.act.pos6@example.com", "88834019", "Postulante Actividad B UC34", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC34 Actividad Cupo");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        PostulacionResponse postulacionA = crearPostulacionService.darDeAltaPostulacionActividad(
                postulanteA.idUsuario(), postulanteA.idPerfil(),
                publicado.idProyecto(), idRequerimiento);
        PostulacionResponse postulacionB = crearPostulacionService.darDeAltaPostulacionActividad(
                postulanteB.idUsuario(), postulanteB.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        aceptarPostulacionService.aceptarActividad(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacionA.idPostulacion());

        String estadoB = jdbcTemplate.queryForObject("""
                SELECT estado FROM postulacion_actividad WHERE id_postulacion = ?
                """, String.class, postulacionB.idPostulacion());
        assertThat(estadoB).isEqualTo("RECHAZADA");

        Long notificacionB = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'POSTULACION_RECHAZADA'
                  AND entidad_tipo = 'POSTULACION_ACTIVIDAD' AND entidad_id = ?
                """, Long.class, postulanteB.idUsuario(), postulacionB.idPostulacion());
        assertThat(notificacionB).isEqualTo(1L);
    }

    @Test
    void aceptarActividad_sinDisponibilidad_noCreaMiembroNiAsignacion() {
        PerfilCreado director = crearPerfil("uc34.act.dir2@example.com", "88834012", "Directora Actividad UC34 Dos", "modelo");
        PerfilCreado postulante = crearPerfil("uc34.act.pos2@example.com", "88834013", "Postulante Actividad UC34 Dos", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC34 Actividad Bloqueada");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        PostulacionResponse postulacion = crearPostulacionService.darDeAltaPostulacionActividad(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        Agenda agenda = agendaRepository.findByUsuario_IdUsuario(postulante.idUsuario()).orElseThrow();
        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now().plusDays(60);

        bloqueoAgendaRepository.saveAndFlush(BloqueoAgenda.builder()
                .agenda(agenda)
                .fechaHoraInicio(desde)
                .fechaHoraFin(hasta)
                .motivo("Bloqueo UC34")
                .build());

        assertThatThrownBy(() -> aceptarPostulacionService.aceptarActividad(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion()))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("disponibilidad");

        String estado = jdbcTemplate.queryForObject("""
                SELECT estado FROM postulacion_actividad WHERE id_postulacion = ?
                """, String.class, postulacion.idPostulacion());
        assertThat(estado).isEqualTo("PENDIENTE");

        Long asignaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM asignacion_actividad
                WHERE id_requerimiento_act = ?
                """, Long.class, idRequerimiento);
        assertThat(asignaciones).isZero();

        Long miembros = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM miembros_proyecto
                WHERE id_proyecto = ? AND id_perfil = ? AND estado_participacion = 'ACTIVO'
                """, Long.class, publicado.idProyecto(), postulante.idPerfil());
        assertThat(miembros).isZero();
    }

    @Test
    void aceptarActividad_miembroExistente_reutilizaMembresia() {
        PerfilCreado director = crearPerfil("uc34.act.dir3@example.com", "88834014", "Directora Actividad UC34 Tres", "modelo");
        PerfilCreado miembroPerfil = crearPerfil("uc34.act.mem1@example.com", "88834015", "Miembro Actividad UC34", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC34 Actividad Miembro");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        Proyecto proyecto = proyectoRepository.findById(publicado.idProyecto()).orElseThrow();
        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(miembroPerfil.idPerfil(), miembroPerfil.idUsuario())
                .orElseThrow();
        RolProyecto rolMiembro = rolProyectoRepository.findByCodigo("MIEMBRO").orElseThrow();

        MiembroProyecto existente = miembroProyectoRepository.saveAndFlush(
                MiembroProyecto.builder()
                        .proyecto(proyecto)
                        .perfil(perfil)
                        .rolProyecto(rolMiembro)
                        .estadoParticipacion(EstadoParticipacion.ACTIVO)
                        .fechaAlta(LocalDateTime.now())
                        .build());

        RequerimientoActividad requerimiento = requerimientoActividadRepository
                .findById(idRequerimiento).orElseThrow();
        PostulacionActividad postulacion = postulacionActividadRepository.saveAndFlush(
                PostulacionActividad.builder()
                        .perfil(perfil)
                        .requerimiento(requerimiento)
                        .estado(EstadoSolicitud.PENDIENTE)
                        .fechaPostulacion(LocalDateTime.now())
                        .build());

        PostulacionGestionadaResponse respuesta = aceptarPostulacionService.aceptarActividad(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacion.getIdPostulacion());

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(respuesta.idMiembro()).isEqualTo(existente.getIdMiembro());

        Long miembros = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM miembros_proyecto
                WHERE id_proyecto = ? AND id_perfil = ? AND estado_participacion = 'ACTIVO'
                """, Long.class, publicado.idProyecto(), miembroPerfil.idPerfil());
        assertThat(miembros).isEqualTo(1L);

        Long asignaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM asignacion_actividad
                WHERE id_requerimiento_act = ? AND id_miembro = ?
                """, Long.class, idRequerimiento, existente.getIdMiembro());
        assertThat(asignaciones).isEqualTo(1L);
    }

    @Test
    void aceptarActividad_directorNoPuedePostularse() {
        PerfilCreado director = crearPerfil("uc34.act.dir4@example.com", "88834016", "Directora Actividad UC34 Cuatro", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC34 Actividad Director");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        Perfil perfilDirector = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(director.idPerfil(), director.idUsuario())
                .orElseThrow();
        RequerimientoActividad requerimiento = requerimientoActividadRepository
                .findById(idRequerimiento).orElseThrow();

        PostulacionActividad postulacion = postulacionActividadRepository.saveAndFlush(
                PostulacionActividad.builder()
                        .perfil(perfilDirector)
                        .requerimiento(requerimiento)
                        .estado(EstadoSolicitud.PENDIENTE)
                        .fechaPostulacion(LocalDateTime.now())
                        .build());

        assertThatThrownBy(() -> aceptarPostulacionService.aceptarActividad(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacion.getIdPostulacion()))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("director");

        Long asignaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM asignacion_actividad
                WHERE id_requerimiento_act = ?
                """, Long.class, idRequerimiento);
        assertThat(asignaciones).isZero();
    }

    @Test
    void rechazarGeneral_cambiaEstadoYNotificaSinCrearMiembro() {
        PerfilCreado director = crearPerfil("uc34.rej.dir1@example.com", "88834020", "Directora Rechazo UC34", "modelo");
        PerfilCreado postulante = crearPerfil("uc34.rej.pos1@example.com", "88834021", "Postulante Rechazo UC34", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC34 Rechazo");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        PostulacionResponse postulacion = crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        PostulacionGestionadaResponse respuesta = rechazarPostulacionService.rechazarGeneral(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion());

        assertThat(respuesta.tipo()).isEqualTo(TipoPostulacion.GENERAL);
        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(respuesta.idMiembro()).isNull();
        assertThat(respuesta.idAsignacionActividad()).isNull();

        String estado = jdbcTemplate.queryForObject("""
                SELECT estado FROM postulacion_gral WHERE id_postulacion_gral = ?
                """, String.class, postulacion.idPostulacion());
        assertThat(estado).isEqualTo("RECHAZADA");

        Long miembros = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM miembros_proyecto
                WHERE id_proyecto = ? AND id_perfil = ? AND estado_participacion = 'ACTIVO'
                """, Long.class, publicado.idProyecto(), postulante.idPerfil());
        assertThat(miembros).isZero();

        Long notificacion = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'POSTULACION_RECHAZADA'
                  AND entidad_tipo = 'POSTULACION_GRAL' AND entidad_id = ?
                """, Long.class, postulante.idUsuario(), postulacion.idPostulacion());
        assertThat(notificacion).isEqualTo(1L);

        assertThatThrownBy(() -> rechazarPostulacionService.rechazarGeneral(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion()))
                .isInstanceOf(PostulacionYaGestionadaException.class);
    }

    @Test
    void rechazarActividad_cambiaEstadoYNotificaSinCrearAsignacion() {
        PerfilCreado director = crearPerfil("uc34.rej.dir2@example.com", "88834022", "Directora Rechazo Actividad UC34", "modelo");
        PerfilCreado postulante = crearPerfil("uc34.rej.pos2@example.com", "88834023", "Postulante Rechazo Actividad UC34", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC34 Rechazo Actividad");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        PostulacionResponse postulacion = crearPostulacionService.darDeAltaPostulacionActividad(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        PostulacionGestionadaResponse respuesta = rechazarPostulacionService.rechazarActividad(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion());

        assertThat(respuesta.tipo()).isEqualTo(TipoPostulacion.ACTIVIDAD);
        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(respuesta.idMiembro()).isNull();
        assertThat(respuesta.idAsignacionActividad()).isNull();

        String estado = jdbcTemplate.queryForObject("""
                SELECT estado FROM postulacion_actividad WHERE id_postulacion = ?
                """, String.class, postulacion.idPostulacion());
        assertThat(estado).isEqualTo("RECHAZADA");

        Long asignaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM asignacion_actividad WHERE id_requerimiento_act = ?
                """, Long.class, idRequerimiento);
        assertThat(asignaciones).isZero();

        Long notificacion = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'POSTULACION_RECHAZADA'
                  AND entidad_tipo = 'POSTULACION_ACTIVIDAD' AND entidad_id = ?
                """, Long.class, postulante.idUsuario(), postulacion.idPostulacion());
        assertThat(notificacion).isEqualTo(1L);
    }

    @Test
    void postularActividad_comoMiembroActivo_permite() {
        PerfilCreado director = crearPerfil("uc26.mem.dir1@example.com", "88827001", "Directora Miembro UC26", "modelo");
        PerfilCreado miembro = crearPerfil("uc26.mem.pos1@example.com", "88827002", "Miembro UC26", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC26 Miembro Actividad");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        agregarMiembro(publicado, miembro);

        PostulacionResponse respuesta = crearPostulacionService.darDeAltaPostulacionActividad(
                miembro.idUsuario(), miembro.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(respuesta.tipo()).isEqualTo(TipoPostulacion.ACTIVIDAD);
    }

    @Test
    void postularActividad_comoDirector_lanzaExcepcion() {
        PerfilCreado director = crearPerfil("uc26.dir.act@example.com", "88827003", "Director UC26 Actividad", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC26 Director Actividad");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        assertThatThrownBy(() -> crearPostulacionService.darDeAltaPostulacionActividad(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), idRequerimiento))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("director");
    }

    @Test
    void postularGeneral_comoMiembroActivo_lanzaExcepcion() {
        PerfilCreado director = crearPerfil("uc26.mem.dir2@example.com", "88827004", "Directora Miembro General UC26", "modelo");
        PerfilCreado miembro = crearPerfil("uc26.mem.pos2@example.com", "88827005", "Miembro General UC26", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC26 Miembro General");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        agregarMiembro(publicado, miembro);

        assertThatThrownBy(() -> crearPostulacionService.darDeAltaPostulacionGeneral(
                miembro.idUsuario(), miembro.idPerfil(),
                publicado.idProyecto(), idRequerimiento))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("miembro activo");
    }

    @Test
    void postularGeneral_enProyectoConfirmado_permite() {
        PerfilCreado director = crearPerfil("uc26.conf.dir@example.com", "88827006", "Directora Confirmado UC26", "modelo");
        PerfilCreado postulante = crearPerfil("uc26.conf.pos@example.com", "88827007", "Postulante Confirmado UC26", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC26 Confirmado");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        Proyecto proyecto = proyectoRepository.findById(publicado.idProyecto()).orElseThrow();
        proyecto.setEstado(EstadoProyecto.CONFIRMADO);
        proyecto.setFechaInicio(java.time.LocalDate.now());
        proyectoRepository.saveAndFlush(proyecto);

        PostulacionResponse respuesta = crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(respuesta.tipo()).isEqualTo(TipoPostulacion.GENERAL);
    }

    @Test
    void general_duplicada_lanzaExcepcionYNoPersisteSegundaFila() {
        PerfilCreado director = crearPerfil("uc26.dir2@example.com", "88826003", "Directora Dup", "modelo");
        PerfilCreado postulante = crearPerfil("uc26.pos2@example.com", "88826004", "Postulante Dup", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC26 Dup");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(), publicado.idProyecto(), idRequerimiento);

        assertThatThrownBy(() -> crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(), publicado.idProyecto(), idRequerimiento))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("Ya existe una postulación");

        Long postulaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM postulacion_gral
                WHERE id_perfil = ? AND id_requerimiento_gral = ?
                """, Long.class, postulante.idPerfil(), idRequerimiento);
        assertThat(postulaciones).isEqualTo(1L);
    }

    @Test
    void general_proyectoCancelado_lanzaExcepcion() {
        PerfilCreado director = crearPerfil("uc26.dir3@example.com", "88826005", "Directora Cancela", "modelo");
        PerfilCreado postulante = crearPerfil("uc26.pos3@example.com", "88826006", "Postulante Cancela", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC26 Cancelado");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        cancelarProyectoService.cancelar(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CancelarProyectoRequest("Proyecto cancelado en UC-26"));

        assertThatThrownBy(() -> crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(), publicado.idProyecto(), idRequerimiento))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("ya no acepta postulaciones");
    }

    @Test
    void general_profesionDistinta_lanzaExcepcion() {
        PerfilCreado director = crearPerfil("uc26.dir4@example.com", "88826007", "Directora Prof", "modelo");
        PerfilCreado fotografa = crearPerfil("uc26.pos4@example.com", "88826008", "Fotógrafa UC26", "fotografo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC26 Profesión");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        assertThatThrownBy(() -> crearPostulacionService.darDeAltaPostulacionGeneral(
                fotografa.idUsuario(), fotografa.idPerfil(), publicado.idProyecto(), idRequerimiento))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("profesión");
    }

    @Test
    void general_directorYaEsMiembroActivo_lanzaExcepcion() {
        PerfilCreado director = crearPerfil("uc26.dir5@example.com", "88826009", "Directora Miembro", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC26 Miembro");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        assertThatThrownBy(() -> crearPostulacionService.darDeAltaPostulacionGeneral(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(), idRequerimiento))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("miembro activo");
    }

    // ---------------------------------------------------------------
    // Requerimiento de actividad
    // ---------------------------------------------------------------

    @Test
    void actividad_conFechaInicioConfirmada_persisteYNotificaAlDirector() {
        PerfilCreado director = crearPerfil("uc26.dir6@example.com", "88826010", "Directora Act", "modelo");
        PerfilCreado postulante = crearPerfil("uc26.pos6@example.com", "88826011", "Postulante Act", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC26 Actividad");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        confirmarInicioDelProyecto(publicado.idProyecto());

        PostulacionResponse respuesta = crearPostulacionService.darDeAltaPostulacionActividad(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        assertThat(respuesta.tipo()).isEqualTo(TipoPostulacion.ACTIVIDAD);
        assertThat(respuesta.tentativa()).isFalse();
        assertThat(respuesta.idRequerimiento()).isEqualTo(idRequerimiento);

        Long postulaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM postulacion_actividad
                WHERE id_perfil = ? AND id_requerimiento_act = ? AND estado = 'PENDIENTE'
                """, Long.class, postulante.idPerfil(), idRequerimiento);
        assertThat(postulaciones).isEqualTo(1L);

        Long notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'POSTULACION_RECIBIDA'
                  AND entidad_tipo = 'POSTULACION_ACTIVIDAD'
                  AND entidad_id = ?
                """, Long.class, director.idUsuario(), respuesta.idPostulacion());
        assertThat(notificaciones).isEqualTo(1L);
    }

    @Test
    void actividad_sinFechaInicioDeProyecto_marcaLaDisponibilidadComoProvisional() {
        PerfilCreado director = crearPerfil("uc26.dir7@example.com", "88826012", "Directora Tentativa", "modelo");
        PerfilCreado postulante = crearPerfil("uc26.pos7@example.com", "88826013", "Postulante Tentativa", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC26 Tentativo");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        PostulacionResponse respuesta = crearPostulacionService.darDeAltaPostulacionActividad(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        assertThat(respuesta.tentativa()).isTrue();
    }

    @Test
    void actividad_sinHuecoLibre_lanzaExcepcionYNoPersiste() {
        PerfilCreado director = crearPerfil("uc26.dir8@example.com", "88826014", "Directora Ocupada", "modelo");
        PerfilCreado postulante = crearPerfil("uc26.pos8@example.com", "88826015", "Postulante Ocupada", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC26 Ocupado");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        LocalDateTime inicio = confirmarInicioDelProyecto(publicado.idProyecto());
        LocalDate fechaEntrega = jdbcTemplate.queryForObject("""
                SELECT p.fecha_entrega FROM planificacion p WHERE p.id_proyecto = ?
                """, LocalDate.class, publicado.idProyecto());
        bloquearTodoElRangoFactible(postulante, inicio, fechaEntrega.atTime(LocalTime.of(23, 59)));

        assertThatThrownBy(() -> crearPostulacionService.darDeAltaPostulacionActividad(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("disponibilidad");

        Long postulaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM postulacion_actividad
                WHERE id_perfil = ? AND id_requerimiento_act = ?
                """, Long.class, postulante.idPerfil(), idRequerimiento);
        assertThat(postulaciones).isZero();
    }

    // ---------------------------------------------------------------
    // Datos de prueba
    // ---------------------------------------------------------------

    private MiembroProyecto agregarMiembro(
            ProyectoResponse publicado,
            PerfilCreado perfilCreado) {

        Proyecto proyecto = proyectoRepository.findById(publicado.idProyecto()).orElseThrow();
        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(perfilCreado.idPerfil(), perfilCreado.idUsuario())
                .orElseThrow();
        RolProyecto rolMiembro = rolProyectoRepository.findByCodigo("MIEMBRO").orElseThrow();

        return miembroProyectoRepository.saveAndFlush(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .fechaAlta(LocalDateTime.now())
                .build());
    }

    private PerfilCreado crearPerfil(
            String correo,
            String dni,
            String nombreArtistico,
            String codigoProfesion) {

        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("UC26").apellido("Test").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(26))
                .correo(correo)
                .estado(org.mgroko.backend.modelo.enums.EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Long idProfesion = profesionRepository.findByCodigoIgnoreCase(codigoProfesion)
                .map(Profesion::getIdProfesion)
                .orElseGet(() -> profesionRepository.buscar("%" + codigoProfesion + "%", codigoProfesion)
                        .get(0).getIdProfesion());

        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesion,
                        "Perfil de prueba UC-26.", List.of()));

        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, id_tyc, descripcion, fecha_inicio) VALUES (?, 1, 'TyC UC-26', NOW())",
                perfil.idPerfil());

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private Long idProfesion(String codigo) {
        return profesionRepository.buscar("%" + codigo + "%", codigo).get(0).getIdProfesion();
    }

    private CrearProyectoRequest proyectoConRequerimientoGral(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Proyecto de prueba UC-26", Privacidad.PUBLICO,
                LocalDate.now().plusDays(30), true,
                new UbicacionRequest("0602801002"),
                null,
                List.of(new CrearRequerimientoGralRequest(
                        1, idProfesion("modelo"), "Una modelo", null, null)),
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private CrearProyectoRequest proyectoConActividad(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Proyecto de prueba UC-26 con actividad", Privacidad.PUBLICO,
                LocalDate.now().plusDays(30), false,
                new UbicacionRequest("0602801002"),
                null,
                List.of(),
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private ProyectoResponse publicarProyectoConRequerimientoGral(
            PerfilCreado director,
            String nombre) {

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(), proyectoConRequerimientoGral(nombre));

        return publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto());
    }

    private ProyectoResponse publicarProyectoConActividad(
            PerfilCreado director,
            String nombre) {

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(), proyectoConActividad(nombre));

        ActividadResponse actividad = crearActividadService.crear(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto(),
                new CrearActividadRequest(
                        "Prueba UC26", "Actividad de prueba UC-26", 2, UnidadDuracion.HORAS,
                        null, null,
                        List.of(new CrearRequerimientoActividadRequest(
                                1, idProfesion("modelo"), "Una modelo", null, null))));

        assertThat(actividad.idActividad()).isNotNull();

        return publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto());
    }

    private Long requerimientoDeActividad(ProyectoResponse publicado) {
        return requerimientoActividadRepository
                .findByActividad_IdActividad(idActividadDe(publicado.idProyecto()))
                .get(0)
                .getIdRequerimiento();
    }

    private Long idActividadDe(Long idProyecto) {
        return jdbcTemplate.queryForObject("""
                SELECT a.id_actividad FROM actividad a
                JOIN planificacion p ON p.id_planificacion = a.id_planificacion
                WHERE p.id_proyecto = ?
                """, Long.class, idProyecto);
    }

    /**
     * Fija una fecha de inicio real en el próximo día hábil a las 09:00 para
     * que la verificación de disponibilidad deje de ser tentativa.
     */
    private LocalDateTime confirmarInicioDelProyecto(Long idProyecto) {
        LocalDate fecha = LocalDate.now().plusDays(1);
        while (fecha.getDayOfWeek().getValue() > 5) {
            fecha = fecha.plusDays(1);
        }
        LocalDateTime inicio = fecha.atTime(LocalTime.of(9, 0));

        Proyecto proyecto = proyectoRepository.findById(idProyecto).orElseThrow();
        proyecto.setFechaInicio(inicio);
        proyectoRepository.saveAndFlush(proyecto);

        return inicio;
    }

    /**
     * Bloquea toda la ventana factible (desde el inicio confirmado hasta la
     * fecha de entrega) para que no quede ningún hueco en la jornada. La regla
     * de disponibilidad acepta cualquier hueco dentro del rango factible, no
     * solo el primer día.
     */
    private void bloquearTodoElRangoFactible(
            PerfilCreado perfil,
            LocalDateTime inicio,
            LocalDateTime fin) {
        Agenda agenda = agendaRepository.findByUsuario_IdUsuario(perfil.idUsuario()).orElseThrow();

        bloqueoAgendaRepository.saveAndFlush(BloqueoAgenda.builder()
                .agenda(agenda)
                .fechaHoraInicio(inicio)
                .fechaHoraFin(fin)
                .motivo("Jornada completa ocupada")
                .build());
    }
}
