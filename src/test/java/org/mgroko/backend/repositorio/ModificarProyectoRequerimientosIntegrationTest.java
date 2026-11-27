package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.ModificarProyectoRequest;
import org.mgroko.backend.proyectos.dto.ModificarRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.dto.RequerimientoGralResponse;
import org.mgroko.backend.proyectos.exception.EstadoProyectoNoModificableException;
import org.mgroko.backend.proyectos.exception.RequerimientoGralProtegidoException;
import org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.ModificarProyectoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integración de UC-36 con el contrato estricto de ids: el PUT solo
 * actualiza/elimina requerimientos existentes y las altas se hacen por el
 * endpoint individual.
 */
@SpringBootTest
@Transactional
class ModificarProyectoRequerimientosIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolGlobalRepository rolGlobalRepository;
    @Autowired
    private GeneroRepository generoRepository;
    @Autowired
    private ProfesionRepository profesionRepository;
    @Autowired
    private MiembroProyectoRepository miembroProyectoRepository;
    @Autowired
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    @Autowired
    private ProyectoRepository proyectoRepository;
    @Autowired
    private CrearPerfilService crearPerfilService;
    @Autowired
    private CrearProyectoService crearProyectoService;
    @Autowired
    private ModificarProyectoService modificarProyectoService;

    @Test
    void crearRequerimiento_agregaFilaYDevuelveId() {
        Datos datos = crearProyectoConUnRequerimiento("uc36.crear@example.com", "88836001");

        RequerimientoGralResponse response = modificarProyectoService.crearRequerimiento(
                datos.idUsuario(), datos.idPerfil(), datos.idProyecto(),
                new CrearRequerimientoGralRequest(
                        1, datos.idProfesion(), "Segundo modelo", null, null));

        assertThat(response.idRequerimientoGral()).isNotNull();
        assertThat(requerimientoGralProyectoRepository
                .findByProyectoIdProyecto(datos.idProyecto())).hasSize(2);
    }

    @Test
    void modificar_conRequerimientoSinId_lanzaExcepcionYNoDuplica() {
        Datos datos = crearProyectoConUnRequerimiento("uc36.sinid@example.com", "88836002");

        ModificarProyectoRequest request = new ModificarProyectoRequest(
                "Descripción nueva", Privacidad.PUBLICO, false, null, null, null,
                List.of(new ModificarRequerimientoGralRequest(
                        null, 1, datos.idProfesion(), "duplicado", null, null)),
                null);

        assertThatThrownBy(() -> modificarProyectoService.modificar(
                datos.idUsuario(), datos.idPerfil(), datos.idProyecto(), request))
                .isInstanceOf(RequerimientoInvalidoException.class)
                .hasMessageContaining("debe incluir su id");

        assertThat(requerimientoGralProyectoRepository
                .findByProyectoIdProyecto(datos.idProyecto())).hasSize(1);
    }

    @Test
    void modificar_conId_actualizaSinDuplicar() {
        Datos datos = crearProyectoConUnRequerimiento("uc36.actualiza@example.com", "88836003");
        RequerimientoGralProyecto existente = requerimientoGralProyectoRepository
                .findByProyectoIdProyecto(datos.idProyecto()).getFirst();

        ModificarProyectoRequest request = new ModificarProyectoRequest(
                "Descripción nueva", Privacidad.PUBLICO, false, null, null, null,
                List.of(new ModificarRequerimientoGralRequest(
                        existente.getIdRequerimientoGral(), 2, datos.idProfesion(),
                        "actualizado", null, null)),
                null);

        modificarProyectoService.modificar(
                datos.idUsuario(), datos.idPerfil(), datos.idProyecto(), request);

        List<RequerimientoGralProyecto> guardados = requerimientoGralProyectoRepository
                .findByProyectoIdProyecto(datos.idProyecto());
        assertThat(guardados).hasSize(1);
        assertThat(guardados.getFirst().getCantidad()).isEqualTo(2);
        assertThat(guardados.getFirst().getDescripcion()).isEqualTo("actualizado");
    }

    @Test
    void eliminarRequerimiento_noProtegido_elimina() {
        Datos datos = crearProyectoConUnRequerimiento("uc36.elimina@example.com", "88836004");
        RequerimientoGralResponse segundo = modificarProyectoService.crearRequerimiento(
                datos.idUsuario(), datos.idPerfil(), datos.idProyecto(),
                new CrearRequerimientoGralRequest(
                        1, datos.idProfesion(), "Segundo", null, null));
        RequerimientoGralProyecto primero = requerimientoGralProyectoRepository
                .findByProyectoIdProyecto(datos.idProyecto()).stream()
                .filter(r -> !r.getIdRequerimientoGral().equals(segundo.idRequerimientoGral()))
                .findFirst()
                .orElseThrow();

        modificarProyectoService.eliminarRequerimiento(
                datos.idUsuario(), datos.idPerfil(), datos.idProyecto(),
                primero.getIdRequerimientoGral());

        assertThat(requerimientoGralProyectoRepository
                .findByProyectoIdProyecto(datos.idProyecto())).hasSize(1);
    }

    @Test
    void eliminarRequerimiento_protegido_lanzaExcepcion() {
        Datos datos = crearProyectoConUnRequerimiento("uc36.protegido@example.com", "88836005");
        RequerimientoGralProyecto requerimiento = requerimientoGralProyectoRepository
                .findByProyectoIdProyecto(datos.idProyecto()).getFirst();

        MiembroProyecto director = miembroProyectoRepository
                .findMiembroActivoConRol(datos.idProyecto(), datos.idPerfil())
                .orElseThrow();
        director.setRequerimientoGral(requerimiento);
        miembroProyectoRepository.saveAndFlush(director);

        assertThatThrownBy(() -> modificarProyectoService.eliminarRequerimiento(
                datos.idUsuario(), datos.idPerfil(), datos.idProyecto(),
                requerimiento.getIdRequerimientoGral()))
                .isInstanceOf(RequerimientoGralProtegidoException.class);
    }

    @Test
    void crearRequerimiento_enProyectoFinalizado_lanzaEstadoNoModificable() {
        Datos datos = crearProyectoConUnRequerimiento("uc36.finalizado@example.com", "88836006");
        Proyecto proyecto = proyectoRepository.findById(datos.idProyecto()).orElseThrow();
        proyecto.setEstado(EstadoProyecto.FINALIZADO);
        proyectoRepository.saveAndFlush(proyecto);

        assertThatThrownBy(() -> modificarProyectoService.crearRequerimiento(
                datos.idUsuario(), datos.idPerfil(), datos.idProyecto(),
                new CrearRequerimientoGralRequest(
                        1, datos.idProfesion(), "No permitido", null, null)))
                .isInstanceOf(EstadoProyectoNoModificableException.class);
    }

    private Datos crearProyectoConUnRequerimiento(String correo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("UC36")
                .apellido("Test")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(28))
                .correo(correo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Long idProfesion = profesionRepository.findByCodigoIgnoreCase("modelo")
                .map(Profesion::getIdProfesion)
                .orElseThrow();
        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest("Perfil UC36", idProfesion,
                        "Perfil de prueba UC-36.", List.of()));

        ProyectoResponse proyecto = crearProyectoService.crear(
                usuario.getIdUsuario(), perfil.idPerfil(),
                new CrearProyectoRequest(
                        "Proyecto UC36 " + dni,
                        "Proyecto de prueba UC-36",
                        Privacidad.PUBLICO,
                        LocalDate.now().plusDays(30),
                        true,
                        null,
                        null,
                        List.of(new CrearRequerimientoGralRequest(
                                1, idProfesion, "Modelo", null, null)),
                        null));

        return new Datos(usuario.getIdUsuario(), perfil.idPerfil(),
                proyecto.idProyecto(), idProfesion);
    }

    private record Datos(
            Long idUsuario,
            Long idPerfil,
            Long idProyecto,
            Long idProfesion) {
    }
}
