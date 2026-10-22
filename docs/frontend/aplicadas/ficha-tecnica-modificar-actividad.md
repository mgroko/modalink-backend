# Ficha Técnica Frontend: Modificar Actividad de Planificación (UC-50)

Especificación de la API, formulario, validaciones, semántica de campos opcionales, reglas de cronograma y manejo de errores para editar una actividad existente.

---

## 1. Endpoints y Contexto de Autenticación

### 1.1 Obtener detalle

- **Método:** `GET`
- **URL:** `/proyectos/{idProyecto}/actividades/{idActividad}`
- **Respuesta exitosa:** `200 OK` con `ActividadResponse`.
- **Permiso efectivo:** `MODIFICAR_ACTIVIDAD` en el proyecto.
- **Uso:** precargar el formulario de edición.

### 1.2 Modificar

- **Método:** `PUT`
- **URL:** `/proyectos/{idProyecto}/actividades/{idActividad}`
- **Content-Type:** `application/json`
- **Respuesta exitosa:** `200 OK` con `ActividadResponse`.
- **Permiso efectivo:** `MODIFICAR_ACTIVIDAD` en el proyecto.

### 1.3 Autenticación común

- JWT en cookie HTTP-only `jwt`.
- CSRF en header `X-XSRF-TOKEN`; la cookie `XSRF-TOKEN` no es HTTP-only.
- Todas las peticiones deben usar `credentials: "include"`.
- Perfil activo obligatorio en la sesión. El frontend no envía `idPerfilActivo`.
- El backend no usa el header `Authorization`.

### 1.4 Precondiciones del `PUT`

- Proyecto en estado `BORRADOR`, `PUBLICADO` o `CONFIRMADO`.
- Perfil activo, miembro activo y no dado de baja.
- Actividad existente y perteneciente a la planificación del proyecto.
- Actividad no `FINALIZADA` ni `CANCELADA`.

El `PUT` valida primero el permiso `MODIFICAR_ACTIVIDAD` y luego el estado del proyecto. Sin permiso responde `403` aunque el proyecto no admita modificaciones.

El `GET` de detalle valida perfil activo, membresía y permiso, pero no bloquea por estado del proyecto ni de la actividad. Esto permite mostrar el detalle aunque la edición no esté habilitada.

---

## 2. Flujo de Pantalla

### 2.1 Entrada

Desde el detalle o la planificación del proyecto, el Director selecciona **Editar actividad**.

Antes de habilitar el formulario, consultar `GET /proyectos/{idProyecto}` y comprobar:

- `permisos` contiene `MODIFICAR_ACTIVIDAD`.
- `estado` es `Borrador`, `Publicado` o `Confirmado`.
- El perfil activo es miembro activo del proyecto.

### 2.2 Precarga de datos

Usar:

`GET /proyectos/{idProyecto}/actividades/{idActividad}`

La respuesta incluye todos los datos necesarios para el formulario:

- Datos generales, fechas calculadas y estado.
- `ubicacion`.
- `predecesoras` con `idActividad` y `nombre`.
- `requerimientos` con características y habilidades.
- `profesionalesAsignados` con perfil y profesión.

No es necesario reconstruir datos desde la búsqueda de actividades.

### 2.3 Edición

1. Precargar el detalle completo.
2. El Director modifica los campos.
3. El frontend envía un único `PUT` con el estado completo del formulario.
4. Con `200`, actualizar el estado local con la respuesta y refrescar la planificación completa.
5. Las fechas de otras actividades pueden cambiar por el recálculo del cronograma.

---

## 3. Request Payload

```json
{
  "nombre": "Montaje final",
  "descripcion": "Montaje y prueba final del escenario.",
  "duracion": 2,
  "unidadDuracion": "HORAS",
  "ubicacion": {
    "localidadId": "06049010000",
    "provinciaId": "06"
  },
  "predecesoras": [201],
  "requerimientos": [
    {
      "idRequerimiento": 45,
      "cantidad": 2,
      "idProfesion": 2,
      "descripcion": "Modelos para la prueba",
      "caracteristicas": [
        { "idCaracteristica": 1, "valorMin": 165, "valorMax": 180 },
        { "idCaracteristica": 5, "valores": [1, 4] }
      ],
      "habilidades": [1, 4]
    },
    {
      "cantidad": 1,
      "idProfesion": 3,
      "descripcion": "Nuevo asistente",
      "caracteristicas": [],
      "habilidades": []
    }
  ],
  "estado": "EN_CURSO"
}
```

