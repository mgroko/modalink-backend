package org.mgroko.backend.requerimientos.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.requerimientos.dto.MotivoNoPostulable;

class ElegibilidadRequerimientoServiceTest {

    private static final Long ID_PROFESION = 2L;
    private static final Long OTRA_PROFESION = 3L;

    private final ElegibilidadRequerimientoService servicio =
            new ElegibilidadRequerimientoService();

    @Test
    void general_habilitado_paraNoMiembroConProfesionYCupo() {
        ResultadoElegibilidad resultado = servicio.evaluarGeneral(
                proyecto(EstadoProyecto.PUBLICADO, true),
                requerimientoGeneral(ID_PROFESION),
                cupo(2, 0),
                ID_PROFESION,
                null);

        assertThat(resultado.puedePostularse()).isTrue();
        assertThat(resultado.motivo()).isNull();
    }

    @Test
    void general_bloqueado_siElPerfilEsMiembroActivo() {
        ResultadoElegibilidad resultado = servicio.evaluarGeneral(
                proyecto(EstadoProyecto.PUBLICADO, true),
                requerimientoGeneral(ID_PROFESION),
                cupo(2, 0),
                ID_PROFESION,
                RolProyectoUsuario.MIEMBRO);

        assertThat(resultado.puedePostularse()).isFalse();
        assertThat(resultado.motivo()).isEqualTo(MotivoNoPostulable.MIEMBRO_ACTIVO);
    }

    @Test
    void general_bloqueado_siNoAceptaPostulacionesGenerales() {
        ResultadoElegibilidad resultado = servicio.evaluarGeneral(
                proyecto(EstadoProyecto.PUBLICADO, false),
                requerimientoGeneral(ID_PROFESION),
                cupo(2, 0),
                ID_PROFESION,
                null);

        assertThat(resultado.motivo()).isEqualTo(MotivoNoPostulable.PROYECTO_NO_ACEPTA_GENERAL);
    }

    @Test
    void general_bloqueado_siLaProfesionNoCoincide() {
        ResultadoElegibilidad resultado = servicio.evaluarGeneral(
                proyecto(EstadoProyecto.PUBLICADO, true),
                requerimientoGeneral(ID_PROFESION),
                cupo(2, 0),
                OTRA_PROFESION,
                null);

        assertThat(resultado.motivo()).isEqualTo(MotivoNoPostulable.PROFESION);
    }

    @Test
    void general_bloqueado_siNoHayCupo() {
        ResultadoElegibilidad resultado = servicio.evaluarGeneral(
                proyecto(EstadoProyecto.PUBLICADO, true),
                requerimientoGeneral(ID_PROFESION),
                cupo(1, 1),
                ID_PROFESION,
                null);

        assertThat(resultado.motivo()).isEqualTo(MotivoNoPostulable.CUPO);
    }

    @Test
    void actividad_habilitada_paraMiembroConProfesionYCupo() {
        ResultadoElegibilidad resultado = servicio.evaluarActividad(
                proyecto(EstadoProyecto.PUBLICADO, false),
                requerimientoActividad(ID_PROFESION, EstadoActividad.PENDIENTE),
                cupo(1, 0),
                ID_PROFESION,
                RolProyectoUsuario.MIEMBRO);

        assertThat(resultado.puedePostularse()).isTrue();
        assertThat(resultado.motivo()).isNull();
    }

    @Test
    void actividad_bloqueada_paraDirector() {
        ResultadoElegibilidad resultado = servicio.evaluarActividad(
                proyecto(EstadoProyecto.PUBLICADO, false),
                requerimientoActividad(ID_PROFESION, EstadoActividad.PENDIENTE),
                cupo(1, 0),
                ID_PROFESION,
                RolProyectoUsuario.DIRECTOR);

        assertThat(resultado.motivo()).isEqualTo(MotivoNoPostulable.DIRECTOR);
    }

    @Test
    void actividad_bloqueada_siNoEstaPendiente() {
        ResultadoElegibilidad resultado = servicio.evaluarActividad(
                proyecto(EstadoProyecto.PUBLICADO, false),
                requerimientoActividad(ID_PROFESION, EstadoActividad.EN_CURSO),
                cupo(1, 0),
                ID_PROFESION,
                null);

        assertThat(resultado.motivo()).isEqualTo(MotivoNoPostulable.ACTIVIDAD_NO_PENDIENTE);
    }

    @Test
    void actividad_bloqueada_siLaProfesionNoCoincide() {
        ResultadoElegibilidad resultado = servicio.evaluarActividad(
                proyecto(EstadoProyecto.PUBLICADO, false),
                requerimientoActividad(ID_PROFESION, EstadoActividad.PENDIENTE),
                cupo(1, 0),
                OTRA_PROFESION,
                null);

        assertThat(resultado.motivo()).isEqualTo(MotivoNoPostulable.PROFESION);
    }

    private Proyecto proyecto(EstadoProyecto estado, boolean aceptaGeneral) {
        return Proyecto.builder()
                .idProyecto(5L)
                .nombre("Proyecto")
                .estado(estado)
                .aceptaPostulacionGral(aceptaGeneral)
                .build();
    }

    private RequerimientoGralProyecto requerimientoGeneral(Long idProfesion) {
        return RequerimientoGralProyecto.builder()
                .idRequerimientoGral(30L)
                .cantidad(2)
                .profesion(Profesion.builder().idProfesion(idProfesion).nombre("Modelo").build())
                .proyecto(proyecto(EstadoProyecto.PUBLICADO, true))
                .build();
    }

    private RequerimientoActividad requerimientoActividad(
            Long idProfesion,
            EstadoActividad estadoActividad) {

        Proyecto proyecto = proyecto(EstadoProyecto.PUBLICADO, false);
        Actividad actividad = Actividad.builder()
                .idActividad(40L)
                .nombre("Actividad")
                .estado(estadoActividad)
                .planificacion(Planificacion.builder()
                        .idPlanificacion(50L)
                        .proyecto(proyecto)
                        .build())
                .build();

        return RequerimientoActividad.builder()
                .idRequerimiento(20L)
                .cantidad(1)
                .profesion(Profesion.builder().idProfesion(idProfesion).nombre("Modelo").build())
                .actividad(actividad)
                .build();
    }

    private CupoRequerimiento cupo(int cantidad, long cubiertos) {
        int faltantes = (int) Math.max(0, cantidad - cubiertos);
        return new CupoRequerimiento(cantidad, cubiertos, faltantes, cubiertos >= cantidad);
    }
}
