package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.contratos.dto.BuscarContratosFiltro;
import org.mgroko.backend.contratos.dto.ProyectoFiltroContratoResponse;
import org.mgroko.backend.contratos.especificacion.ContratoSpecifications;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.repositorio.proyeccion.ContratoResumenProyeccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
public class ContratoBusquedaRepositoryImpl implements ContratoBusquedaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<ContratoResumenProyeccion> buscar(
            BuscarContratosFiltro filtro,
            Long idPerfilActivo,
            boolean miFirmaPendiente,
            Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<ContratoResumenProyeccion> query =
                cb.createQuery(ContratoResumenProyeccion.class);
        Root<Contrato> root = query.from(Contrato.class);
        Predicate predicate = ContratoSpecifications
                .conFiltros(filtro, idPerfilActivo, miFirmaPendiente)
                .toPredicate(root, query, cb);

        query.select(cb.construct(
                ContratoResumenProyeccion.class,
                root.get("idContrato"),
                root.get("nombre"),
                root.get("estado"),
                root.get("fechaCreacion"),
                root.get("fechaEmision"),
                root.get("fechaModificacion"),
                root.get("versionActual"),
                root.get("proyecto").get("idProyecto"),
                root.get("proyecto").get("nombre"),
                root.get("proyecto").get("estado"),
                root.get("tipoAcuerdo").get("idTipoAcuerdo"),
                root.get("tipoAcuerdo").get("codigo"),
                root.get("tipoAcuerdo").get("nombre"),
                root.get("miembroEmisor").get("idMiembro")));

        query.where(predicate);
        query.orderBy(
                cb.desc(root.get("fechaEmision")),
                cb.desc(root.get("idContrato")));

        TypedQuery<ContratoResumenProyeccion> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<ContratoResumenProyeccion> contenido = typedQuery.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Contrato> countRoot = countQuery.from(Contrato.class);
        Predicate countPredicate = ContratoSpecifications
                .conFiltros(filtro, idPerfilActivo, miFirmaPendiente)
                .toPredicate(countRoot, countQuery, cb);

        countQuery.select(cb.countDistinct(countRoot.get("idContrato")));
        countQuery.where(countPredicate);

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(contenido, pageable, total);
    }

    @Override
    public List<ProyectoFiltroContratoResponse> listarProyectosVisibles(
            Long idPerfilActivo) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<ProyectoFiltroContratoResponse> query =
                cb.createQuery(ProyectoFiltroContratoResponse.class);
        Root<Contrato> root = query.from(Contrato.class);

        query.select(cb.construct(
                ProyectoFiltroContratoResponse.class,
                root.get("proyecto").get("idProyecto"),
                root.get("proyecto").get("nombre")));
        query.where(ContratoSpecifications.conVisibilidad(
                query, cb, root, idPerfilActivo));
        query.distinct(true);
        query.orderBy(cb.asc(root.get("proyecto").get("nombre")));

        return entityManager.createQuery(query).getResultList();
    }
}