Reglas del payload:

- `nombre`, `duracion`, `unidadDuracion`, `estado`, `predecesoras` y `requerimientos` son obligatorios.
- `predecesoras` y `requerimientos` pueden ser `[]`, pero no `null` ni ausentes.
- No se envían `fechaHoraInicio` ni `fechaHoraFin`; las calcula el backend.
- `idRequerimiento` se envía solo para requerimientos existentes.
- Para crear un requerimiento nuevo, se omite `idRequerimiento`.
- Cada requerimiento debe incluir `caracteristicas` y `habilidades`; pueden ser `[]`.

---

## 4. Matriz de Campos y Validaciones

| Campo | Tipo / Formato | Obligatorio | Reglas / Restricciones Backend | Notas UI/UX |
| :--- | :--- | :---: | :--- | :--- |
| `nombre` | `string` | **Sí** | No blanco. Máx. 20 caracteres. Se aplica `trim`. Único en la planificación, sin distinguir mayúsculas. Excluye la propia actividad. | Contador `(x/20)`. Error `409` inline. |
| `descripcion` | `string` | No | Máx. 200 caracteres. Se aplica `trim`. `null` o ausente **borra** la descripción. | Siempre reenviar la descripción actual si no se quiere borrar. |
| `duracion` | `int` | **Sí** | Entero `> 0`. La conversión a minutos debe caber en un `int`. | Number input, mínimo `1`, sin decimales. |
| `unidadDuracion` | `enum` | **Sí** | `DIAS` o `HORAS` en mayúsculas. `1 DIA = 1440` min; `1 HORA = 60` min. | Select o segmented control. |
| `ubicacion` | `object \| null` | No | Si el objeto está presente, `localidadId` es obligatorio y no blanco. `null` o ausente **borra** la ubicación. | Si la ubicación no cambia, reenviar el objeto completo actual. |
| `ubicacion.localidadId` | `string` | Condicional | Debe existir en el catálogo Georef. | Select dependiente de provincia. |
| `ubicacion.provinciaId` | `string` | No | Si se envía, debe corresponder a la localidad. | Select de provincia. |
| `predecesoras` | `array<long>` | **Sí** | Lista no nula. IDs no nulos, sin duplicados, existentes, de la misma planificación y distintos de la propia actividad. `[]` borra todas. | Multi-select. |
| `requerimientos` | `array` | **Sí** | Lista no nula. Ver sección 6. `[]` elimina los requerimientos no protegidos. | Lista dinámica. |
| `requerimientos[].idRequerimiento` | `long` | No | Si se envía, debe pertenecer a la actividad. | Enviar solo para filas existentes. |
| `requerimientos[].cantidad` | `int` | **Sí** por ítem | `> 0`. | Number input. |
| `requerimientos[].idProfesion` | `long` | **Sí** por ítem | Debe existir. | Select de profesiones. |
| `requerimientos[].descripcion` | `string` | No | Máx. 200 caracteres. `null` borra la descripción al actualizar. | Textarea con contador. |
| `requerimientos[].caracteristicas` | `array` | **Sí** | Lista no nula; puede ser `[]`. Sin IDs duplicados. Cada característica debe pertenecer a la profesión del requerimiento. Al actualizar, reemplaza las características actuales. | Cargar características de la profesión elegida. |
| `requerimientos[].caracteristicas[].idCaracteristica` | `long` | **Sí** por ítem | Debe existir. | Select. |
| `...caracteristicas[].valorMin` / `valorMax` | `decimal` | Condicional | **NUMÉRICO:** ambos obligatorios, `>= 0` y `valorMin <= valorMax`. **ENUMERADO/TEXTO:** deben ser `null`. | Mostrar rango solo para `NUMERICO`. |
| `...caracteristicas[].valores` | `array<long>` | Condicional | **ENUMERADO:** al menos un valor, todos pertenecientes a la característica. **NUMÉRICO/TEXTO:** vacío o `null`. Los elementos no pueden ser `null`. | Checklist solo para `ENUMERADO`. |
| `requerimientos[].habilidades` | `array<long>` | **Sí** | Lista no nula; puede ser `[]`. Cada ID debe existir y no ser `null`. Al actualizar, reemplaza las habilidades actuales. | Multi-select. |
| `estado` | `enum` | **Sí** | `PENDIENTE`, `EN_CURSO`, `FINALIZADA`, `CANCELADA`. Matriz estricta de transiciones aplicada en Fase 5. | Select o acciones de estado. |

