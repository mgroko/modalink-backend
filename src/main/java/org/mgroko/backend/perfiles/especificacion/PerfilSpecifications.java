package org.mgroko.backend.perfiles.especificacion;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.BuscarPerfilesFiltro;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class PerfilSpecifications {

    private PerfilSpecifications() {}

    public static Specification<Perfil> conFiltros(BuscarPerfilesFiltro filtro) {
        return (root, query, cb) -> {
            // Evitamos registros duplicados al hacer joins con colecciones
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            // Regla de negocio estricta: perfil Activo y usuario Activo
            predicates.add(cb.equal(root.get("estado"), EstadoPerfil.Activo));
            predicates.add(cb.equal(root.get("usuario").get("estado"), EstadoUsuario.Activo));

            if (filtro == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // Filtrado por nombre artístico (parcial, insensible a mayúsculas)
            if (filtro.nombreArtistico() != null && !filtro.nombreArtistico().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombreArtistico")),
                        "%" + filtro.nombreArtistico().trim().toLowerCase() + "%"));
            }

            // Filtrado por nombre de pila del usuario
            if (filtro.nombre() != null && !filtro.nombre().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("usuario").get("nombre")),
                        "%" + filtro.nombre().trim().toLowerCase() + "%"));
            }

            // Filtrado por apellido del usuario
            if (filtro.apellido() != null && !filtro.apellido().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("usuario").get("apellido")),
                        "%" + filtro.apellido().trim().toLowerCase() + "%"));
            }

            // Filtrado por profesión
            if (filtro.idProfesion() != null) {
                predicates.add(cb.equal(root.get("profesion").get("idProfesion"), filtro.idProfesion()));
            } else if (filtro.profesion() != null && !filtro.profesion().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("profesion").get("nombre")),
                        "%" + filtro.profesion().trim().toLowerCase() + "%"));
            }

            // Filtrado por género
            if (filtro.idGenero() != null) {
                predicates.add(cb.equal(root.get("usuario").get("genero").get("idGenero"), filtro.idGenero()));
            } else if (filtro.genero() != null && !filtro.genero().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("usuario").get("genero").get("codigo")),
                        filtro.genero().trim().toLowerCase()));
            }

            // Filtrado por ubicación
            if (filtro.idUbicacion() != null) {
                predicates.add(cb.equal(root.get("usuario").get("ubicacion").get("idUbicacion"), filtro.idUbicacion()));
            }
            if (filtro.localidad() != null && !filtro.localidad().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("usuario").get("ubicacion").get("localidad")),
                        "%" + filtro.localidad().trim().toLowerCase() + "%"));
            }
            if (filtro.provincia() != null && !filtro.provincia().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("usuario").get("ubicacion").get("provincia")),
                        "%" + filtro.provincia().trim().toLowerCase() + "%"));
            }

            // Filtrado por habilidades (contiene al menos una de las habilidades especificadas)
            if (filtro.idsHabilidades() != null && !filtro.idsHabilidades().isEmpty()) {
                Join<Perfil, Habilidad> joinHabilidades = root.join("habilidades", JoinType.INNER);
                predicates.add(joinHabilidades.get("idHabilidad").in(filtro.idsHabilidades()));
            }

            // Filtrado por características técnicas
            if (filtro.idCaracteristica() != null) {
                Join<Perfil, CaracteristicaPerfil> joinCaracteristicas = root.join("caracteristicas", JoinType.INNER);
                predicates.add(cb.equal(joinCaracteristicas.get("caracteristicaTecnica").get("idCaracteristica"),
                        filtro.idCaracteristica()));

                if (filtro.idValorCaracteristica() != null) {
                    predicates.add(cb.equal(joinCaracteristicas.get("valorCaracteristica").get("idValor"),
                            filtro.idValorCaracteristica()));
                } else if (filtro.valorCaracteristica() != null && !filtro.valorCaracteristica().isBlank()) {
                    predicates.add(cb.like(cb.lower(joinCaracteristicas.get("valor")),
                            "%" + filtro.valorCaracteristica().trim().toLowerCase() + "%"));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
