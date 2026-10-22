# Ficha Técnica Frontend: Crear Actividad en Planificación (UC-49)

Especificación de la API, formulario, validaciones, reglas de cronograma y manejo de errores para crear una actividad dentro de la planificación de un proyecto.

---

## 1. Endpoint y Contexto de Autenticación

- **Método:** `POST`
- **URL:** `/proyectos/{idProyecto}/actividades`
- **Content-Type:** `application/json`
- **Respuesta exitosa:** `201 Created` con `ActividadResponse`.
- **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`).
- **Perfil activo:** obligatorio en la sesión. El frontend no envía `idPerfilActivo`.
- **Permiso efectivo:** `CREAR_ACTIVIDAD` en el proyecto. Hoy lo posee el rol **Director de proyecto**.
- **Precondiciones del proyecto:**
  - Estado `BORRADOR`, `PUBLICADO` o `CONFIRMADO`.
  - Debe existir su planificación y esta debe tener fecha de entrega.
  - El perfil activo debe ser miembro activo y no estar dado de baja.

> El backend busca la planificación del proyecto. Si el proyecto no existe o no tiene planificación, responde `404`.

---

## 2. Flujo de Pantalla

### 2.1 Entrada

Desde el dashboard o la planificación del proyecto, el Director selecciona **Agregar actividad**.

Antes de habilitar el formulario, usar `GET /proyectos/{idProyecto}` y comprobar:

- `permisos` contiene `CREAR_ACTIVIDAD`.
- `estado` es `Borrador`, `Publicado` o `Confirmado`.
- `fechaEntregaEstipulada` no es `null`.

Si falta la fecha de entrega, mostrar un aviso con acceso a la edición del proyecto. Si falta el permiso o el estado no lo permite, ocultar o deshabilitar la acción.

### 2.2 Carga inicial

Cargar en paralelo:

- Detalle del proyecto: `GET /proyectos/{idProyecto}`.
- Profesiones: `GET /profesiones`.
- Provincias: `GET /ubicaciones/provincias`.
- Actividades existentes para predecesoras: `GET /proyectos/{idProyecto}/actividades/buscar?todos=true`.

Cargar bajo demanda:

- Características de la profesión elegida: `GET /profesiones/{id}/caracteristicas-tecnicas`.
- Localidades de la provincia elegida: `GET /ubicaciones/localidades?provinciaId={id}`.
- Habilidades: ver sección 7.

### 2.3 Alta

1. El Director completa los datos básicos.
2. Opcionalmente agrega ubicación, predecesoras y requerimientos profesionales.
3. Confirma el alta.
4. El frontend envía un único `POST`.
5. Con `201`, cerrar el formulario, mostrar confirmación y refrescar la planificación usando `idActividad` para resaltar la nueva fila.

La actividad siempre se crea en estado `PENDIENTE`. El estado no se envía ni se elige.

---

## 3. Request Payload

```json
{
  "nombre": "Prueba de vestuario",
  "descripcion": "Prueba final con modelos y ajustes.",
  "duracion": 3,
  "unidadDuracion": "DIAS",
  "ubicacion": {
    "localidadId": "06049010000",
    "provinciaId": "06"
  },
  "predecesoras": [201],
  "requerimientos": [
    {
      "cantidad": 2,
      "idProfesion": 2,
      "descripcion": "Modelos para la prueba",
      "caracteristicas": [
        { "idCaracteristica": 1, "valorMin": 165, "valorMax": 180 },
        { "idCaracteristica": 5, "valores": [1, 4] }
      ],
      "habilidades": [1, 4]
    }
  ]
}
```

No enviar `fechaHoraInicio`, `fechaHoraFin` ni `estado`: el backend los calcula o los fuerza.

---

## 4. Matriz de Campos y Validaciones

| Campo | Tipo / Formato | Obligatorio | Reglas / Restricciones Backend | Notas UI/UX |
| :--- | :--- | :---: | :--- | :--- |
| `nombre` | `string` | **Sí** | No blanco. Máx. 20 caracteres. Único en la planificación, comparado sin distinguir mayúsculas y sin espacios al inicio/fin. | Input con contador `(x/20)`. Aplicar `trim`. Error `409` inline. |
| `descripcion` | `string` | No | Máx. 200 caracteres. | Textarea con contador `(x/200)`. |
| `duracion` | `int` | **Sí** | Entero `> 0`. Valores demasiado grandes pueden producir `400`. | Number input, mínimo `1`, sin decimales. |
| `unidadDuracion` | `enum` | **Sí** | `DIAS` o `HORAS` (mayúsculas). | Segmented control o select. |
| `ubicacion` | `object` | No | Si se envía, `localidadId` es obligatorio y no puede estar en blanco. | Enviar solo si el usuario eligió una localidad. |
| `ubicacion.localidadId` | `string` | Condicional | Debe existir en el catálogo Georef. | Select dependiente de la provincia. |
| `ubicacion.provinciaId` | `string` | No | Si se envía, debe corresponder a la localidad. | Select de provincia. |
| `predecesoras` | `array<long>` | No | IDs no nulos, sin duplicados, existentes y de la **misma planificación**. | Multi-select de actividades del proyecto. |
| `requerimientos` | `array` | No | Lista dinámica. Puede omitirse o enviarse vacía. | Sección `+ Agregar requerimiento`. |
| `requerimientos[].cantidad` | `int` | **Sí** | `> 0`. | Number input. |
| `requerimientos[].idProfesion` | `long` | **Sí** | Debe existir. | Select de profesiones. |
| `requerimientos[].descripcion` | `string` | No | Máx. 200 caracteres. | Input o textarea con contador. |
| `requerimientos[].caracteristicas` | `array` | No | Sin IDs de característica duplicados. Cada característica debe pertenecer a la profesión del requerimiento. | Cargar solo las características de la profesión seleccionada. |
| `requerimientos[].caracteristicas[].idCaracteristica` | `long` | **Sí** | Debe existir. | Select. |
| `...caracteristicas[].valorMin` / `valorMax` | `decimal` | Condicional | **NUMERICO:** ambos obligatorios, `>= 0` y `valorMin <= valorMax`. **ENUMERADO/TEXTO:** deben ser `null`. | Mostrar rango solo si `tipoDato = NUMERICO`. |
| `...caracteristicas[].valores` | `array<long>` | Condicional | **ENUMERADO:** al menos un valor, todos pertenecientes a la característica. **NUMERICO/TEXTO:** vacío o `null`. | Checklist solo si `tipoDato = ENUMERADO`. |
| `requerimientos[].habilidades` | `array<long>` | No | Cada ID debe existir. | Multi-select. Ver dependencia en sección 7. |

Reglas adicionales:

- La descripción no es obligatoria; no enviar `""` si no se cargó.
- Las listas de predecesoras y requerimientos aceptan `null` o `[]`.
- Un error en cualquier requerimiento revierte toda la creación: no queda una actividad parcial.
- No existe un campo para asignar miembros del proyecto en este endpoint. La asignación directa del paso 4.a del UC no está implementada en esta API.

---

## 5. Respuesta Exitosa

```json
{
  "idActividad": 301,
  "idProyecto": 105,
  "nombreProyecto": "Colección Primavera-Verano",
  "nombre": "Prueba de vestuario",
  "descripcion": "Prueba final con modelos y ajustes.",
  "duracionMinutos": 4320,
  "fechaHoraInicio": "2026-12-12T23:59",
  "fechaHoraFin": "2026-12-15T23:59",
  "estado": "PENDIENTE",
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
        },
        {
          "idCaracteristica": 5,
          "codigo": "COLOR_OJOS",
          "tipoDato": "ENUMERADO",
          "valorMin": null,
          "valorMax": null,
          "valores": [1, 4]
        }
      ],
      "habilidades": [1, 4]
    }
  ]
}
```

Notas de respuesta:

- `estado` se devuelve con el nombre del enum en mayúsculas: `PENDIENTE`, `EN_CURSO`, `FINALIZADA`, `CANCELADA`.
- `duracionMinutos` es la duración persistida. Para mostrarla: `1 día = 1440` minutos y `1 hora = 60` minutos.
- `fechaHoraInicio` y `fechaHoraFin` son fechas locales ISO-8601, sin zona horaria.
- `fechaHoraFin` no se almacena: se deriva de `fechaHoraInicio + duracionMinutos`.
- `predecesoras` se devuelve ordenado por `nombre`.
- La respuesta incluye la actividad ya recalculada por el cronograma.

---

## 6. Reglas de Programación

El backend usa programación regresiva básica desde la fecha de entrega de la planificación. No es un CPM completo.

1. El cronograma termina el día de entrega a las **23:59**.
2. La nueva actividad no tiene sucesoras, por lo que siempre termina a las 23:59 de la fecha de entrega.
3. Su inicio se calcula como `fecha de entrega 23:59 - duración`.
4. Si se indican predecesoras, las predecesoras `PENDIENTE` se reprograman hacia atrás para terminar cuando comienza la nueva actividad.
5. Las actividades `EN_CURSO`, `FINALIZADA` y `CANCELADA` no se modifican.
6. Si el inicio calculado de cualquier actividad `PENDIENTE` es anterior a `proyecto.fechaInicio`, la operación falla con `409`.
7. Si `proyecto.fechaInicio` es `null`, no se valida ese límite inferior.
8. Si la planificación no tiene fecha de entrega, la operación falla con `400`.
9. Si ya existe un ciclo entre actividades pendientes, la operación falla con `409`.
10. La duración se calcula con aritmética simple de minutos. No contempla jornadas laborales, feriados, fines de semana ni disponibilidad de personal.

Consecuencia de UX: al elegir predecesoras, la nueva actividad se ubica al final del cronograma y las predecesoras se mueven hacia atrás. Después del `201`, refrescar toda la planificación, no solo la fila nueva.

### Limitaciones conocidas

- No hay CPM completo ni validación de holguras: eso corresponde a UC-33.
- El backend no valida solapamientos con actividades no `PENDIENTE` usadas como predecesoras.
- No se asignan miembros a la actividad desde este endpoint.

---

## 7. Catálogos y Endpoints Auxiliares

| Uso | Endpoint | Respuesta relevante |
| :--- | :--- | :--- |
| Detalle y permisos del proyecto | `GET /proyectos/{idProyecto}` | `estado`, `fechaEntregaEstipulada`, `permisos`, `rolUsuario`, `fechaInicio`. |
| Profesiones | `GET /profesiones?nombre={texto}` | `idProfesion`, `codigo`, `nombre`, `descripcion`. |
| Características de una profesión | `GET /profesiones/{id}/caracteristicas-tecnicas?codigo={texto}&unidad={texto}` | `idCaracteristica`, `codigo`, `nombre`, `tipoDato`, `unidad`, `valores[]`. |
| Provincias | `GET /ubicaciones/provincias` | `idExterno`, `nombre`. |
| Localidades | `GET /ubicaciones/localidades?provinciaId={id}&nombre={texto}` | `idExterno`, `nombre`, `idProvincia`, `nombreProvincia`. |
| Predecesoras | `GET /proyectos/{idProyecto}/actividades/buscar?todos=true` | `PaginaResponse<ActividadBusquedaResponse>` con `idActividad`, `nombre`, `estado`, fechas y requerimientos resumidos. |

Para `tipoDato` de características:

- `NUMERICO`: mostrar dos inputs numéricos (`valorMin`, `valorMax`).
- `ENUMERADO`: mostrar checklist de `valores[]`; enviar los `idValor` elegidos.
- `TEXTO`: no mostrar rango ni checklist; solo seleccionar la característica.

### Dependencia pendiente: habilidades

El request acepta `habilidades` como IDs, pero este backend no expone actualmente un endpoint público para listar habilidades (UC-48). Opciones para frontend:

- Coordinar con backend la exposición del catálogo antes de habilitar el multi-select.
- Mantener el control deshabilitado y enviar `habilidades: []` mientras no exista el catálogo.
- No inventar IDs ni reutilizar nombres de habilidades como si fueran IDs.

---

## 8. Manejo de Errores

Errores de validación (`400`):

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

Errores de regla de negocio:

```json
{
  "message": "Ya existe una actividad con el nombre 'Prueba de vestuario' en esta planificacion.",
  "httpStatus": 409,
  "timestamp": 1760000000000
}
```

| Código | Excepción / Causa | Acción Frontend |
| :---: | :--- | :--- |
| **400** | `MethodArgumentNotValidException`: campos vacíos, largos máximos, `duracion <= 0`, `cantidad <= 0`, `idProfesion` nulo, `localidadId` vacío. | Resaltar inputs con `errores[campo]`; para listas, mapear el índice a la fila. |
| **400** | `DuracionInvalidaException`: duración o unidad ausente, duración demasiado grande. | Error en duración/unidad. |
| **400** | `FechaEntregaNoDefinidaException`: la planificación no tiene fecha de entrega. | Bloquear el formulario y ofrecer editar la fecha del proyecto. |
| **400** | `PredecesoraInvalidaException`: IDs nulos, duplicados, inexistentes o de otra planificación. | Limpiar la selección y recargar actividades del proyecto. |
| **400** | `RequerimientoInvalidoException`: característica duplicada, rango invertido/negativo, rango en no numérica, enumerada sin valores. | Mostrar `message` bajo la sección de requerimientos. |
| **400** | `ProfesionNoEncontradaException` | Error en el select de profesión. |
| **400** | `CaracteristicaNoEncontradaException` | Error en la característica elegida. |
| **400** | `CaracteristicaProfesionNoCoincideException` | Recargar características de la profesión y limpiar la selección inválida. |
| **400** | `ValorCaracteristicaNoEncontradoException` | Recargar valores de la característica. |
| **400** | `LocalidadNoEncontradaException`, `LocalidadSinProvinciaException`, `ProvinciaSinLocalidadException` | Error en el selector de ubicación. |
| **400** | JSON malformado, `unidadDuracion` fuera de `DIAS`/`HORAS` | Validar en cliente y mostrar error de formulario. |
| **400** | `DataIntegrityViolationException` | Alerta genérica; no reintentar automáticamente. |
| **401** | Sesión ausente o inválida | Redirigir a login. |
| **403** | `AccesoDenegadoProyectoException`: sin `CREAR_ACTIVIDAD` o sin membresía activa. | Ocultar la acción; no permitir reintento. |
| **404** | `PerfilActivoNoSeleccionadoException` | Modal de selección de perfil. |
| **404** | `PerfilNoEncontradoException` | Re-sincronizar sesión. |
| **404** | `ProyectoNoEncontradoException`: proyecto inexistente o sin planificación. | Volver al listado de proyectos. |
| **404** | `HabilidadNoEncontradaException`: ID de habilidad inexistente. | Recargar el catálogo o limpiar la selección. |
| **409** | `NombreActividadDuplicadoException` | Error inline en `nombre`. |
| **409** | `EstadoProyectoNoPermiteActividadesException`: proyecto `FINALIZADO` o `CANCELADO`. | Cerrar el formulario y refrescar el proyecto. |
| **409** | `CronogramaInviableException`: inicio antes de `proyecto.fechaInicio` o ciclo existente. | Alerta bloqueante; sugerir acortar la duración, quitar predecesoras o ajustar fechas. La actividad no se creó. |
| **409** | `PerfilEnBajaException`: perfil activo dado de baja. | Re-sincronizar sesión / elegir otro perfil. |

---

## 9. Consideraciones de UX

1. **Fecha de entrega obligatoria:** si el proyecto no tiene `fechaEntregaEstipulada`, no permitir confirmar. Mostrar un aviso con enlace a la edición del proyecto.
2. **Estado calculado, no editable:** mostrar un resumen de `fechaHoraInicio` y `fechaHoraFin` estimados, pero no incluir inputs para esas fechas.
3. **Duración flexible:** permitir alternar `DIAS`/`HORAS` sin perder el valor numérico. Mostrar equivalencia en la vista previa.
4. **Predecesoras:** multi-select con nombre, estado y fechas. No permitir duplicados. Aclarar que la nueva actividad quedará al final del cronograma y que las predecesoras pendientes se reprogramarán.
5. **Requerimientos dinámicos:** cada fila tiene profesión, cantidad, descripción, características y habilidades. Al cambiar la profesión, limpiar características y valores dependientes.
6. **Coherencia por tipo de dato:** renderizar rango, checklist o ningún control según `tipoDato`. No enviar `valorMin`/`valorMax` para enumeradas ni `valores` para numéricas.
7. **Ubicación opcional:** provincia y localidad en cascada. Si no se elige localidad, omitir el objeto `ubicacion`.
8. **Sin asignación de miembros:** no mostrar un selector de integrantes en esta pantalla; el endpoint no lo admite.
9. **Doble envío:** deshabilitar el botón mientras la petición está en curso. No hay clave de idempotencia.
10. **Post-creación:** usar `idActividad` para resaltar la nueva actividad y refrescar la planificación completa, porque las predecesoras pueden haber cambiado de fecha.
11. **Formato de estado:** mapear `PENDIENTE` a “Pendiente”, `EN_CURSO` a “En curso”, etc. No mostrar el valor crudo del enum.
12. **Accesibilidad:** asociar cada error inline con su input y mover el foco al primer campo inválido.

---

## 10. Criterios de Aceptación

- Con nombre, duración y unidad válidos, el `POST` devuelve `201` y una actividad `PENDIENTE`.
- La nueva actividad aparece en la planificación y sus fechas son consistentes con la fecha de entrega.
- Sin fecha de entrega, el frontend bloquea el envío y, si se fuerza la petición, el backend responde `400`.
- Un nombre duplicado en la misma planificación (sin distinguir mayúsculas) devuelve `409` y se muestra inline.
- Una duración `<= 0`, no entera o fuera de rango se rechaza en cliente y backend.
- Una predecesora de otro proyecto devuelve `400` y no se crea la actividad.
- Un requerimiento numérico exige `valorMin` y `valorMax`; uno enumerado exige al menos un valor válido.
- Una característica de otra profesión devuelve `400`.
- Sin `CREAR_ACTIVIDAD`, la acción no está disponible y el backend responde `403` si se intenta.
- Un proyecto `FINALIZADO` o `CANCELADO` devuelve `409`.
- Si el cronograma no es viable, devuelve `409` y no queda ninguna actividad parcial.
- Ubicación, predecesoras y requerimientos pueden omitirse sin errores.
- La respuesta incluye `duracionMinutos`, `fechaHoraInicio`, `fechaHoraFin`, `estado`, `predecesoras` y `requerimientos`.

---

## 11. Fuera de Alcance

- Asignación directa de miembros del proyecto a la actividad.
- Modificación y eliminación de actividades (UC-50 y UC-51).
- Búsqueda/listado completo de actividades (UC-52), salvo su uso para elegir predecesoras.
- CPM completo, holguras y validación de disponibilidad de personal (UC-33).
- Catálogo público de habilidades (UC-48), pendiente de exposición.
- Carga de imágenes o archivos.
