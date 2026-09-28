# Auditoría de Gestión de Ubicaciones

## Contexto
La plataforma ha migrado de una tabla única `ubicacion` a un modelo normalizado con tablas `pais`, `provincia`, `ciudad` y `ubicacion`. Se requiere compatibilidad con múltiples APIs (GEOREF, GEONAMES) sin nombres hardcodeados.

---

## Hallazgos Críticos

### 1. UbicacionService.java - País hardcodeado
**Archivo:** `src/main/java/.../ubicacion/servicio/UbicacionService.java:26-27,85-90`

- Las constantes `CODIGO_PAIS_DEFAULT = "AR"` y `NOMBRE_PAIS_DEFAULT = "Argentina"` están hardcodeadas.
- En el método `crear(LocalidadGeoref localidad)` (línea 85), se busca o crea un país usando siempre el código ISO "AR" (Argentina).
- Esto rompe la compatibilidad con otras APIs y países distintos a Argentina.

### 2. Ubicacion.java - Builder hardcodeado
**Archivo:** `src/main/java/.../modelo/Ubicacion.java:121`

- En el método `build()` del builder interno (línea 121), al crear una ciudad sin proporcionar los datos completos, se defaultea automáticamente a:
  ```java
  Pais p = Pais.builder().nombre(this.paisTemp != null ? this.paisTemp : "Argentina").codigoIso("AR").build();
  ```
- Esto obliga a siempre tener Argentina como país por defecto.

### 3. Flujo de creación de ubicación no usa la API completa
**Archivo:** `src/main/java/.../ubicacion/servicio/UbicacionService.java:78-81`

- El método `obtenerOCrear(String localidadId, String provinciaId)` llama a `catalogoGeoref.obtenerLocalidad(localidadId)` y luego busca por `localidad.nombre()` y `localidad.provincia().nombre()`.
- **Problema:** No extrae y usa el país de la localidad de Georef, siempre usa el default "Argentina".

### 4. DTO UbicacionResponse es compatible pero limitado
**Archivo:** `src/main/java/.../ubicacion/dto/UbicacionResponse.java`

- El DTO expone `pais` como campo String, lo cual es correcto para la vista, pero los datos provienen de la navegación `ciudad.getProvincia().getPais().getNombre()` en `Ubicacion.java:getPais()` (líneas 42-46).

### 5. Base de datos normalizada y consistente
**Archivo:** `docs/3_diseño/ModaLinkBD.sql`

Las tablas están correctamente estructuradas:

| Tabla | Columnas clave |
|-------|----------------|
| `pais` | `id_pais`, `codigo_iso` (NOT NULL), `nombre` (NOT NULL), `activo` |
| `provincia` | `id_provincia`, `nombre` (NOT NULL), `id_externo`, `fuente_api`, `activo`, `id_pais` (FK → pais) |
| `ciudad` | `id_ciudad`, `nombre` (NOT NULL), `id_externo`, `fuente_api`, `activo`, `latitud_defecto`, `longitud_defecto`, `id_provincia` (FK → provincia) |
| `ubicacion` | `id_ubicacion`, `latitud`, `longitud`, `id_ciudad` (FK → ciudad) |

- El flujo esperado es: `ubicacion → ciudad → provincia → pais`
- Las entidades Java `Pais.java`, `Provincia.java`, `Ciudad.java` mapean correctamente a este esquema.

### 6. GeorefCatalogoService usa archivos estáticos
**Archivo:** `src/main/java/.../ubicacion/servicio/GeorefCatalogoService.java`

- Carga provincias y localidades desde archivos JSON locales (`provincias.json`, `localidades.json`).
- **Oportunidad:** El servicio debe ser extensible para soportar GEONAMES u otras APIs en el futuro, sin modificar la firma de los métodos principales.

### 7. Controladores acoplados a Georef
**Archivos:** `UbicacionCatalogoController.java`, `UbicacionUsuarioController.java`

- Los controladores trabajan específicamente con el catálogo de Georef.
- **Requerimiento:** Deben ser lo suficientemente genéricos para aceptar cualquier proveedor de localizaciones.

---

## Recomendaciones

1. **Eliminar hardcodeo de país en UbicacionService:**
   - Que el país venga determinado por la API externa (GEOREF/GEONAMES) o sea un parámetro configurable.
   - Agregar un repositorio o servicio inyectable que permita resolver el país por nombre/código ISO de forma dinámica.

2. **Modificar Ubicacion.java builder:**
   - Remover la lógica de default "Argentina".
   - El builder debe requerir que se proporcione un objeto `Ciudad` completo o los datos del país deben venir desde el servicio caller.

3. **Actualizar UbicacionService.obtenerOCrear:**
   - Extraer el país del objeto `LocalidadGeoref` si está disponible, o recibirlo como parámetro.
   - Implementar una estrategia de "fallback" que no asuma siempre Argentina.

4. **Hacer el código genérico para múltiples APIs:**
   - Crear una interfaz/estrategia para "Catálogo de localizaciones" que acepte GEOREF, GEONAMES, etc.
   - Los DTOs y mappers deben trabajar con el modelo de dominio (Ciudad/Provincia/Pais) y no con detalles de la API externa.

5. **Actualizar GeorefCatalogoService:**
   - Inyectar la fuente API en los objetos del catálogo o añadirlos como campo.
   - Permitir que el mismo servicio pueda cargar datos de fuentes distintas en el futuro.

6. **Validar con `spring.jpa.hibernate.ddl-auto=validate`:**
   - Una vez corregidos los hardcodeos, esta configuración debería pasar sin errores dado que el modelo de entidades coincide con la BD normalizada.

---

## Resumen de archivos auditados

| Categoría | Archivos |
|-----------|----------|
| Entidades | `Ciudad.java`, `Pais.java`, `Provincia.java`, `Ubicacion.java` |
| Servicios | `UbicacionService.java`, `GeorefCatalogoService.java`, `UbicacionUsuarioService.java` |
| Controladores | `UbicacionCatalogoController.java`, `UbicacionUsuarioController.java` |
| Mappers/DTOs | `UbicacionMapper.java`, `UbicacionRequest.java`, `UbicacionResponse.java`, `ValidUbicacionRequest.java` |
| Excepciones | `ProvinciaSinLocalidadException.java`, `LocalidadNoEncontradaException.java` |
| SQL Schema | `docs/3_diseño/ModaLinkBD.sql` |