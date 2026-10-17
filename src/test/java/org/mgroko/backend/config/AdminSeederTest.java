package org.mgroko.backend.config;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.repositorio.CiudadRepository;
import org.mgroko.backend.repositorio.GeneroRepository;
import org.mgroko.backend.repositorio.PaisRepository;
import org.mgroko.backend.repositorio.ProvinciaRepository;
import org.mgroko.backend.repositorio.RolGlobalRepository;
import org.mgroko.backend.repositorio.UbicacionRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolGlobalRepository rolGlobalRepository;

    @Mock
    private GeneroRepository generoRepository;

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private CiudadRepository ciudadRepository;

    @Mock
    private ProvinciaRepository provinciaRepository;

    @Mock
    private PaisRepository paisRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminSeeder seeder;

    private Pais paisArgentina;

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(seeder, "adminCorreo", "admin@modalink.com");
        ReflectionTestUtils.setField(seeder, "adminPassword", "admin123");
        ReflectionTestUtils.setField(seeder, "adminDni", "00000000");
        paisArgentina = Pais.builder()
                .idPais(1L)
                .codigoIso("AR")
                .nombre("Argentina")
                .activo(true)
                .build();
    }

    private void prepararFlujoDeCreacion() {
        when(usuarioRepository.existsByCorreo("admin@modalink.com")).thenReturn(false);
        when(rolGlobalRepository.findByNombre("Administrador"))
                .thenReturn(Optional.of(RolGlobal.builder().idRolGlobal(1L).nombre("Administrador").build()));
        when(generoRepository.findByCodigo("no_decirlo"))
                .thenReturn(Optional.of(Genero.builder().idGenero(1L).codigo("no_decirlo").nombre("No decirlo").build()));
        when(ubicacionRepository.findAll()).thenReturn(List.of());
        when(paisRepository.findByCodigoIso("AR")).thenReturn(Optional.of(paisArgentina));
        when(ciudadRepository.findByFuenteApiAndIdExterno("GEOREF", "LA_PLATA")).thenReturn(Optional.empty());
        when(ciudadRepository.save(org.mockito.ArgumentMatchers.any(Ciudad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ubicacionRepository.save(org.mockito.ArgumentMatchers.any(Ubicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void ubicacionDefault_creaProvinciaBuenosAiresConIdExterno06() {
        prepararFlujoDeCreacion();
        when(provinciaRepository.findByFuenteApiAndIdExterno("GEOREF", "06")).thenReturn(Optional.empty());
        when(provinciaRepository.findByNombreAndPais_IdPais("Buenos Aires", 1L)).thenReturn(Optional.empty());
        when(provinciaRepository.save(org.mockito.ArgumentMatchers.any(Provincia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        seeder.run();

        ArgumentCaptor<Provincia> captor = ArgumentCaptor.forClass(Provincia.class);
        verify(provinciaRepository).save(captor.capture());
        Provincia guardada = captor.getValue();
        assertThat(guardada.getNombre()).isEqualTo("Buenos Aires");
        assertThat(guardada.getIdExterno()).isEqualTo("06");
        assertThat(guardada.getFuenteApi()).isEqualTo("GEOREF");
        assertThat(guardada.getPais()).isSameAs(paisArgentina);
    }

    @Test
    void ubicacionDefault_reutilizaProvinciaPorClaveNatural() {
        prepararFlujoDeCreacion();
        Provincia existente = Provincia.builder()
                .idProvincia(9L)
                .nombre("Buenos Aires")
                .idExterno("06")
                .fuenteApi("GEOREF")
                .pais(paisArgentina)
                .activo(true)
                .build();
        when(provinciaRepository.findByFuenteApiAndIdExterno("GEOREF", "06"))
                .thenReturn(Optional.of(existente));

        seeder.run();

        verify(provinciaRepository, never()).save(org.mockito.ArgumentMatchers.any(Provincia.class));
    }

    @Test
    void ubicacionDefault_siYaExisteFilaLegacyPorNombre_laReutilizaSinGuardar() {
        prepararFlujoDeCreacion();
        Provincia legacy = Provincia.builder()
                .idProvincia(5L)
                .nombre("Buenos Aires")
                .idExterno("02")
                .fuenteApi("GEOREF")
                .pais(paisArgentina)
                .activo(true)
                .build();
        when(provinciaRepository.findByFuenteApiAndIdExterno("GEOREF", "06")).thenReturn(Optional.empty());
        when(provinciaRepository.findByNombreAndPais_IdPais("Buenos Aires", 1L))
                .thenReturn(Optional.of(legacy));

        seeder.run();

        verify(provinciaRepository, never()).save(org.mockito.ArgumentMatchers.any(Provincia.class));
    }
}
