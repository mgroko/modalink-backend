package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.ProyectoPublicacionFiltroResponse;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListarProyectosPublicacionesPerfilServiceTest {

    @Mock
    private VerPerfilService verPerfilService;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    private ListarProyectosPublicacionesPerfilService servicio;

    @BeforeEach
    void setUp() {
        servicio = new ListarProyectosPublicacionesPerfilService(
                verPerfilService, publicacionRepository, proyectoVisibilidadService);

        Perfil perfil = Perfil.builder().idPerfil(7L).build();
        when(verPerfilService.obtenerPerfilVisible(7L, 1L))
                .thenReturn(new VerPerfilService.PerfilVisible(perfil, false));
    }

    @Test
    void listar_devuelveSoloProyectosVisiblesConPublicaciones() {
        Proyecto visible = Proyecto.builder().idProyecto(25L).nombre("Campaña").build();
        Proyecto privado = Proyecto.builder().idProyecto(30L).nombre("Privado").build();

        when(publicacionRepository.findProyectosConPublicaciones(7L, EstadoPublicacion.ACTIVA))
                .thenReturn(List.of(visible, privado));
        when(proyectoVisibilidadService.puedeVer(visible, 2L)).thenReturn(true);
        when(proyectoVisibilidadService.puedeVer(privado, 2L)).thenReturn(false);

        List<ProyectoPublicacionFiltroResponse> response =
                servicio.listar(7L, 1L, 2L);

        assertEquals(1, response.size());
        assertEquals(25L, response.get(0).idProyecto());
        assertEquals("Campaña", response.get(0).nombre());
    }

    @Test
    void listar_sinProyectos_devuelveListaVacia() {
        when(publicacionRepository.findProyectosConPublicaciones(7L, EstadoPublicacion.ACTIVA))
                .thenReturn(List.of());

        assertTrue(servicio.listar(7L, 1L, 2L).isEmpty());
    }
}
