# Auditoría del Módulo de Características Técnicas

**Fecha original:** 2026-09-28  
**Fecha revisión:** 2026-09-28  
**Proyecto:** modalink-backend  
**Alcance:** Migración de arquitectura de 3 tablas a 4 tablas normalizadas  
**Revisión 3:** Se agrega hallazgo H-20 (PK de `caracteristica_perfil` incorrectamente mapeada como `@EmbeddedId`)  
**Fuente de verdad:** `docs/3_diseño/ModaLinkBD.sql`

> [!NOTE]
> Esta auditoría fue revisada y corregida. Los hallazgos H-01, H-07 y H-10 de la versión
> original eran **falsos positivos** y se marcaron como tales con evidencia.
> Se agregaron los hallazgos H-13 a H-19 (omisiones de la auditoría original).
> **Revisión 3:** Se agrega H-20 — la entidad `CaracteristicaPerfil` usa `@EmbeddedId` cuando la BD
> define `id_caracteristica_perfil` como PK simple IDENTITY.

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
| `unidad_medida` | **NUEVA**: id_unidad PK, nombre, simbolo (UNIQUE), tipo_dato_permitido |
| `caracteristica_perfil` | id_caracteristica_perfil PK, valor, fecha_registro, id_perfil FK, id_valor FK, id_caracteristica FK |
| `valor_caracteristica` | **PK compuesta**: (id_valor, id_caracteristica), etiqueta, color_hex |

### Regla de negocio condicional
- Si `tipo_dato = ENUMERADO`: en `caracteristica_perfil`, `id_valor` NOT NULL y `valor` NULL
- Si `tipo_dato = TEXTO|NUMERICO`: en `caracteristica_perfil`, `valor` NOT NULL y `id_valor` NULL

### Triggers de BD implementados
1. `trg_caracteristica_tecnica_de_profesion`: valida que la característica pertenezca a la profesión del perfil
2. `trg_caracteristica_perfil_valor`: valida XOR valor/id_valor según tipo_dato; además valida que `id_valor` pertenezca a la misma `id_caracteristica`
3. `trg_caracteristica_valor_no_negativo`: valida que valores numéricos no sean negativos

---

## 2. Tabla de Hallazgos

### Hallazgos vigentes

