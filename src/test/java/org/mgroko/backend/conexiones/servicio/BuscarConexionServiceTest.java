package org.mgroko.backend.conexiones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.conexiones.dto.ConexionResponse;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoConexion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.presencia.servicio.PresenciaService;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuscarConexionServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTIVO = 7L;
    private static final Long ID_CONTRAPARTE = 42L;

    @Mock
    private PerfilActivoService perfilActivoService;

    @Mock
    private ConexionRepository conexionRepository;

    @Mock
    private PresenciaService presenciaService;

    private BuscarConexionService service;

    private Perfil perfilActivo;
    private Perfil contraparte;

    @BeforeEach
    void setUp() {
        service = new BuscarConexionService(
                perfilActivoService, conexionRepository, presenciaService);

        perfilActivo = perfil(ID_PERFIL_ACTIVO, "Ana Moda", 1L, "Ana", "Modelo");
        contraparte = perfil(ID_CONTRAPARTE, "Bruno Estilo", 2L, "Bruno", "Fotógrafo");

        when(perfilActivoService.resolver(ID_USUARIO, ID_PERFIL_ACTIVO))
                .thenReturn(perfilActivo);
        when(presenciaService.ahora())
                .thenReturn(LocalDateTime.of(2026, 11, 9, 12, 0));
        when(presenciaService.limite(any(LocalDateTime.class)))
                .thenAnswer(invocacion -> ((LocalDateTime) invocacion.getArgument(0))
                        .minusSeconds(120));
        when(presenciaService.estaEnLinea(any(Perfil.class), any(LocalDateTime.class)))
                .thenReturn(true);
    }

    @Test
    void buscar_devuelveContraparteYFechaDeConexion() {
        Conexion conexion = conexion(perfilActivo, contraparte);
        mockPagina(conexion);

        PaginaResponse<ConexionResponse> respuesta = service.buscar(
                ID_USUARIO, ID_PERFIL_ACTIVO, null, 0, 20);

        assertThat(respuesta.contenido()).hasSize(1);
        ConexionResponse item = respuesta.contenido().get(0);
        assertThat(item.idConexion()).isEqualTo(55L);
        assertThat(item.fechaInicioConexion())
                .isEqualTo(LocalDateTime.of(2026, 11, 1, 10, 0));
        assertThat(item.perfil().idPerfil()).isEqualTo(ID_CONTRAPARTE);
        assertThat(item.perfil().ciudad().nombre()).isEqualTo("Posadas");
        assertThat(item.perfil().ciudad().provincia().nombre()).isEqualTo("Misiones");
    }

    @Test
    void buscar_perfilActivoEsPerfil2_devuelvePerfil1ComoContraparte() {
        Conexion conexion = conexion(contraparte, perfilActivo);
        mockPagina(conexion);

        PaginaResponse<ConexionResponse> respuesta = service.buscar(
                ID_USUARIO, ID_PERFIL_ACTIVO, null, 0, 20);

        assertThat(respuesta.contenido().get(0).perfil().idPerfil())
                .isEqualTo(ID_CONTRAPARTE);
    }

    @Test
    void buscar_sinResultados_devuelvePaginaVacia() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(conexionRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        PaginaResponse<ConexionResponse> respuesta = service.buscar(
                ID_USUARIO, ID_PERFIL_ACTIVO, null, 0, 20);

        assertThat(respuesta.contenido()).isEmpty();
        assertThat(respuesta.totalElementos()).isZero();
    }

    @Test
    void buscar_tamanoMayorAlMaximo_seAcotaA100() {
        Conexion conexion = conexion(perfilActivo, contraparte);
        mockPagina(conexion);

        service.buscar(ID_USUARIO, ID_PERFIL_ACTIVO, null, 0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(conexionRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(captor.getValue().getSort().getOrderFor("fechaInicioConexion")
                .isDescending()).isTrue();
    }

    private void mockPagina(Conexion conexion) {
        PageRequest pageable = PageRequest.of(0, 20);
        when(conexionRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conexion), pageable, 1));
        when(conexionRepository.findAllByIdInWithPerfiles(
                List.of(conexion.getIdConexion())))
                .thenReturn(List.of(conexion));
    }

    private Conexion conexion(Perfil perfil1, Perfil perfil2) {
        return Conexion.builder()
                .idConexion(55L)
                .perfil1(perfil1)
                .perfil2(perfil2)
                .estado(EstadoConexion.ACTIVA)
                .fechaInicioConexion(LocalDateTime.of(2026, 11, 1, 10, 0))
                .build();
    }

    private Perfil perfil(
            Long idPerfil,
            String nombreArtistico,
            Long idUsuario,
            String nombre,
            String nombreProfesion) {

        Pais pais = Pais.builder().idPais(1L).nombre("Argentina").build();
        Provincia provincia = Provincia.builder()
                .idProvincia(1L).nombre("Misiones").pais(pais).build();
        Ciudad ciudad = Ciudad.builder()
                .idCiudad(1L).nombre("Posadas").provincia(provincia).build();
        Ubicacion ubicacion = Ubicacion.builder().idUbicacion(1L).ciudad(ciudad).build();

        Usuario usuario = Usuario.builder()
                .idUsuario(idUsuario)
                .nombre(nombre)
                .apellido("Prueba")
                .estado(EstadoUsuario.Activo)
                .ubicacion(ubicacion)
                .build();

        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de prueba")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .profesion(Profesion.builder()
                        .idProfesion(idPerfil == ID_PERFIL_ACTIVO ? 1L : 2L)
                        .codigo("PROF_" + idPerfil)
                        .nombre(nombreProfesion)
                        .build())
                .build();
    }
}
