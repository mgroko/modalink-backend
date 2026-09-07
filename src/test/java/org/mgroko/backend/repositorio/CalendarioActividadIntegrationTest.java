package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Agenda;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.BloqueoAgenda;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Integración del bloqueo calculado por actividad (trigger V14c) y de la
 * consulta {@link ActividadRepository#findActividadesDeUsuario}.
 *
 * Requiere armar la cadena completa usuario -> perfil -> miembro ->
 * asignación -> actividad -> planificación -> proyecto, por lo que se usa
 * {@link EntityManager} para las entidades que no tienen repositorio.
 */
@SpringBootTest
@Transactional
class CalendarioActividadIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private BloqueoAgendaRepository bloqueoAgendaRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    private Usuario guardarUsuario(String correo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Ana").apellido("Gomez").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(22))
                .correo(correo).rolGlobal(rol).genero(genero).build());
    }

    private Agenda agendaDe(Usuario usuario) {
        return agendaRepository.findByUsuario_IdUsuario(usuario.getIdUsuario()).orElseThrow();
    }

    private RolProyecto rolMiembro() {
        return em.createQuery("SELECT r FROM RolProyecto r WHERE r.nombre = :n", RolProyecto.class)
                .setParameter("n", "Miembro")
                .getSingleResult();
    }

    /**
     * Construye la cadena completa para que {@code usuario} quede asignado
     * a una actividad de un proyecto en el estado indicado.
     *
     * @return la Actividad persistida (con fechaHoraFin recargada de la BD)
     */
    private Actividad crearCadenaActividad(Usuario usuario, EstadoProyecto estado,
                                           LocalDateTime actInicio, int duracionMin) {
        Profesion profesion = Profesion.builder().nombre("ProfTestCalendario").build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Luna").biografia("Perfil de test")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario).profesion(profesion).build();
        em.persist(perfil);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto Test").descripcion("Descripción de test")
                .fechaInicio(LocalDate.now()).estado(estado).privacidad(Privacidad.Publico).build();
        em.persist(proyecto);

        Planificacion planificacion = Planificacion.builder().proyecto(proyecto).build();
        em.persist(planificacion);

        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto).perfil(perfil).rolProyecto(rolMiembro())
                .estadoParticipacion(EstadoParticipacion.Activo).build();
        em.persist(miembro);

        Actividad actividad = Actividad.builder()
                .nombre("Sesión").duracionMinutos(duracionMin)
                .fechaHoraInicio(actInicio).planificacion(planificacion).build();
        em.persist(actividad);

        AsignacionActividad asignacion = AsignacionActividad.builder()
                .miembro(miembro).actividad(actividad).build();
        em.persist(asignacion);

        em.flush();
        em.refresh(actividad); // carga la columna generada fecha_hora_fin
        return actividad;
    }

    private BloqueoAgenda guardarBloqueo(Agenda agenda, LocalDateTime inicio, LocalDateTime fin) {
        return bloqueoAgendaRepository.saveAndFlush(BloqueoAgenda.builder()
                .agenda(agenda).fechaHoraInicio(inicio).fechaHoraFin(fin).build());
    }

    // ------------------------------------------------------------------
    // Trigger: no liberar un bloqueo cubierto por actividad activa
    // ------------------------------------------------------------------

    @Test
    void eliminarBloqueoCubiertoPorActividad_lanzaExcepcion() {
        Usuario usuario = guardarUsuario("act@example.com", "88880001");
        Agenda agenda = agendaDe(usuario);
        // Actividad 10:00-12:00 (margen 60 -> ocupa 09:00-13:00)
        crearCadenaActividad(usuario, EstadoProyecto.Publicado,
                LocalDateTime.of(2026, 9, 15, 10, 0), 120);
        BloqueoAgenda bloqueo = guardarBloqueo(agenda,
                LocalDateTime.of(2026, 9, 15, 10, 30), LocalDateTime.of(2026, 9, 15, 11, 30));

        assertExcepcionConMensaje(() -> {
            bloqueoAgendaRepository.delete(bloqueo);
            em.flush();
        }, "No se puede liberar un horario comprometido por una actividad");
    }

    @Test
    void eliminarBloqueoCubiertoSoloPorMargen_lanzaExcepcion() {
        Usuario usuario = guardarUsuario("act.margen@example.com", "88880002");
        Agenda agenda = agendaDe(usuario);
        // Actividad 10:00-12:00 (margen 60 -> ocupa 09:00-13:00)
        crearCadenaActividad(usuario, EstadoProyecto.Confirmado,
                LocalDateTime.of(2026, 9, 15, 10, 0), 120);
        // Bloqueo [08:30, 09:30]: solapa solo por el margen de la actividad.
        BloqueoAgenda bloqueo = guardarBloqueo(agenda,
                LocalDateTime.of(2026, 9, 15, 8, 30), LocalDateTime.of(2026, 9, 15, 9, 30));

        assertExcepcionConMensaje(() -> {
            bloqueoAgendaRepository.delete(bloqueo);
            em.flush();
        }, "No se puede liberar un horario comprometido por una actividad");
    }

    @Test
    void eliminarBloqueoConProyectoNoActivo_permite() {
        Usuario usuario = guardarUsuario("act.borrador@example.com", "88880003");
        Agenda agenda = agendaDe(usuario);
        crearCadenaActividad(usuario, EstadoProyecto.Borrador,
                LocalDateTime.of(2026, 9, 15, 10, 0), 120);
        BloqueoAgenda bloqueo = guardarBloqueo(agenda,
                LocalDateTime.of(2026, 9, 15, 10, 30), LocalDateTime.of(2026, 9, 15, 11, 30));

        assertDoesNotThrow(() -> {
            bloqueoAgendaRepository.delete(bloqueo);
            em.flush();
        });
        assertTrue(bloqueoAgendaRepository.findByIdBloqueoAndAgenda_IdAgenda(
                bloqueo.getIdBloqueo(), agenda.getIdAgenda()).isEmpty());
    }

    @Test
    void eliminarBloqueoFueraDeActividad_permite() {
        Usuario usuario = guardarUsuario("act.lejos@example.com", "88880004");
        Agenda agenda = agendaDe(usuario);
        crearCadenaActividad(usuario, EstadoProyecto.Publicado,
                LocalDateTime.of(2026, 9, 15, 10, 0), 120);
        // Bloqueo un día después: sin solape con la actividad.
        BloqueoAgenda bloqueo = guardarBloqueo(agenda,
                LocalDateTime.of(2026, 9, 16, 10, 0), LocalDateTime.of(2026, 9, 16, 12, 0));

        assertDoesNotThrow(() -> {
            bloqueoAgendaRepository.delete(bloqueo);
            em.flush();
        });
    }

    // ------------------------------------------------------------------
    // ActividadRepository.findActividadesDeUsuario
    // ------------------------------------------------------------------

    @Test
    void findActividadesDeUsuario_proyectosActivos_devuelve() {
        Usuario usuario = guardarUsuario("find.act@example.com", "88880005");
        Actividad actividad = crearCadenaActividad(usuario, EstadoProyecto.Publicado,
                LocalDateTime.of(2026, 9, 15, 10, 0), 120);

        List<Actividad> actividades = actividadRepository.findActividadesDeUsuario(
                usuario.getIdUsuario(), List.of(EstadoProyecto.Publicado, EstadoProyecto.Confirmado));

        assertEquals(1, actividades.size());
        assertEquals(actividad.getIdActividad(), actividades.get(0).getIdActividad());
    }

    @Test
    void findActividadesDeUsuario_proyectoBorrador_noDevuelve() {
        Usuario usuario = guardarUsuario("find.borrador@example.com", "88880006");
        crearCadenaActividad(usuario, EstadoProyecto.Borrador,
                LocalDateTime.of(2026, 9, 15, 10, 0), 120);

        List<Actividad> actividades = actividadRepository.findActividadesDeUsuario(
                usuario.getIdUsuario(), List.of(EstadoProyecto.Publicado, EstadoProyecto.Confirmado));

        assertTrue(actividades.isEmpty());
    }

    @Test
    void findActividadesDeUsuario_noIncluyeActividadesDeOtroUsuario() {
        Usuario usuario = guardarUsuario("find.act@example.com", "88880007");
        Usuario otro = guardarUsuario("find.otro@example.com", "88880008");
        // La actividad se asigna a otro usuario.
        crearCadenaActividad(otro, EstadoProyecto.Publicado,
                LocalDateTime.of(2026, 9, 15, 10, 0), 120);

        List<Actividad> actividades = actividadRepository.findActividadesDeUsuario(
                usuario.getIdUsuario(), List.of(EstadoProyecto.Publicado, EstadoProyecto.Confirmado));

        assertTrue(actividades.isEmpty());
    }

    /**
     * Afirma que ejecutar {@code op} lanza una excepción cuya cadena de
     * causas contiene el fragmento de mensaje esperado. Los triggers usan
     * {@code RAISE EXCEPTION}, que Hibernate envuelve en distintos tipos
     * de excepción según la vía de ejecución, por lo que se valida el
     * mensaje raíz en lugar del tipo exacto.
     */
    private void assertExcepcionConMensaje(Executable op, String fragmento) {
        Exception ex = assertThrows(Exception.class, op);
        assertTrue(contieneMensaje(ex, fragmento),
                "Se esperaba el mensaje '" + fragmento + "' pero fue: " + ex.getMessage());
    }

    private boolean contieneMensaje(Throwable t, String fragmento) {
        if (t == null) {
            return false;
        }
        if (t.getMessage() != null && t.getMessage().contains(fragmento)) {
            return true;
        }
        return contieneMensaje(t.getCause(), fragmento);
    }
}