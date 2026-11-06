# Ficha técnica frontend: margen total, Gantt y reprogramación automática

**Proyecto:** ModaLink
**Backend:** Spring Boot
**Fecha:** 2026-11-03
**Estado:** instrucciones para el frontend; backend de margen y Gantt implementado, reprogramación automática en implementación.

---

## 1. Alcance

Esta ficha reúne los cambios de contrato que impactan en el frontend:

1. El margen por actividad pasa a ser **margen total entre actividades**.
2. El Gantt recibe fechas **persistidas** además de las **proyectadas**.
3. `PUT /calendario/jornada` devuelve un resumen de **reprogramación automática** de proyectos confirmados.
4. Nuevo endpoint para que un director **reintente** la reprogramación de un proyecto que falló.

No modifica los endpoints existentes de calendario, cronograma ni actividades; solo agrega campos y un endpoint nuevo.

---

## 2. Margen total entre actividades

### 2.1. Semántica

`margenActividadMinutos` ya no es un buffer “a cada lado”. Ahora es la separación **total** entre el fin de una actividad y el inicio de la siguiente.

```text
margen 30  -> 30 minutos entre dos actividades consecutivas
margen 0   -> sin separación adicional
margen 15  -> 15 minutos totales (repartidos 7 y 8)
```

El backend reparte el margen en mitades:

- `margenIzquierdo = margen / 2`
- `margenDerecho = margen - margenIzquierdo`

### 2.2. Qué cambia en la UI del calendario

`GET /calendario` y `GET /calendario/perfil/{id}` siguen devolviendo `actividades[]` como `BloqueoActividadResponse`.

Ahora `fechaHoraInicio` y `fechaHoraFin` ya vienen expandidos por **la mitad del margen** a cada lado. Con margen 30:

```text
Actividad real: 09:00 – 11:00
Bloque visible: 08:45 – 11:15
```

Por lo tanto:

- **No sumar ni restar el margen en el frontend.**
- Pintar el rango tal como llega.
- Dos bloques consecutivos del mismo usuario quedarán separados por el margen total.
- El margen también se aplica contra bloqueos manuales y actividades de otros proyectos.

### 2.3. Configuración de jornada

El request y el response de `PUT /calendario/jornada` conservan `margenActividadMinutos`, pero su significado es el de la sección 2.1.

En el formulario:

- Mostrar el campo como “Margen entre actividades (min)”.
- El mínimo sigue siendo `0`.
- No mostrar “buffer a cada lado”.

---

## 3. Gantt: fechas persistidas vs proyectadas

### 3.1. Nuevos campos en `ActividadCronogramaResponse`

`GET /proyectos/{idProyecto}/cronograma` agrega dos campos al final de cada actividad:

```json
{
  "idActividad": 3,
  "nombre": "Prendas de catálogo",
  "inicioProyectado": "2026-10-23T00:00:00",
  "finProyectado": "2026-11-07T00:00:00",
  "finConHolgura": "2026-11-23T00:00:00",
  "fijo": false,
  "tentativo": false,
  "inicioPersistido": null,
  "finPersistido": null
}
```

| Campo | Significado |
|---|---|
| `inicioProyectado` / `finProyectado` | Proyección CPM. No es una reserva. |
| `inicioPersistido` / `finPersistido` | Fechas reales guardadas en la actividad. |
| `fijo` | La actividad no se puede reprogramar. |
| `tentativo` | El proyecto no tiene fecha de inicio confirmada. |

### 3.2. Regla de visualización

```text
inicio visible = inicioPersistido ?? inicioProyectado
fin visible    = finPersistido ?? finProyectado
```

- **Proyecto confirmado:** usar `inicioPersistido`/`finPersistido` para la barra sólida. Coinciden con el calendario.
- **Proyecto no confirmado:** usar `inicioProyectado`/`finProyectado` y mostrar la barra como proyectada/tentativa.
- Si `inicioPersistido` es `null`, la actividad no tiene horario persistido; es una proyección.
- `finConHolgura` sigue siendo CPM: `finProyectado + holguraMakespan`. No reemplazarlo por fechas persistidas.
- Mantener la etiqueta visual de “Proyectada” cuando se use `inicioProyectado`.
- Si `tentativo === true`, mostrar la fecha como estimada.

### 3.3. Tabla CPM

La tabla CPM puede seguir usando `inicioProyectado`/`finProyectado`, porque es una tabla de cálculo. El Gantt es la vista que debe priorizar lo persistido.

---

## 4. Reprogramación automática al cambiar la jornada

### 4.1. Comportamiento

Al guardar `PUT /calendario/jornada`:

1. Se guarda la nueva jornada y el margen.
2. El backend busca proyectos **CONFIRMADOS** donde el usuario tiene asignaciones activas.
3. Replanifica automáticamente cada proyecto afectado:
   - mueve actividades `PENDIENTE`;
   - conserva `EN_CURSO` y `FINALIZADA`;
   - no toca actividades `CANCELADA`.
4. Si un proyecto no tiene plan viable, **ese proyecto queda intacto** y se informa como fallido.
5. Se notifica a directores y a los integrantes asignados a actividades movidas.

No hay paso de aprobación: la reprogramación es automática. El frontend debe mostrar el resumen y refrescar las vistas afectadas.

### 4.2. Nuevo campo en `ConfigJornadaResponse`

Además de `margenActividadMinutos` y `dias`, la respuesta agrega:

```json
{
  "margenActividadMinutos": 30,
  "dias": [
    {
      "diaSemana": 1,
      "horaInicioManana": "09:00:00",
      "horaFinManana": null,
      "horaInicioTarde": null,
      "horaFinTarde": "18:00:00"
    }
  ],
  "reprogramacion": {
    "totalProyectos": 2,
    "exitosos": [
      {
        "idProyecto": 100,
        "nombreProyecto": "Desfile Primavera",
        "motivo": "CAMBIO_JORNADA",
        "fechaEntrega": "2026-12-15",
        "actividades": [
          {
            "idActividad": 55,
            "nombre": "Montaje",
            "estado": "PENDIENTE",
            "inicioAnterior": "2026-11-05T09:00:00",
            "inicioNuevo": "2026-11-05T10:00:00"
          }
        ],
        "conflictosFijos": []
      }
    ],
    "fallidos": [
      {
        "idProyecto": 101,
        "nombreProyecto": "Campaña Otoño",
        "codigo": "SIN_DISPONIBILIDAD",
        "mensaje": "La actividad 'Fitting' no tiene disponibilidad común.",
        "bloqueos": [
          {
            "codigo": "SIN_DISPONIBILIDAD",
            "mensaje": "No hay hueco común.",
            "entidad": "ACTIVIDAD",
            "idEntidad": 77
          }
        ]
      }
    ]
  }
}
```

### 4.3. Cómo tratarlo en la UI

- Si `reprogramacion.totalProyectos === 0`: no mostrar nada o un aviso discreto (“No hubo proyectos confirmados afectados”).
- Si hay `exitosos`: mostrar un resumen con proyecto y cantidad de actividades movidas. Ofrecer un enlace al cronograma del proyecto.
- Si hay `fallidos`: mostrar una advertencia persistente. El proyecto **no cambió**. Ofrecer la acción “Reintentar” si el usuario es director.
- Si un proyecto exitoso trae `conflictosFijos`, mostrar una advertencia adicional: hay actividades en curso/finalizadas que quedaron fuera de la nueva disponibilidad y no se movieron.
- La respuesta HTTP sigue siendo `200` aunque haya fallidos. No interpretar `fallidos` como error de guardado de la jornada.
- Si la jornada no cambió, el backend devuelve `reprogramacion.totalProyectos = 0`.

### 4.4. Refrescos necesarios

Después de `PUT /calendario/jornada` con reprogramación:

- refrescar `GET /calendario`;
- refrescar los proyectos afectados (`GET /proyectos/{id}`);
- refrescar el cronograma de cada proyecto afectado (`GET /proyectos/{id}/cronograma`);
- refrescar notificaciones;
- si el usuario tiene la vista de actividades abierta, refrescar `GET /proyectos/{id}/actividades`.

No confiar en los horarios anteriores que estén en memoria.

---

## 5. Reintento manual de un proyecto fallido

### 5.1. Endpoint

```http
POST /proyectos/{idProyecto}/reprogramar
```

- Requiere sesión y perfil activo.
- Solo puede ejecutarlo un **director activo** del proyecto.
- Recalcula y persiste el plan del proyecto confirmado.
- No mueve `EN_CURSO` ni `FINALIZADA`.
- Si no hay plan viable, responde `409` con los bloqueos de `ProyectoNoConfirmableException`.
- Si funciona, responde `200` con:

```json
{
  "idProyecto": 101,
  "nombreProyecto": "Campaña Otoño",
  "motivo": "REINTENTO",
  "fechaEntrega": "2026-12-20",
  "actividades": [
    {
      "idActividad": 77,
      "nombre": "Fitting",
      "estado": "PENDIENTE",
      "inicioAnterior": "2026-11-06T09:00:00",
      "inicioNuevo": "2026-11-07T10:30:00"
    }
  ],
  "conflictosFijos": []
}
```

### 5.2. UI

- Mostrar la acción “Reintentar reprogramación” solo a directores.
- Si responde `409`, mostrar los `bloqueos` y no cambiar el cronograma en pantalla.
- Si responde `200`, reemplazar el cronograma del proyecto con la respuesta o volver a consultar `GET /proyectos/{id}/cronograma`.

---

## 6. Notificaciones

La reprogramación automática genera notificaciones existentes:

| Tipo | Cuándo | Destinatarios |
|---|---|---|
| `AJUSTE_HORARIO` | Un proyecto se reprogramó correctamente. | Directores y asignados a actividades movidas. |
| `SOBRECOMPROMISO` | Un proyecto no pudo reprogramarse o tiene conflictos fijos. | Directores. |

No hay tipos nuevos ni cambios en el contrato de notificaciones. El frontend debe refrescar el contador y la bandeja después de guardar la jornada.

---

## 7. Resumen de cambios para el frontend

- [ ] Cambiar la etiqueta del margen a “Margen total entre actividades”.
- [ ] No sumar/restar margen a los bloques del calendario.
- [ ] Usar `inicioPersistido`/`finPersistido` en el Gantt cuando existan.
- [ ] Mantener `inicioProyectado`/`finProyectado` como fallback y etiquetarlos como proyectados.
- [ ] Leer `reprogramacion` en la respuesta de `PUT /calendario/jornada`.
- [ ] Mostrar éxitos, fallidos y conflictos fijos.
- [ ] Refrescar calendario, proyectos, cronogramas y notificaciones.
- [ ] Agregar la acción “Reintentar reprogramación” para directores.
- [ ] Manejar `409` del reintento con los bloqueos.
- [ ] No asumir que un `200` de jornada implica que todos los proyectos se reprogramaron.

---

## 8. Compatibilidad

- `ConfigJornadaResponse` mantiene `margenActividadMinutos` y `dias`; `reprogramacion` es un campo nuevo.
- `ActividadCronogramaResponse` mantiene todos los campos anteriores; `inicioPersistido` y `finPersistido` son nuevos.
- Los clientes que no lean los campos nuevos siguen funcionando, pero no podrán distinguir fechas persistidas de proyectadas ni mostrar el resultado de la reprogramación.
