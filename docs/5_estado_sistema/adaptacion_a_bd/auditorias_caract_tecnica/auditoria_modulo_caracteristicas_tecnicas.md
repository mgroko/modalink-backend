# Auditoría del Módulo de Características Técnicas

**Fecha:** 2026-09-28  
**Proyecto:** modalink-backend  
**Alcance:** Migración de arquitectura de 3 tablas a 4 tablas normalizadas

---

## 1. Resumen de la Migración

### Arquitectura ANTIGUA (3 tablas)
| Tabla | Columnas clave |
|-------|----------------|
| `caracteristica_tecnica` | id_caracteristica PK, codigo, **unidad varchar nullable**, tipo_dato, id_profesion FK |
| `caracteristica_perfil` | id_caracteristica_perfil PK, valor, fecha_registro, id_valor FK, id_caracteristica FK, id_perfil FK |
| `valor_caracteristica` | **id_valor PK simple**, codigo, color_hex, id_caracteristica FK |

### Arquitectura NUEVA (4 tablas)
| Tabla | Columnas clave |
|-------|----------------|
| `caracteristica_tecnica` | id_caracteristica PK, codigo, nombre, tipo_dato, id_profesion FK, **id_unidad FK nullable** |
| `unidad_medida` | **NUEVA**: id_unidad PK, nombre, simbolo, tipo_dato_permitido |
| `caracteristica_perfil` | id_caracteristica_perfil PK, valor, fecha_registro, id_perfil FK, id_valor FK, id_caracteristica FK |
| `valor_caracteristica` | **PK compuesta**: id_valor + id_caracteristica, etiqueta, color_hex |

### Regla de negocio condicional
- Si `tipo_dato = ENUMERADO`: en `caracteristica_perfil`, `id_valor` NOT NULL y `valor` NULL
- Si `tipo_dato = TEXTO|NUMERICO`: en `caracteristica_perfil`, `valor` NOT NULL y `id_valor` NULL

### Triggers de BD implementados
1. `trg_caracteristica_tecnica_de_profesion`: valida que la característica pertenezca a la profesión del perfil
2. `trg_caracteristica_perfil_valor`: valida XOR valor/id_valor según tipo_dato
3. `trg_caracteristica_valor_no_negativo`: valida que valores numéricos no sean negativos

---

## 2. Tabla de Hallazgos

