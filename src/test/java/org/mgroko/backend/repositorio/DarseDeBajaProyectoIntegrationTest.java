package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.dto.DarseDeBajaProyectoRequest;
import org.mgroko.backend.proyectos.dto.EliminarIntegranteRequest;
import org.mgroko.backend.proyectos.dto.HistorialProyectoEventoResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.UltimoDirectorActivoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.DarseDeBajaProyectoService;
import org.mgroko.backend.proyectos.servicio.EliminarIntegranteService;
import org.mgroko.backend.proyectos.servicio.ListarHistorialProyectoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
class DarseDeBajaProyectoIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private DarseDeBajaProyectoService darseDeBajaProyectoService;

    @Autowired
    private CupoRequerimientoService cupoRequerimientoService;

    @Autowired
    private ListarHistorialProyectoService listarHistorialProyectoService;

    @Autowired
    private EliminarIntegranteService eliminarIntegranteService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void darseDeBaja_conservaAsignacionesYReabreCupos() {
        Escenario escenario = crearEscenario(false);

        darseDeBajaProyectoService.darseDeBaja(
                escenario.idUsuarioMiembro(),
                escenario.idPerfilMiembro(),
                escenario.idProyecto(),
                new DarseDeBajaProyectoRequest("  Nuevo proyecto  "));

        Map<String, Object> miembro = jdbcTemplate.queryForMap(
                "SELECT estado_participacion, \"motivoBaja\", fecha_baja "
                        + "FROM miembros_proyecto WHERE id_miembro = ?",
                escenario.idMiembro());

        assertThat(miembro.get("estado_participacion")).isEqualTo("BAJA_VOLUNTARIA");
        assertThat(miembro.get("motivoBaja")).isEqualTo("Nuevo proyecto");
        assertThat(miembro.get("fecha_baja")).isNotNull();

        assertThat(contar(
                "SELECT COUNT(*) FROM asignacion_actividad WHERE id_miembro = ?",
                escenario.idMiembro())).isEqualTo(1);

        RequerimientoGralProyecto requerimientoGral = em.find(
                RequerimientoGralProyecto.class, escenario.idRequerimientoGral());
        RequerimientoActividad requerimientoActividad = em.find(
                RequerimientoActividad.class, escenario.idRequerimientoActividad());

        CupoRequerimiento cupoGral =
                cupoRequerimientoService.calcularGeneral(requerimientoGral);
        CupoRequerimiento cupoActividad =
                cupoRequerimientoService.calcularActividad(requerimientoActividad);

        assertThat(cupoGral.cubiertos()).isZero();
        assertThat(cupoGral.faltantes()).isEqualTo(1);
        assertThat(cupoActividad.cubiertos()).isZero();
        assertThat(cupoActividad.faltantes()).isEqualTo(1);

        Map<String, Object> notificacion = jdbcTemplate.queryForMap(
                "SELECT tipo, entidad_tipo, entidad_id, id_proyecto, mensaje "
                        + "FROM notificacion WHERE id_usuario_destino = ? AND tipo = ?",
                escenario.idUsuarioDirector(),
                NotificacionService.TIPO_INTEGRANTE_BAJA_VOLUNTARIA);
        assertThat(notificacion.get("entidad_tipo")).isEqualTo("PROYECTO");
        assertThat(((Number) notificacion.get("entidad_id")).longValue())
                .isEqualTo(escenario.idProyecto());
        assertThat(((Number) notificacion.get("id_proyecto")).longValue())
                .isEqualTo(escenario.idProyecto());
        assertThat((String) notificacion.get("mensaje")).contains("Nuevo proyecto");

        assertThat(contar(
                "SELECT COUNT(*) FROM notificacion WHERE id_usuario_destino = ? AND tipo = ?",
                escenario.idUsuarioMiembro(),
                NotificacionService.TIPO_INTEGRANTE_BAJA_VOLUNTARIA)).isZero();
    }

    @Test
    @Transactional
    void historial_bajaVoluntariaDevuelveEventoConActividadYAutor() {
        Escenario escenario = crearEscenario(false);

        darseDeBajaProyectoService.darseDeBaja(
                escenario.idUsuarioMiembro(),
                escenario.idPerfilMiembro(),
                escenario.idProyecto(),
                new DarseDeBajaProyectoRequest("  Nuevo proyecto  "));

        PaginaResponse<HistorialProyectoEventoResponse> pagina =
                listarHistorialProyectoService.listar(
                        escenario.idUsuarioDirector(),
                        escenario.idPerfilDirector(),
                        escenario.idProyecto(),
                        null,
                        null,
                        0,
                        20);

        assertThat(pagina.contenido()).singleElement().satisfies(evento -> {
            assertThat(evento.tipo()).isEqualTo("MIEMBRO_BAJA_VOLUNTARIA");
            assertThat(evento.estadoParticipacion()).isEqualTo("BAJA_VOLUNTARIA");
            assertThat(evento.fecha()).isNotNull();
            assertThat(evento.idPerfilAfectado())
                    .isEqualTo(escenario.idPerfilMiembro());
            assertThat(evento.idPerfilActor())
                    .isEqualTo(escenario.idPerfilMiembro());
            assertThat(evento.motivo()).isEqualTo("Nuevo proyecto");
            assertThat(evento.nombreProfesionAfectado()).isNotNull();
            assertThat(evento.idActividad()).isNotNull();
            assertThat(evento.nombreActividad()).isEqualTo("Actividad UC40");
        });
    }

    @Test
    @Transactional
    void historial_eliminadoRegistraActorYNoDirectorRecibe403() {
        Escenario escenario = crearEscenario(false);

        eliminarIntegranteService.eliminar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                escenario.idMiembro(),
                new EliminarIntegranteRequest("Falta de compromiso"));

        PaginaResponse<HistorialProyectoEventoResponse> pagina =
                listarHistorialProyectoService.listar(
                        escenario.idUsuarioDirector(),
                        escenario.idPerfilDirector(),
                        escenario.idProyecto(),
                        "MIEMBRO_ELIMINADO",
                        null,
                        0,
                        20);

        assertThat(pagina.contenido()).singleElement().satisfies(evento -> {
            assertThat(evento.tipo()).isEqualTo("MIEMBRO_ELIMINADO");
            assertThat(evento.estadoParticipacion()).isEqualTo("ELIMINADO");
            assertThat(evento.idPerfilAfectado())
                    .isEqualTo(escenario.idPerfilMiembro());
            assertThat(evento.idPerfilActor())
                    .isEqualTo(escenario.idPerfilDirector());
            assertThat(evento.motivo()).isEqualTo("Falta de compromiso");
        });

        assertThatThrownBy(() -> listarHistorialProyectoService.listar(
                escenario.idUsuarioMiembro(),
                escenario.idPerfilMiembro(),
                escenario.idProyecto(),
                null,
                null,
                0,
                20))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    @Transactional
    void directorConOtroDirector_puedeDarseDeBajaYNotificaAlRestante() {
        Escenario escenario = crearEscenario(true);

        darseDeBajaProyectoService.darseDeBaja(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                new DarseDeBajaProyectoRequest("Nuevo proyecto"));

        String estado = jdbcTemplate.queryForObject(
                "SELECT estado_participacion FROM miembros_proyecto WHERE id_miembro = ?",
                String.class,
                escenario.idMiembroDirector());
        assertThat(estado).isEqualTo("BAJA_VOLUNTARIA");

        assertThat(contar(
                "SELECT COUNT(*) FROM notificacion WHERE id_usuario_destino = ? AND tipo = ?",
                escenario.idUsuarioSegundoDirector(),
                NotificacionService.TIPO_INTEGRANTE_BAJA_VOLUNTARIA)).isEqualTo(1);

        assertThat(contar(
                "SELECT COUNT(*) FROM notificacion WHERE id_usuario_destino = ? AND tipo = ?",
                escenario.idUsuarioDirector(),
                NotificacionService.TIPO_INTEGRANTE_BAJA_VOLUNTARIA)).isZero();
    }

    @Test
    @Transactional
    void directorUnico_noPuedeDarseDeBaja() {
        Escenario escenario = crearEscenario(false);

        assertThatThrownBy(() -> darseDeBajaProyectoService.darseDeBaja(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                new DarseDeBajaProyectoRequest("Nuevo proyecto")))
                .isInstanceOf(UltimoDirectorActivoException.class);

        String estado = jdbcTemplate.queryForObject(
                "SELECT estado_participacion FROM miembros_proyecto WHERE id_miembro = ?",
                String.class,
                escenario.idMiembroDirector());
        assertThat(estado).isEqualTo("ACTIVO");
    }

    private int contar(String sql, Object... parametros) {
        Integer cantidad = jdbcTemplate.queryForObject(sql, Integer.class, parametros);
        return cantidad != null ? cantidad : 0;
    }

    private Escenario crearEscenario(boolean agregarSegundoDirector) {
        String sufijo = String.valueOf(Math.abs(System.nanoTime()));
        String baseDni = sufijo.substring(Math.max(0, sufijo.length() - 11));
        String baseCorreo = "uc40." + sufijo + ".";

        RolGlobal rolGlobal = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        Profesion profesion = em.find(Profesion.class, 2L);

        Usuario usuarioDirector = crearUsuario(
                baseCorreo + "director@example.com", baseDni + "1", rolGlobal, genero);
        Usuario usuarioMiembro = crearUsuario(
                baseCorreo + "miembro@example.com", baseDni + "2", rolGlobal, genero);

        Perfil perfilDirector = crearPerfil(
                usuarioDirector, "Perfil Director UC40 " + sufijo, profesion);
        Perfil perfilMiembro = crearPerfil(
                usuarioMiembro, "Perfil Miembro UC40 " + sufijo, profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC40 " + sufijo)
                .descripcion("Proyecto de test UC40")
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
                proyecto, perfilDirector, rolDirector, null);
        MiembroProyecto miembro = crearMiembro(
                proyecto, perfilMiembro, rolMiembro, null);

        Usuario usuarioSegundoDirector = null;
        if (agregarSegundoDirector) {
            usuarioSegundoDirector = crearUsuario(
                    baseCorreo + "director2@example.com", baseDni + "3", rolGlobal, genero);
            Perfil perfilSegundoDirector = crearPerfil(
                    usuarioSegundoDirector, "Perfil Director 2 UC40 " + sufijo, profesion);
            crearMiembro(proyecto, perfilSegundoDirector, rolDirector, null);
        }

        RequerimientoGralProyecto requerimientoGral =
                RequerimientoGralProyecto.builder()
                        .proyecto(proyecto)
                        .profesion(profesion)
                        .cantidad(1)
                        .descripcion("Requerimiento general UC40")
                        .build();
        em.persist(requerimientoGral);
        miembro.setRequerimientoGral(requerimientoGral);

        Actividad actividad = Actividad.builder()
                .nombre("Actividad UC40")
                .descripcion("Actividad de test UC40")
                .duracionMinutos(60)
                .fechaHoraInicio(LocalDateTime.now().plusDays(1))
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();
        em.persist(actividad);

        RequerimientoActividad requerimientoActividad = RequerimientoActividad.builder()
                .actividad(actividad)
                .profesion(profesion)
                .cantidad(1)
                .descripcion("Requerimiento actividad UC40")
                .build();
        em.persist(requerimientoActividad);

        em.persist(AsignacionActividad.builder()
                .miembro(miembro)
                .actividad(actividad)
                .requerimientoActividad(requerimientoActividad)
                .build());
        em.flush();

        return new Escenario(
                proyecto.getIdProyecto(),
                usuarioDirector.getIdUsuario(),
                perfilDirector.getIdPerfil(),
                usuarioMiembro.getIdUsuario(),
                perfilMiembro.getIdPerfil(),
                miembro.getIdMiembro(),
                miembroDirector.getIdMiembro(),
                requerimientoGral.getIdRequerimientoGral(),
                requerimientoActividad.getIdRequerimiento(),
                usuarioSegundoDirector != null
                        ? usuarioSegundoDirector.getIdUsuario()
                        : null);
    }

    private Usuario crearUsuario(
            String correo,
            String dni,
            RolGlobal rolGlobal,
            Genero genero) {

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Usuario")
                .apellido("UC40")
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
                .biografia("Perfil de test UC40")
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
            RequerimientoGralProyecto requerimientoGral) {

        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolProyecto)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .requerimientoGral(requerimientoGral)
                .build();
        em.persist(miembro);
        return miembro;
    }

    private record Escenario(
            Long idProyecto,
            Long idUsuarioDirector,
            Long idPerfilDirector,
            Long idUsuarioMiembro,
            Long idPerfilMiembro,
            Long idMiembro,
            Long idMiembroDirector,
            Long idRequerimientoGral,
            Long idRequerimientoActividad,
            Long idUsuarioSegundoDirector) {
    }
}
