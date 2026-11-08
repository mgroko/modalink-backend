package org.mgroko.backend.contratos.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.contratos.dto.ContratoBusquedaResponse;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.dto.ContratoEventoResponse;
import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.OpcionesGeneracionContratoResponse;
import org.mgroko.backend.contratos.dto.ParteContratoResponse;
import org.mgroko.backend.contratos.dto.ParteContratoResumenResponse;
import org.mgroko.backend.contratos.dto.PrevisualizacionContratoResponse;
import org.mgroko.backend.contratos.dto.PrevisualizacionModificacionResponse;
import org.mgroko.backend.contratos.dto.ProyectoContratoResponse;
import org.mgroko.backend.contratos.dto.ProyectoFiltroContratoResponse;
import org.mgroko.backend.contratos.dto.TipoAcuerdoContratoResponse;
import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.contratos.exception.ContratoNoModificableException;
import org.mgroko.backend.contratos.servicio.BuscarContratoService;
import org.mgroko.backend.contratos.servicio.EliminarContratoService;
import org.mgroko.backend.contratos.servicio.FirmarContratoService;
import org.mgroko.backend.contratos.servicio.GenerarContratoService;
import org.mgroko.backend.contratos.servicio.ListarEventosContratoService;
import org.mgroko.backend.contratos.servicio.ListarProyectosContratoService;
import org.mgroko.backend.contratos.servicio.ModificarContratoService;
import org.mgroko.backend.contratos.servicio.ObtenerContratoService;
import org.mgroko.backend.contratos.servicio.ObtenerOpcionesContratoService;
import org.mgroko.backend.contratos.servicio.PrevisualizarContratoService;
import org.mgroko.backend.contratos.servicio.PrevisualizarModificacionContratoService;
import org.mgroko.backend.contratos.servicio.RechazarContratoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = ContratoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContratoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObtenerOpcionesContratoService opcionesService;

    @MockitoBean
    private PrevisualizarContratoService previsualizarService;

    @MockitoBean
    private GenerarContratoService generarService;

    @MockitoBean
    private BuscarContratoService buscarContratoService;

    @MockitoBean
    private ListarEventosContratoService listarEventosContratoService;

    @MockitoBean
    private ListarProyectosContratoService listarProyectosService;

    @MockitoBean
    private ObtenerContratoService obtenerContratoService;

    @MockitoBean
    private PrevisualizarModificacionContratoService previsualizarModificacionService;

    @MockitoBean
    private ModificarContratoService modificarContratoService;

    @MockitoBean
    private FirmarContratoService firmarContratoService;

    @MockitoBean
    private RechazarContratoService rechazarContratoService;

    @MockitoBean
    private EliminarContratoService eliminarContratoService;

    @Test
    void opciones_devuelve200ConDatosDelProyecto() throws Exception {
        var auth = autenticacion(10L);

        when(opcionesService.obtener(1L, 10L, 100L))
                .thenReturn(new OpcionesGeneracionContratoResponse(
                        new ProyectoContratoResponse(
                                100L, "Editorial Primavera", "Producción",
                                "PUBLICADO", null, null),
                        null,
                        List.of(new TipoAcuerdoContratoResponse(
                                1L, "CESION_DERECHOS_IMAGEN",
                                "Cesión de derechos de imagen", "Descripción")),
                        List.of(),
                        List.of()));

        mockMvc.perform(get("/proyectos/100/contratos/opciones")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proyecto.nombre").value("Editorial Primavera"))
                .andExpect(jsonPath("$.tiposAcuerdo[0].codigo")
                        .value("CESION_DERECHOS_IMAGEN"));
    }

    @Test
    void generar_conDatosValidos_devuelve201() throws Exception {
        var auth = autenticacion(10L);

        ContratoGeneradoResponse response = new ContratoGeneradoResponse(
                55L,
                100L,
                "Cesión - Editorial Primavera",
                "Contrato generado",
                "PENDIENTE_FIRMA",
                1,
                LocalDateTime.of(2026, 11, 7, 12, 0),
                LocalDateTime.of(2026, 11, 7, 12, 0),
                new TipoAcuerdoContratoResponse(
                        1L, "CESION_DERECHOS_IMAGEN",
                        "Cesión de derechos de imagen", null),
                "TEXTO",
                List.of(new ParteContratoResponse(
                        11L, 3L, "Ana Pérez", "DIRECTOR", "PENDIENTE")));

        when(generarService.generar(eq(1L), eq(10L), eq(100L), any()))
                .thenReturn(response);

        mockMvc.perform(post("/proyectos/100/contratos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idTipoAcuerdo": 1,
                                  "idsMiembro": [8],
                                  "clausulasAdicionales": ["Cláusula adicional"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idContrato").value(55L))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.partes[0].rolParte").value("DIRECTOR"));
    }

    @Test
    void previsualizar_devuelve200ConConflictos() throws Exception {
        var auth = autenticacion(10L);

        when(previsualizarService.previsualizar(eq(1L), eq(10L), eq(100L), any()))
                .thenReturn(new PrevisualizacionContratoResponse(
                        "TEXTO",
                        false,
                        List.of()));

        mockMvc.perform(post("/proyectos/100/contratos/previsualizar")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idTipoAcuerdo": 1,
                                  "idsMiembro": [8]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeGenerarse").value(false));
    }

    @Test
    void generar_sinDestinatarios_devuelve400() throws Exception {
        var auth = autenticacion(10L);

        mockMvc.perform(post("/proyectos/100/contratos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idTipoAcuerdo": 1,
                                  "idsMiembro": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generar_conDuplicadoActivo_devuelve409ConCodigo() throws Exception {
        var auth = autenticacion(10L);

        when(generarService.generar(eq(1L), eq(10L), eq(100L), any()))
                .thenThrow(new ContratoNoGenerableException(
                        "CONTRATO_DUPLICADO_ACTIVO",
                        "Uno o mas destinatarios ya tienen un contrato activo.",
                        List.of()));

        mockMvc.perform(post("/proyectos/100/contratos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idTipoAcuerdo": 1,
                                  "idsMiembro": [8]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONTRATO_DUPLICADO_ACTIVO"));
    }

    @Test
    void generar_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(post("/proyectos/100/contratos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idTipoAcuerdo": 1,
                                  "idsMiembro": [8]
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscar_devuelve200ConPagina() throws Exception {
        var auth = autenticacion(10L);

        ContratoBusquedaResponse fila = new ContratoBusquedaResponse(
                55L, "Contrato", 100L, "Proyecto", "PUBLICADO", 1L, "CESION",
                "Cesion", "PENDIENTE_FIRMA", null, null, null, null, 1,
                null, List.of(), "PENDIENTE", null, true,
                true, true, 0, 1, true);

        when(buscarContratoService.buscar(
                eq(1L), eq(10L), any(), eq(0), eq(20), eq(false)))
                .thenReturn(new PaginaResponse<>(
                        List.of(fila), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/contratos/buscar").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idContrato").value(55L))
                .andExpect(jsonPath("$.contenido[0].puedeFirmar").value(true))
                .andExpect(jsonPath("$.contenido[0].totalPartes").value(1));
    }

    @Test
    void eventosDelContrato_devuelveHistorialVisibleParaLasPartes() throws Exception {
        var auth = autenticacion(10L);

        when(listarEventosContratoService.listar(1L, 10L, 100L, 55L))
                .thenReturn(List.of(new ContratoEventoResponse(
                        1L,
                        "FIRMADO",
                        LocalDateTime.of(2026, 11, 8, 10, 0),
                        11L,
                        7L,
                        "Ana Pérez",
                        1L,
                        "Ana Gómez",
                        1,
                        null)));

        mockMvc.perform(get("/proyectos/100/contratos/55/eventos")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoEvento").value("FIRMADO"))
                .andExpect(jsonPath("$[0].nombreParte").value("Ana Pérez"))
                .andExpect(jsonPath("$[0].version").value(1));
    }

    @Test
    void buscar_conMiFirmaPendiente_pasaElFiltroAlServicio() throws Exception {
        var auth = autenticacion(10L);

        when(buscarContratoService.buscar(
                eq(1L), eq(10L), any(), eq(0), eq(20), eq(true)))
                .thenReturn(new PaginaResponse<>(
                        List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/contratos/buscar")
                        .param("miFirmaPendiente", "true")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isEmpty());
    }

    @Test
    void proyectos_devuelveSoloLosVisiblesParaElPerfil() throws Exception {
        var auth = autenticacion(10L);

        when(listarProyectosService.listar(1L, 10L))
                .thenReturn(List.of(
                        new ProyectoFiltroContratoResponse(100L, "Proyecto histórico"),
                        new ProyectoFiltroContratoResponse(101L, "Proyecto actual")));

        mockMvc.perform(get("/contratos/proyectos").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProyecto").value(100L))
                .andExpect(jsonPath("$[0].nombre").value("Proyecto histórico"))
                .andExpect(jsonPath("$[1].idProyecto").value(101L));
    }

    @Test
    void firmar_devuelve200() throws Exception {
        var auth = autenticacion(10L);

        when(firmarContratoService.firmar(1L, 10L, 100L, 55L))
                .thenReturn(detalle());

        mockMvc.perform(post("/proyectos/100/contratos/55/firmar")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idContrato").value(55L));
    }

    @Test
    void rechazar_devuelve200() throws Exception {
        var auth = autenticacion(10L);

        when(rechazarContratoService.rechazar(
                eq(1L), eq(10L), eq(100L), eq(55L), any()))
                .thenReturn(detalle());

        mockMvc.perform(post("/proyectos/100/contratos/55/rechazar")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "motivo": "No estoy de acuerdo con la cláusula."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idContrato").value(55L));
    }

    @Test
    void rechazar_sinMotivo_devuelve400() throws Exception {
        var auth = autenticacion(10L);

        mockMvc.perform(post("/proyectos/100/contratos/55/rechazar")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "motivo": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detalle_devuelve200() throws Exception {
        var auth = autenticacion(10L);

        when(obtenerContratoService.obtener(1L, 10L, 100L, 55L))
                .thenReturn(detalle());

        mockMvc.perform(get("/proyectos/100/contratos/55").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idContrato").value(55L));
    }

    @Test
    void previsualizarModificacion_devuelve200() throws Exception {
        var auth = autenticacion(10L);

        when(previsualizarModificacionService.previsualizar(
                eq(1L), eq(10L), eq(100L), eq(55L), any()))
                .thenReturn(new PrevisualizacionModificacionResponse(
                        "TEXTO", true, List.of()));

        mockMvc.perform(post("/proyectos/100/contratos/55/previsualizar")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idTipoAcuerdo": 1,
                                  "idsMiembro": [8]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("TEXTO"));
    }

    @Test
    void modificar_devuelve200() throws Exception {
        var auth = autenticacion(10L);

        when(modificarContratoService.modificar(
                eq(1L), eq(10L), eq(100L), eq(55L), any()))
                .thenReturn(detalle());

        mockMvc.perform(put("/proyectos/100/contratos/55")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idTipoAcuerdo": 1,
                                  "idsMiembro": [8]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idContrato").value(55L));
    }

    @Test
    void eliminar_devuelve204() throws Exception {
        var auth = autenticacion(10L);

        mockMvc.perform(delete("/proyectos/100/contratos/55")
                        .principal(auth))
                .andExpect(status().isNoContent());

        verify(eliminarContratoService).eliminar(1L, 10L, 100L, 55L);
    }

    @Test
    void eliminar_conProyectoNoApto_devuelve409ConCodigo() throws Exception {
        var auth = autenticacion(10L);

        doThrow(new ContratoNoModificableException(
                        "PROYECTO_NO_APTO_PARA_ELIMINACION",
                        "No se puede eliminar un contrato de un proyecto finalizado o cancelado."))
                .when(eliminarContratoService).eliminar(1L, 10L, 100L, 55L);

        mockMvc.perform(delete("/proyectos/100/contratos/55")
                        .principal(auth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo")
                        .value("PROYECTO_NO_APTO_PARA_ELIMINACION"));
    }

    private ContratoDetalleResponse detalle() {
        return new ContratoDetalleResponse(
                55L, 100L, "Proyecto", "PUBLICADO", 1L, "CESION", "Cesion",
                "Contrato", "Descripcion", "PENDIENTE_FIRMA", 1,
                LocalDateTime.of(2026, 11, 7, 12, 0),
                LocalDateTime.of(2026, 11, 7, 12, 0),
                null, null, "TEXTO", null,
                List.of(new ParteContratoResumenResponse(
                        11L, 3L, "Ana Pérez", "DIRECTOR", "PENDIENTE", null)),
                "PENDIENTE", null, true, null,
                true, true, 0, 1, true, null);
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idPerfilActivo) {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, idPerfilActivo, "Ana"));
        return auth;
    }
}
