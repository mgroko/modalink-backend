# Ficha técnica — Pestaña Cronograma (UC-33)

**Endpoint:** `GET /proyectos/{idProyecto}/cronograma`  
**Acceso:** `VER_PROYECTO` (miembros activos del proyecto).  
**Uso:** tarjetas de resumen, ruta crítica, Gantt y tabla CPM detallada.

---

## 1. Contrato

```json
{
  "resumen": {
    "fechaInicio": "2026-10-14T00:00:00",
    "fechaFinProyectada": "2026-12-04T00:00:00",
    "fechaEntrega": "2027-03-15",
    "makespanMinutos": 73440,
    "margenMinutos": 146879,
    "tentativo": false,
    "parcial": false,
    "estadoCronograma": "NORMAL",
    "viable": true,
    "totalActividades": 8,
    "actividadesCriticas": 5,
    "rutaCritica": [1, 2, 5, 6, 7]
  },
  "actividades": [
    {
      "idActividad": 3,
      "nombre": "Prendas de catálogo",
      "descripcion": "Prendas para el catálogo",
      "estado": "PENDIENTE",
      "duracionMinutos": 21600,
      "predecesoras": [
        { "idActividad": 2, "nombre": "Fitting y prueba de vestuario" }
      ],
      "inicioProyectado": "2026-10-23T00:00:00",
      "finProyectado": "2026-11-07T00:00:00",
      "finConHolgura": "2026-11-23T00:00:00",
      "critica": false,
      "enRutaCritica": false,
      "holguraMakespan": 23040,
      "holguraEntrega": 169920,
      "rangoInicio": { "inicioMinutos": 12960, "finMinutos": 12960 },
      "rangoEjecucion": { "inicioMinutos": 12960, "finMinutos": 169920 },
      "conflicto": false,
      "motivoConflicto": null,
      "fijo": false,
      "tentativo": false
    }
  ],
  "conflictos": []
}
```

---

## 2. Reglas de presentación

### Tarjetas

| Tarjeta | Campos |
| :--- | :--- |
| Duración estimada | `resumen.makespanMinutos`, `fechaInicio`, `fechaFinProyectada` |
| Ruta crítica | `resumen.actividadesCriticas`, `resumen.totalActividades`, `rutaCritica` |
| Margen hasta la entrega | `resumen.margenMinutos`, `resumen.fechaEntrega` |

- `fechaFinProyectada` es **exclusiva** (`t0 + makespan`). La UI muestra el
  último día incluido: 04/12 exclusivo se muestra como 03/12.
- `margenMinutos` ya contempla la entrega a las 23:59. No recalcularlo
  restando `fechaFinProyectada` de `fechaEntrega`; daría un día menos.
- `tentativo = true` cuando el proyecto no tiene fecha de inicio confirmada.
- `parcial = true` cuando no hay fecha de entrega completa.
- `estadoCronograma`: `NORMAL`, `ALERTA_DESBORDE` o `SIN_FECHA_ENTREGA`.

### Ruta crítica

- `resumen.rutaCritica` contiene ids con `enRutaCritica = true`, ordenados por
  inicio temprano.
- El chip "Crítica" de la tabla y el conteo de la tarjeta usan
  **`enRutaCritica`**, no `critica`.
- `critica` significa holgura cero; una actividad fija puede tener holgura cero
  sin pertenecer a la cadena que define el makespan.
- Si hay ramas críticas paralelas, `rutaCritica` es un conjunto ordenado, no
  una cadena lineal.

### Tabla CPM

- `Holgura` = `holguraMakespan`.
- `Inicio – Fin` = `inicioProyectado` / `finProyectado`; el fin es exclusivo y
  la UI muestra el día inclusive (una actividad de 1 día 22/10–22/10 tiene
  `finProyectado` 23/10).
- `Depende de` = `predecesoras` no canceladas.
- Las actividades canceladas no aparecen en este endpoint.

### Gantt

- Barra sólida: `inicioProyectado` → `finProyectado`.
- Barra clara: `finProyectado` → `finConHolgura`.
- **No usar `rangoEjecucion` para la barra clara**: incluye el margen global
  hasta la entrega y dibujaría una barra mucho más larga.
- Color por `enRutaCritica` / holgura.
- Al hacer click en una barra, el popover usa los datos de la fila. Para el
  detalle completo (responsables, requerimientos), llamar a
  `GET /proyectos/{idProyecto}/actividades/{idActividad}`.

### Tooltips

Campos disponibles: `holguraEntrega`, `rangoInicio`, `rangoEjecucion`,
`conflicto`, `motivoConflicto`, `fijo`, `tentativo`.

---

## 3. Errores

| Código | Caso |
| :--- | :--- |
| `403` | El perfil no es miembro activo del proyecto |
| `404` | Proyecto inexistente o sin perfil activo |
| `409` | Ciclo o cronograma inviable |
| `400` | Duración inválida |

---

## 4. Front

- La pestaña se carga al abrirla, no en la carga inicial del proyecto.
- No duplicar CPM ni márgenes en JavaScript.
- Refrescar el cronograma después de crear, modificar, eliminar o cancelar
  actividades.
