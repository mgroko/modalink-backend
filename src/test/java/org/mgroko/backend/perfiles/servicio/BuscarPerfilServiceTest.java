package org.mgroko.backend.perfiles.servicio;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.BuscarPerfilesFiltro;
import org.mgroko.backend.perfiles.dto.PerfilBusquedaResponse;
import org.mgroko.backend.repositorio.PerfilRepository;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BuscarPerfilServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    private BuscarPerfilService buscarPerfilService;

    @BeforeEach
    void setUp() {
        buscarPerfilService = new BuscarPerfilService(perfilRepository);
    }

    private Perfil crearPerfilMock(Long id, String nombreArtistico) {
        Genero genero = Genero.builder().idGenero(1L).codigo("MASC").build();
        Ubicacion ubicacion = Ubicacion.builder().idUbicacion(1L).localidad("Rosario").provincia("Santa Fe").build();
        Usuario usuario = Usuario.builder()
                .idUsuario(10L)
                .nombre("Juan")
                .apellido("Perez")
                .estado(EstadoUsuario.Activo)
                .genero(genero)
                .ubicacion(ubicacion)
                .build();
        Profesion profesion = Profesion.builder().idProfesion(1L).nombre("Fotografo").build();

        return Perfil.builder()
                .idPerfil(id)
                .nombreArtistico(nombreArtistico)
                .biografia("Bio de prueba")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .caracteristicas(Set.of())
                .habilidades(Set.of())
                .build();
    }

    @Test
    void buscarPerfiles_conPaginacionPorDefecto_retornaPaginaResponse() {
        Perfil p1 = crearPerfilMock(1L, "Arte1");
        Perfil p2 = crearPerfilMock(2L, "Arte2");
        PageImpl<Perfil> page = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 20), 2);

        when(perfilRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        BuscarPerfilesFiltro filtro = new BuscarPerfilesFiltro(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PaginaResponse<PerfilBusquedaResponse> resultado = buscarPerfilService.buscarPerfiles(filtro, 0, 20, false);

        assertThat(resultado).isNotNull();
        assertThat(resultado.contenido()).hasSize(2);
        assertThat(resultado.totalElementos()).isEqualTo(2);
        assertThat(resultado.tamanoPagina()).isEqualTo(20);
        assertThat(resultado.paginaActual()).isEqualTo(0);
        assertThat(resultado.contenido().get(0).nombreArtistico()).isEqualTo("Arte1");
    }

    @Test
    void buscarPerfiles_conOpcionTodos_usaPageableUnpaged() {
        Perfil p1 = crearPerfilMock(1L, "Arte1");
        PageImpl<Perfil> page = new PageImpl<>(List.of(p1));

        when(perfilRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        BuscarPerfilesFiltro filtro = new BuscarPerfilesFiltro("Arte", null, null, null, null, null, null, null, null, null, null, null, null, null);
        PaginaResponse<PerfilBusquedaResponse> resultado = buscarPerfilService.buscarPerfiles(filtro, 0, 0, true);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(perfilRepository).findAll(any(Specification.class), captor.capture());

        assertThat(captor.getValue().isUnpaged()).isTrue();
        assertThat(resultado.contenido()).hasSize(1);
    }

    @Test
    void buscarPerfiles_sinCoincidencias_retornaPaginaVacia() {
        PageImpl<Perfil> page = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
        when(perfilRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        BuscarPerfilesFiltro filtro = new BuscarPerfilesFiltro("Inexistente", null, null, null, null, null, null, null, null, null, null, null, null, null);
        PaginaResponse<PerfilBusquedaResponse> resultado = buscarPerfilService.buscarPerfiles(filtro, 0, 50, false);

        assertThat(resultado.contenido()).isEmpty();
        assertThat(resultado.totalElementos()).isZero();
        assertThat(resultado.tamanoPagina()).isEqualTo(50);
    }
}
