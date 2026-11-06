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
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.dto.EliminarIntegranteRequest;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.exception.AutoeliminacionIntegranteException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.EliminarIntegranteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
class EliminarIntegranteIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private EliminarIntegranteService eliminarIntegranteService;

    @Autowired
    private CupoRequerimientoService cupoRequerimientoService;

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
    void eliminar_conservaAsignacionesYReabreSoloCuposActuales() {
        Escenario escenario = crearEscenario();

        eliminarIntegranteService.eliminar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                escenario.idMiembro(),
                new EliminarIntegranteRequest("  Falta de compromiso  "));

        Map<String, Object> miembro = jdbcTemplate.queryForMap(
                "SELECT estado_participacion, \"motivoBaja\", fecha_baja "
                        + "FROM miembros_proyecto WHERE id_miembro = ?",
                escenario.idMiembro());

        assertThat(miembro.get("estado_participacion")).isEqualTo("ELIMINADO");
        assertThat(miembro.get("motivoBaja")).isEqualTo("Falta de compromiso");
        assertThat(miembro.get("fecha_baja")).isNotNull();

        assertThat(contar(
                "SELECT COUNT(*) FROM asignacion_actividad WHERE id_miembro = ?",
                escenario.idMiembro())).isEqualTo(3);

        RequerimientoActividad requerimientoCurso = em.find(
                RequerimientoActividad.class, escenario.idRequerimientoCurso());
        RequerimientoActividad requerimientoFinalizado = em.find(
                RequerimientoActividad.class, escenario.idRequerimientoFinalizado());
        RequerimientoActividad requerimientoPendiente = em.find(
                RequerimientoActividad.class, escenario.idRequerimientoPendiente());

        CupoRequerimiento cupoCurso = cupoRequerimientoService
                .calcularActividad(requerimientoCurso);
        CupoRequerimiento cupoFinalizado = cupoRequerimientoService
                .calcularActividad(requerimientoFinalizado);
        CupoRequerimiento cupoPendiente = cupoRequerimientoService
                .calcularActividad(requerimientoPendiente);

        assertThat(cupoCurso.faltantes()).isEqualTo(1);
        assertThat(cupoCurso.satisfecho()).isFalse();
        assertThat(cupoFinalizado.faltantes()).isZero();
        assertThat(cupoFinalizado.satisfecho()).isTrue();
        assertThat(cupoPendiente.faltantes()).isEqualTo(1);
        assertThat(cupoPendiente.satisfecho()).isFalse();

        Map<String, Object> notificacion = jdbcTemplate.queryForMap(
                "SELECT tipo, entidad_tipo, entidad_id, id_proyecto, mensaje "
                        + "FROM notificacion WHERE id_usuario_destino = ? AND tipo = ?",
                escenario.idUsuarioMiembro(),
                NotificacionService.TIPO_INTEGRANTE_ELIMINADO);
        assertThat(notificacion.get("entidad_tipo")).isEqualTo("PROYECTO");
        assertThat(((Number) notificacion.get("entidad_id")).longValue())
                .isEqualTo(escenario.idProyecto());
        assertThat(((Number) notificacion.get("id_proyecto")).longValue())
                .isEqualTo(escenario.idProyecto());
        assertThat((String) notificacion.get("mensaje"))
                .contains("Falta de compromiso");

        Map<String, Object> historial = jdbcTemplate.queryForMap(
                "SELECT estado_actividad, estado_participacion, nombre_artistico, "
                        + "motivo_baja_miembro "
                        + "FROM v_historial_asignaciones_actividad "
                        + "WHERE id_miembro = ? AND id_actividad = ?",
                escenario.idMiembro(),
                escenario.idActividadFinalizada());
        assertThat(historial.get("estado_actividad")).isEqualTo("FINALIZADA");
        assertThat(historial.get("estado_participacion")).isEqualTo("ELIMINADO");
        assertThat(historial.get("nombre_artistico")).isEqualTo("Perfil Miembro UC38");
        assertThat(historial.get("motivo_baja_miembro")).isEqualTo("Falta de compromiso");
    }

    @Test
    @Transactional
    void eliminar_autoeliminacion_noCambiaLaMembresia() {
        Escenario escenario = crearEscenario();

        assertThatThrownBy(() -> eliminarIntegranteService.eliminar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                escenario.idMiembroDirector(),
                new EliminarIntegranteRequest("Baja")))
                .isInstanceOf(AutoeliminacionIntegranteException.class);

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

    private Escenario crearEscenario() {
        String sufijo = String.valueOf(Math.abs(System.nanoTime()));
        String baseDni = sufijo.substring(Math.max(0, sufijo.length() - 11));
        String baseCorreo = "uc38." + sufijo + ".";

        RolGlobal rolGlobal = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        Profesion profesion = em.find(Profesion.class, 2L);

        Usuario usuarioDirector = crearUsuario(
                baseCorreo + "director@example.com", baseDni + "1", rolGlobal, genero);
        Usuario usuarioMiembro = crearUsuario(
                baseCorreo + "miembro@example.com", baseDni + "2", rolGlobal, genero);

        Perfil perfilDirector = crearPerfil(
                usuarioDirector, "Perfil Director UC38", profesion);
        Perfil perfilMiembro = crearPerfil(
                usuarioMiembro, "Perfil Miembro UC38", profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC38 " + sufijo)
                .descripcion("Proyecto de test UC38")
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
                proyecto, perfilDirector, rolDirector);
        MiembroProyecto miembro = crearMiembro(
                proyecto, perfilMiembro, rolMiembro);

        Actividad pendiente = crearActividad(
                planificacion, "Pendiente UC38", EstadoActividad.PENDIENTE, 1);
        Actividad enCurso = crearActividad(
                planificacion, "En curso UC38", EstadoActividad.EN_CURSO, 2);
        Actividad finalizada = crearActividad(
                planificacion, "Finalizada UC38", EstadoActividad.FINALIZADA, 3);
        em.flush();

        RequerimientoActividad requerimientoPendiente = crearRequerimiento(
                pendiente, profesion);
        RequerimientoActividad requerimientoCurso = crearRequerimiento(
                enCurso, profesion);
        RequerimientoActividad requerimientoFinalizado = crearRequerimiento(
                finalizada, profesion);

        em.persist(AsignacionActividad.builder()
                .miembro(miembro)
                .actividad(pendiente)
                .requerimientoActividad(requerimientoPendiente)
                .build());
        em.persist(AsignacionActividad.builder()
                .miembro(miembro)
                .actividad(enCurso)
                .requerimientoActividad(requerimientoCurso)
                .build());
        em.persist(AsignacionActividad.builder()
                .miembro(miembro)
                .actividad(finalizada)
                .requerimientoActividad(requerimientoFinalizado)
                .build());
        em.flush();

        return new Escenario(
                proyecto.getIdProyecto(),
                usuarioDirector.getIdUsuario(),
                perfilDirector.getIdPerfil(),
                usuarioMiembro.getIdUsuario(),
                miembro.getIdMiembro(),
                miembroDirector.getIdMiembro(),
                requerimientoPendiente.getIdRequerimiento(),
                requerimientoCurso.getIdRequerimiento(),
                requerimientoFinalizado.getIdRequerimiento(),
                finalizada.getIdActividad());
    }

    private Usuario crearUsuario(
            String correo,
            String dni,
            RolGlobal rolGlobal,
            Genero genero) {

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Usuario")
                .apellido("UC38")
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
                .biografia("Perfil de test UC38")
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
            RolProyecto rolProyecto) {

        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolProyecto)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build();
        em.persist(miembro);
        return miembro;
    }

    private Actividad crearActividad(
            Planificacion planificacion,
            String nombre,
            EstadoActividad estado,
            int dia) {

        Actividad actividad = Actividad.builder()
                .nombre(nombre)
                .descripcion("Actividad de test UC38")
                .duracionMinutos(60)
                .fechaHoraInicio(LocalDateTime.now().plusDays(dia))
                .estado(estado)
                .planificacion(planificacion)
                .build();
        em.persist(actividad);
        return actividad;
    }

    private RequerimientoActividad crearRequerimiento(
            Actividad actividad,
            Profesion profesion) {

        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .actividad(actividad)
                .profesion(profesion)
                .cantidad(1)
                .descripcion("Requerimiento de test UC38")
                .build();
        em.persist(requerimiento);
        return requerimiento;
    }

    private record Escenario(
            Long idProyecto,
            Long idUsuarioDirector,
            Long idPerfilDirector,
            Long idUsuarioMiembro,
            Long idMiembro,
            Long idMiembroDirector,
            Long idRequerimientoPendiente,
            Long idRequerimientoCurso,
            Long idRequerimientoFinalizado,
            Long idActividadFinalizada) {
    }
}
