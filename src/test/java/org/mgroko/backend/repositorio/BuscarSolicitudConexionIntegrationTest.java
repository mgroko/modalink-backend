package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.conexiones.dto.BuscarSolicitudesConexionFiltro;
import org.mgroko.backend.conexiones.dto.DireccionSolicitudConexion;
import org.mgroko.backend.conexiones.dto.SolicitudConexionBusquedaResponse;
import org.mgroko.backend.conexiones.servicio.BuscarSolicitudConexionService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-46 contra PostgreSQL real: búsqueda por perfil activo, dirección, estado,
 * texto sin acentos, profesión, fechas y paginación.
 */
@SpringBootTest
@Transactional
class BuscarSolicitudConexionIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final LocalDateTime FECHA_AB =
            LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final LocalDateTime FECHA_EA =
            LocalDateTime.of(2026, 11, 8, 10, 0);
    private static final LocalDateTime FECHA_CA =
            LocalDateTime.of(2026, 11, 5, 10, 0);
    private static final LocalDateTime FECHA_DA =
            LocalDateTime.of(2026, 11, 9, 10, 0);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private ProfesionRepository profesionRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private SolicitudConexionRepository solicitudConexionRepository;

    @Autowired
    private BuscarSolicitudConexionService buscarSolicitudConexionService;

    private Perfil perfilA;
    private Perfil perfilB;
    private Perfil perfilC;
    private Perfil perfilD;
    private Perfil perfilE;

    private SolicitudConexion solicitudAB;
    private SolicitudConexion solicitudEA;
    private SolicitudConexion solicitudCA;
    private SolicitudConexion solicitudDA;

    @BeforeEach
    void setUp() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        perfilA = crearPerfil("A", "Ana", "Pérez", "Ana Moda", "Modelo", 1, rol, genero);
        perfilB = crearPerfil("B", "Bruno", "Gómez", "Bruno Estilo", "Fotógrafo", 2, rol, genero);
        perfilC = crearPerfil("C", "Carla", "Núñez", "Carla Diseño", "Diseñadora", 3, rol, genero);
        perfilD = crearPerfil("D", "Diego", "Álvarez", "Diego Textil", "Textil", 4, rol, genero);
        perfilE = crearPerfil("E", "Elena", "Ibáñez", "Elena Accesorios", "Accesorios", 5, rol, genero);

        solicitudAB = crearSolicitud(
                perfilA, perfilB, EstadoSolicitud.PENDIENTE, FECHA_AB);
        solicitudEA = crearSolicitud(
                perfilE, perfilA, EstadoSolicitud.PENDIENTE, FECHA_EA);
        solicitudCA = crearSolicitud(
                perfilC, perfilA, EstadoSolicitud.ACEPTADA, FECHA_CA);
        solicitudDA = crearSolicitud(
                perfilD, perfilA, EstadoSolicitud.RECHAZADA, FECHA_DA);
    }

    @Test
    void buscar_porDefecto_soloPendientesDelPerfilActivo() {
        PaginaResponse<SolicitudConexionBusquedaResponse> respuesta =
                buscar(null, null, null, null, null, null, 0, 20);

        assertEquals(2, respuesta.totalElementos());
        assertEquals(
                List.of(solicitudEA.getIdSolicitudConexion(), solicitudAB.getIdSolicitudConexion()),
                ids(respuesta));
        assertTrue(respuesta.contenido().stream()
                .allMatch(item -> item.estado() == EstadoSolicitud.PENDIENTE));
        assertEquals(DireccionSolicitudConexion.RECIBIDA, respuesta.contenido().get(0).direccion());
        assertEquals(DireccionSolicitudConexion.ENVIADA, respuesta.contenido().get(1).direccion());
    }

    @Test
    void buscar_filtraPorDireccion() {
        PaginaResponse<SolicitudConexionBusquedaResponse> enviadas =
                buscar(DireccionSolicitudConexion.ENVIADA, null, null, null, null, null, 0, 20);
        PaginaResponse<SolicitudConexionBusquedaResponse> recibidas =
                buscar(DireccionSolicitudConexion.RECIBIDA, null, null, null, null, null, 0, 20);

        assertEquals(List.of(solicitudAB.getIdSolicitudConexion()), ids(enviadas));
        assertEquals(List.of(solicitudEA.getIdSolicitudConexion()), ids(recibidas));
    }

    @Test
    void buscar_filtraPorEstado() {
        PaginaResponse<SolicitudConexionBusquedaResponse> aceptadas =
                buscar(null, EstadoSolicitud.ACEPTADA, null, null, null, null, 0, 20);
        PaginaResponse<SolicitudConexionBusquedaResponse> rechazadas =
                buscar(null, EstadoSolicitud.RECHAZADA, null, null, null, null, 0, 20);

        assertEquals(List.of(solicitudCA.getIdSolicitudConexion()), ids(aceptadas));
        assertEquals(List.of(solicitudDA.getIdSolicitudConexion()), ids(rechazadas));
    }

    @Test
    void buscar_filtraPorTextoYProfesionSinAcentos() {
        Long idProfesionC = perfilC.getProfesion().getIdProfesion();

        assertEquals(List.of(solicitudCA.getIdSolicitudConexion()),
                ids(buscar(null, EstadoSolicitud.ACEPTADA, "Carla", null, null, null, 0, 20)));
        assertEquals(List.of(solicitudCA.getIdSolicitudConexion()),
                ids(buscar(null, EstadoSolicitud.ACEPTADA, null, "Nunez", null, null, 0, 20)));
        assertEquals(List.of(solicitudCA.getIdSolicitudConexion()),
                ids(buscar(null, EstadoSolicitud.ACEPTADA, null, null, "Diseno", null, 0, 20)));
        assertEquals(List.of(solicitudCA.getIdSolicitudConexion()),
                ids(buscar(null, EstadoSolicitud.ACEPTADA, null, null, null, idProfesionC, 0, 20)));
    }

    @Test
    void buscar_filtraPorRangoDeFechas() {
        PaginaResponse<SolicitudConexionBusquedaResponse> enRango =
                buscar(null, EstadoSolicitud.ACEPTADA, null, null, null, null,
                        LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 5));
        PaginaResponse<SolicitudConexionBusquedaResponse> fueraDeRango =
                buscar(null, EstadoSolicitud.ACEPTADA, null, null, null, null,
                        LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 10));

        assertEquals(List.of(solicitudCA.getIdSolicitudConexion()), ids(enRango));
        assertTrue(fueraDeRango.contenido().isEmpty());
    }

    @Test
    void buscar_paginaYOrdenaPorFechaDescendente() {
        PaginaResponse<SolicitudConexionBusquedaResponse> primera =
                buscar(null, null, null, null, null, null, 0, 1);
        PaginaResponse<SolicitudConexionBusquedaResponse> segunda =
                buscar(null, null, null, null, null, null, 1, 1);

        assertEquals(2, primera.totalElementos());
        assertEquals(List.of(solicitudEA.getIdSolicitudConexion()), ids(primera));
        assertEquals(List.of(solicitudAB.getIdSolicitudConexion()), ids(segunda));
    }

    @Test
    void buscar_todosLosEstados_devuelvePendientesAceptadasYRechazadas() {
        PaginaResponse<SolicitudConexionBusquedaResponse> respuesta =
                buscarSolicitudConexionService.buscar(
                        perfilA.getIdPerfil(),
                        new BuscarSolicitudesConexionFiltro(
                                null, null, null, null, null, null,
                                null, null, true),
                        0,
                        20);

        assertEquals(4, respuesta.totalElementos());
        assertTrue(respuesta.contenido().stream()
                .anyMatch(item -> item.estado() == EstadoSolicitud.PENDIENTE));
        assertTrue(respuesta.contenido().stream()
                .anyMatch(item -> item.estado() == EstadoSolicitud.ACEPTADA));
        assertTrue(respuesta.contenido().stream()
                .anyMatch(item -> item.estado() == EstadoSolicitud.RECHAZADA));
    }

    @Test
    void buscar_sinResultados_devuelvePaginaVacia() {
        PaginaResponse<SolicitudConexionBusquedaResponse> respuesta =
                buscar(null, EstadoSolicitud.ACEPTADA, "Nadie", null, null, null, 0, 20);

        assertTrue(respuesta.contenido().isEmpty());
        assertEquals(0, respuesta.totalElementos());
    }

    private PaginaResponse<SolicitudConexionBusquedaResponse> buscar(
            DireccionSolicitudConexion direccion,
            EstadoSolicitud estado,
            String nombre,
            String apellido,
            String nombreArtistico,
            Long idProfesion,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {

        return buscarSolicitudConexionService.buscar(
                perfilA.getIdPerfil(),
                new BuscarSolicitudesConexionFiltro(
                        direccion, estado, nombre, apellido, nombreArtistico,
                        idProfesion, fechaDesde, fechaHasta, false),
                0,
                20);
    }

    private PaginaResponse<SolicitudConexionBusquedaResponse> buscar(
            DireccionSolicitudConexion direccion,
            EstadoSolicitud estado,
            String nombre,
            String apellido,
            String nombreArtistico,
            Long idProfesion,
            int page,
            int size) {
        return buscarSolicitudConexionService.buscar(
                perfilA.getIdPerfil(),
                new BuscarSolicitudesConexionFiltro(
                        direccion, estado, nombre, apellido, nombreArtistico,
                        idProfesion, null, null, false),
                page,
                size);
    }

    private List<Long> ids(PaginaResponse<SolicitudConexionBusquedaResponse> respuesta) {
        return respuesta.contenido().stream()
                .map(SolicitudConexionBusquedaResponse::idSolicitudConexion)
                .toList();
    }

    private SolicitudConexion crearSolicitud(
            Perfil remitente,
            Perfil destinatario,
            EstadoSolicitud estado,
            LocalDateTime fechaEnvio) {

        SolicitudConexion solicitud = solicitudConexionRepository.saveAndFlush(
                SolicitudConexion.builder()
                        .perfilRemitente(remitente)
                        .perfilDestinatario(destinatario)
                        .estado(EstadoSolicitud.PENDIENTE)
                        .fechaEnvio(fechaEnvio)
                        .build());

        if (estado != EstadoSolicitud.PENDIENTE) {
            solicitud.setEstado(estado);
            solicitud.setFechaRespuesta(fechaEnvio.plusDays(1));
            solicitud = solicitudConexionRepository.saveAndFlush(solicitud);
        }

        return solicitud;
    }

    private Perfil crearPerfil(
            String etiqueta,
            String nombre,
            String apellido,
            String nombreArtistico,
            String nombreProfesion,
            int offset,
            RolGlobal rol,
            Genero genero) {

        int dni = 90_000_000 + offset;
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .dni(String.valueOf(dni))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("uc46-" + etiqueta + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = profesionRepository.saveAndFlush(Profesion.builder()
                .codigo("UC46_" + etiqueta)
                .nombre(nombreProfesion + " " + etiqueta)
                .build());

        return perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de prueba UC-46")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .profesion(profesion)
                .build());
    }
}
