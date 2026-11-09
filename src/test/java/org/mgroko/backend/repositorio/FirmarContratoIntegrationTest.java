package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.exception.ContratoNoModificableException;
import org.mgroko.backend.contratos.servicio.FirmarContratoService;
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
 * Integración de UC-55: firmar contrato.
 */
@SpringBootTest
@Transactional
class FirmarContratoIntegrationTest
        extends AbstractFirmaContratoIntegrationTest {

    @Autowired
    private FirmarContratoService firmarContratoService;

    @Test
    void primeraFirma_dejaElContratoPendienteYSinNotificarActivo() {
        Datos datos = crearProyectoConContrato();

        ContratoDetalleResponse response = firmar(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos);

        assertThat(response.estado()).isEqualTo("PENDIENTE_FIRMA");
        assertThat(response.miEstadoFirma()).isEqualTo("FIRMADO");
        assertThat(response.puedeFirmar()).isFalse();
        assertThat(response.firmados()).isEqualTo(1);
        assertThat(response.totalPartes()).isEqualTo(2);

        assertThat(contrato(datos.idContrato()).getEstado())
                .isEqualTo(EstadoContrato.PENDIENTE_FIRMA);

        FirmaContrato miFirma = firma(
                datos.idContrato(), datos.miembroProyecto().getIdMiembro());
        assertThat(miFirma.getEstadoFirma()).isEqualTo(EstadoFirmaContrato.FIRMADO);
        assertThat(miFirma.getFechaFirma()).isNotNull();
        assertThat(miFirma.getContratoVersion()).isNotNull();

        assertThat(notificaciones(datos.idContrato()))
                .noneMatch(n -> NotificacionService.TIPO_CONTRATO_ACTIVO
                        .equals(n.getTipo()));
    }

    @Test
    void ultimaFirma_activaElContratoYNotificaUnaSolaVezPorParte() {
        Datos datos = crearProyectoConContrato();

        firmar(datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(), datos);

        ContratoDetalleResponse response = firmar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos);

        assertThat(response.estado()).isEqualTo("ACTIVO");
        assertThat(response.miEstadoFirma()).isEqualTo("FIRMADO");
        assertThat(response.puedeFirmar()).isFalse();
        assertThat(response.firmados()).isEqualTo(2);
        assertThat(response.totalPartes()).isEqualTo(2);

        assertThat(contrato(datos.idContrato()).getEstado())
                .isEqualTo(EstadoContrato.ACTIVO);

        assertThat(firmas(datos.idContrato()))
                .allMatch(f -> f.getEstadoFirma() == EstadoFirmaContrato.FIRMADO);

        List<ContratoEvento> eventos = eventos(datos.idContrato());
        assertThat(eventos)
                .extracting(ContratoEvento::getTipoEvento)
                .contains(TipoEventoContrato.ACTIVADO);

        List<Notificacion> activo = notificaciones(datos.idContrato()).stream()
                .filter(n -> NotificacionService.TIPO_CONTRATO_ACTIVO
                        .equals(n.getTipo()))
                .toList();

        assertThat(activo).hasSize(2);
        assertThat(activo)
                .allMatch(n -> datos.proyecto().getIdProyecto()
                        .equals(n.getIdProyecto()));
    }

    @Test
    void firmarDosVeces_devuelveContratoYaFirmado() {
        Datos datos = crearProyectoConContrato();

        firmar(datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(), datos);

        assertThatThrownBy(() -> firmar(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos))
                .isInstanceOf(ContratoNoModificableException.class)
                .extracting("codigo")
                .isEqualTo("CONTRATO_YA_FIRMADO");
    }

    @Test
    void firmarSinSerParte_devuelveAccesoDenegado() {
        Datos datos = crearProyectoConContrato();

        assertThatThrownBy(() -> firmar(
                datos.noParte().getIdUsuario(),
                datos.perfilNoParte().getIdPerfil(),
                datos))
                .isInstanceOf(AccesoDenegadoProyectoException.class);

        assertThatThrownBy(() -> firmar(
                datos.ajeno().getIdUsuario(),
                datos.perfilAjeno().getIdPerfil(),
                datos))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void firmarConProyectoNoApto_devuelveErrorDeNegocio() {
        Datos datos = crearProyectoConContrato();
        cambiarEstadoProyecto(datos, EstadoProyecto.CANCELADO);

        assertThatThrownBy(() -> firmar(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos))
                .isInstanceOf(ContratoNoModificableException.class)
                .extracting("codigo")
                .isEqualTo("PROYECTO_NO_APTO_PARA_CONTRATO");
    }

    @Test
    void firmaCanceladaNoBloqueaLaActivacion() {
        Datos datos = crearProyectoConContrato();
        cancelarFirma(
                datos.idContrato(),
                datos.miembroProyecto().getIdMiembro());

        ContratoDetalleResponse response = firmar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos);

        assertThat(response.estado()).isEqualTo("ACTIVO");
        assertThat(response.firmados()).isEqualTo(1);
        assertThat(response.totalPartes()).isEqualTo(1);
        assertThat(contrato(datos.idContrato()).getEstado())
                .isEqualTo(EstadoContrato.ACTIVO);
    }

    @Test
    void firmarContratoYaActivo_devuelveContratoNoPendiente() {
        Datos datos = crearProyectoConContrato();

        firmar(datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(), datos);
        firmar(datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(), datos);

        assertThatThrownBy(() -> firmar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos))
                .isInstanceOf(ContratoNoModificableException.class)
                .extracting("codigo")
                .isEqualTo("CONTRATO_NO_PENDIENTE_FIRMA");
    }

    private ContratoDetalleResponse firmar(
            Long idUsuario,
            Long idPerfil,
            Datos datos) {

        return firmarContratoService.firmar(
                idUsuario,
                idPerfil,
                datos.proyecto().getIdProyecto(),
                datos.idContrato());
    }
}