| ID | Severidad | Tipo | Descripción | Ubicación | Impacto | Corrección propuesta |
|----|-----------|------|-------------|-----------|---------|----------------------|
| H-01 | **CRÍTICO** | Repositorio faltante | No existe repositorio para la entidad `UnidadMedida`. No se pueden consultar, crear, actualizar o eliminar unidades de medida desde la aplicación. | `src/main/java/org/mgroko/backend/repositorio/` (ausente) | La funcionalidad de administración de unidades de medida está completamente rota. No se pueden listar unidades válidas para asignar a características. | Crear `UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long>` con métodos CRUD y consultas necesarias. |
| H-02 | **CRÍTICO** | Entidad desadaptada | `ValorCaracteristica` tiene `@Id` simple en `idValor`, pero la BD define PK compuesta `(id_valor, id_caracteristica)`. El mapeo JPA no refleja la estructura real. | `src/main/java/org/mgroko/backend/modelo/ValorCaracteristica.java:12-14` | Las operaciones de persistencia y consulta pueden fallar o comportarse incorrectamente. No se puede garantizar la integridad de la PK compuesta. | Crear clase `ValorCaracteristicaId` con `@Embeddable` conteniendo `idValor` e `idCaracteristica`, y usar `@EmbeddedId` en `ValorCaracteristica`. |
| H-03 | **CRÍTICO** | FK compuesta no mapeada | `CaracteristicaPerfil` mapea `valorCaracteristica` como `@ManyToOne` simple, pero la BD define FK compuesta `(id_valor, id_caracteristica)` hacia `valor_caracteristica`. | `src/main/java/org/mgroko/backend/modelo/CaracteristicaPerfil.java:40-42` | Las inserciones/actualizaciones en `caracteristica_perfil` pueden fallar por constraint violation o no respetar la FK compuesta. | Mapear la FK compuesta usando `@JoinColumns` con `@JoinColumn(name = "id_valor", referencedColumnName = "id_valor")` y `@JoinColumn(name = "id_caracteristica", referencedColumnName = "id_caracteristica")`. |
| H-04 | **ALTO** | DTO desadaptado | `AdminCaracteristicaTecnicaRequest` usa `unidad` como `String` en vez de `idUnidad` como `Long`. No se valida que la unidad exista en la tabla `unidad_medida`. | `src/main/java/org/mgroko/backend/admin/dto/AdminCaracteristicaTecnicaRequest.java:12` | Se pueden asignar unidades inexistentes o inválidas a las características técnicas. No se respeta la FK `id_unidad → unidad_medida(id_unidad)`. | Cambiar `String unidad` por `Long idUnidad` y validar que exista en la tabla `unidad_medida` antes de guardar. |
| H-05 | **ALTO** | Validación faltante | No se valida que el `tipo_dato_permitido` de la unidad de medida coincida con el `tipo_dato` de la característica técnica. | `src/main/java/org/mgroko/backend/admin/servicio/AdminCaracteristicaTecnicaService.java:64-95` | Se pueden asignar unidades incompatibles (ej: unidad "kg" a una característica de tipo TEXTO). | Agregar validación en `crear()` y `actualizar()`: si `idUnidad` no es null, verificar que `unidadMedida.tipoDatoPermitido` coincida con `caracteristica.tipoDato`. |
| H-06 | **ALTO** | Validación faltante | No se valida que la unidad de medida exista antes de asignarla a una característica. | `src/main/java/org/mgroko/backend/admin/servicio/AdminCaracteristicaTecnicaService.java:80-86` | Se pueden insertar características con `id_unidad` inexistente, violando la FK. | Inyectar `UnidadMedidaRepository` y validar existencia antes de guardar. |
| H-07 | **MEDIO** | Test desadaptado | El test de integración busca características por unidad "color", pero en los seeds no existe ninguna unidad con símbolo "color". | `src/test/java/org/mgroko/backend/repositorio/CaracteristicaTecnicaRepositoryIntegrationTest.java:56-60` | El test fallará o dará resultados incorrectos. | Cambiar el filtro de unidad a "cm" (que sí existe en seeds) o agregar una unidad "color" a los seeds. |
| H-08 | **MEDIO** | Persistencia problemática | `setUnidad(String)` en `CaracteristicaTecnica` crea un `UnidadMedida` nuevo con solo el simbolo, sin `idUnidad`. Al guardar, JPA intentará insertar una nueva unidad en vez de referenciar una existente. | `src/main/java/org/mgroko/backend/modelo/CaracteristicaTecnica.java:48-58` | Puede causar errores de persistencia o crear registros duplicados en `unidad_medida`. | Cambiar la lógica para que `setUnidad` reciba un `Long idUnidad` y busque la unidad existente en el repositorio, o eliminar este método y usar directamente `setUnidadMedida(UnidadMedida)`. |
| H-09 | **MEDIO** | Builder problemático | El builder de `CaracteristicaTecnica` tiene un campo `unidadTemp` que crea un `UnidadMedida` sin id, similar al problema H-08. | `src/main/java/org/mgroko/backend/modelo/CaracteristicaTecnica.java:60-83` | Mismo impacto que H-08. | Rediseñar el builder para no usar `unidadTemp` y manejar la relación a `UnidadMedida` correctamente. |
| H-10 | **BAJO** | Validación faltante | `CaracteristicaPerfilHelper` no valida que la característica pertenezca a la profesión del perfil (aunque el trigger de BD lo hace). | `src/main/java/org/mgroko/backend/perfiles/servicio/CaracteristicaPerfilHelper.java:60-68` | Dependencia exclusiva del trigger de BD. Si el trigger se deshabilita o hay migraciones manuales, la validación se pierde. | Agregar validación explícita en Java para defender la regla de negocio en la capa de aplicación. |
| H-11 | **BAJO** | DTO sin campo nombre | `CaracteristicaTecnicaResponse` no incluye el campo `nombre` de la característica técnica, que sí existe en la BD. | `src/main/java/org/mgroko/backend/perfiles/dto/CaracteristicaTecnicaResponse.java:5-13` | La información del nombre no se expone a los clientes de la API. | Agregar `String nombre` al DTO y mapearlo en `CaracteristicaTecnicaMapper`. |
| H-12 | **BAJO** | DTO sin campo tipoDatoPermitido | `UnidadMedida` no tiene DTO de respuesta para exponer su información. | `src/main/java/org/mgroko/backend/perfiles/dto/` (ausente) | No se puede listar las unidades de medida disponibles desde la API. | Crear `UnidadMedidaResponse` y endpoint para listar unidades. |

---

## 3. Detalle de Hallazgos Críticos

### H-01: Repositorio UnidadMedida faltante

**Evidencia:**
```bash
$ grep -r "UnidadMedidaRepository" src/main/java/
# Sin resultados
```

**Impacto:** La tabla `unidad_medida` existe en la BD con 4 registros seed (cm, m, in, kg), pero no se puede acceder desde la aplicación.

**Solución:**
```java
package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {
    boolean existsBySimbolo(String simbolo);
    List<UnidadMedida> findByTipoDatoPermitido(String tipoDatoPermitido);
}
```

---

### H-02: PK compuesta en ValorCaracteristica

**Esquema BD:**
```sql
CREATE TABLE valor_caracteristica(
    id_valor             int8  NOT NULL,
    id_caracteristica    int8  NOT NULL,
    etiqueta             varchar(255),
    color_hex            varchar(7),
    CONSTRAINT "PK_valor_caracteristica" PRIMARY KEY (id_valor, id_caracteristica)
);
```

**Entidad actual:**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id_valor")
private Long idValor;  // Debería ser clave compuesta
```

**Solución:**
```java
@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ValorCaracteristicaId implements Serializable {
    private Long idValor;
    private Long idCaracteristica;
}

