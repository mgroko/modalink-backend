package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.AsignacionActividadResponse;
import org.mgroko.backend.actividades.dto.AsignarActividadRequest;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.dto.CrearRequerimientoActividadRequest;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.exception.AsignacionActividadNoPermitidaException;
import org.mgroko.backend.actividades.exception.AsignacionNoDisponibleException;
import org.mgroko.backend.actividades.servicio.AsignarActividadService;
import org.mgroko.backend.actividades.servicio.CrearActividadService;
import org.mgroko.backend.modelo.Agenda;
import org.mgroko.backend.modelo.BloqueoAgenda;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.PublicarProyectoService;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración de UC-70 contra PostgreSQL real: asignación directa, vínculo
 * automático con requerimiento, cupo, disponibilidad y notificación por
 * trigger.
 */
@SpringBootTest
@Transactional
class AsignarActividadIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private AsignarActividadService asignarActividadService;
    @Autowired
    private CrearPerfilService crearPerfilService;
    @Autowired
    private CrearProyectoService crearProyectoService;
    @Autowired
    private PublicarProyectoService publicarProyectoService;
    @Autowired
    private CrearActividadService crearActividadService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolGlobalRepository rolGlobalRepository;
    @Autowired
    private GeneroRepository generoRepository;
    @Autowired
    private ProfesionRepository profesionRepository;
    @Autowired
    private ProyectoRepository proyectoRepository;
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private RolProyectoRepository rolProyectoRepository;
    @Autowired
    private MiembroProyectoRepository miembroProyectoRepository;
    @Autowired
    private AgendaRepository agendaRepository;
    @Autowired
    private BloqueoAgendaRepository bloqueoAgendaRepository;
    @Autowired
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record PerfilCreado(Long idUsuario, Long idPerfil) {}

    @Test
    void asignar_persisteVinculaRequerimientoYNotifica() {
        PerfilCreado director = crearPerfil(
                "uc70.dir1@example.com", "88870001", "Directora UC70", "modelo");
        PerfilCreado miembro = crearPerfil(
                "uc70.mie1@example.com", "88870002", "Miembro UC70", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(
                director, "Desfile UC70 Uno");
        Long idActividad = idActividadDe(publicado.idProyecto());
        Long idRequerimiento = requerimientoDeActividad(publicado);
        MiembroProyecto membresia = agregarMiembro(publicado, miembro);

        AsignacionActividadResponse response = asignarActividadService.asignar(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), idActividad,
                new AsignarActividadRequest(miembro.idPerfil()));

        assertThat(response.idAsignacionActividad()).isNotNull();
        assertThat(response.idMiembro()).isEqualTo(membresia.getIdMiembro());
        assertThat(response.idRequerimientoActividad()).isEqualTo(idRequerimiento);
        assertThat(response.requerimientoSatisfecho()).isTrue();
        assertThat(response.provisional()).isTrue();

        Long asignaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM asignacion_actividad
                WHERE id_actividad = ? AND id_miembro = ? AND id_requerimiento_act = ?
                """, Long.class, idActividad, membresia.getIdMiembro(), idRequerimiento);
        assertThat(asignaciones).isEqualTo(1L);

        Long notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'ACTIVIDAD_ASIGNADA'
                  AND entidad_tipo = 'ACTIVIDAD'
                  AND entidad_id = ?
                """, Long.class, miembro.idUsuario(), idActividad);
        assertThat(notificaciones).isEqualTo(1L);

        LocalDateTime fechaInicio = jdbcTemplate.queryForObject("""
                SELECT fecha_hora_inicio FROM actividad WHERE id_actividad = ?
                """, LocalDateTime.class, idActividad);
        assertThat(fechaInicio).isNull();
    }

    @Test
    void asignar_enProyectoConfirmado_marcaDisponibilidadNoProvisional() {
        PerfilCreado director = crearPerfil(
                "uc70.dir2@example.com", "88870003", "Directora UC70 Dos", "modelo");
        PerfilCreado miembro = crearPerfil(
                "uc70.mie2@example.com", "88870004", "Miembro UC70 Dos", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(
                director, "Desfile UC70 Dos");
        Long idActividad = idActividadDe(publicado.idProyecto());
        agregarMiembro(publicado, miembro);
        confirmarInicioDelProyecto(publicado.idProyecto());

        AsignacionActividadResponse response = asignarActividadService.asignar(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), idActividad,
                new AsignarActividadRequest(miembro.idPerfil()));

        assertThat(response.provisional()).isFalse();
        assertThat(response.inicioSeleccionado()).isNotNull();
    }

    @Test
    void asignar_sinDisponibilidad_noPersiste() {
        PerfilCreado director = crearPerfil(
                "uc70.dir3@example.com", "88870005", "Directora UC70 Tres", "modelo");
        PerfilCreado miembro = crearPerfil(
                "uc70.mie3@example.com", "88870006", "Miembro UC70 Tres", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(
                director, "Desfile UC70 Tres");
        Long idActividad = idActividadDe(publicado.idProyecto());
        agregarMiembro(publicado, miembro);
        bloquearTodoElRangoFactible(miembro);

        assertThatThrownBy(() -> asignarActividadService.asignar(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), idActividad,
                new AsignarActividadRequest(miembro.idPerfil())))
                .isInstanceOf(AsignacionNoDisponibleException.class);

        Long asignaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM asignacion_actividad WHERE id_actividad = ?
                """, Long.class, idActividad);
        assertThat(asignaciones).isZero();
    }

    @Test
    void asignar_duplicada_lanzaConflicto() {
        PerfilCreado director = crearPerfil(
                "uc70.dir4@example.com", "88870007", "Directora UC70 Cuatro", "modelo");
        PerfilCreado miembro = crearPerfil(
                "uc70.mie4@example.com", "88870008", "Miembro UC70 Cuatro", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(
                director, "Desfile UC70 Cuatro");
        Long idActividad = idActividadDe(publicado.idProyecto());
        agregarMiembro(publicado, miembro);

        asignarActividadService.asignar(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), idActividad,
                new AsignarActividadRequest(miembro.idPerfil()));

        assertThatThrownBy(() -> asignarActividadService.asignar(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), idActividad,
                new AsignarActividadRequest(miembro.idPerfil())))
                .isInstanceOf(AsignacionActividadNoPermitidaException.class)
                .hasMessageContaining("ya está asignado");
    }

    @Test
    void asignar_aDirector_noConsumeCupo() {
        PerfilCreado director = crearPerfil(
                "uc70.dir5@example.com", "88870009", "Directora UC70 Cinco", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(
                director, "Desfile UC70 Cinco");
        Long idActividad = idActividadDe(publicado.idProyecto());
        Long idRequerimiento = requerimientoDeActividad(publicado);

        AsignacionActividadResponse response = asignarActividadService.asignar(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), idActividad,
                new AsignarActividadRequest(director.idPerfil()));

        assertThat(response.idRequerimientoActividad()).isNull();

        Long cubiertos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM asignacion_actividad
                WHERE id_requerimiento_act = ?
                """, Long.class, idRequerimiento);
        assertThat(cubiertos).isZero();
    }

    @Test
    void asignar_sinFechaEntrega_lanzaExcepcion() {
        PerfilCreado director = crearPerfil(
                "uc70.dir6@example.com", "88870010", "Directora UC70 Seis", "modelo");
        PerfilCreado miembro = crearPerfil(
                "uc70.mie6@example.com", "88870011", "Miembro UC70 Seis", "modelo");

        // Un proyecto BORRADOR puede existir sin fecha de entrega; la base
        // impide quitársela a uno PUBLICADO.
        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                new CrearProyectoRequest(
                        "Desfile UC70 Seis", "Proyecto de prueba UC-70", Privacidad.PUBLICO,
                        null, false,
                        new UbicacionRequest("0602801002"),
                        null,
                        List.of(),
                        new CrearMoodboardRequest("Telas frías, tonos neutros")));

        Long idProfesion = profesionRepository.findByCodigoIgnoreCase("modelo")
                .map(Profesion::getIdProfesion)
                .orElseThrow();

        crearActividadService.crear(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto(),
                new CrearActividadRequest(
                        "Prueba UC70 Sin Fecha", "Actividad de prueba UC-70", 2,
                        UnidadDuracion.HORAS, null, null,
                        List.of(new CrearRequerimientoActividadRequest(
                                1, idProfesion, "Una modelo", null, null))));

        Long idActividad = idActividadDe(borrador.idProyecto());
        agregarMiembro(borrador, miembro);

        assertThatThrownBy(() -> asignarActividadService.asignar(
                director.idUsuario(), director.idPerfil(),
                borrador.idProyecto(), idActividad,
                new AsignarActividadRequest(miembro.idPerfil())))
                .isInstanceOf(org.mgroko.backend.actividades.exception
                        .FechaEntregaNoDefinidaException.class);
    }

    // ---------------------------------------------------------------
    // Datos de prueba
    // ---------------------------------------------------------------

    private MiembroProyecto agregarMiembro(
            ProyectoResponse publicado,
            PerfilCreado perfilCreado) {

        Proyecto proyecto = proyectoRepository.findById(publicado.idProyecto()).orElseThrow();
        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(
                        perfilCreado.idPerfil(), perfilCreado.idUsuario())
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
                .nombre("UC70").apellido("Test").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(26))
                .correo(correo)
                .estado(org.mgroko.backend.modelo.enums.EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Long idProfesion = profesionRepository.findByCodigoIgnoreCase(codigoProfesion)
                .map(Profesion::getIdProfesion)
                .orElseGet(() -> profesionRepository
                        .buscar("%" + codigoProfesion + "%", codigoProfesion)
                        .get(0).getIdProfesion());

        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesion,
                        "Perfil de prueba UC-70.", List.of()));

        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, id_tyc, descripcion, fecha_inicio) "
                        + "VALUES (?, 1, 'TyC UC-70', NOW())",
                perfil.idPerfil());

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private ProyectoResponse publicarProyectoConActividad(
            PerfilCreado director,
            String nombre) {

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                new CrearProyectoRequest(
                        nombre, "Proyecto de prueba UC-70", Privacidad.PUBLICO,
                        LocalDate.now().plusDays(30), false,
                        new UbicacionRequest("0602801002"),
                        null,
                        List.of(),
                        new CrearMoodboardRequest("Telas frías, tonos neutros")));

        Long idProfesion = profesionRepository.findByCodigoIgnoreCase("modelo")
                .map(Profesion::getIdProfesion)
                .orElseThrow();

        crearActividadService.crear(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto(),
                new CrearActividadRequest(
                        "Prueba UC70", "Actividad de prueba UC-70", 2, UnidadDuracion.HORAS,
                        null, null,
                        List.of(new CrearRequerimientoActividadRequest(
                                1, idProfesion, "Una modelo", null, null))));

        return publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto());
    }

    private Long idActividadDe(Long idProyecto) {
        return jdbcTemplate.queryForObject("""
                SELECT a.id_actividad FROM actividad a
                JOIN planificacion p ON p.id_planificacion = a.id_planificacion
                WHERE p.id_proyecto = ?
                """, Long.class, idProyecto);
    }

    private Long requerimientoDeActividad(ProyectoResponse publicado) {
        return requerimientoActividadRepository
                .findByActividad_IdActividad(idActividadDe(publicado.idProyecto()))
                .get(0)
                .getIdRequerimiento();
    }

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

    private void bloquearTodoElRangoFactible(PerfilCreado perfil) {
        Agenda agenda = agendaRepository
                .findByUsuario_IdUsuario(perfil.idUsuario())
                .orElseThrow();

        bloqueoAgendaRepository.saveAndFlush(BloqueoAgenda.builder()
                .agenda(agenda)
                .fechaHoraInicio(LocalDateTime.now().minusDays(1))
                .fechaHoraFin(LocalDateTime.now().plusDays(60))
                .motivo("Bloqueo UC-70")
                .build());
    }
}
