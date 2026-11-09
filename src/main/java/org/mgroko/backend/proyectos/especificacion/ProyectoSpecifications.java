package org.mgroko.backend.proyectos.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class ProyectoSpecifications {

    private ProyectoSpecifications() {}

    /** Patrón de rol director: código 'DIRECTOR' o nombre 'Director...' (semilla de BD). */
    public static final String PATRON_ROL_DIRECTOR = "director%";

    /** Patrón de rol miembro: código 'MIEMBRO' o nombre 'Miembro...' (semilla de BD). */
    public static final String PATRON_ROL_MIEMBRO = "miembro%";

    /**
     * Búsqueda global (UC-29) con regla de visibilidad base:
     * un proyecto es visible para el perfil activo si es miembro activo,
     * o si no está en Borrador ni Cancelado y es Público. Los proyectos
     * cancelados solo se consultan desde "proyectos propios" (historial).
     */
    public static Specification<Proyecto> conFiltros(BuscarProyectosFiltro filtro, Long idPerfilActivo) {
        return (root, query, cb) -> {
            // Evitamos registros duplicados al hacer joins con colecciones
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(conPredicadoVisibilidad(query, cb, root, idPerfilActivo));

            if (filtro == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // Filtrado por nombre del proyecto (parcial, insensible a mayúsculas)
            if (filtro.nombre() != null && !filtro.nombre().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")),
                        LikePatrones.contiene(filtro.nombre()), LikePatrones.CARACTER_ESCAPE));
            }

            // Filtrado por estado del proyecto
            if (filtro.estado() != null) {
                predicates.add(cb.equal(root.get("estado"), filtro.estado()));
            }

            // Filtrado por director: miembro activo con rol director cuyo perfil coincide
            if (filtro.director() != null && !filtro.director().isBlank()) {
                predicates.add(conSubqueryDirector(query, cb, root, filtro.director()));
            }

            // Filtrado por profesión: el proyecto tiene al menos un requerimiento
            // abierto (general o de actividad Pendiente/En curso) de esa profesión.
            if (filtro.idProfesion() != null) {
                predicates.add(conProfesionRequerida(query, cb, root, filtro.idProfesion()));
            }

            // Filtrado por ubicación del proyecto (provincia y/o localidad Georef).
            if (tieneTexto(filtro.provinciaId()) || tieneTexto(filtro.localidadId())) {
                predicates.add(conUbicacion(cb, root, filtro.provinciaId(), filtro.localidadId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    /**
     * Ubicación del proyecto: {@code proyecto.ubicacion.ciudad} y su provincia.
     * Los ids son los identificadores externos del catálogo Georef, que es lo
     * que expone la API pública de ubicaciones.
     */
    private static Predicate conUbicacion(
            CriteriaBuilder cb,
            Path<Proyecto> root,
            String provinciaId,
            String localidadId) {

        Path<Ciudad> ciudad = root.get("ubicacion").get("ciudad");
        List<Predicate> predicates = new ArrayList<>();

        if (tieneTexto(provinciaId)) {
            predicates.add(cb.equal(
                    ciudad.get("provincia").get("idExterno"), provinciaId));
        }
        if (tieneTexto(localidadId)) {
            predicates.add(cb.equal(ciudad.get("idExterno"), localidadId));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    /**
     * El proyecto necesita esa profesión si tiene al menos un requerimiento
     * abierto: general no archivado, o de actividad no archivado en una
     * actividad Pendiente o En curso.
     */
    private static Predicate conProfesionRequerida(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> root,
            Long idProfesion) {

        Subquery<Long> general = query.subquery(Long.class);
        Root<RequerimientoGralProyecto> requerimientoGral =
                general.from(RequerimientoGralProyecto.class);
        general.select(requerimientoGral.get("idRequerimientoGral"));
        general.where(
                cb.equal(requerimientoGral.get("proyecto"), root),
                cb.isNull(requerimientoGral.get("fechaArchivado")),
                cb.equal(requerimientoGral.get("profesion").get("idProfesion"),
                        idProfesion));

        Subquery<Long> actividad = query.subquery(Long.class);
        Root<RequerimientoActividad> requerimientoActividad =
                actividad.from(RequerimientoActividad.class);
        actividad.select(requerimientoActividad.get("idRequerimiento"));
        actividad.where(
                cb.equal(requerimientoActividad.get("actividad")
                                .get("planificacion").get("proyecto"), root),
                cb.isNull(requerimientoActividad.get("fechaArchivado")),
                requerimientoActividad.get("actividad").get("estado")
                        .in(EstadoActividad.PENDIENTE, EstadoActividad.EN_CURSO),
                cb.equal(requerimientoActividad.get("profesion").get("idProfesion"),
                        idProfesion));

        return cb.or(cb.exists(general), cb.exists(actividad));
    }

    /**
     * Visibilidad: el perfil activo es miembro activo del proyecto, o el proyecto
     * es Público, no está en Borrador y no está Cancelado (misma regla que el
     * detalle del proyecto).
     */
    private static Predicate conPredicadoVisibilidad(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> root,
            Long idPerfilActivo) {

        Predicate esMiembroActivo = conSubqueryMiembroActivo(query, cb, root, idPerfilActivo);

        Predicate publicoNoBorrador = cb.and(
                cb.notEqual(root.get("estado"), EstadoProyecto.BORRADOR),
                cb.equal(root.get("privacidad"), Privacidad.PUBLICO));

        Predicate noCancelado = cb.notEqual(root.get("estado"), EstadoProyecto.CANCELADO);

        return cb.and(noCancelado, cb.or(esMiembroActivo, publicoNoBorrador));
    }

    /** Subquery: el perfil activo tiene una membresía ACTIVA en el proyecto. */
    private static Predicate conSubqueryMiembroActivo(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> root,
            Long idPerfilActivo) {

        var subquery = query.subquery(Long.class);
        var miembro = subquery.from(MiembroProyecto.class);

        subquery.select(miembro.get("proyecto").get("idProyecto"));
        subquery.where(
                cb.equal(miembro.get("proyecto"), root),
                cb.equal(miembro.get("estadoParticipacion"), EstadoParticipacion.ACTIVO),
                cb.equal(miembro.get("perfil").get("idPerfil"), idPerfilActivo));

        return cb.exists(subquery);
    }

    /** Predicado genérico: el miembro posee el rol Director (por codigo o por nombre). */
    public static Predicate esRolDirector(Path<MiembroProyecto> miembro, CriteriaBuilder cb) {
        return cb.or(
                cb.equal(cb.lower(miembro.get("rolProyecto").get("codigo")), "director"),
                cb.like(cb.lower(miembro.get("rolProyecto").get("nombre")),
                        PATRON_ROL_DIRECTOR, LikePatrones.CARACTER_ESCAPE));
    }

    private static Predicate conSubqueryDirector(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> root,
            String director) {

        var subquery = query.subquery(Long.class);
        var miembro = subquery.from(MiembroProyecto.class);

        subquery.select(miembro.get("proyecto").get("idProyecto"));

        List<Predicate> subqueryPredicates = new ArrayList<>();
        subqueryPredicates.add(cb.equal(miembro.get("proyecto"), root));
        subqueryPredicates.add(cb.equal(miembro.get("estadoParticipacion"), EstadoParticipacion.ACTIVO));
        subqueryPredicates.add(esRolDirector(miembro, cb));

        Join<MiembroProyecto, Perfil> perfil = miembro.join("perfil");
        var usuario = perfil.join("usuario");

        String patron = LikePatrones.contiene(director);
        subqueryPredicates.add(cb.or(
                cb.like(cb.lower(perfil.get("nombreArtistico")), patron, LikePatrones.CARACTER_ESCAPE),
                cb.like(cb.lower(usuario.get("nombre")), patron, LikePatrones.CARACTER_ESCAPE),
                cb.like(cb.lower(usuario.get("apellido")), patron, LikePatrones.CARACTER_ESCAPE)));

        subquery.where(subqueryPredicates.toArray(new Predicate[0]));
        return cb.exists(subquery);
    }
}