Validaciones adicionales:

- `duracion` demasiado grande produce `400 DuracionInvalidaException`.
- Una característica duplicada, rango invertido/negativo o valores incorrectos produce `400`.
- Un ID de predecesora inválido produce `400`.
- Listas obligatorias ausentes o `null` producen `400`.
- Un elemento `null` dentro de `predecesoras`, `requerimientos`, `caracteristicas`, `habilidades` o `valores` produce `400`.
- Un ciclo introducido por la nueva lista de predecesoras produce `409` antes de guardar, aunque involucre actividades no pendientes. El trigger SQL queda como red de seguridad.
- `estado` fuera del enum o JSON malformado produce `400`.

---

## 5. Semántica de `null` / Ausente vs. Lista Vacía

| Campo | Ausente o `null` | Lista vacía `[]` |
| :--- | :--- | :--- |
| `descripcion` | Borra la descripción | No aplica |
| `ubicacion` | Borra la ubicación | No aplica |
| `predecesoras` | **Inválido:** `400` | Borra todas |
| `requerimientos` | **Inválido:** `400` | Elimina los no protegidos |
| `requerimientos[].caracteristicas` | **Inválido:** `400` | Borra las características |
| `requerimientos[].habilidades` | **Inválido:** `400` | Borra las habilidades |

> El `PUT` es de reemplazo total. No existe actualización parcial: todos los campos obligatorios y ambas listas deben reenviarse en cada edición.

---

## 6. Reglas de Requerimientos

Cada requerimiento se identifica por `idRequerimiento`:

1. Si `idRequerimiento` está presente, debe existir dentro de la actividad.
2. No se permiten IDs duplicados en la solicitud.
3. Los requerimientos existentes que no se envían y no tienen postulaciones ni invitaciones se eliminan.
4. Un requerimiento con `idRequerimiento: null` se crea.
5. Un requerimiento existente enviado se actualiza.
6. Al actualizar, `caracteristicas` y `habilidades` se reemplazan por completo.
7. `caracteristicas` y `habilidades` son obligatorias; `[]` las deja vacías.

### Protección de requerimientos

**Estado actual (Fase 3):** un requerimiento con cualquier postulación o invitación asociada, sin importar su estado, no se puede modificar ni eliminar. Si se envía explícitamente o se omite del array, el backend responde `409 RequerimientoConVinculosException`.

Consecuencia para el frontend:

- Un requerimiento protegido no se puede actualizar ni eliminar mientras conserve vínculos.
- Como el `PUT` es de reemplazo total, si la actividad tiene requerimientos protegidos, cualquier guardado que los envíe u omita responde `409`.
- Mostrar los requerimientos protegidos en modo solo lectura y ofrecer resolver sus postulaciones o invitaciones antes de editar la actividad.

---

## 7. Reglas de Estado y Cronograma

### 7.1 Estado de la actividad

- `FINALIZADA` y `CANCELADA` no se pueden modificar.
- Estado actual: la edicion solo se permite desde `PENDIENTE` o `EN_CURSO`.
- Matriz estricta implementada en Fase 5:
  - Se permite conservar el mismo estado: `PENDIENTE -> PENDIENTE`, `EN_CURSO -> EN_CURSO`.
  - `PENDIENTE → EN_CURSO → FINALIZADA`.
  - `CANCELADA` desde `PENDIENTE` o `EN_CURSO`.
  - `FINALIZADA` y `CANCELADA` inmutables.
  - Transición inválida responde `409` con error específico.

| Estado actual | Estados destino permitidos |
|---|---|
| `PENDIENTE` | `PENDIENTE`, `EN_CURSO`, `CANCELADA` |
| `EN_CURSO` | `EN_CURSO`, `FINALIZADA`, `CANCELADA` |
| `FINALIZADA` | ninguno; la actividad no se modifica |
| `CANCELADA` | ninguno; la actividad no se modifica |

### 7.2 Recálculo

