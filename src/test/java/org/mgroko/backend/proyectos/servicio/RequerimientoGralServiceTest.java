package org.mgroko.backend.proyectos.servicio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.modelo.ValorCaracteristicaId;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.HabilidadRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequerimientoGralServiceTest {

    @Mock
    private ProfesionRepository profesionRepository;

    @Mock
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @Mock
    private ValorCaracteristicaRepository valorCaracteristicaRepository;

    @Mock
    private HabilidadRepository habilidadRepository;

    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;

    @Mock
    private RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RequerimientoGralService requerimientoGralService;

    private Profesion profesion;
    private RequerimientoGralProyecto requerimiento;

    @BeforeEach
    void setUp() {
        profesion = Profesion.builder().idProfesion(2L).nombre("Modelo").build();

        CaracteristicaTecnica caracteristica = CaracteristicaTecnica.builder()
                .idCaracteristica(5L)
                .codigo("COLOR_OJOS")
                .tipoDato(CaracteristicaTecnica.TIPO_ENUMERADO)
                .profesion(profesion)
                .build();

        RequerimientoGralCaract caract = RequerimientoGralCaract.builder()
                .idReqGralCaract(100L)
                .caracteristica(caracteristica)
                .valorMin(null)
                .valorMax(null)
                .build();

        requerimiento = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(10L)
                .cantidad(1)
                .descripcion("Viejo")
                .profesion(profesion)
                .proyecto(Proyecto.builder().idProyecto(100L).build())
                .caracteristicas(new ArrayList<>(List.of(caract)))
                .habilidades(new HashSet<>())
                .build();
        caract.setRequerimientoGral(requerimiento);

        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesion));
        when(caracteristicaTecnicaRepository.findById(5L)).thenReturn(Optional.of(caracteristica));
        when(valorCaracteristicaRepository.findById(new ValorCaracteristicaId(2L, 5L)))
                .thenReturn(Optional.of(new ValorCaracteristica()));
        when(habilidadRepository.findById(7L))
                .thenReturn(Optional.of(Habilidad.builder().idHabilidad(7L).build()));
        when(requerimientoGralProyectoRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void actualizar_conservaCaracteristicaYReemplazaValoresYHabilidades() {
        CrearRequerimientoCaractRequest caractRequest =
                new CrearRequerimientoCaractRequest(5L, null, null, List.of(2L));

        requerimientoGralService.actualizar(
                requerimiento,
                2L,
                3,
                "Nuevo",
                List.of(caractRequest),
                List.of(7L));

        assertThat(requerimiento.getCantidad()).isEqualTo(3);
        assertThat(requerimiento.getDescripcion()).isEqualTo("Nuevo");
        assertThat(requerimiento.getCaracteristicas()).hasSize(1);
        assertThat(requerimiento.getCaracteristicas().get(0).getIdReqGralCaract())
                .isEqualTo(100L);
        assertThat(requerimiento.getHabilidades())
                .extracting(Habilidad::getIdHabilidad)
                .containsExactly(7L);

        verify(requerimientoGralCaractValorRepository).deleteByIdReqGralCaract(100L);
        verify(requerimientoGralCaractValorRepository).save(any(RequerimientoGralCaractValor.class));
        verify(requerimientoGralCaractValorRepository).save(argThat(valor ->
                valor.getIdReqGralCaract().equals(100L)
                        && valor.getIdCaracteristica().equals(5L)
                        && valor.getIdValor().equals(2L)));
    }
}