| ID | Severidad | Tipo | Descripción | Ubicación | Impacto |
|----|-----------|------|-------------|-----------|---------|
| H-02 | **CRÍTICO** | Entidad desadaptada | `ValorCaracteristica` tiene `@Id` simple en `idValor`, pero la BD define PK compuesta `(id_valor, id_caracteristica)`. | `modelo/ValorCaracteristica.java:11-14` | Operaciones de persistencia y consulta pueden fallar. No se garantiza integridad de la PK compuesta. |
| H-03 | **CRÍTICO** | FK compuesta no mapeada | `CaracteristicaPerfil` mapea `valorCaracteristica` con `@JoinColumn` simple, pero la BD define FK compuesta `(id_valor, id_caracteristica)`. | `modelo/CaracteristicaPerfil.java:40-42` | Inserciones/actualizaciones pueden violar constraint de FK compuesta. |
| H-20 | **CRÍTICO** | PK de entidad no corresponde a la BD | `CaracteristicaPerfil` usa `@EmbeddedId CaracteristicaPerfilId(idPerfil, idCaracteristica)`, pero la BD define `id_caracteristica_perfil` (IDENTITY) como PK simple. La columna PK real no está mapeada en la entidad. `(id_perfil, id_caracteristica)` es solo un UNIQUE INDEX, no la PK. | `modelo/CaracteristicaPerfil.java:24-25`, `modelo/CaracteristicaPerfilId.java` | La entidad no mapea la columna PK real de la tabla. JPA opera sobre una PK ficticia. El `CaracteristicaPerfilRepository` usa tipo de ID incorrecto (`CaracteristicaPerfilId` en vez de `Long`). Impacta la solución de H-03 (la FK compuesta). |
| H-04 | **ALTO** | DTO desadaptado | `AdminCaracteristicaTecnicaRequest` usa `String unidad` en vez de `Long idUnidad`. No referencia la tabla `unidad_medida` por ID. | `admin/dto/AdminCaracteristicaTecnicaRequest.java:12` | Se pueden asignar unidades inexistentes o inválidas a las características técnicas. |
| H-05 | **ALTO** | Validación faltante | No se valida que `tipo_dato_permitido` de la unidad coincida con `tipo_dato` de la característica. | `admin/servicio/AdminCaracteristicaTecnicaService.java:64-95` | Se pueden asignar unidades incompatibles (ej: unidad "kg" a una característica de tipo TEXTO). |
| H-06 | **ALTO** | Mecanismo de asignación roto | El servicio recibe `String unidad` → llama a `setUnidad(String)` → crea `UnidadMedida` transient sin id → JPA intentará INSERT en vez de referenciar entidad existente. | `admin/servicio/AdminCaracteristicaTecnicaService.java:82,126` | El flujo completo de asignación de unidad está roto: no busca por ID, no valida existencia, crea entidades transient. |
| H-08 | **MEDIO** | Persistencia problemática | `setUnidad(String)` en `CaracteristicaTecnica` crea un `UnidadMedida` nuevo con solo el símbolo, sin `idUnidad`. Al guardar, JPA intentará insertar una nueva unidad. | `modelo/CaracteristicaTecnica.java:48-58` | Errores de persistencia o registros duplicados en `unidad_medida`. |
| H-09 | **MEDIO** | Builder problemático | El builder de `CaracteristicaTecnica` tiene un campo `unidadTemp` que crea un `UnidadMedida` sin id, mismo mecanismo problemático de H-08. | `modelo/CaracteristicaTecnica.java:60-83` | Mismo impacto que H-08. |
| H-11 | **BAJO** | DTO sin campo nombre | `CaracteristicaTecnicaResponse` no incluye el campo `nombre` de la característica técnica, que sí existe en la BD. | `perfiles/dto/CaracteristicaTecnicaResponse.java` | La información del nombre no se expone a los clientes de la API. |
| H-12 | **BAJO** | DTO sin endpoint | `UnidadMedida` no tiene DTO de respuesta para exponer su información ni endpoint para listar unidades. | `perfiles/dto/` (ausente) | No se puede listar las unidades de medida disponibles desde la API. |
| H-13 | **CRÍTICO** | Repositorio con tipo ID incorrecto | `ValorCaracteristicaRepository` declara `JpaRepository<ValorCaracteristica, Long>`. Con la PK compuesta debe ser `JpaRepository<ValorCaracteristica, ValorCaracteristicaId>`. | `repositorio/ValorCaracteristicaRepository.java:8` | No compilará tras corregir H-02. Todos los `findById(Long)` fallarán. |
| H-14 | **CRÍTICO** | Servicio admin usa `findById(Long)` para valores | `AdminCaracteristicaTecnicaService.actualizarValor()` y `eliminarValor()` usan `findById(idValor)` con Long, incompatible con PK compuesta. | `admin/servicio/AdminCaracteristicaTecnicaService.java:172,191` | No compilará tras corregir H-02. Necesita recibir `(idValor, idCaracteristica)`. |
| H-15 | **ALTO** | Query derivada rota en repositorio | `CaracteristicaPerfilRepository.existsByValorCaracteristicaIdValor(Long)` usa navegación de propiedad simple. Con `@EmbeddedId` la navegación cambia. | `repositorio/CaracteristicaPerfilRepository.java:11` | No resolverá la propiedad correctamente con PK compuesta. |
| H-16 | **ALTO** | Helper usa `findById(Long)` para valores | `CaracteristicaPerfilHelper` en L82 llama `valorCaracteristicaRepository.findById(car.idValor())` con Long simple. | `perfiles/servicio/CaracteristicaPerfilHelper.java:82` | No compilará tras corregir H-02. Necesita `ValorCaracteristicaId`. |
| H-17 | **MEDIO** | Mapper accede a getter que cambiará | `ValorCaracteristicaMapper.toResponse()` usa `valor.getIdValor()`. Con `@EmbeddedId` será `valor.getId().getIdValor()`. | `perfiles/mapper/ValorCaracteristicaMapper.java:12` | No compilará tras corregir H-02. |
| H-18 | **BAJO** | DTO request admin sin campo nombre | `AdminCaracteristicaTecnicaRequest` no incluye `String nombre`. Ni `crear()` ni `actualizar()` lo setean, pero la BD tiene `nombre varchar(100)`. | `admin/dto/AdminCaracteristicaTecnicaRequest.java` | No se puede asignar nombre para display a las características desde la API admin. |
| H-19 | **BAJO** | Mapper no mapea nombre ni unidad completa | `CaracteristicaTecnicaMapper.toResponse()` devuelve solo el símbolo como `getUnidad()` (String). No expone id de unidad, nombre de unidad, ni nombre de la CT. | `perfiles/mapper/CaracteristicaTecnicaMapper.java:28-30` | Información incompleta en la API. |

### Hallazgos descartados (falsos positivos de la auditoría original)

