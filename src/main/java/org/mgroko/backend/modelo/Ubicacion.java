package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ubicacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ubicacion")
    private Long idUbicacion;

    @Column(name = "direccion", length = 100)
    private String direccion;

    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Column(name = "latitud", precision = 10, scale = 8)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 11, scale = 8)
    private BigDecimal longitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ciudad", nullable = false)
    private Ciudad ciudad;

    // Métodos helper para compatibilidad con código existente (DTOs, Mappers, etc.)
    public String getLocalidad() {
        return ciudad != null ? ciudad.getNombre() : null;
    }

    public String getProvincia() {
        return (ciudad != null && ciudad.getProvincia() != null) ? ciudad.getProvincia().getNombre() : null;
    }

    public String getPais() {
        return (ciudad != null && ciudad.getProvincia() != null && ciudad.getProvincia().getPais() != null)
                ? ciudad.getProvincia().getPais().getNombre()
                : null;
    }

    public String getIdGeoref() {
        return ciudad != null ? ciudad.getIdExterno() : null;
    }

    public static UbicacionBuilder builder() {
        return new UbicacionBuilder();
    }

    public static class UbicacionBuilder {
        private Long idUbicacion;
        private String direccion;
        private String codigoPostal;
        private BigDecimal latitud;
        private BigDecimal longitud;
        private Ciudad ciudad;
        private String localidadTemp;
        private String provinciaTemp;
        private String paisTemp;
        private String idGeorefTemp;

        public UbicacionBuilder idUbicacion(Long idUbicacion) {
            this.idUbicacion = idUbicacion;
            return this;
        }

        public UbicacionBuilder direccion(String direccion) {
            this.direccion = direccion;
            return this;
        }

        public UbicacionBuilder codigoPostal(String codigoPostal) {
            this.codigoPostal = codigoPostal;
            return this;
        }

        public UbicacionBuilder latitud(BigDecimal latitud) {
            this.latitud = latitud;
            return this;
        }

        public UbicacionBuilder longitud(BigDecimal longitud) {
            this.longitud = longitud;
            return this;
        }

        public UbicacionBuilder ciudad(Ciudad ciudad) {
            this.ciudad = ciudad;
            return this;
        }

        public UbicacionBuilder localidad(String localidad) {
            this.localidadTemp = localidad;
            return this;
        }

        public UbicacionBuilder provincia(String provincia) {
            this.provinciaTemp = provincia;
            return this;
        }

        public UbicacionBuilder pais(String pais) {
            this.paisTemp = pais;
            return this;
        }

        public UbicacionBuilder idGeoref(String idGeoref) {
            this.idGeorefTemp = idGeoref;
            return this;
        }

        public Ubicacion build() {
            Ciudad c = this.ciudad;
            if (c == null && (this.localidadTemp != null || this.provinciaTemp != null || this.idGeorefTemp != null)) {
                Pais p = Pais.builder().nombre(this.paisTemp != null ? this.paisTemp : "Argentina").codigoIso("AR").build();
                Provincia prov = Provincia.builder().nombre(this.provinciaTemp != null ? this.provinciaTemp : "Provincia").pais(p).build();
                c = Ciudad.builder()
                        .nombre(this.localidadTemp != null ? this.localidadTemp : "Localidad")
                        .idExterno(this.idGeorefTemp)
                        .latitudDefecto(this.latitud)
                        .longitudDefecto(this.longitud)
                        .provincia(prov)
                        .build();
            }

            return new Ubicacion(
                    this.idUbicacion,
                    this.direccion,
                    this.codigoPostal,
                    this.latitud,
                    this.longitud,
                    c
            );
        }
    }
}