1. El cronograma se calcula con CPM en dos pasadas: una hacia adelante desde `proyecto.fechaInicio` y otra hacia atrás desde la fecha de entrega a las **23:59**.
2. La pasada hacia adelante programa solo actividades `PENDIENTE` en `max(proyecto.fechaInicio, fin temprano de sus predecesoras)`. Las actividades no pendientes conservan su `fechaHoraInicio`.
3. Si `proyecto.fechaInicio` es `null`, la pasada hacia adelante usa la fecha actual a las `00:00`.
4. La pasada hacia atrás calcula los inicios tardíos de todas las actividades.
5. Si el fin calculado de cualquier actividad supera la fecha de entrega a las **23:59**, el `PUT` responde `200` con `estadoCronograma: "ALERTA_DESBORDE"` y persiste la modificación. No se revierte. También se marca alerta si el inicio tardío calculado queda antes de `proyecto.fechaInicio`.
6. `estadoCronograma` no se persiste en una columna: se calcula en cada `GET` a partir de las fechas persistidas y también se devuelve en el `PUT`.
7. Si la planificación no tiene fecha de entrega, falla con `400`.
8. Un ciclo se detecta por DFS en memoria antes del guardado y falla con `409 CicloActividadException`. Si el trigger SQL detecta un ciclo concurrente, también responde `409`.
9. El cálculo usa minutos corridos; no contempla jornadas laborales, feriados, fines de semana ni disponibilidad de personal.

### 7.3 Notificaciones

- Se notifica a las asignaciones activas de la actividad.
- Se crea una notificación por usuario/perfil con tipo `ACTIVIDAD_MODIFICADA`.
- La notificación no forma parte de la respuesta HTTP.
- Implementado en Fase 5: se desacopla con `@TransactionalEventListener(AFTER_COMMIT)` y listener con `REQUIRES_NEW`; los fallos de notificación no revierten la edición.

---

## 8. Respuesta Exitosa (`200 OK`)

Aplica a `GET` de detalle y `PUT`. Ejemplo:

```json
{
  "idActividad": 301,
  "idProyecto": 105,
  "nombreProyecto": "Colección Primavera-Verano",
  "nombre": "Montaje final",
  "descripcion": "Montaje y prueba final del escenario.",
  "duracionMinutos": 120,
  "fechaHoraInicio": "2026-12-14T21:59",
  "fechaHoraFin": "2026-12-14T23:59",
  "estado": "EN_CURSO",
  "estadoCronograma": "NORMAL",
  "ubicacion": {
    "idUbicacion": 12,
    "direccion": null,
    "codigoPostal": null,
    "latitud": null,
    "longitud": null,
    "ciudad": {
      "idCiudad": 490,
      "idExterno": "06049010000",
      "fuenteApi": "GEOREF",
      "nombre": "Azul",
      "provincia": {
        "idProvincia": 6,
        "idExterno": "06",
        "fuenteApi": "GEOREF",
        "nombre": "Buenos Aires",
        "pais": {
          "idPais": 1,
          "codigoIso": "AR",
          "nombre": "Argentina"
        }
      }
    }
  },
  "predecesoras": [
    { "idActividad": 201, "nombre": "Confección de muestras" }
  ],
  "requerimientos": [
    {
      "idRequerimiento": 45,
      "cantidad": 2,
      "descripcion": "Modelos para la prueba",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "caracteristicas": [
        {
          "idCaracteristica": 1,
          "codigo": "ALTURA",
          "tipoDato": "NUMERICO",
          "valorMin": 165,
          "valorMax": 180,
          "valores": []
        }
      ],
      "habilidades": [1, 4]
    }
  ],
  "profesionalesAsignados": [
    {
      "idAsignacionActividad": 77,
      "idMiembroProyecto": 88,
      "idPerfil": 10,
      "idUsuario": 1,
      "nombreArtistico": "Luna",
      "idProfesion": 2,
      "nombreProfesion": "Modelo"
    }
  ]
}
```

Notas de respuesta:

- `estado` se devuelve en mayúsculas.
- `estadoCronograma` devuelve `NORMAL` o `ALERTA_DESBORDE`; no se persiste y se calcula a partir de las fechas de la planificación.
- `duracionMinutos` es el valor persistido.
- `fechaHoraInicio` y `fechaHoraFin` son fechas locales ISO-8601, sin zona horaria.
- `fechaHoraFin` se deriva de `fechaHoraInicio + duracionMinutos`.
- `predecesoras` y `profesionalesAsignados` se devuelven ordenados por nombre.
- `ubicacion` puede ser `null`.
- `requerimientos` y `profesionalesAsignados` pueden ser listas vacías.

---

## 9. Manejo de Errores

### Formato general

```json
{
  "message": "No se puede modificar una actividad en estado FINALIZADA.",
  "httpStatus": 409,
  "timestamp": 1760000000000
}
```

### Errores de validación (`400`)

