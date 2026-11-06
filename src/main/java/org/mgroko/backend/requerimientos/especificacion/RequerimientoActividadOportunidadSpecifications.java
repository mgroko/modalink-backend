package org.mgroko.backend.requerimientos.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActCaractValor;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.requerimientos.dto.BuscarRequerimientosGlobalFiltro;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Filtros del buscador global de requerimientos de actividad abiertos (UC-73).
 *
 * <p>El alcance es global: no exige un proyecto en la ruta. Sólo devuelve
 * requerimientos activos en actividades Pendiente, de proyectos
 * Publicado/Confirmado y con cupo disponible.</p>
 */
public final class RequerimientoActividadOportunidadSpecifications {

    private static final String PATRON_DIRECTOR = "director%";

    private RequerimientoActividadOportunidadSpecifications() {
    }

    public static Specification<RequerimientoActividad> conFiltros(
            Long idPerfilActivo,
            Long idProfesionPerfil,
            BuscarRequerimientosGlobalFiltro filtro) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNull(root.get("fechaArchivado")));

            Path<Actividad> actividad = root.get("actividad");
            Path<Planificacion> planificacion = actividad.get("planificacion");
            Path<Proyecto> proyecto = planificacion.get("proyecto");

            predicates.add(cb.equal(actividad.get("estado"), EstadoActividad.PENDIENTE));
            predicates.add(proyecto.get("estado")
                    .in(EstadoProyecto.PUBLICADO, EstadoProyecto.CONFIRMADO));

            predicates.add(conCupoDisponible(query, cb, root));
            predicates.add(conVisibilidad(query, cb, proyecto, idPerfilActivo));

            if (filtro != null) {
                aplicarFiltros(query, cb, root, actividad, proyecto, idPerfilActivo,
                        idProfesionPerfil, filtro, predicates);
            }

            ordenar(query, cb, root, actividad, idProfesionPerfil);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void aplicarFiltros(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoActividad> root,
            Path<Actividad> actividad,
            Path<Proyecto> proyecto,
            Long idPerfilActivo,
            Long idProfesionPerfil,
            BuscarRequerimientosGlobalFiltro filtro,
            List<Predicate> predicates) {

        Long idProfesion = filtro.idProfesion();
        if (idProfesion == null && Boolean.TRUE.equals(filtro.soloMiProfesion())) {
            idProfesion = idProfesionPerfil;
        }
        if (idProfesion != null) {
            predicates.add(cb.equal(root.get("profesion").get("idProfesion"), idProfesion));
        }

        if (filtro.idProyecto() != null) {
            predicates.add(cb.equal(proyecto.get("idProyecto"), filtro.idProyecto()));
        }

        if (filtro.idActividad() != null) {
            predicates.add(cb.equal(actividad.get("idActividad"), filtro.idActividad()));
        }

        if (tieneTexto(filtro.texto())) {
            String patron = LikePatrones.contiene(filtro.texto());
            predicates.add(cb.or(
                    cb.like(cb.lower(proyecto.get("nombre")), patron,
                            LikePatrones.CARACTER_ESCAPE),
                    cb.like(cb.lower(actividad.get("nombre")), patron,
                            LikePatrones.CARACTER_ESCAPE),
                    cb.like(cb.lower(actividad.get("descripcion")), patron,
                            LikePatrones.CARACTER_ESCAPE),
                    cb.like(cb.lower(root.get("descripcion")), patron,
                            LikePatrones.CARACTER_ESCAPE),
                    cb.like(cb.lower(root.get("profesion").get("nombre")), patron,
                            LikePatrones.CARACTER_ESCAPE)));
        }

        if (filtro.idCaracteristica() != null || filtro.idValorCaracteristica() != null) {
            predicates.add(conCaracteristica(
                    query, cb, root, filtro.idCaracteristica(),
                    filtro.idValorCaracteristica()));
        }

        if (filtro.idsHabilidades() != null && !filtro.idsHabilidades().isEmpty()) {
            predicates.add(conHabilidad(query, cb, root, filtro.idsHabilidades()));
        }

        if (Boolean.TRUE.equals(filtro.soloPostulables()) && idPerfilActivo != null) {
            predicates.add(sinDirectorActivo(query, cb, proyecto, idPerfilActivo));
        }

        if (Boolean.TRUE.equals(filtro.excluirYaPostulados()) && idPerfilActivo != null) {
            predicates.add(sinPostulacion(query, cb, root, idPerfilActivo));
        }
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static Predicate conCupoDisponible(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoActividad> root) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<AsignacionActividad> asignacion = subquery.from(AsignacionActividad.class);
        subquery.select(cb.count(asignacion));
        subquery.where(
                cb.equal(asignacion.get("requerimientoActividad"), root),
                cb.equal(asignacion.get("miembro").get("estadoParticipacion"),
                        EstadoParticipacion.ACTIVO));

        return cb.greaterThan(root.<Integer>get("cantidad").as(Long.class), subquery);
    }

    private static Predicate conVisibilidad(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> proyecto,
            Long idPerfilActivo) {

        Predicate publico = cb.equal(proyecto.get("privacidad"), Privacidad.PUBLICO);
        if (idPerfilActivo == null) {
            return publico;
        }

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<MiembroProyecto> miembro = subquery.from(MiembroProyecto.class);
        subquery.select(miembro.get("idMiembro"));
        subquery.where(
                cb.equal(miembro.get("proyecto"), proyecto),
                cb.equal(miembro.get("perfil").get("idPerfil"), idPerfilActivo),
                cb.equal(miembro.get("estadoParticipacion"), EstadoParticipacion.ACTIVO));

        return cb.or(publico, cb.exists(subquery));
    }

    private static Predicate conCaracteristica(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoActividad> root,
            Long idCaracteristica,
            Long idValorCaracteristica) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<RequerimientoActCaract> caracteristica =
                subquery.from(RequerimientoActCaract.class);
        subquery.select(caracteristica.get("idReqActCaract"));

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(caracteristica.get("requerimientoActividad"), root));

        if (idCaracteristica != null) {
            predicates.add(cb.equal(
                    caracteristica.get("caracteristica").get("idCaracteristica"),
                    idCaracteristica));
        }

        if (idValorCaracteristica != null) {
            Subquery<Long> valorSubquery = query.subquery(Long.class);
            Root<RequerimientoActCaractValor> valor =
                    valorSubquery.from(RequerimientoActCaractValor.class);
            valorSubquery.select(valor.get("idReqActCaract"));
            valorSubquery.where(
                    cb.equal(valor.get("idReqActCaract"),
                            caracteristica.get("idReqActCaract")),
                    cb.equal(valor.get("idValor"), idValorCaracteristica));
            predicates.add(cb.exists(valorSubquery));
        }

        subquery.where(predicates.toArray(new Predicate[0]));
        return cb.exists(subquery);
    }

    private static Predicate conHabilidad(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoActividad> root,
            List<Long> idsHabilidades) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<RequerimientoActividad> correlacionado = subquery.correlate(root);
        Join<RequerimientoActividad, Habilidad> habilidad =
                correlacionado.join("habilidades");
        subquery.select(habilidad.get("idHabilidad"));
        subquery.where(habilidad.get("idHabilidad").in(idsHabilidades));
        return cb.exists(subquery);
    }

    private static Predicate sinDirectorActivo(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> proyecto,
            Long idPerfilActivo) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<MiembroProyecto> miembro = subquery.from(MiembroProyecto.class);
        Join<MiembroProyecto, RolProyecto> rol = miembro.join("rolProyecto");
        subquery.select(miembro.get("idMiembro"));
        subquery.where(
                cb.equal(miembro.get("proyecto"), proyecto),
                cb.equal(miembro.get("perfil").get("idPerfil"), idPerfilActivo),
                cb.equal(miembro.get("estadoParticipacion"), EstadoParticipacion.ACTIVO),
                cb.or(
                        cb.equal(cb.lower(rol.get("codigo")), "director"),
                        cb.like(cb.lower(rol.get("nombre")), PATRON_DIRECTOR,
                                LikePatrones.CARACTER_ESCAPE)));
        return cb.not(cb.exists(subquery));
    }

    private static Predicate sinPostulacion(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoActividad> root,
            Long idPerfilActivo) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<PostulacionActividad> postulacion =
                subquery.from(PostulacionActividad.class);
        subquery.select(postulacion.get("idPostulacion"));
        subquery.where(
                cb.equal(postulacion.get("requerimiento"), root),
                cb.equal(postulacion.get("perfil").get("idPerfil"), idPerfilActivo));
        return cb.not(cb.exists(subquery));
    }

    private static void ordenar(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoActividad> root,
            Path<Actividad> actividad,
            Long idProfesionPerfil) {

        if (query == null || Number.class.isAssignableFrom(query.getResultType())) {
            return;
        }

        List<Order> orden = new ArrayList<>();
        if (idProfesionPerfil != null) {
            orden.add(cb.asc(cb.selectCase()
                    .when(cb.equal(root.get("profesion").get("idProfesion"), idProfesionPerfil),
                            0)
                    .otherwise(1)));
        }
        orden.add(cb.asc(actividad.get("fechaHoraInicio")));
        orden.add(cb.asc(root.get("idRequerimiento")));
        query.orderBy(orden);
    }
}
