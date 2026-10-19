package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro;
import org.mgroko.backend.proyectos.dto.BuscarProyectosPropiosFiltro;
import org.mgroko.backend.proyectos.dto.CancelarProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.ProyectoPropioResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.especificacion.ProyectoSpecifications;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.servicio.BuscarProyectosPropiosService;
import org.mgroko.backend.proyectos.servicio.CancelarProyectoService;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.ObtenerProyectoService;
import org.mgroko.backend.proyectos.servicio.PublicarProyectoService;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CancelarProyectoIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private BuscarProyectosPropiosService buscarProyectosPropiosService;

    @Autowired
    private ObtenerProyectoService obtenerProyectoService;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record PerfilCreado(Long idUsuario, Long idPerfil) {}

    private PerfilCreado crearPerfil(String correo, String dni, String nombreArtistico) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Cancelar").apellido("Test").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(26))
                .correo(correo)
                .estado(org.mgroko.backend.modelo.enums.EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Long idProfesionModelo = profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();

        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesionModelo, "Perfil de prueba UC-35.", List.of()));

        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, id_tyc, descripcion, fecha_inicio) VALUES (?, 1, 'TyC UC-35', NOW())",
                perfil.idPerfil());

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private CrearProyectoRequest proyectoCompleto(String nombre) {
        Long idProfesionModelo = profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();
        return new CrearProyectoRequest(
                nombre, "Descripción completa del proyecto", Privacidad.PUBLICO,
                LocalDate.now().plusDays(30), true,
                new UbicacionRequest("0602801002"),
                null,
                List.of(new CrearRequerimientoGralRequest(2, idProfesionModelo, "Dos modelos", null, null)),
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private List<String> visiblesEnBusquedaGlobal(Long idPerfil, BuscarProyectosFiltro filtro) {
        Page<Proyecto> resultados = proyectoRepository.findAll(
                ProyectoSpecifications.conFiltros(filtro, idPerfil),
                PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "nombre")));
        return resultados.map(Proyecto::getNombre).getContent();
    }

    private List<String> visiblesEnProyectosPropios(Long idPerfil) {
        PaginaResponse<ProyectoPropioResponse> pagina = buscarProyectosPropiosService.buscar(
                idPerfil, new BuscarProyectosPropiosFiltro(null, null, null), 0, 50, false);
        return pagina.contenido().stream()
                .map(ProyectoPropioResponse::nombre)
                .toList();
    }

    @Test
    void cancelar_publicado_ocultaDeBusquedaGlobalYMantieneEnHistorialPropio() {
        PerfilCreado director = crearPerfil("can35.dir1@example.com", "78735001", "Director UC35");
        PerfilCreado ajeno = crearPerfil("can35.aju1@example.com", "78735002", "Observador UC35");

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(), proyectoCompleto("Desfile Cancelable"));
        publicarProyectoService.publicar(director.idUsuario(), director.idPerfil(), borrador.idProyecto());

        assertThat(visiblesEnBusquedaGlobal(ajeno.idPerfil(), null)).contains("Desfile Cancelable");

        ProyectoResponse cancelado = cancelarProyectoService.cancelar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto(),
                new CancelarProyectoRequest("Falta de presupuesto"));

        assertThat(cancelado.estado()).isEqualTo("Cancelado");
        assertThat(cancelado.motivoCancelacion()).isEqualTo("Falta de presupuesto");

        Proyecto recargado = proyectoRepository.findById(borrador.idProyecto()).orElseThrow();
        assertThat(recargado.getEstado()).isEqualTo(EstadoProyecto.CANCELADO);
        assertThat(recargado.getMotivoCancelacion()).isEqualTo("Falta de presupuesto");

        assertThat(visiblesEnBusquedaGlobal(ajeno.idPerfil(), null)).doesNotContain("Desfile Cancelable");
        assertThat(visiblesEnBusquedaGlobal(director.idPerfil(), null)).doesNotContain("Desfile Cancelable");
        assertThat(visiblesEnProyectosPropios(director.idPerfil())).contains("Desfile Cancelable");

        assertThatThrownBy(() -> obtenerProyectoService.obtener(
                ajeno.idUsuario(), ajeno.idPerfil(), borrador.idProyecto()))
                .isInstanceOf(AccesoDenegadoProyectoException.class);

        Long notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM notificacion
                WHERE id_usuario_destino = ?
                  AND tipo = 'PROYECTO_CANCELADO'
                  AND entidad_id = ?
                  AND mensaje LIKE '%Falta de presupuesto%'
                """, Long.class, director.idUsuario(), borrador.idProyecto());
        assertThat(notificaciones).isEqualTo(1);
    }

    @Test
    void cancelado_sigueVisibleParaExMiembroYPermiteAbrirDetalle() {
        PerfilCreado director = crearPerfil("can35.dir2@example.com", "78735003", "Ex Director UC35");
        PerfilCreado directorReemplazo = crearPerfil(
                "can35.dir3@example.com", "78735004", "Director Reemplazo UC35");

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(), proyectoCompleto("Desfile Historial"));
        publicarProyectoService.publicar(director.idUsuario(), director.idPerfil(), borrador.idProyecto());
        cancelarProyectoService.cancelar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto(),
                new CancelarProyectoRequest("Fin del proyecto"));

        Long idRolDirector = jdbcTemplate.queryForObject(
                "SELECT id_rol_proyecto FROM rol_proyecto WHERE codigo = 'DIRECTOR'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO miembros_proyecto
                    (id_rol_proyecto, estado_participacion, id_proyecto, id_perfil)
                VALUES (?, 'ACTIVO', ?, ?)
                """, idRolDirector, borrador.idProyecto(), directorReemplazo.idPerfil());

        jdbcTemplate.update("""
                UPDATE miembros_proyecto
                SET estado_participacion = 'BAJA_VOLUNTARIA'
                WHERE id_proyecto = ? AND id_perfil = ?
                """, borrador.idProyecto(), director.idPerfil());

        assertThat(visiblesEnProyectosPropios(director.idPerfil())).contains("Desfile Historial");

        ProyectoResponse detalle = obtenerProyectoService.obtener(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto());
        assertThat(detalle.estado()).isEqualTo("Cancelado");
        assertThat(detalle.motivoCancelacion()).isEqualTo("Fin del proyecto");
        assertThat(detalle.esMiembro()).isFalse();
    }
}
