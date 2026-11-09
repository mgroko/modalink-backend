package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.contratos.dto.BuscarContratosFiltro;
import org.mgroko.backend.contratos.dto.ContratoBusquedaResponse;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.exception.ContratoNoModificableException;
import org.mgroko.backend.contratos.servicio.BuscarContratoService;
import org.mgroko.backend.contratos.servicio.EliminarContratoService;
import org.mgroko.backend.contratos.servicio.FirmarContratoService;
import org.mgroko.backend.contratos.servicio.ObtenerContratoService;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.ContratoEvento;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.TipoEventoContrato;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración de UC-54: baja lógica de contrato.
 */
@SpringBootTest
@Transactional
class EliminarContratoIntegrationTest
        extends AbstractFirmaContratoIntegrationTest {

    @Autowired
    private EliminarContratoService eliminarContratoService;

    @Autowired
    private FirmarContratoService firmarContratoService;

    @Autowired
    private ObtenerContratoService obtenerContratoService;

    @Autowired
    private BuscarContratoService buscarContratoService;

    @Autowired
    private ContratoRepository contratoRepository;

    @Test
    void eliminarContratoPendienteDeFirma_registraBajaYNotifica() {
        Datos datos = crearProyectoConContrato();

        eliminarContratoService.eliminar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        em.flush();
        em.clear();

        Contrato contrato = contrato(datos.idContrato());
        assertThat(contrato.getEstado()).isEqualTo(EstadoContrato.ELIMINADO);
        assertThat(contrato.getFechaBaja()).isNotNull();

        assertThat(firmas(datos.idContrato()))
                .allMatch(f -> f.getEstadoFirma() == EstadoFirmaContrato.CANCELADA);

        assertThat(eventos(datos.idContrato()))
                .extracting(ContratoEvento::getTipoEvento)
                .contains(TipoEventoContrato.ELIMINADO);

        List<Notificacion> eliminado = notificaciones(datos.idContrato()).stream()
                .filter(n -> NotificacionService.TIPO_CONTRATO_ELIMINADO
                        .equals(n.getTipo()))
                .toList();

        assertThat(eliminado).hasSize(2);
        assertThat(eliminado)
                .allMatch(n -> datos.proyecto().getIdProyecto()
                        .equals(n.getIdProyecto()));

        assertThat(buscar(datos, false).totalElementos()).isZero();
        assertThat(buscar(datos, true).totalElementos()).isEqualTo(1);
    }

    @Test
    void eliminarContratoActivo_tambienEsBajaLogica() {
        Datos datos = crearProyectoConContrato();

        firmarContratoService.firmar(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());
        firmarContratoService.firmar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        assertThat(contrato(datos.idContrato()).getEstado())
                .isEqualTo(EstadoContrato.ACTIVO);

        eliminarContratoService.eliminar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        em.flush();
        em.clear();

        Contrato contrato = contrato(datos.idContrato());
        assertThat(contrato.getEstado()).isEqualTo(EstadoContrato.ELIMINADO);
        assertThat(contrato.getFechaBaja()).isNotNull();

        assertThat(firmas(datos.idContrato()))
                .allMatch(f -> f.getEstadoFirma() == EstadoFirmaContrato.FIRMADO);

        assertThat(notificaciones(datos.idContrato()))
                .filteredOn(n -> NotificacionService.TIPO_CONTRATO_ELIMINADO
                        .equals(n.getTipo()))
                .hasSize(2);
    }

    @Test
    void eliminarContratoBorrador_noNotifica() {
        Datos datos = crearProyectoConContrato();

        Contrato borrador = Contrato.builder()
                .proyecto(datos.proyecto())
                .tipoAcuerdo(datos.tipo())
                .miembroEmisor(datos.emisor())
                .nombre("Borrador UC54")
                .descripcion("Borrador")
                .versionActual(1)
                .estado(EstadoContrato.BORRADOR)
                .build();
        contratoRepository.saveAndFlush(borrador);

        em.flush();
        em.clear();

        eliminarContratoService.eliminar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                borrador.getIdContrato());

        em.flush();
        em.clear();

        Contrato eliminado = contrato(borrador.getIdContrato());
        assertThat(eliminado.getEstado()).isEqualTo(EstadoContrato.ELIMINADO);
        assertThat(eliminado.getFechaBaja()).isNotNull();

        assertThat(notificaciones(borrador.getIdContrato()))
                .noneMatch(n -> NotificacionService.TIPO_CONTRATO_ELIMINADO
                        .equals(n.getTipo()));
    }

    @Test
    void eliminarDosVeces_esIdempotenteYNoDuplicaNotificaciones() {
        Datos datos = crearProyectoConContrato();

        eliminarContratoService.eliminar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        long notificacionesAntes = notificaciones(datos.idContrato()).stream()
                .filter(n -> NotificacionService.TIPO_CONTRATO_ELIMINADO
                        .equals(n.getTipo()))
                .count();

        eliminarContratoService.eliminar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        em.flush();
        em.clear();

        assertThat(contrato(datos.idContrato()).getEstado())
                .isEqualTo(EstadoContrato.ELIMINADO);
        assertThat(notificaciones(datos.idContrato()).stream()
                .filter(n -> NotificacionService.TIPO_CONTRATO_ELIMINADO
                        .equals(n.getTipo()))
                .count()).isEqualTo(notificacionesAntes);
    }

    @Test
    void eliminarConProyectoFinalizado_devuelveErrorDeNegocio() {
        Datos datos = crearProyectoConContrato();
        cambiarEstadoProyecto(datos, EstadoProyecto.FINALIZADO);

        assertThatThrownBy(() -> eliminarContratoService.eliminar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato()))
                .isInstanceOf(ContratoNoModificableException.class)
                .extracting("codigo")
                .isEqualTo("PROYECTO_NO_APTO_PARA_ELIMINACION");
    }

    @Test
    void eliminarComoMiembroNoDirector_devuelveAccesoDenegado() {
        Datos datos = crearProyectoConContrato();

        assertThatThrownBy(() -> eliminarContratoService.eliminar(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato()))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void eliminarContratoDeOtroProyecto_devuelveNoEncontrado() {
        Datos datos = crearProyectoConContrato();

        assertThatThrownBy(() -> eliminarContratoService.eliminar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto() + 1,
                datos.idContrato()))
                .isInstanceOf(org.mgroko.backend.contratos.exception
                        .ContratoNoEncontradoException.class);
    }

    @Test
    void detalleDeContratoEliminadoNoPermiteEliminar() {
        Datos datos = crearProyectoConContrato();

        eliminarContratoService.eliminar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        ContratoDetalleResponse detalle = obtenerContratoService.obtener(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        assertThat(detalle.estado()).isEqualTo("ELIMINADO");
        assertThat(detalle.puedeEliminar()).isFalse();
        assertThat(detalle.motivoNoEliminable()).isEqualTo("CONTRATO_YA_ELIMINADO");
    }

    private PaginaResponse<ContratoBusquedaResponse> buscar(
            Datos datos,
            boolean incluirEliminados) {

        List<EstadoContrato> estados = incluirEliminados
                ? List.of(EstadoContrato.ELIMINADO)
                : List.of(
                        EstadoContrato.PENDIENTE_FIRMA,
                        EstadoContrato.ACTIVO,
                        EstadoContrato.RECHAZADO_POR_PARTES,
                        EstadoContrato.CANCELADO);

        BuscarContratosFiltro filtro = new BuscarContratosFiltro(
                datos.proyecto().getIdProyecto(),
                estados,
                null, null, null, null, null, null, null);

        return buscarContratoService.buscar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                filtro,
                0,
                20);
    }
}
