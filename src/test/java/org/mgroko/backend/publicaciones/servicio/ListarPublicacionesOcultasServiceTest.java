package org.mgroko.backend.publicaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.publicaciones.dto.PublicacionOcultadaResponse;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ListarPublicacionesOcultasServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private PublicacionDatosService publicacionDatosService;

    private ListarPublicacionesOcultasService service;
    private Perfil perfil;

    @BeforeEach
    void setUp() {
        service = new ListarPublicacionesOcultasService(
                publicacionRepository, publicacionDatosService);

        perfil = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .build();
    }

    @Test
    void listar_devuelveSoloTituloYDescripcionDeLasOcultasDelPerfil() {
        Publicacion publicacion = Publicacion.builder()
                .idPublicacion(30L)
                .titulo("Editorial Otoño")
                .descripcion("Producción oculta")
                .fechaPublicacion(LocalDateTime.of(2026, 11, 10, 9, 0))
                .estado(EstadoPublicacion.OCULTA)
                .perfil(perfil)
                .build();

        when(publicacionDatosService.validarPerfilActivo(1L, 7L)).thenReturn(perfil);
        when(publicacionRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(publicacion),
                        PageRequest.of(0, 10),
                        1));

        PaginaResponse<PublicacionOcultadaResponse> response =
                service.listar(1L, 7L, "otoño", 0, 10);

        assertThat(response.totalElementos()).isEqualTo(1);
        assertThat(response.contenido()).singleElement().satisfies(item -> {
            assertThat(item.idPublicacion()).isEqualTo(30L);
            assertThat(item.titulo()).isEqualTo("Editorial Otoño");
            assertThat(item.descripcion()).isEqualTo("Producción oculta");
            assertThat(item.estado()).isEqualTo(EstadoPublicacion.OCULTA);
        });
    }
}
