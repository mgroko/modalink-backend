package org.mgroko.backend.requerimientos.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.especificacion.ProyectoVisibilidadSpecifications;
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
 * Filtros del buscador global de requerimientos generales abiertos (UC-72).
 *
 * <p>Sólo devuelve requerimientos activos, de proyectos Publicado/Confirmado,
 * con postulación general habilitada y cupo disponible. La visibilidad, la
 * profesión y las postulaciones previas se resuelven con subqueries para no
 * duplicar filas ni romper la paginación.</p>
 */
public final class RequerimientoGralOportunidadSpecifications {

    private RequerimientoGralOportunidadSpecifications() {
    }

    public static Specification<RequerimientoGralProyecto> conFiltros(
            Long idPerfilActivo,
            Long idProfesionPerfil,
            BuscarRequerimientosGlobalFiltro filtro) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNull(root.get("fechaArchivado")));

            Path<Proyecto> proyecto = root.get("proyecto");
            predicates.add(proyecto.get("estado")
                    .in(EstadoProyecto.PUBLICADO, EstadoProyecto.CONFIRMADO));
            predicates.add(cb.equal(proyecto.get("aceptaPostulacionGral"), Boolean.TRUE));

            predicates.add(conCupoDisponible(query, cb, root));
            predicates.add(ProyectoVisibilidadSpecifications.visibleParaPerfil(
                    query, cb, proyecto, idPerfilActivo));

            if (filtro != null) {
                aplicarFiltros(query, cb, root, proyecto, idPerfilActivo,
                        idProfesionPerfil, filtro, predicates);
            }

            ordenar(query, cb, root, idProfesionPerfil);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void aplicarFiltros(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoGralProyecto> root,
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

        if (tieneTexto(filtro.provinciaId()) || tieneTexto(filtro.localidadId())) {
            predicates.add(conUbicacion(cb, proyecto,
                    filtro.provinciaId(), filtro.localidadId()));
        }

        if (tieneTexto(filtro.texto())) {
            String patron = LikePatrones.contiene(filtro.texto());
            predicates.add(cb.or(
                    cb.like(cb.lower(proyecto.get("nombre")), patron,
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
            predicates.add(sinMembresiaActiva(query, cb, proyecto, idPerfilActivo));
        }

        if (Boolean.TRUE.equals(filtro.excluirYaPostulados()) && idPerfilActivo != null) {
            predicates.add(sinPostulacion(query, cb, root, idPerfilActivo));
        }
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    /**
     * Ubicación del proyecto de la oportunidad. Los ids son los identificadores
     * externos del catálogo Georef expuestos por la API de ubicaciones.
     */
    private static Predicate conUbicacion(
            CriteriaBuilder cb,
            Path<Proyecto> proyecto,
            String provinciaId,
            String localidadId) {

        Path<Ciudad> ciudad = proyecto.get("ubicacion").get("ciudad");
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

    private static Predicate conCupoDisponible(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoGralProyecto> root) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<MiembroProyecto> miembro = subquery.from(MiembroProyecto.class);
        subquery.select(cb.count(miembro));
        subquery.where(
                cb.equal(miembro.get("requerimientoGral"), root),
                cb.equal(miembro.get("estadoParticipacion"), EstadoParticipacion.ACTIVO));

        return cb.greaterThan(root.<Integer>get("cantidad").as(Long.class), subquery);
    }

    private static Predicate conCaracteristica(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoGralProyecto> root,
            Long idCaracteristica,
            Long idValorCaracteristica) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<RequerimientoGralCaract> caracteristica =
                subquery.from(RequerimientoGralCaract.class);
        subquery.select(caracteristica.get("idReqGralCaract"));

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(caracteristica.get("requerimientoGral"), root));

        if (idCaracteristica != null) {
            predicates.add(cb.equal(
                    caracteristica.get("caracteristica").get("idCaracteristica"),
                    idCaracteristica));
        }

        if (idValorCaracteristica != null) {
            Subquery<Long> valorSubquery = query.subquery(Long.class);
            Root<RequerimientoGralCaractValor> valor =
                    valorSubquery.from(RequerimientoGralCaractValor.class);
            valorSubquery.select(valor.get("idReqGralCaract"));
            valorSubquery.where(
                    cb.equal(valor.get("idReqGralCaract"),
                            caracteristica.get("idReqGralCaract")),
                    cb.equal(valor.get("idValor"), idValorCaracteristica));
            predicates.add(cb.exists(valorSubquery));
        }

        subquery.where(predicates.toArray(new Predicate[0]));
        return cb.exists(subquery);
    }

    private static Predicate conHabilidad(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoGralProyecto> root,
            List<Long> idsHabilidades) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<RequerimientoGralProyecto> correlacionado = subquery.correlate(root);
        Join<RequerimientoGralProyecto, Habilidad> habilidad =
                correlacionado.join("habilidades");
        subquery.select(habilidad.get("idHabilidad"));
        subquery.where(habilidad.get("idHabilidad").in(idsHabilidades));
        return cb.exists(subquery);
    }

    private static Predicate sinMembresiaActiva(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Path<Proyecto> proyecto,
            Long idPerfilActivo) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<MiembroProyecto> miembro = subquery.from(MiembroProyecto.class);
        subquery.select(miembro.get("idMiembro"));
        subquery.where(
                cb.equal(miembro.get("proyecto"), proyecto),
                cb.equal(miembro.get("perfil").get("idPerfil"), idPerfilActivo),
                cb.equal(miembro.get("estadoParticipacion"), EstadoParticipacion.ACTIVO));
        return cb.not(cb.exists(subquery));
    }

    private static Predicate sinPostulacion(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoGralProyecto> root,
            Long idPerfilActivo) {

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<PostulacionGral> postulacion = subquery.from(PostulacionGral.class);
        subquery.select(postulacion.get("idPostulacionGral"));
        subquery.where(
                cb.equal(postulacion.get("requerimientoGral"), root),
                cb.equal(postulacion.get("perfil").get("idPerfil"), idPerfilActivo));
        return cb.not(cb.exists(subquery));
    }

    private static void ordenar(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<RequerimientoGralProyecto> root,
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
        orden.add(cb.asc(root.get("proyecto").get("fechaInicio")));
        orden.add(cb.asc(root.get("idRequerimientoGral")));
        query.orderBy(orden);
    }
}
