package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.dto.AsignacionRequerimientoGralResponse;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.MiembroPublicoTycProyectoResponse;
import org.mgroko.backend.proyectos.dto.MiembroTycProyectoResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.RequerimientoGralNoAsignableException;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.ListarMiembrosPublicosProyectoService;
import org.mgroko.backend.proyectos.servicio.ListarTycMiembrosProyectoService;
import org.mgroko.backend.proyectos.servicio.PublicarProyectoService;
import org.mgroko.backend.proyectos.servicio.RequerimientoGralAsignacionService;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integración de UC-36: asignar, reasignar y desasignar requerimientos
 * generales desde el staff.
 */
@SpringBootTest
@Transactional
class AsignarRequerimientoGralIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolGlobalRepository rolGlobalRepository;
    @Autowired
    private GeneroRepository generoRepository;
    @Autowired
    private ProfesionRepository profesionRepository;
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private ProyectoRepository proyectoRepository;
    @Autowired
    private RolProyectoRepository rolProyectoRepository;
    @Autowired
    private MiembroProyectoRepository miembroProyectoRepository;
    @Autowired
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    @Autowired
    private CrearPerfilService crearPerfilService;
    @Autowired
    private CrearProyectoService crearProyectoService;
    @Autowired
    private PublicarProyectoService publicarProyectoService;
    @Autowired
    private RequerimientoGralAsignacionService requerimientoGralAsignacionService;
    @Autowired
    private ListarTycMiembrosProyectoService listarTycMiembrosProyectoService;
    @Autowired
    private ListarMiembrosPublicosProyectoService listarMiembrosPublicosProyectoService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void asignarMiembroSinRequerimiento_actualizaCupoYNotifica() {
        Datos datos = crearProyectoPublicadoConUnRequerimiento("uc36.asig@example.com", "88836101");
        MiembroProyecto miembro = agregarMiembro(
                datos.publicado(), datos.modelo(), "Modelo UC36 Asignar");

        AsignacionRequerimientoGralResponse response =
                requerimientoGralAsignacionService.asignar(
                        datos.idUsuarioDirector(),
                        datos.idPerfilDirector(),
                        datos.publicado().idProyecto(),
                        miembro.getIdMiembro(),
                        datos.idRequerimiento());

        assertThat(response.idRequerimientoGral()).isEqualTo(datos.idRequerimiento());
        assertThat(response.satisfecho()).isTrue();
        assertThat(response.faltantes()).isZero();

        MiembroProyecto actualizado = miembroProyectoRepository
                .findById(miembro.getIdMiembro()).orElseThrow();
        assertThat(actualizado.getRequerimientoGral()).isNotNull();

        Long notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'REQUERIMIENTO_ASIGNADO'
                """, Long.class, datos.modelo().idUsuario());
        assertThat(notificaciones).isEqualTo(1L);

        Long auditoria = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auditoria
                WHERE entidad_afectada = 'miembros_proyecto'
                  AND id_afectado = ?
                  AND tipo_evento = 'MIEMBRO_REQUERIMIENTO_ASIGNADO'
                """, Long.class, miembro.getIdMiembro());
        assertThat(auditoria).isEqualTo(1L);
    }

    @Test
    void listarTycMiembros_devuelveElTextoAceptadoPorCadaMiembro() {
        Datos datos = crearProyectoPublicadoConUnRequerimiento(
                "uc36.tyc@example.com", "88836120");
        MiembroProyecto miembro = agregarMiembro(
                datos.publicado(), datos.modelo(), "Modelo UC36 TyC");

        Long idTyc = jdbcTemplate.queryForObject("""
                INSERT INTO perfil_tyc
                    (id_perfil, descripcion, fecha_inicio, fecha_fin)
                VALUES (?, 'Texto completo del TyC aceptado.', NOW() - INTERVAL '1 day', NULL)
                RETURNING id_tyc
                """, Long.class, datos.modelo().idPerfil());

        miembro.setIdTycAceptado(idTyc);
        miembroProyectoRepository.saveAndFlush(miembro);

        List<MiembroTycProyectoResponse> resultado =
                listarTycMiembrosProyectoService.listar(
                        datos.idUsuarioDirector(),
                        datos.idPerfilDirector(),
                        datos.publicado().idProyecto());

        assertThat(resultado)
                .anySatisfy(fila -> {
                    assertThat(fila.idPerfil()).isEqualTo(datos.modelo().idPerfil());
                    assertThat(fila.idTycAceptado()).isEqualTo(idTyc);
                    assertThat(fila.numeroVersionAceptado()).isNotNull();
                    assertThat(fila.descripcion())
                            .isEqualTo("Texto completo del TyC aceptado.");
                });
    }

    @Test
    void listarMiembrosPublicos_permiteNoMiembroEnProyectoPublico() {
        Datos datos = crearProyectoPublicadoConUnRequerimiento(
                "uc36.publico@example.com", "88836121");
        MiembroProyecto miembro = agregarMiembro(
                datos.publicado(), datos.modelo(), "Modelo UC36 Público");

        Long idTyc = jdbcTemplate.queryForObject("""
                INSERT INTO perfil_tyc
                    (id_perfil, descripcion, fecha_inicio, fecha_fin)
                VALUES (?, 'Texto público del TyC aceptado.', NOW() - INTERVAL '1 day', NULL)
                RETURNING id_tyc
                """, Long.class, datos.modelo().idPerfil());

        miembro.setIdTycAceptado(idTyc);
        miembroProyectoRepository.saveAndFlush(miembro);

        PerfilCreado observador = crearPerfil(
                "uc36.observador@example.com",
                "88836122",
                "Observador UC36",
                "modelo");

        List<MiembroPublicoTycProyectoResponse> resultado =
                listarMiembrosPublicosProyectoService.listar(
                        observador.idUsuario(),
                        observador.idPerfil(),
                        datos.publicado().idProyecto());

        assertThat(resultado)
                .anySatisfy(fila -> {
                    assertThat(fila.idPerfil())
                            .isEqualTo(datos.modelo().idPerfil());
                    assertThat(fila.idTycAceptado()).isEqualTo(idTyc);
                    assertThat(fila.numeroVersionAceptado()).isNotNull();
                    assertThat(fila.descripcion())
                            .isEqualTo("Texto público del TyC aceptado.");
                });
    }

    @Test
    void desasignarMiembro_liberaCupoYNotifica() {
        Datos datos = crearProyectoPublicadoConUnRequerimiento("uc36.desasig@example.com", "88836102");
        MiembroProyecto miembro = agregarMiembro(
                datos.publicado(), datos.modelo(), "Modelo UC36 Desasignar");
        asignar(datos, miembro, datos.idRequerimiento());

        AsignacionRequerimientoGralResponse response =
                requerimientoGralAsignacionService.asignar(
                        datos.idUsuarioDirector(),
                        datos.idPerfilDirector(),
                        datos.publicado().idProyecto(),
                        miembro.getIdMiembro(),
                        null);

        assertThat(response.idRequerimientoGral()).isEqualTo(datos.idRequerimiento());
        assertThat(response.faltantes()).isEqualTo(1);

        MiembroProyecto actualizado = miembroProyectoRepository
                .findById(miembro.getIdMiembro()).orElseThrow();
        assertThat(actualizado.getRequerimientoGral()).isNull();
        assertThat(actualizado.getEstadoParticipacion())
                .isEqualTo(EstadoParticipacion.ACTIVO);

        Long notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'REQUERIMIENTO_DESASIGNADO'
                """, Long.class, datos.modelo().idUsuario());
        assertThat(notificaciones).isEqualTo(1L);

        Long auditoria = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auditoria
                WHERE entidad_afectada = 'miembros_proyecto'
                  AND id_afectado = ?
                  AND tipo_evento = 'MIEMBRO_REQUERIMIENTO_DESASIGNADO'
                """, Long.class, miembro.getIdMiembro());
        assertThat(auditoria).isEqualTo(1L);
    }

    @Test
    void reasignarMiembro_cambiaCupoEntreRequerimientos() {
        Datos datos = crearProyectoPublicadoConDosRequerimientos(
                "uc36.reasig@example.com", "88836103");
        MiembroProyecto miembro = agregarMiembro(
                datos.publicado(), datos.modelo(), "Modelo UC36 Reasignar");
        Long primero = datos.publicado().requerimientosGral().get(0).idRequerimientoGral();
        Long segundo = datos.publicado().requerimientosGral().get(1).idRequerimientoGral();

        asignar(datos, miembro, primero);
        AsignacionRequerimientoGralResponse response =
                requerimientoGralAsignacionService.asignar(
                        datos.idUsuarioDirector(),
                        datos.idPerfilDirector(),
                        datos.publicado().idProyecto(),
                        miembro.getIdMiembro(),
                        segundo);

        assertThat(response.idRequerimientoGral()).isEqualTo(segundo);

        Long cubiertosPrimero = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM miembros_proyecto
                WHERE id_requerimiento_gral = ? AND estado_participacion = 'ACTIVO'
                """, Long.class, primero);
        Long cubiertosSegundo = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM miembros_proyecto
                WHERE id_requerimiento_gral = ? AND estado_participacion = 'ACTIVO'
                """, Long.class, segundo);

        assertThat(cubiertosPrimero).isZero();
        assertThat(cubiertosSegundo).isEqualTo(1L);

        Long notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'REQUERIMIENTO_REASIGNADO'
                """, Long.class, datos.modelo().idUsuario());
        assertThat(notificaciones).isEqualTo(1L);
    }

    @Test
    void asignarProfesionDistinta_lanzaExcepcion() {
        Datos datos = crearProyectoPublicadoConUnRequerimiento("uc36.prof@example.com", "88836104");
        PerfilCreado fotografo = crearPerfil(
                "uc36.foto@example.com", "88836105", "Fotógrafo UC36", "fotografo");
        MiembroProyecto miembro = agregarMiembro(
                datos.publicado(), fotografo, "Fotógrafo UC36");

        assertThatThrownBy(() -> requerimientoGralAsignacionService.asignar(
                datos.idUsuarioDirector(),
                datos.idPerfilDirector(),
                datos.publicado().idProyecto(),
                miembro.getIdMiembro(),
                datos.idRequerimiento()))
                .isInstanceOf(RequerimientoGralNoAsignableException.class)
                .extracting("codigo")
                .isEqualTo("PROFESION_INCOMPATIBLE");
    }

    @Test
    void asignarDirector_lanzaExcepcion() {
        Datos datos = crearProyectoPublicadoConUnRequerimiento("uc36.dir@example.com", "88836106");
        MiembroProyecto director = miembroProyectoRepository
                .findMiembroActivoConRol(
                        datos.publicado().idProyecto(), datos.idPerfilDirector())
                .orElseThrow();

        assertThatThrownBy(() -> requerimientoGralAsignacionService.asignar(
                datos.idUsuarioDirector(),
                datos.idPerfilDirector(),
                datos.publicado().idProyecto(),
                director.getIdMiembro(),
                datos.idRequerimiento()))
                .isInstanceOf(RequerimientoGralNoAsignableException.class)
                .extracting("codigo")
                .isEqualTo("MIEMBRO_DIRECTOR");
    }

    @Test
    void asignarEnBorrador_lanzaExcepcion() {
        Datos datos = crearProyectoBorradorConUnRequerimiento("uc36.borrador@example.com", "88836107");
        MiembroProyecto miembro = agregarMiembro(
                datos.publicado(), datos.modelo(), "Modelo UC36 Borrador");

        assertThatThrownBy(() -> requerimientoGralAsignacionService.asignar(
                datos.idUsuarioDirector(),
                datos.idPerfilDirector(),
                datos.publicado().idProyecto(),
                miembro.getIdMiembro(),
                datos.idRequerimiento()))
                .isInstanceOf(RequerimientoGralNoAsignableException.class)
                .extracting("codigo")
                .isEqualTo("PROYECTO_ESTADO_INVALIDO");
    }

    @Test
    void asignarRequerimientoSatisfecho_lanzaExcepcion() {
        Datos datos = crearProyectoPublicadoConUnRequerimiento("uc36.lleno@example.com", "88836108");
        MiembroProyecto primero = agregarMiembro(
                datos.publicado(), datos.modelo(), "Modelo UC36 Lleno Uno");
        asignar(datos, primero, datos.idRequerimiento());

        PerfilCreado segundoPerfil = crearPerfil(
                "uc36.lleno.dos@example.com", "88836109", "Modelo UC36 Lleno Dos", "modelo");
        MiembroProyecto segundo = agregarMiembro(
                datos.publicado(), segundoPerfil, "Modelo UC36 Lleno Dos");

        assertThatThrownBy(() -> requerimientoGralAsignacionService.asignar(
                datos.idUsuarioDirector(),
                datos.idPerfilDirector(),
                datos.publicado().idProyecto(),
                segundo.getIdMiembro(),
                datos.idRequerimiento()))
                .isInstanceOf(RequerimientoGralNoAsignableException.class)
                .extracting("codigo")
                .isEqualTo("REQUERIMIENTO_SATISFECHO");
    }

    @Test
    void asignarSinPermiso_lanzaAccesoDenegado() {
        Datos datos = crearProyectoPublicadoConUnRequerimiento("uc36.permiso@example.com", "88836110");
        MiembroProyecto miembro = agregarMiembro(
                datos.publicado(), datos.modelo(), "Modelo UC36 Permiso");

        assertThatThrownBy(() -> requerimientoGralAsignacionService.asignar(
                datos.modelo().idUsuario(),
                datos.modelo().idPerfil(),
                datos.publicado().idProyecto(),
                miembro.getIdMiembro(),
                datos.idRequerimiento()))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    private void asignar(Datos datos, MiembroProyecto miembro, Long idRequerimiento) {
        requerimientoGralAsignacionService.asignar(
                datos.idUsuarioDirector(),
                datos.idPerfilDirector(),
                datos.publicado().idProyecto(),
                miembro.getIdMiembro(),
                idRequerimiento);
    }

    private MiembroProyecto agregarMiembro(
            ProyectoResponse publicado,
            PerfilCreado perfilCreado,
            String nombre) {

        Proyecto proyecto = proyectoRepository.findById(publicado.idProyecto()).orElseThrow();
        Perfil perfil = perfilRepository.findById(perfilCreado.idPerfil()).orElseThrow();
        RolProyecto rolMiembro = rolProyectoRepository.findByCodigo("MIEMBRO").orElseThrow();

        return miembroProyectoRepository.saveAndFlush(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());
    }

    private Datos crearProyectoPublicadoConUnRequerimiento(String correo, String dni) {
        Datos datos = crearProyectoConUnRequerimiento(correo, dni, true);
        return new Datos(
                datos.idUsuarioDirector(),
                datos.idPerfilDirector(),
                publicarProyectoService.publicar(
                        datos.idUsuarioDirector(),
                        datos.idPerfilDirector(),
                        datos.publicado().idProyecto()),
                datos.modelo());
    }

    private Datos crearProyectoPublicadoConDosRequerimientos(String correo, String dni) {
        Datos datos = crearProyectoConDosRequerimientos(correo, dni, true);
        return new Datos(
                datos.idUsuarioDirector(),
                datos.idPerfilDirector(),
                publicarProyectoService.publicar(
                        datos.idUsuarioDirector(),
                        datos.idPerfilDirector(),
                        datos.publicado().idProyecto()),
                datos.modelo());
    }

    private Datos crearProyectoBorradorConUnRequerimiento(String correo, String dni) {
        return crearProyectoConUnRequerimiento(correo, dni, false);
    }

    private Datos crearProyectoConUnRequerimiento(
            String correo,
            String dni,
            boolean publicar) {

        PerfilCreado director = crearPerfil(
                correo, dni, "Director UC36 " + dni, "modelo");
        PerfilCreado modelo = crearPerfil(
                "modelo." + correo, dni + "9", "Modelo UC36 " + dni, "modelo");

        ProyectoResponse proyecto = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                requestProyecto(
                        "Proyecto UC36 " + dni,
                        List.of(new CrearRequerimientoGralRequest(
                                1, idProfesion("modelo"), "Modelo", null, null))));

        return new Datos(
                director.idUsuario(),
                director.idPerfil(),
                proyecto,
                modelo);
    }

    private Datos crearProyectoConDosRequerimientos(
            String correo,
            String dni,
            boolean publicar) {

        PerfilCreado director = crearPerfil(
                correo, dni, "Director UC36 " + dni, "modelo");
        PerfilCreado modelo = crearPerfil(
                "modelo." + correo, dni + "9", "Modelo UC36 " + dni, "modelo");

        ProyectoResponse proyecto = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                requestProyecto(
                        "Proyecto UC36 Dos " + dni,
                        List.of(
                                new CrearRequerimientoGralRequest(
                                        1, idProfesion("modelo"), "Modelo A", null, null),
                                new CrearRequerimientoGralRequest(
                                        1, idProfesion("modelo"), "Modelo B", null, null))));

        return new Datos(
                director.idUsuario(),
                director.idPerfil(),
                proyecto,
                modelo);
    }

    private CrearProyectoRequest requestProyecto(
            String nombre,
            List<CrearRequerimientoGralRequest> requerimientos) {

        return new CrearProyectoRequest(
                nombre,
                "Proyecto de prueba UC-36",
                Privacidad.PUBLICO,
                LocalDate.now().plusDays(30),
                true,
                new UbicacionRequest("0602801002"),
                null,
                requerimientos,
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private PerfilCreado crearPerfil(
            String correo,
            String dni,
            String nombreArtistico,
            String codigoProfesion) {

        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("UC36")
                .apellido("Test")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(27))
                .correo(correo)
                .estado(org.mgroko.backend.modelo.enums.EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Long idProfesion = profesionRepository.findByCodigoIgnoreCase(codigoProfesion)
                .map(Profesion::getIdProfesion)
                .orElseThrow();

        PerfilResponse perfil = crearPerfilService.crear(
                usuario.getIdUsuario(),
                new CrearPerfilRequest(
                        nombreArtistico, idProfesion,
                        "Perfil de prueba UC-36.", List.of()));

        jdbcTemplate.update("""
                INSERT INTO perfil_tyc
                    (id_perfil, descripcion, fecha_inicio, fecha_fin)
                VALUES (?, 'TyC UC-36', NOW() - INTERVAL '1 day', NOW())
                """, perfil.idPerfil());

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private Long idProfesion(String codigo) {
        return profesionRepository.findByCodigoIgnoreCase(codigo)
                .map(Profesion::getIdProfesion)
                .orElseThrow();
    }

    private record PerfilCreado(Long idUsuario, Long idPerfil) {
    }

    private record Datos(
            Long idUsuarioDirector,
            Long idPerfilDirector,
            ProyectoResponse publicado,
            PerfilCreado modelo) {

        Long idRequerimiento() {
            return publicado.requerimientosGral().getFirst().idRequerimientoGral();
        }
    }
}