| ID original | Descripción original | Motivo de descarte | Evidencia |
|-------------|---------------------|--------------------|-----------|
| ~~H-01~~ | "No existe repositorio `UnidadMedidaRepository`" | **El repositorio SÍ existe.** Tiene métodos `findBySimbolo` y `findByNombre`. | `repositorio/UnidadMedidaRepository.java` — 13 líneas, `JpaRepository<UnidadMedida, Long>` |
| ~~H-07~~ | "Test busca unidad 'color' que no existe en seeds" | **La explicación es incorrecta.** El test filtra por `unidadMedida.simbolo LIKE '%color%'`. Las CT enumeradas (COLOR_OJOS, etc.) hacen `LEFT JOIN unidad_medida ON simbolo = 'color'` en el seed, pero como no existe unidad con símbolo `'color'`, `id_unidad` queda NULL → el test devuelve 0, no 3. El test fallará pero no por la razón que la auditoría indica. **La raíz del problema está en el seed, no en el test.** |
| ~~H-10~~ | "`CaracteristicaPerfilHelper` no valida profesión" | **La validación SÍ está implementada.** Líneas 64-68 validan `ct.getProfesion().getIdProfesion().equals(profesion.getIdProfesion())` y lanzan `CaracteristicaProfesionNoCoincideException`. | `perfiles/servicio/CaracteristicaPerfilHelper.java:64-68` |

---

## 3. Detalle de Hallazgos Críticos

### H-02: PK compuesta en ValorCaracteristica

**Esquema BD:**
```sql
CREATE TABLE valor_caracteristica(
    id_valor             int8  GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    id_caracteristica    int8  NOT NULL,
    etiqueta             varchar(255),
    color_hex            varchar(7)
                         CHECK (color_hex IS NULL OR color_hex ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT "PK_valor_caracteristica" PRIMARY KEY (id_valor, id_caracteristica)
);
```

**Entidad actual (incorrecta):**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id_valor")
private Long idValor;  // ← PK simple, debería ser compuesta
```

**Solución requerida:**
```java
@Embeddable
public class ValorCaracteristicaId implements Serializable {
    @Column(name = "id_valor")
    private Long idValor;
    @Column(name = "id_caracteristica")
    private Long idCaracteristica;
    // equals + hashCode
}

