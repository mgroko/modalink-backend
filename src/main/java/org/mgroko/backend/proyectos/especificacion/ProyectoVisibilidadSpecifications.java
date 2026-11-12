package org.mgroko.backend.proyectos.especificacion;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoConexion;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Regla única de visibilidad de proyectos para consultas con Criteria.
 *
 * <p>Un proyecto es visible si el perfil activo es miembro activo, si el
 * proyecto es Público y no está en Borrador ni Cancelado, o si el proyecto es
 * Privado y el perfil es contacto de algún director activo del proyecto. Los
 * proyectos Cancelados solo quedan disponibles para miembros y ex-miembros
 * desde sus propios historiales.</p>
 */
public final class ProyectoVisibilidadSpecifications {

    public static final String PATRON_ROL_DIRECTOR = "director%";

    private ProyectoVisibilidadSpecifications() {
    }

    public static Predicate visibleParaPerfil(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> proyecto,
            Long idPerfilActivo) {

        Predicate esMiembro = idPerfilActivo == null
                ? cb.disjunction()
                : conSubqueryMiembroActivo(query, cb, proyecto, idPerfilActivo);

        Predicate noCancelado = cb.notEqual(proyecto.get("estado"), EstadoProyecto.CANCELADO);
        Predicate noBorrador = cb.notEqual(proyecto.get("estado"), EstadoProyecto.BORRADOR);

        Predicate publico = cb.and(
                noBorrador,
                cb.equal(proyecto.get("privacidad"), Privacidad.PUBLICO));

        Predicate privadoContacto = idPerfilActivo == null
                ? cb.disjunction()
                : cb.and(
                        noBorrador,
                        cb.equal(proyecto.get("privacidad"), Privacidad.PRIVADO),
                        conSubqueryContactoConDirector(query, cb, proyecto, idPerfilActivo));

        return cb.and(noCancelado, cb.or(esMiembro, publico, privadoContacto));
    }

    /** Predicado genérico: el miembro posee el rol Director (por código o por nombre). */
    public static Predicate esRolDirector(Path<MiembroProyecto> miembro, CriteriaBuilder cb) {
        return cb.or(
                cb.equal(cb.lower(miembro.get("rolProyecto").get("codigo")), "director"),
                cb.like(cb.lower(miembro.get("rolProyecto").get("nombre")),
                        PATRON_ROL_DIRECTOR, LikePatrones.CARACTER_ESCAPE));
    }

    private static Predicate conSubqueryMiembroActivo(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> proyecto,
            Long idPerfilActivo) {

        var subquery = query.subquery(Long.class);
        var miembro = subquery.from(MiembroProyecto.class);

        subquery.select(miembro.get("proyecto").get("idProyecto"));
        subquery.where(
                cb.equal(miembro.get("proyecto"), proyecto),
                cb.equal(miembro.get("estadoParticipacion"), EstadoParticipacion.ACTIVO),
                cb.equal(miembro.get("perfil").get("idPerfil"), idPerfilActivo));

        return cb.exists(subquery);
    }

    /**
     * Existe un director activo del proyecto (con perfil activo) que tiene una
     * conexión activa con el perfil consultante. La conexión es simétrica, por
     * eso se evalúan las dos orientaciones del par canónico.
     */
    private static Predicate conSubqueryContactoConDirector(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> proyecto,
            Long idPerfilActivo) {

        var subquery = query.subquery(Long.class);
        var miembro = subquery.from(MiembroProyecto.class);
        var conexion = subquery.from(Conexion.class);

        subquery.select(miembro.get("idMiembro"));
        subquery.where(
                cb.equal(miembro.get("proyecto"), proyecto),
                cb.equal(miembro.get("estadoParticipacion"), EstadoParticipacion.ACTIVO),
                cb.equal(miembro.get("perfil").get("estado"), EstadoPerfil.ACTIVO),
                esRolDirector(miembro, cb),
                cb.equal(conexion.get("estado"), EstadoConexion.ACTIVA),
                cb.or(
                        cb.and(
                                cb.equal(conexion.get("perfil1").get("idPerfil"), idPerfilActivo),
                                cb.equal(conexion.get("perfil2").get("idPerfil"),
                                        miembro.get("perfil").get("idPerfil"))),
                        cb.and(
                                cb.equal(conexion.get("perfil2").get("idPerfil"), idPerfilActivo),
                                cb.equal(conexion.get("perfil1").get("idPerfil"),
                                        miembro.get("perfil").get("idPerfil")))));

        return cb.exists(subquery);
    }
}
