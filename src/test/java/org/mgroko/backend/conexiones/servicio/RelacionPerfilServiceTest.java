package org.mgroko.backend.conexiones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.conexiones.modelo.EstadoRelacionPerfil;
import org.mgroko.backend.conexiones.modelo.RelacionPerfil;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.SolicitudConexionRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelacionPerfilServiceTest {

    @Mock
    private SolicitudConexionRepository solicitudConexionRepository;

    @Mock
    private ConexionRepository conexionRepository;

    @InjectMocks
    private RelacionPerfilService service;

    @Test
    void obtener_sinPerfilActivo_devuelveNinguna() {
        RelacionPerfil relacion = service.obtener(null, 20L);

        assertThat(relacion.estado()).isEqualTo(EstadoRelacionPerfil.NINGUNA);
        verifyNoInteractions(conexionRepository, solicitudConexionRepository);
    }

    @Test
    void obtener_mismoPerfil_devuelveNinguna() {
        RelacionPerfil relacion = service.obtener(10L, 10L);

        assertThat(relacion.estado()).isEqualTo(EstadoRelacionPerfil.NINGUNA);
        verifyNoInteractions(conexionRepository, solicitudConexionRepository);
    }

    @Test
    void obtener_conexionActiva_devuelveConectado() {
        Conexion conexion = Conexion.builder()
                .idConexion(5L)
                .perfil1(Perfil.builder().idPerfil(10L).build())
                .perfil2(Perfil.builder().idPerfil(20L).build())
                .build();

        when(conexionRepository.findActivaEntre(10L, 20L))
                .thenReturn(Optional.of(conexion));

        RelacionPerfil relacion = service.obtener(10L, 20L);

        assertThat(relacion.estado()).isEqualTo(EstadoRelacionPerfil.CONECTADO);
        assertThat(relacion.idConexion()).isEqualTo(5L);
    }

    @Test
    void obtener_solicitudEnviada_devuelveSolicitudEnviada() {
        SolicitudConexion solicitud = SolicitudConexion.builder()
                .idSolicitudConexion(7L)
                .perfilRemitente(Perfil.builder().idPerfil(10L).build())
                .perfilDestinatario(Perfil.builder().idPerfil(20L).build())
                .build();

        when(conexionRepository.findActivaEntre(10L, 20L))
                .thenReturn(Optional.empty());
        when(solicitudConexionRepository.findPendienteEntre(10L, 20L))
                .thenReturn(Optional.of(solicitud));

        RelacionPerfil relacion = service.obtener(10L, 20L);

        assertThat(relacion.estado()).isEqualTo(EstadoRelacionPerfil.SOLICITUD_ENVIADA);
        assertThat(relacion.idSolicitudConexion()).isEqualTo(7L);
    }

    @Test
    void obtener_solicitudRecibida_devuelveSolicitudRecibida() {
        SolicitudConexion solicitud = SolicitudConexion.builder()
                .idSolicitudConexion(7L)
                .perfilRemitente(Perfil.builder().idPerfil(20L).build())
                .perfilDestinatario(Perfil.builder().idPerfil(10L).build())
                .build();

        when(conexionRepository.findActivaEntre(10L, 20L))
                .thenReturn(Optional.empty());
        when(solicitudConexionRepository.findPendienteEntre(10L, 20L))
                .thenReturn(Optional.of(solicitud));

        RelacionPerfil relacion = service.obtener(10L, 20L);

        assertThat(relacion.estado()).isEqualTo(EstadoRelacionPerfil.SOLICITUD_RECIBIDA);
        assertThat(relacion.idSolicitudConexion()).isEqualTo(7L);
    }
}