@Entity
public class ValorCaracteristica {
    @EmbeddedId
    private ValorCaracteristicaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idCaracteristica")
    @JoinColumn(name = "id_caracteristica")
    private CaracteristicaTecnica caracteristicaTecnica;
    // ...
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

**Entidad actual (incorrecta):**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_valor")
private ValorCaracteristica valorCaracteristica;  // ← FK simple
```

**Solución requerida:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumns({
    @JoinColumn(name = "id_valor", referencedColumnName = "id_valor"),
    @JoinColumn(name = "id_caracteristica", referencedColumnName = "id_caracteristica",
                insertable = false, updatable = false)
})
private ValorCaracteristica valorCaracteristica;
```

> Nota: `id_caracteristica` ya está mapeado por `@MapsId("idCaracteristica")` del `@EmbeddedId`,
> por lo que se requiere `insertable = false, updatable = false` para evitar mapeo duplicado.

---

### H-13: Repositorio con tipo de ID incorrecto

**Estado actual:**
```java
public interface ValorCaracteristicaRepository extends JpaRepository<ValorCaracteristica, Long> {
```

**Corrección:**
```java
public interface ValorCaracteristicaRepository extends JpaRepository<ValorCaracteristica, ValorCaracteristicaId> {
```

---

### H-14: Servicio admin usa findById(Long) para valores

**Estado actual (L172):**
```java
ValorCaracteristica valor = valorCaracteristicaRepository.findById(idValor)  // Long
```

**Corrección:** los endpoints de `actualizarValor` y `eliminarValor` deben recibir
`(Long idCaracteristica, Long idValor)` y construir un `ValorCaracteristicaId` para la búsqueda.

---

## 4. Sección de Verificación

### H-02: PK compuesta no mapeada
```sql
-- Verificar la PK en la BD
SELECT constraint_name, column_name
FROM information_schema.key_column_usage
WHERE table_name = 'valor_caracteristica';
-- Resultado: PK compuesta (id_valor, id_caracteristica)
```

### H-03: FK compuesta no mapeada
```sql
-- Verificar la FK en la BD
SELECT * FROM information_schema.table_constraints
WHERE table_name = 'caracteristica_perfil'
AND constraint_type = 'FOREIGN KEY';
-- Resultado: FK compuesta (id_valor, id_caracteristica)
```

### H-04/H-05/H-06: Validaciones y mecanismo roto
```java
// Intentar crear una característica con unidad como string
POST /admin/caracteristicas-tecnicas
{
  "codigo": "TEST",
  "unidad": "INVALIDA",  // String, no Long; no valida existencia ni compatibilidad
  "idProfesion": 1,
  "tipoDato": "NUMERICO"
}
// El servicio crea UnidadMedida transient → error o duplicado
```

### Falsos positivos descartados
```bash
# H-01 (DESCARTADO): El repositorio SÍ existe
$ grep -rl "UnidadMedidaRepository" src/main/java/
src/main/java/org/mgroko/backend/repositorio/UnidadMedidaRepository.java

# H-10 (DESCARTADO): La validación de profesión SÍ existe
$ grep -n "CaracteristicaProfesionNoCoincideException" src/main/java/org/mgroko/backend/perfiles/servicio/CaracteristicaPerfilHelper.java
66:                throw new CaracteristicaProfesionNoCoincideException(
```

---

## 5. Recomendaciones de Corrección

### Prioridad Inmediata (Críticos — H-02, H-03, H-13, H-14, H-20)
1. Crear `ValorCaracteristicaId` con `@Embeddable`
2. Corregir `ValorCaracteristica` → `@EmbeddedId`
3. Corregir `CaracteristicaPerfil` → PK simple `id_caracteristica_perfil` con `@Id @GeneratedValue(IDENTITY)`, eliminar `@EmbeddedId`, conservar `(id_perfil, id_caracteristica)` como `@UniqueConstraint`, y mapear FK compuesta a `valor_caracteristica` con `@JoinColumns`
4. Eliminar `CaracteristicaPerfilId.java` (ya no se necesita `@EmbeddedId` para esta entidad)
5. Actualizar `CaracteristicaPerfilRepository` → tipo de ID cambia de `CaracteristicaPerfilId` a `Long`
6. Actualizar `ValorCaracteristicaRepository` → tipo de ID correcto
7. Actualizar `AdminCaracteristicaTecnicaService` → buscar por PK compuesta

### Prioridad Alta (H-04, H-05, H-06, H-15, H-16)
6. Cambiar `AdminCaracteristicaTecnicaRequest.unidad` de `String` a `Long idUnidad`
7. Inyectar `UnidadMedidaRepository` en el servicio admin y validar existencia + compatibilidad
8. Corregir `CaracteristicaPerfilRepository` → query derivada con navegación de `@EmbeddedId`
9. Corregir `CaracteristicaPerfilHelper` → usar PK compuesta para buscar valores

### Prioridad Media (H-08, H-09, H-17)
10. Eliminar `setUnidad(String)` y builder `unidadTemp` de `CaracteristicaTecnica`
11. Corregir `ValorCaracteristicaMapper` → acceso vía `@EmbeddedId`

### Prioridad Baja (H-11, H-12, H-18, H-19)
12. Agregar campo `nombre` a `CaracteristicaTecnicaResponse` y al request admin
13. Crear `UnidadMedidaResponse` y endpoint para listar unidades
14. Enriquecer mapper para exponer datos completos de unidad

---

## 6. Archivos Afectados

### Entidades
- `src/main/java/org/mgroko/backend/modelo/ValorCaracteristica.java` — PK compuesta
- `src/main/java/org/mgroko/backend/modelo/CaracteristicaPerfil.java` — PK simple IDENTITY + FK compuesta + UniqueConstraint
- `src/main/java/org/mgroko/backend/modelo/CaracteristicaTecnica.java` — eliminar setUnidad/builder legacy
- **(CREAR)** `src/main/java/org/mgroko/backend/modelo/ValorCaracteristicaId.java` — @Embeddable
- **(ELIMINAR)** `src/main/java/org/mgroko/backend/modelo/CaracteristicaPerfilId.java` — ya no se necesita @EmbeddedId para esta entidad

### Repositorios
- `src/main/java/org/mgroko/backend/repositorio/ValorCaracteristicaRepository.java` — tipo ID + queries
- `src/main/java/org/mgroko/backend/repositorio/CaracteristicaPerfilRepository.java` — query derivada

### Servicios
- `src/main/java/org/mgroko/backend/admin/servicio/AdminCaracteristicaTecnicaService.java` — flujo completo
- `src/main/java/org/mgroko/backend/perfiles/servicio/CaracteristicaPerfilHelper.java` — findById

### DTOs
- `src/main/java/org/mgroko/backend/admin/dto/AdminCaracteristicaTecnicaRequest.java` — unidad + nombre
- `src/main/java/org/mgroko/backend/perfiles/dto/CaracteristicaTecnicaResponse.java` — nombre

### Mappers
- `src/main/java/org/mgroko/backend/perfiles/mapper/ValorCaracteristicaMapper.java` — getter de id
- `src/main/java/org/mgroko/backend/perfiles/mapper/CaracteristicaTecnicaMapper.java` — nombre + unidad

### Tests
- `src/test/java/org/mgroko/backend/repositorio/CaracteristicaTecnicaRepositoryIntegrationTest.java`
- `src/test/java/org/mgroko/backend/admin/servicio/AdminCaracteristicaTecnicaServiceTest.java`

---

**Fin de la auditoría (revisión 3)**
