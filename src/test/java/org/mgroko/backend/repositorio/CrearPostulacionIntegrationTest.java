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
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.postulaciones.dto.PostulacionResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.exception.PostulacionNoPermitidaException;
import org.mgroko.backend.postulaciones.servicio.CrearPostulacionService;
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
    private ProyectoRepository proyectoRepository;
    @Autowired
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Autowired
    private AgendaRepository agendaRepository;
    @Autowired
    private BloqueoAgendaRepository bloqueoAgendaRepository;
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
        bloquearTodaLaJornada(postulante, inicio);

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

    private void bloquearTodaLaJornada(PerfilCreado perfil, LocalDateTime inicio) {
        Agenda agenda = agendaRepository.findByUsuario_IdUsuario(perfil.idUsuario()).orElseThrow();

        bloqueoAgendaRepository.saveAndFlush(BloqueoAgenda.builder()
                .agenda(agenda)
                .fechaHoraInicio(inicio)
                .fechaHoraFin(inicio.plusHours(9))
                .motivo("Jornada completa ocupada")
                .build());
    }
}
