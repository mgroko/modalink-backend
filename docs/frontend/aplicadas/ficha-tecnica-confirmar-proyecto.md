# Ficha técnica — UC-60 Confirmar proyecto

**Módulo:** Proyectos  
**Actor:** Director de proyecto  
**Estado backend:** implementado con tests unitarios, MVC e integración con PostgreSQL (Testcontainers).

---

## 1. Resumen

El director confirma un proyecto en estado `Publicado`. El backend:

1. valida que todos los requerimientos generales y de actividad estén completos;
2. valida que cada actividad no cancelada tenga integrantes activos;
3. fija `fechaInicio` con el reloj del backend;
4. calcula el CPM y coloca todas las actividades en las agendas de sus integrantes;
5. persiste los horarios y pasa el proyecto a `Confirmado`;
6. notifica a todos los miembros activos después del commit.

La operación es atómica. Si falla cualquier validación o la planificación, el proyecto sigue `Publicado` y no se persiste ningún horario nuevo.

---

## 2. Endpoints

### 2.1. Previsualizar confirmación

```http
GET /proyectos/{idProyecto}/confirmacion
```

Requiere sesión, perfil activo y permiso `CONFIRMAR_PROYECTO`. Es de solo lectura.

Respuesta `200`:

```json
{
  "puedeConfirmar": false,
  "estado": "Publicado",
  "fechaInicioPropuesta": "2026-10-28T15:30:00",
  "fechaEntrega": "2026-11-10",
  "bloqueos": [
    {
      "codigo": "REQUERIMIENTOS_INCOMPLETOS",
      "mensaje": "El requerimiento de Fotógrafo necesita 1 integrante más.",
      "entidadTipo": "REQUERIMIENTO_GRAL",
      "entidadId": 10
    }
  ],
  "requerimientosGenerales": [
    {
      "idRequerimiento": 10,
      "idActividad": null,
      "nombreActividad": null,
      "idProfesion": 1,
      "nombreProfesion": "Fotógrafo",
      "cantidad": 2,
      "cubiertos": 1,
      "faltantes": 1,
      "satisfecho": false
    }
  ],
  "requerimientosActividad": [],
  "propuestaHorarios": []
}
```

Si `puedeConfirmar = true`, `propuestaHorarios` contiene la propuesta del planificador (sin persistir).

### 2.2. Confirmar

```http
POST /proyectos/{idProyecto}/confirmar
```

Sin body. Respuesta `200`:

```json
{
  "proyecto": {
    "idProyecto": 100,
    "nombre": "Desfile Primavera",
    "estado": "Confirmado",
    "fechaInicio": "2026-10-28",
    "fechaEntregaEstipulada": "2026-11-10",
    "requerimientosGral": []
  },
  "fechaInicioConfirmada": "2026-10-28T15:30:00",
  "cronograma": {
    "resumen": {
      "fechaInicio": "2026-10-28T15:30:00",
      "fechaFinProyectada": "2026-11-10T18:00:00",
      "fechaEntrega": "2026-11-10",
      "margenMinutos": 120,
      "tentativo": false,
      "viable": true
    },
    "actividades": [],
    "conflictos": []
  }
}
```

`ProyectoResponse.fechaInicio` sigue siendo `LocalDate`. La hora exacta se expone en `fechaInicioConfirmada`.

---

## 3. Reglas de negocio

### 3.1. Requerimiento completo

Un requerimiento está completo cuando `cubiertos >= cantidad`, contando:

- miembros activos vinculados al requerimiento general;
- asignaciones activas vinculadas al requerimiento de actividad.

Las actividades canceladas no se consideran. El proyecto debe tener al menos una actividad no cancelada.

### 3.2. Cierre de postulaciones

No se persiste una bandera de cierre. Un requerimiento acepta nuevas postulaciones mientras:

- el proyecto esté `Publicado` o `Confirmado`;
- el requerimiento tenga `faltantes > 0`;
- se cumplan las demás reglas de UC-26.

Al confirmar, todos los requerimientos están completos, por lo que ninguno acepta postulaciones. Si una baja o eliminación deja un requerimiento con faltantes, ese requerimiento vuelve a aceptar postulaciones.

### 3.3. Notificaciones

- Tipo: `PROYECTO_CONFIRMADO`.
- Destinatarios: todos los miembros activos, incluido el director, una notificación por perfil.
- `entidadTipo = "PROYECTO"`, `entidadId = idProyecto`.
- Se generan después del commit. Un fallo de notificación no revierte la confirmación.

---

## 4. Errores

| HTTP | Código | Cuándo |
|---:|---|---|
| `403` | `ACCESO_DENEGADO` | No es director activo o no tiene `CONFIRMAR_PROYECTO`. |
| `404` | `PROYECTO_NO_ENCONTRADO` | El proyecto no existe. |
| `404` | `PERFIL_ACTIVO_NO_SELECCIONADO` | No hay perfil activo. |
| `409` | `PROYECTO_YA_CONFIRMADO` | Ya está `Confirmado`. |
| `409` | `ESTADO_PROYECTO_INVALIDO` | No está `Publicado`. |
| `409` | `SIN_ACTIVIDADES` | No hay actividades no canceladas. |
| `409` | `REQUERIMIENTOS_INCOMPLETOS` | Falta cupo general o de actividad. |
| `409` | `ACTIVIDADES_SIN_ASIGNACION` | Alguna actividad no tiene integrantes activos. |
| `409` | `FECHA_ENTREGA_INVALIDA` | No hay entrega o es anterior al inicio. |
| `409` | `CRONOGRAMA_NO_VIABLE` | Ciclo, conflicto, holgura negativa o desborde de entrega. |
| `409` | `SIN_DISPONIBILIDAD` | No hay hueco común para una actividad. |
| `409` | `DATOS_INCONSISTENTES` | Actividad fija sin inicio, asignación sin vínculo, etc. |

El sobre de error conserva `{ message, httpStatus, timestamp }` y agrega `codigo` y `bloqueos`.

---

## 5. Recomendaciones para el frontend

1. Al abrir el diálogo de confirmación, llamar al `GET` y mostrar `bloqueos` y `requerimientos`.
2. Habilitar “Confirmar” solo si `puedeConfirmar === true`.
3. Si el `POST` responde `409`, mostrar `message` y, si existe, `bloqueos`; refrescar proyecto y cronograma.
4. Tras un `200`, usar `estado = "Confirmado"` y `fechaInicioConfirmada`.
5. No inferir el cierre de postulaciones localmente: usar `puedePostularse` de cada requerimiento.
6. No reutilizar `propuestaHorarios` del preview como horarios persistidos: el `POST` vuelve a planificar.
7. Refrescar notificaciones después de confirmar.

---

## 6. Referencias

- `src/main/java/org/mgroko/backend/proyectos/servicio/ConfirmarProyectoService.java`
- `src/main/java/org/mgroko/backend/proyectos/servicio/ContextoConfirmacionProyectoService.java`
- `src/main/java/org/mgroko/backend/proyectos/servicio/PlanificadorRecursosService.java`
- `src/main/java/org/mgroko/backend/proyectos/servicio/PrevisualizarConfirmacionService.java`
- `src/main/java/org/mgroko/backend/proyectos/controlador/ProyectoController.java`
- `docs/planes/plan_de_accion_uc60_confirmar_proyecto.md`