```json
{
  "message": "Validación fallida",
  "errores": {
    "nombre": "El nombre no puede superar los 20 caracteres.",
    "requerimientos[0].cantidad": "La cantidad debe ser mayor que cero."
  },
  "httpStatus": 400,
  "timestamp": 1760000000000
}
```

### Tabla de errores

| HTTP | Excepción / Causa | Acción Frontend |
| :---: | :--- | :--- |
| **400** | `MethodArgumentNotValidException`: campos obligatorios, listas nulas, elementos nulos, largos máximos, `duracion <= 0`, `cantidad <= 0`, `localidadId` vacío. | Resaltar inputs con `errores[campo]`; mapear índices a las filas. |
| **400** | `DuracionInvalidaException`: duración/unidad ausente o duración demasiado grande. | Error en duración/unidad. |
| **400** | `PredecesoraInvalidaException`: IDs nulos, duplicados, inexistentes, de otra planificación o la propia actividad. | Limpiar selección y recargar actividades. |
| **400** | `RequerimientoInvalidoException`: requerimiento ajeno o duplicado, listas obligatorias ausentes, característica duplicada, rango inválido, valores incorrectos. | Mostrar `message` en la sección de requerimientos. |
| **400** | `FechaEntregaNoDefinidaException`: la planificación no tiene fecha de entrega. | Bloquear el guardado y ofrecer editar el proyecto. |
| **400** | `ProfesionNoEncontradaException`, `CaracteristicaNoEncontradaException`, `CaracteristicaProfesionNoCoincideException`, `ValorCaracteristicaNoEncontradoException`. | Recargar catálogos y corregir la selección. |
| **400** | `LocalidadNoEncontradaException`, `LocalidadSinProvinciaException`, `ProvinciaSinLocalidadException`. | Error en el selector de ubicación. |
| **400** | `DataIntegrityViolationException`: violación de integridad referencial. | Alerta genérica; no reintentar automáticamente. |
| **400** | JSON malformado o valor de enum inválido. | Validar en cliente y mostrar error de formulario. |
| **401** | Sesión ausente o inválida. | Redirigir a login. |
| **403** | `AccesoDenegadoProyectoException`: sin `MODIFICAR_ACTIVIDAD` o sin membresía activa. | Ocultar/deshabilitar la acción. |
| **404** | `PerfilActivoNoSeleccionadoException`. | Modal de selección de perfil. |
| **404** | `PerfilNoEncontradoException`. | Re-sincronizar sesión. |
| **404** | `ProyectoNoEncontradoException`: proyecto inexistente o sin planificación. | Volver al listado de proyectos. |
| **404** | `ActividadNoEncontradaException`: actividad inexistente o de otro proyecto. | Volver a la planificación y recargar. |
| **404** | `HabilidadNoEncontradaException`. | Recargar catálogo o limpiar selección. |
| **409** | `EstadoProyectoNoPermiteModificarActividadesException`: proyecto `FINALIZADO` o `CANCELADO`. | Cerrar el formulario y refrescar el proyecto. |
| **409** | `TransicionEstadoActividadInvalidaException`: transicion fuera de la matriz permitida. | Ofrecer solo los estados validos para el estado actual. |
| **409** | `ActividadNoModificableException`: actividad `FINALIZADA` o `CANCELADA`. | Cerrar el formulario y refrescar la actividad. |
| **409** | `NombreActividadDuplicadoException`. | Error inline en `nombre`. |
| **409** | `RequerimientoConVinculosException`: requerimiento con postulaciones o invitaciones asociadas. | Mostrar badge de solo lectura y ofrecer resolver los vínculos antes de editar. |
| **409** | `CicloActividadException`: ciclo de predecesoras detectado antes de guardar o por el trigger SQL. | Alerta bloqueante; quitar la predecesora que genera el ciclo. |
| **409** | `CronogramaInviableException`: ciclo detectado durante el recálculo. | Alerta bloqueante; quitar la predecesora que genera el ciclo. |
| **409** | `PerfilEnBajaException`: perfil activo dado de baja. | Re-sincronizar sesión o elegir otro perfil. |

> `PerfilActivoNoSeleccionadoException` responde `404`, no `401`. Es una convención existente del backend.

---

## 10. Consideraciones de UX

