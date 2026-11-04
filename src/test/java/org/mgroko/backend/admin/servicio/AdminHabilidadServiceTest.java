package org.mgroko.backend.admin.servicio;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.AdminHabilidadRequest;
import org.mgroko.backend.admin.exception.HabilidadDuplicadaException;
import org.mgroko.backend.admin.exception.HabilidadEnUsoException;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.perfiles.evento.HabilidadEliminadaEvent;
import org.mgroko.backend.perfiles.evento.HabilidadModificadaEvent;
import org.mgroko.backend.perfiles.servicio.HabilidadService;
import org.mgroko.backend.proyectos.exception.HabilidadNoEncontradaException;
import org.mgroko.backend.repositorio.HabilidadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AdminHabilidadServiceTest {

    @Mock
    private HabilidadRepository habilidadRepository;

    @Mock
    private HabilidadService habilidadService;

    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;

    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminHabilidadService adminHabilidadService;

    private Habilidad habilidad() {
        return Habilidad.builder()
                .idHabilidad(1L)
                .codigo("FOTO_ESTUDIO")
                .nombre("Fotografía de Estudio")
                .descripcion("Iluminación y sets.")
                .build();
    }

    private AdminHabilidadRequest request(String nombre, String descripcion) {
        return new AdminHabilidadRequest(nombre, descripcion);
    }

    // ------------------------------------------------------------- listar

    @Test
    void listar_delegaEnBusquedaPublica() {
        HabilidadResponse response = new HabilidadResponse(
                1L, "FOTO_ESTUDIO", "Fotografía de Estudio", "Iluminación y sets.");
        when(habilidadService.buscar("foto")).thenReturn(List.of(response));

        List<HabilidadResponse> resultado = adminHabilidadService.listar("foto");

        assertEquals(1, resultado.size());
        verify(habilidadService).buscar("foto");
    }

    // ------------------------------------------------------------ obtener

    @Test
    void obtener_existente_retornaResponse() {
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(habilidad()));

        HabilidadResponse response = adminHabilidadService.obtener(1L);

        assertEquals(1L, response.idHabilidad());
        assertEquals("FOTO_ESTUDIO", response.codigo());
    }

    @Test
    void obtener_inexistente_lanza404() {
        when(habilidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(HabilidadNoEncontradaException.class,
                () -> adminHabilidadService.obtener(99L));
    }

    // -------------------------------------------------------------- crear

    @Test
    void crear_datosValidos_generaCodigoYGuarda() {
        when(habilidadRepository.existsByNombreIgnoreCase("Fotografía de Estudio"))
                .thenReturn(false);
        when(habilidadRepository.existsByCodigoIgnoreCase("FOTOGRAFIA_DE_ESTUDIO"))
                .thenReturn(false);
        when(habilidadRepository.save(any(Habilidad.class))).thenAnswer(invocation -> {
            Habilidad guardada = invocation.getArgument(0);
            guardada.setIdHabilidad(11L);
            return guardada;
        });

        HabilidadResponse response = adminHabilidadService.crear(
                request("  Fotografía de Estudio  ", "  Iluminación y sets.  "));

        assertEquals(11L, response.idHabilidad());
        assertEquals("FOTOGRAFIA_DE_ESTUDIO", response.codigo());
        assertEquals("Fotografía de Estudio", response.nombre());
        assertEquals("Iluminación y sets.", response.descripcion());
    }

    @Test
    void crear_nombreDuplicado_lanza409() {
        when(habilidadRepository.existsByNombreIgnoreCase("Fotografía de Estudio"))
                .thenReturn(true);

        assertThrows(HabilidadDuplicadaException.class,
                () -> adminHabilidadService.crear(
                        request("Fotografía de Estudio", null)));

        verify(habilidadRepository, never()).save(any(Habilidad.class));
    }

    @Test
    void crear_codigoExistente_agregaSufijo() {
        when(habilidadRepository.existsByNombreIgnoreCase("Fotografía de Estudio"))
                .thenReturn(false);
        when(habilidadRepository.existsByCodigoIgnoreCase("FOTOGRAFIA_DE_ESTUDIO"))
                .thenReturn(true);
        when(habilidadRepository.existsByCodigoIgnoreCase("FOTOGRAFIA_DE_ESTUDIO_2"))
                .thenReturn(false);
        when(habilidadRepository.save(any(Habilidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminHabilidadService.crear(request("Fotografía de Estudio", null));

        ArgumentCaptor<Habilidad> captor = ArgumentCaptor.forClass(Habilidad.class);
        verify(habilidadRepository).save(captor.capture());
        assertEquals("FOTOGRAFIA_DE_ESTUDIO_2", captor.getValue().getCodigo());
    }

    @Test
    void crear_descripcionBlanca_guardaNull() {
        when(habilidadRepository.existsByNombreIgnoreCase("Nueva"))
                .thenReturn(false);
        when(habilidadRepository.existsByCodigoIgnoreCase("NUEVA"))
                .thenReturn(false);
        when(habilidadRepository.save(any(Habilidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HabilidadResponse response = adminHabilidadService.crear(
                request("Nueva", "   "));

        assertNull(response.descripcion());
    }

    // ---------------------------------------------------------- actualizar

    @Test
    void actualizar_datosValidos_mantieneCodigo() {
        Habilidad existente = habilidad();
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(habilidadRepository.existsByNombreIgnoreCaseAndIdHabilidadNot(
                "Fotografía Editorial", 1L)).thenReturn(false);
        when(perfilRepository.findIdsPerfilByHabilidad(1L)).thenReturn(List.of());
        when(habilidadRepository.save(any(Habilidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HabilidadResponse response = adminHabilidadService.actualizar(
                1L, request("Fotografía Editorial", "Nueva descripción"));

        assertEquals("FOTO_ESTUDIO", response.codigo());
        assertEquals("Fotografía Editorial", response.nombre());
        assertEquals("Nueva descripción", response.descripcion());
    }

    @Test
    void actualizar_conCambiosYPerfilesAsociados_publicaEvento() {
        Habilidad existente = habilidad();
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(habilidadRepository.existsByNombreIgnoreCaseAndIdHabilidadNot(
                "Fotografía Editorial", 1L)).thenReturn(false);
        when(perfilRepository.findIdsPerfilByHabilidad(1L)).thenReturn(List.of(10L, 11L));
        when(habilidadRepository.save(any(Habilidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminHabilidadService.actualizar(
                1L, request("Fotografía Editorial", "Nueva descripción"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertTrue(captor.getValue() instanceof HabilidadModificadaEvent);

        HabilidadModificadaEvent evento = (HabilidadModificadaEvent) captor.getValue();
        assertEquals(1L, evento.idHabilidad());
        assertEquals("Fotografía Editorial", evento.nombreHabilidad());
        assertEquals(List.of(10L, 11L), evento.idsPerfilesDestinatarios());
    }

    @Test
    void actualizar_sinCambios_noPublicaEvento() {
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(habilidad()));
        when(habilidadRepository.existsByNombreIgnoreCaseAndIdHabilidadNot(
                "Fotografía de Estudio", 1L)).thenReturn(false);
        when(habilidadRepository.save(any(Habilidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminHabilidadService.actualizar(
                1L, request("Fotografía de Estudio", "Iluminación y sets."));

        verifyNoInteractions(eventPublisher);
        verify(perfilRepository, never()).findIdsPerfilByHabilidad(any(Long.class));
    }

    @Test
    void actualizar_nombreDuplicado_lanza409() {
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(habilidad()));
        when(habilidadRepository.existsByNombreIgnoreCaseAndIdHabilidadNot(
                "Otra", 1L)).thenReturn(true);

        assertThrows(HabilidadDuplicadaException.class,
                () -> adminHabilidadService.actualizar(1L, request("Otra", null)));

        verify(habilidadRepository, never()).save(any(Habilidad.class));
    }

    @Test
    void actualizar_inexistente_lanza404() {
        when(habilidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(HabilidadNoEncontradaException.class,
                () -> adminHabilidadService.actualizar(99L, request("Otra", null)));
    }

    // ----------------------------------------------------------- eliminar

    @Test
    void eliminar_usadaEnRequerimientoGral_lanza409() {
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(habilidad()));
        when(requerimientoGralProyectoRepository.existsByHabilidades_IdHabilidad(1L))
                .thenReturn(true);

        assertThrows(HabilidadEnUsoException.class,
                () -> adminHabilidadService.eliminar(1L));

        verify(habilidadRepository, never()).desasociarDePerfiles(any(Long.class));
        verify(habilidadRepository, never()).delete(any(Habilidad.class));
    }

    @Test
    void eliminar_usadaEnRequerimientoActividad_lanza409() {
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(habilidad()));
        when(requerimientoGralProyectoRepository.existsByHabilidades_IdHabilidad(1L))
                .thenReturn(false);
        when(requerimientoActividadRepository.existsByHabilidades_IdHabilidad(1L))
                .thenReturn(true);

        assertThrows(HabilidadEnUsoException.class,
                () -> adminHabilidadService.eliminar(1L));

        verify(habilidadRepository, never()).desasociarDePerfiles(any(Long.class));
    }

    @Test
    void eliminar_sinUso_desasociaPerfilesYBorra() {
        Habilidad existente = habilidad();
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(requerimientoGralProyectoRepository.existsByHabilidades_IdHabilidad(1L))
                .thenReturn(false);
        when(requerimientoActividadRepository.existsByHabilidades_IdHabilidad(1L))
                .thenReturn(false);
        when(perfilRepository.findIdsPerfilByHabilidad(1L)).thenReturn(List.of());
        when(habilidadRepository.desasociarDePerfiles(1L)).thenReturn(2);

        adminHabilidadService.eliminar(1L);

        verify(habilidadRepository).desasociarDePerfiles(1L);
        verify(habilidadRepository).delete(existente);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_conPerfilesAsociados_publicaEvento() {
        Habilidad existente = habilidad();
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(requerimientoGralProyectoRepository.existsByHabilidades_IdHabilidad(1L))
                .thenReturn(false);
        when(requerimientoActividadRepository.existsByHabilidades_IdHabilidad(1L))
                .thenReturn(false);
        when(perfilRepository.findIdsPerfilByHabilidad(1L)).thenReturn(List.of(10L, 11L));
        when(habilidadRepository.desasociarDePerfiles(1L)).thenReturn(2);

        adminHabilidadService.eliminar(1L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertTrue(captor.getValue() instanceof HabilidadEliminadaEvent);

        HabilidadEliminadaEvent evento = (HabilidadEliminadaEvent) captor.getValue();
        assertEquals(1L, evento.idHabilidad());
        assertEquals("Fotografía de Estudio", evento.nombreHabilidad());
        assertEquals(List.of(10L, 11L), evento.idsPerfilesDestinatarios());
    }

    @Test
    void eliminar_inexistente_lanza404() {
        when(habilidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(HabilidadNoEncontradaException.class,
                () -> adminHabilidadService.eliminar(99L));
    }
}
