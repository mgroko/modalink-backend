package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.ProfesionalAsignadoResponse;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MiembrosActividadServiceTest {

    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;

    @InjectMocks
    private MiembrosActividadService service;

    @Test
    void listarExclusivos_mapeaLosDatosDelProfesional() {
        AsignacionActividad asignacion = AsignacionActividad.builder()
                .idAsignacionAct(30L)
                .miembro(MiembroProyecto.builder()
                        .idMiembro(20L)
                        .perfil(Perfil.builder()
                                .idPerfil(10L)
                                .nombreArtistico("Luna")
                                .usuario(Usuario.builder().idUsuario(1L).build())
                                .profesion(Profesion.builder()
                                        .idProfesion(2L)
                                        .nombre("Modelo")
                                        .build())
                                .build())
                        .build())
                .build();
        when(asignacionActividadRepository.findExclusivasByActividadId(55L))
                .thenReturn(List.of(asignacion));

        List<ProfesionalAsignadoResponse> respuesta = service.listarExclusivos(55L);

        assertThat(respuesta).singleElement().satisfies(profesional -> {
            assertThat(profesional.idAsignacionActividad()).isEqualTo(30L);
            assertThat(profesional.idMiembroProyecto()).isEqualTo(20L);
            assertThat(profesional.idPerfil()).isEqualTo(10L);
            assertThat(profesional.idUsuario()).isEqualTo(1L);
            assertThat(profesional.nombreArtistico()).isEqualTo("Luna");
            assertThat(profesional.idProfesion()).isEqualTo(2L);
            assertThat(profesional.nombreProfesion()).isEqualTo("Modelo");
        });
    }

    @Test
    void listarExclusivos_sinAsignaciones_devuelveListaVacia() {
        when(asignacionActividadRepository.findExclusivasByActividadId(55L))
                .thenReturn(List.of());

        assertThat(service.listarExclusivos(55L)).isEmpty();
    }

    @Test
    void guardarMiembro_eliminaLaAsignacionDelPerfil() {
        service.guardarMiembro(55L, 10L);

        verify(asignacionActividadRepository)
                .deleteByActividadIdAndPerfil(55L, 10L);
    }
}