1. **Precarga completa:** usar el `GET` de detalle antes de habilitar la edición.
2. **Ubicación:** si no se modifica, reenviar siempre la ubicación actual. Omitirla la borra.
3. **Descripción:** si no se modifica, reenviar el valor actual. Omitirla la borra.
4. **Requerimientos:** reenviar completos los que se conservan. Omitir uno sin vínculos lo elimina.
5. **Requerimientos protegidos:** mostrar como solo lectura. Enviarlos u omitirlos responde `409`; resolver sus postulaciones o invitaciones antes de guardar la actividad.
6. **Estado:** mostrar el estado actual y permitir cambiarlo solo si la actividad no está finalizada/cancelada.
7. **Duración:** permitir alternar `DIAS`/`HORAS` sin perder el valor; mostrar equivalencia en minutos.
8. **Predecesoras:** multi-select sin duplicados; no permitir la propia actividad.
9. **Cronograma:** advertir que las fechas de otras actividades pueden cambiar tras guardar.
10. **Doble envío:** deshabilitar el botón mientras la petición está en curso. No hay clave de idempotencia.
11. **Post-guardado:** actualizar el estado local con la respuesta y refrescar la planificación completa.
12. **Accesibilidad:** asociar errores inline con su input y mover el foco al primer campo inválido.

---

## 11. Criterios de Aceptación

- El `GET` de detalle devuelve ubicación, predecesoras, requerimientos completos y profesionales asignados.
- El `PUT` con datos válidos devuelve `200` y la actividad actualizada.
- `predecesoras` o `requerimientos` ausentes/`null` devuelven `400`.
- Elementos `null` en listas devuelven `400`, nunca `500`.
- `predecesoras: []` borra las predecesoras.
- `requerimientos: []` elimina los requerimientos no protegidos.
- `caracteristicas: []` y `habilidades: []` vacían esas relaciones.
- El nombre duplicado en la planificación devuelve `409`.
- Una actividad `FINALIZADA` o `CANCELADA` devuelve `409`.
- Una transicion fuera de la matriz (`PENDIENTE -> FINALIZADA`, retrocesos o reapertura de terminales) devuelve `409`.
- La notificacion `ACTIVIDAD_MODIFICADA` se crea despues del commit, una vez por perfil asignado y sin duplicados.
- Un fallo al crear la notificacion se registra y no revierte la modificacion de la actividad.
- Un proyecto `FINALIZADO` o `CANCELADO` devuelve `409`.
- Sin `MODIFICAR_ACTIVIDAD`, el backend responde `403`.
- Sin perfil activo, el backend responde `404`.
- `duracion` y `unidadDuracion` se convierten correctamente a `duracionMinutos`.
- Un requerimiento existente actualizado reemplaza sus características y habilidades.
- `ubicacion: null` borra la ubicación.
- `descripcion: null` borra la descripción.
- Si el cronograma supera la fecha de entrega, devuelve `200` con `estadoCronograma: "ALERTA_DESBORDE"` y conserva la modificación.
- Un ciclo de predecesoras devuelve `409` y no queda una modificación parcial.
- La respuesta incluye fechas recalculadas, `estadoCronograma`, requerimientos actualizados y profesionales asignados.

---

## 12. Fuera de Alcance y Limitaciones

- No se asignan ni desasignan miembros desde este endpoint.
- No se modifican imágenes ni archivos.
- El CPM no contempla disponibilidad de personal, jornadas laborales, feriados ni fines de semana.
- No se implementa reserva/bloqueo de calendario.
- No hay endpoint público para listar habilidades (UC-48).
- `estadoCronograma` no se persiste; se recalcula en cada respuesta.
- La matriz de transiciones es estricta; los estados terminales no se reabren.
- Las notificaciones se emiten despues del commit y sus fallos no revierten la edicion.

---

## 13. Estado de Implementación

- **Fase 1 — Completada:** contrato `PUT` de reemplazo total, `GET` de detalle con profesionales asignados y validación `@NotNull` en listas y elementos.
- **Fase 2 — Completada:** ciclos a `409` con DFS antes de guardar, mapeo del trigger SQL y orden de validación permiso/estado.
- **Fase 3 — Completada:** bloqueo de requerimientos con cualquier postulación o invitación, tanto al enviarlos como al omitirlos, con `409 RequerimientoConVinculosException`.
- **Fase 4 — Completada:** CPM hacia adelante y hacia atrás, `estadoCronograma` derivado (`NORMAL`, `ALERTA_DESBORDE`) y modificación tolerante a desborde sin rollback.
- **Fase 5 — Completada:** matriz de estados y notificaciones desacopladas con `AFTER_COMMIT`.
- **Fase 6 — Pendiente:** cierre formal de la suite completa (937 tests, 0 fallos) e integración.
