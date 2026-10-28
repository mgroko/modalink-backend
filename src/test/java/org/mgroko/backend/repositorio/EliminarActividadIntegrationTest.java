package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ProfesionalAsignadoResponse;
import org.mgroko.backend.actividades.servicio.EliminarActividadService;
import org.mgroko.backend.actividades.servicio.MiembrosActividadService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
class EliminarActividadIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private EliminarActividadService eliminarActividadService;

    @Autowired
    private MiembrosActividadService miembrosActividadService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void eliminar_borraActividadYSusVinculosConservandoMiembrosYNotificando() {
        Escenario escenario = transactionTemplate.execute(status -> crearEscenario());

        eliminarActividadService.eliminar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                escenario.idActividadEliminada(),
                "  Falta de presupuesto  ");

        assertThat(contar(
                "SELECT COUNT(*) FROM actividad WHERE id_actividad = ?",
                escenario.idActividadEliminada())).isZero();
        assertThat(contar(
                "SELECT COUNT(*) FROM dependencia_actividades "
                        + "WHERE id_actividad_predecesora = ? OR id_actividad_sucesora = ?",
                escenario.idActividadEliminada(),
                escenario.idActividadEliminada())).isZero();
        assertThat(contar(
                "SELECT COUNT(*) FROM dependencia_actividades "
                        + "WHERE id_actividad_sucesora = ? AND id_actividad_predecesora = ?",
                escenario.idActividadSucesora(),
                escenario.idActividadPrevia())).isEqualTo(1);
        assertThat(contar(
                "SELECT COUNT(*) FROM requerimiento_actividad WHERE id_actividad = ?",
                escenario.idActividadEliminada())).isZero();
        assertThat(contar(
                "SELECT COUNT(*) FROM postulacion_actividad WHERE id_requerimiento_act = ?",
                escenario.idRequerimiento())).isZero();
        assertThat(contar(
                "SELECT COUNT(*) FROM invitacion_actividad WHERE id_requerimiento_act = ?",
                escenario.idRequerimiento())).isZero();
        assertThat(contar(
                "SELECT COUNT(*) FROM asignacion_actividad WHERE id_actividad = ?",
                escenario.idActividadEliminada())).isZero();
        assertThat(contar(
                "SELECT COUNT(*) FROM asignacion_actividad WHERE id_actividad = ?",
                escenario.idActividadOtra())).isEqualTo(1);
        assertThat(contar(
                "SELECT COUNT(*) FROM miembros_proyecto WHERE id_proyecto = ?",
                escenario.idProyecto())).isEqualTo(3);
        assertThat(contar(
                "SELECT COUNT(*) FROM hito_actividad WHERE id_actividad = ?",
                escenario.idActividadEliminada())).isZero();
        assertThat(contar("SELECT COUNT(*) FROM hito WHERE id_hito = ?",
                escenario.idHito())).isEqualTo(1);

        LocalDateTime inicioPrevia = jdbcTemplate.queryForObject(
                "SELECT fecha_hora_inicio FROM actividad WHERE id_actividad = ?",
                Timestamp.class,
                escenario.idActividadPrevia()).toLocalDateTime();
        LocalDateTime inicioSucesora = jdbcTemplate.queryForObject(
                "SELECT fecha_hora_inicio FROM actividad WHERE id_actividad = ?",
                Timestamp.class,
                escenario.idActividadSucesora()).toLocalDateTime();
        // Fase 2: eliminar no persiste proyecciones; las fechas ya persistidas
        // se conservan aunque se reconecten las dependencias. PostgreSQL guarda
        // microsegundos, por eso se comparan sin nanosegundos.
        assertThat(inicioPrevia).isEqualToIgnoringNanos(escenario.inicioActividadPrevia());
        assertThat(inicioSucesora).isEqualToIgnoringNanos(escenario.inicioActividadSucesora());

        verificarNotificacion(
                escenario.idUsuarioDirector(),
                NotificacionService.TIPO_ACTIVIDAD_ELIMINADA,
                "ACTIVIDAD",
                escenario.idActividadEliminada());
        verificarNotificacion(
                escenario.idUsuarioMiembro(),
                NotificacionService.TIPO_ACTIVIDAD_ELIMINADA,
                "ACTIVIDAD",
                escenario.idActividadEliminada());
        verificarNotificacion(
                escenario.idUsuarioPostulante(),
                NotificacionService.TIPO_POSTULACION_RECHAZADA,
                "POSTULACION_ACTIVIDAD",
                escenario.idPostulacion());
        verificarNotificacion(
                escenario.idUsuarioInvitado(),
                NotificacionService.TIPO_INVITACION_RECHAZADA,
                "INVITACION_ACTIVIDAD",
                escenario.idInvitacion());
    }

    @Test
    @Transactional
    void miembros_listarExclusivosYQuitarAsignacion_conservaElMiembro() {
        Escenario escenario = crearEscenario();

        List<ProfesionalAsignadoResponse> exclusivos =
                miembrosActividadService.listarExclusivos(escenario.idActividadSolo());

        assertThat(exclusivos).singleElement().satisfies(profesional -> {
            assertThat(profesional.idPerfil()).isEqualTo(escenario.idPerfilMiembroSolo());
            assertThat(profesional.nombreArtistico()).isEqualTo("Perfil MiembroSolo");
            assertThat(profesional.nombreProfesion()).isNotNull();
        });

        miembrosActividadService.guardarMiembro(
                escenario.idActividadSolo(),
                escenario.idPerfilMiembroSolo());

        assertThat(contar(
                "SELECT COUNT(*) FROM asignacion_actividad WHERE id_actividad = ?",
                escenario.idActividadSolo())).isZero();
        assertThat(contar(
                "SELECT COUNT(*) FROM miembros_proyecto WHERE id_proyecto = ?",
                escenario.idProyecto())).isEqualTo(3);
    }

    private void verificarNotificacion(
            Long idUsuario,
            String tipo,
            String entidadTipo,
            Long entidadId) {

        Map<String, Object> notificacion = jdbcTemplate.queryForMap(
                "SELECT tipo, entidad_tipo, entidad_id, mensaje FROM notificacion "
                        + "WHERE id_usuario_destino = ? AND tipo = ?",
                idUsuario,
                tipo);

        assertThat(notificacion.get("entidad_tipo")).isEqualTo(entidadTipo);
        assertThat(((Number) notificacion.get("entidad_id")).longValue())
                .isEqualTo(entidadId);
        assertThat((String) notificacion.get("mensaje"))
                .contains("Falta de presupuesto");
    }

    private int contar(String sql, Object... parametros) {
        Integer cantidad = jdbcTemplate.queryForObject(sql, Integer.class, parametros);
        return cantidad != null ? cantidad : 0;
    }

    private Escenario crearEscenario() {
        String sufijo = String.valueOf(Math.abs(System.nanoTime()));
        String baseDni = sufijo.substring(Math.max(0, sufijo.length() - 11));
        String baseCorreo = "uc51." + sufijo + ".";

        RolGlobal rolGlobal = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        Profesion profesion = em.find(Profesion.class, 2L);

        Usuario usuarioDirector = crearUsuario(
                baseCorreo + "director@example.com", baseDni + "1", rolGlobal, genero);
        Usuario usuarioMiembro = crearUsuario(
                baseCorreo + "miembro@example.com", baseDni + "2", rolGlobal, genero);
        Usuario usuarioPostulante = crearUsuario(
                baseCorreo + "postulante@example.com", baseDni + "3", rolGlobal, genero);
        Usuario usuarioInvitado = crearUsuario(
                baseCorreo + "invitado@example.com", baseDni + "4", rolGlobal, genero);
        Usuario usuarioMiembroSolo = crearUsuario(
                baseCorreo + "solo@example.com", baseDni + "5", rolGlobal, genero);

        Perfil perfilDirector = crearPerfil(usuarioDirector, "Perfil Director", profesion);
        Perfil perfilMiembro = crearPerfil(usuarioMiembro, "Perfil Miembro", profesion);
        Perfil perfilPostulante = crearPerfil(usuarioPostulante, "Perfil Postulante", profesion);
        Perfil perfilInvitado = crearPerfil(usuarioInvitado, "Perfil Invitado", profesion);
        Perfil perfilMiembroSolo = crearPerfil(
                usuarioMiembroSolo, "Perfil MiembroSolo", profesion);

        em.persist(PerfilTyc.builder()
                .id(new org.mgroko.backend.modelo.PerfilTycId(perfilDirector.getIdPerfil(), 1L))
                .perfil(perfilDirector)
                .descripcion("T&C Base")
                .fechaInicio(LocalDateTime.now())
                .build());

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC51 " + sufijo)
                .descripcion("Proyecto de test UC51")
                .fechaInicio(LocalDate.now())
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PRIVADO)
                .build();
        em.persist(proyecto);

        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now().plusDays(30))
                .build();
        em.persist(planificacion);

        RolProyecto rolDirector = em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = 'DIRECTOR'", RolProyecto.class)
                .getSingleResult();
        RolProyecto rolMiembro = em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = 'MIEMBRO'", RolProyecto.class)
                .getSingleResult();

        MiembroProyecto miembroDirector = crearMiembro(
                proyecto, perfilDirector, rolDirector, 1L);
        MiembroProyecto miembro = crearMiembro(
                proyecto, perfilMiembro, rolMiembro, null);
        MiembroProyecto miembroSolo = crearMiembro(
                proyecto, perfilMiembroSolo, rolMiembro, null);

        Actividad previa = crearActividad(
                planificacion, "Previa", 60, LocalDateTime.now().plusDays(1));
        Actividad eliminada = crearActividad(
                planificacion, "Eliminar", 90, LocalDateTime.now().plusDays(2));
        eliminada.setPredecesoras(new HashSet<>(Set.of(previa)));

        Actividad sucesora = crearActividad(
                planificacion, "Sucesora", 60, LocalDateTime.now().plusDays(3));
        sucesora.setPredecesoras(new HashSet<>(Set.of(eliminada)));

        Actividad otra = crearActividad(
                planificacion, "Otra", 30, LocalDateTime.now().plusDays(4));
        Actividad exclusiva = crearActividad(
                planificacion, "Exclusiva", 30, LocalDateTime.now().plusDays(5));
        Actividad solo = crearActividad(
                planificacion, "Solo", 30, LocalDateTime.now().plusDays(6));
        em.flush();

        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .actividad(eliminada)
                .profesion(profesion)
                .cantidad(1)
                .descripcion("Requerimiento UC51")
                .build();
        em.persist(requerimiento);

        PostulacionActividad postulacion = PostulacionActividad.builder()
                .perfil(perfilPostulante)
                .requerimiento(requerimiento)
                .build();
        em.persist(postulacion);

        InvitacionActividad invitacion = InvitacionActividad.builder()
                .perfilRemitente(perfilDirector)
                .perfilDestinatario(perfilInvitado)
                .requerimiento(requerimiento)
                .build();
        em.persist(invitacion);

        em.persist(AsignacionActividad.builder()
                .miembro(miembroDirector)
                .actividad(eliminada)
                .build());
        em.persist(AsignacionActividad.builder()
                .miembro(miembro)
                .actividad(eliminada)
                .build());
        em.persist(AsignacionActividad.builder()
                .miembro(miembroDirector)
                .actividad(otra)
                .build());
        em.persist(AsignacionActividad.builder()
                .miembro(miembro)
                .actividad(exclusiva)
                .build());
        em.persist(AsignacionActividad.builder()
                .miembro(miembroSolo)
                .actividad(solo)
                .build());
        em.flush();

        Long idHito = jdbcTemplate.queryForObject(
                "INSERT INTO hito "
                        + "(nombre, descripcion, fecha_limite, estado, id_proyecto) "
                        + "VALUES (?, ?, ?, 'PENDIENTE', ?) RETURNING id_hito",
                Long.class,
                "Hito UC51",
                "Hito de test UC51",
                Timestamp.valueOf(LocalDateTime.now().plusDays(10)),
                proyecto.getIdProyecto());
        jdbcTemplate.update(
                "INSERT INTO hito_actividad (id_hito, id_actividad) VALUES (?, ?)",
                idHito,
                eliminada.getIdActividad());

        return new Escenario(
                proyecto.getIdProyecto(),
                usuarioDirector.getIdUsuario(),
                perfilDirector.getIdPerfil(),
                usuarioMiembro.getIdUsuario(),
                usuarioPostulante.getIdUsuario(),
                usuarioInvitado.getIdUsuario(),
                perfilMiembroSolo.getIdPerfil(),
                eliminada.getIdActividad(),
                previa.getIdActividad(),
                sucesora.getIdActividad(),
                otra.getIdActividad(),
                solo.getIdActividad(),
                requerimiento.getIdRequerimiento(),
                postulacion.getIdPostulacion(),
                invitacion.getIdInvitacion(),
                idHito,
                previa.getFechaHoraInicio(),
                sucesora.getFechaHoraInicio());
    }

    private Usuario crearUsuario(
            String correo,
            String dni,
            RolGlobal rolGlobal,
            Genero genero) {

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Usuario")
                .apellido("UC51")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(correo)
                .rolGlobal(rolGlobal)
                .genero(genero)
                .build());
    }

    private Perfil crearPerfil(
            Usuario usuario,
            String nombreArtistico,
            Profesion profesion) {

        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de test UC51")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private MiembroProyecto crearMiembro(
            Proyecto proyecto,
            Perfil perfil,
            RolProyecto rolProyecto,
            Long idTycAceptado) {

        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolProyecto)
                .idTycAceptado(idTycAceptado)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build();
        em.persist(miembro);
        return miembro;
    }

    private Actividad crearActividad(
            Planificacion planificacion,
            String nombre,
            int duracionMinutos,
            LocalDateTime fechaHoraInicio) {

        Actividad actividad = Actividad.builder()
                .nombre(nombre)
                .descripcion("Actividad de test UC51")
                .duracionMinutos(duracionMinutos)
                .fechaHoraInicio(fechaHoraInicio)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();
        em.persist(actividad);
        return actividad;
    }

    private record Escenario(
            Long idProyecto,
            Long idUsuarioDirector,
            Long idPerfilDirector,
            Long idUsuarioMiembro,
            Long idUsuarioPostulante,
            Long idUsuarioInvitado,
            Long idPerfilMiembroSolo,
            Long idActividadEliminada,
            Long idActividadPrevia,
            Long idActividadSucesora,
            Long idActividadOtra,
            Long idActividadSolo,
            Long idRequerimiento,
            Long idPostulacion,
            Long idInvitacion,
            Long idHito,
            LocalDateTime inicioActividadPrevia,
            LocalDateTime inicioActividadSucesora) {
    }
}