@Entity
@Table(name = "valor_caracteristica")
public class ValorCaracteristica {
    @EmbeddedId
    private ValorCaracteristicaId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idCaracteristica")
    @JoinColumn(name = "id_caracteristica", nullable = false)
    private CaracteristicaTecnica caracteristicaTecnica;
    
    @Column(name = "etiqueta", length = 255)
    private String etiqueta;
    
    @Column(name = "color_hex", length = 7)
    private String colorHex;
}
```

---

### H-03: FK compuesta en CaracteristicaPerfil

**Esquema BD:**
```sql
ALTER TABLE caracteristica_perfil ADD CONSTRAINT "Refvalor_caracteristica1641" 
    FOREIGN KEY (id_valor, id_caracteristica)
    REFERENCES valor_caracteristica(id_valor, id_caracteristica);
```

**Entidad actual:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_valor")
private ValorCaracteristica valorCaracteristica;  // Falta id_caracteristica
```

**Solución:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumns({
    @JoinColumn(name = "id_valor", referencedColumnName = "id_valor"),
    @JoinColumn(name = "id_caracteristica", referencedColumnName = "id_caracteristica")
})
private ValorCaracteristica valorCaracteristica;
```

---

## 4. Sección de Verificación

### Cómo reproducir los hallazgos

#### H-01: Repositorio faltante
```bash
# Buscar cualquier referencia a UnidadMedidaRepository
grep -r "UnidadMedidaRepository" src/main/java/
# Resultado: sin coincidencias
```

#### H-02: PK compuesta no mapeada
```sql
-- Verificar la PK en la BD
SELECT constraint_name, column_name 
FROM information_schema.key_column_usage 
WHERE table_name = 'valor_caracteristica';
-- Resultado: PK compuesta (id_valor, id_caracteristica)
```

#### H-03: FK compuesta no mapeada
```sql
-- Verificar la FK en la BD
SELECT * FROM information_schema.table_constraints 
WHERE table_name = 'caracteristica_perfil' 
AND constraint_type = 'FOREIGN KEY';
-- Resultado: FK compuesta (id_valor, id_caracteristica)
```

#### H-04/H-05/H-06: Validaciones faltantes
```java
// Intentar crear una característica con unidad inexistente
POST /admin/caracteristicas-tecnicas
{
  "codigo": "TEST",
  "unidad": "INVALIDA",  // No existe en unidad_medida
  "idProfesion": 1,
  "tipoDato": "NUMERICO"
}
// Resultado: debería fallar pero no lo hace
```

#### H-07: Test desadaptado
```bash
# Ejecutar el test de integración
mvn test -Dtest=CaracteristicaTecnicaRepositoryIntegrationTest#buscar_porUnidad_color_devuelveTres
# Resultado: falla porque no existe unidad "color" en seeds
```

---

## 5. Recomendaciones de Corrección

### Prioridad Inmediata (Críticos)
1. Crear `UnidadMedidaRepository`
2. Corregir mapeo de PK compuesta en `ValorCaracteristica`
3. Corregir mapeo de FK compuesta en `CaracteristicaPerfil`

### Prioridad Alta
4. Actualizar `AdminCaracteristicaTecnicaRequest` para usar `idUnidad`
5. Agregar validaciones de existencia y compatibilidad de unidades
6. Corregir test de integración

### Prioridad Media
7. Rediseñar `setUnidad` y builder de `CaracteristicaTecnica`
8. Agregar validación de profesión en `CaracteristicaPerfilHelper`

### Prioridad Baja
9. Agregar campo `nombre` a `CaracteristicaTecnicaResponse`
10. Crear DTO y endpoint para `UnidadMedida`

---

## 6. Archivos Afectados

### Entidades
- `src/main/java/org/mgroko/backend/modelo/ValorCaracteristica.java`
- `src/main/java/org/mgroko/backend/modelo/CaracteristicaPerfil.java`
- `src/main/java/org/mgroko/backend/modelo/CaracteristicaTecnica.java`

### Repositorios
- `src/main/java/org/mgroko/backend/repositorio/UnidadMedidaRepository.java` (crear)
- `src/main/java/org/mgroko/backend/repositorio/ValorCaracteristicaRepository.java`
- `src/main/java/org/mgroko/backend/repositorio/CaracteristicaPerfilRepository.java`

### Servicios
- `src/main/java/org/mgroko/backend/admin/servicio/AdminCaracteristicaTecnicaService.java`
- `src/main/java/org/mgroko/backend/perfiles/servicio/CaracteristicaPerfilHelper.java`

### DTOs
- `src/main/java/org/mgroko/backend/admin/dto/AdminCaracteristicaTecnicaRequest.java`
- `src/main/java/org/mgroko/backend/perfiles/dto/CaracteristicaTecnicaResponse.java`

### Tests
- `src/test/java/org/mgroko/backend/repositorio/CaracteristicaTecnicaRepositoryIntegrationTest.java`
- `src/test/java/org/mgroko/backend/admin/servicio/AdminCaracteristicaTecnicaServiceTest.java`

---

**Fin de la auditoría**
