package org.mgroko.backend.admin.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.admin.dto.BuscarUsuariosAdminFiltro;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class UsuarioSpecifications {

    private UsuarioSpecifications() {}

    public static Specification<Usuario> conFiltros(BuscarUsuariosAdminFiltro filtro) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filtro == null) {
                return cb.conjunction();
            }

            // Filtrado por nombre de pila del usuario
            if (filtro.nombre() != null && !filtro.nombre().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")),
                        "%" + filtro.nombre().trim().toLowerCase() + "%"));
            }

            // Filtrado por apellido
            if (filtro.apellido() != null && !filtro.apellido().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("apellido")),
                        "%" + filtro.apellido().trim().toLowerCase() + "%"));
            }

            // Filtrado por correo electrónico
            if (filtro.correo() != null && !filtro.correo().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("correo")),
                        "%" + filtro.correo().trim().toLowerCase() + "%"));
            }

            // Filtrado por estado de usuario (Activo, Deshabilitado, PendienteBaja, Baja)
            if (filtro.estado() != null) {
                predicates.add(cb.equal(root.get("estado"), filtro.estado()));
            }

            // Filtrado por perfiles asociados (usando Subquery sobre Perfil para no requerir mappedBy en Usuario)
            boolean tieneFiltroPerfiles = filtro.idProfesion() != null
                    || (filtro.nombreProfesion() != null && !filtro.nombreProfesion().isBlank())
                    || (filtro.nombreArtisticoPerfil() != null && !filtro.nombreArtisticoPerfil().isBlank());

            if (tieneFiltroPerfiles) {
                var subquery = query.subquery(Long.class);
                var perfilRoot = subquery.from(Perfil.class);
                subquery.select(perfilRoot.get("usuario").get("idUsuario"));

                List<Predicate> subqueryPredicates = new ArrayList<>();
                subqueryPredicates.add(cb.equal(perfilRoot.get("usuario"), root));

                if (filtro.idProfesion() != null) {
                    subqueryPredicates.add(cb.equal(perfilRoot.get("profesion").get("idProfesion"), filtro.idProfesion()));
                }
                if (filtro.nombreProfesion() != null && !filtro.nombreProfesion().isBlank()) {
                    subqueryPredicates.add(cb.like(cb.lower(perfilRoot.get("profesion").get("nombre")),
                            "%" + filtro.nombreProfesion().trim().toLowerCase() + "%"));
                }
                if (filtro.nombreArtisticoPerfil() != null && !filtro.nombreArtisticoPerfil().isBlank()) {
                    subqueryPredicates.add(cb.like(cb.lower(perfilRoot.get("nombreArtistico")),
                            "%" + filtro.nombreArtisticoPerfil().trim().toLowerCase() + "%"));
                }

                subquery.where(subqueryPredicates.toArray(new Predicate[0]));
                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
