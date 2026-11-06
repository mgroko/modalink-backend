# Ficha técnica — UC-70 Asignar actividad a miembro

**Proyecto:** ModaLink
**Backend:** Spring Boot
**Fecha:** 2026-10-29
**Acceso:** director activo del proyecto con permiso `ASIGNAR_ACTIVIDAD`.

---

## 1. Objetivo

Permitir que el director asigne directamente una actividad a un miembro activo
del proyecto, verificando antes la disponibilidad del miembro frente al rango
factible de la actividad.

La asignación no agenda ni reserva un horario: sólo crea el vínculo entre el
miembro y la actividad. El cronograma sigue siendo responsabilidad del CPM.

---

## 2. Endpoints

### 2.1. Listar miembros del proyecto

```http
GET /proyectos/{idProyecto}/miembros?nombre=&rol=&idProfesion=&page=0&size=20&todos=false
```

Uso: poblar el selector de destinatario. Devuelve sólo miembros `ACTIVO`.

```json
{
  "contenido": [
    {
      "idMiembro": 900,
      "idPerfil": 50,
      "idUsuario": 2,
      "nombreArtistico": "Luna",
      "nombreUsuario": "Nombre",
      "apellidoUsuario": "Apellido",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "rol": "MIEMBRO",
      "estadoParticipacion": "ACTIVO"
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 1,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

Filtros opcionales:

- `nombre`: busca por nombre artístico, nombre o apellido del usuario.
- `rol`: `DIRECTOR` o `MIEMBRO`.
- `idProfesion`: identificador de profesión.

Requiere `VER_PROYECTO`. Un usuario que no es miembro activo recibe `403`.

### 2.2. Consultar disponibilidad antes de asignar

```http
GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad?idPerfil=50
```

Es una consulta de solo lectura. Se recomienda usarla para mostrar el hueco y
el motivo antes de confirmar. El `POST` vuelve a validar siempre.

### 2.3. Asignar actividad

```http
POST /proyectos/{idProyecto}/actividades/{idActividad}/asignaciones
Content-Type: application/json
```

Body:

```json
{
  "idPerfil": 50
}
```

Respuesta `201 Created`:

```json
{
  "idAsignacionActividad": 1200,
  "idProyecto": 100,
  "idActividad": 77,
  "idMiembro": 900,
  "idPerfil": 50,
  "idUsuario": 2,
  "idRequerimientoActividad": 123,
  "requerimientoSatisfecho": true,
  "provisional": false,
  "inicioSeleccionado": "2026-11-02T15:00:00",
  "finSeleccionado": "2026-11-02T16:00:00",
  "resultadoPreferencia": "EXACTO",
  "desvioMinutos": 0
}
```

Campos:

| Campo | Interpretación |
|---|---|
| `idRequerimientoActividad` | Requerimiento cubierto. `null` si no hubo coincidencia de profesión o si el miembro es director. |
| `requerimientoSatisfecho` | `true` si el requerimiento vinculado quedó cubierto después de la asignación. |
| `provisional` | `true` si el proyecto no tiene fecha de inicio confirmada; el rango evaluado fue `[ahora, fechaEntrega]`. |
| `inicioSeleccionado`, `finSeleccionado` | Hueco usado para validar. No se persiste en la actividad. |
| `resultadoPreferencia` | `SIN_PREFERENCIA`, `EXACTO`, `DESVIO` o `SIN_LUGAR`. |
| `desvioMinutos` | Desvío respecto del horario preferido; puede ser negativo. |

---

## 3. Reglas de negocio

1. El actor debe ser director activo del proyecto y tener `ASIGNAR_ACTIVIDAD`.
2. El destinatario debe ser un perfil activo con membresía activa en el proyecto.
3. Se permite asignar a directores. En ese caso no se vincula requerimiento y no
   se consume cupo.
4. Para un miembro no director, se busca el primer requerimiento de la actividad
   con su misma profesión que todavía tenga cupo. Si no existe, la asignación se
   crea sin requerimiento.
5. No se permiten duplicados `(miembro, actividad)`.
6. El proyecto debe estar en `BORRADOR`, `PUBLICADO` o `CONFIRMADO`.
7. La actividad no puede estar `CANCELADA` ni `FINALIZADA`.
8. El proyecto debe tener fecha de entrega. Sin ella no se puede calcular CPM ni
   disponibilidad.
9. La verificación de disponibilidad es la misma de UC-26/UC-34: jornada,
   bloqueos manuales, asignaciones existentes, margen y preferencia horaria.
10. La notificación `ACTIVIDAD_ASIGNADA` la genera el trigger
    `trg_notificar_asignacion_actividad`.

---

## 4. Errores

| Código | Caso | Body / mensaje |
|---|---|---|
| `400` | `idPerfil` nulo o inválido | `errores.idPerfil` |
| `400` | Proyecto sin fecha de entrega | Mensaje de `FechaEntregaNoDefinidaException` |
| `403` | Sin perfil activo, sin membresía o sin `ASIGNAR_ACTIVIDAD` | Mensaje de acceso denegado |
| `404` | Proyecto, actividad o perfil inexistente | Mensaje del recurso |
| `409` | Perfil no es miembro activo | “El perfil no es miembro activo del proyecto.” |
| `409` | Usuario o perfil inactivo | “El usuario del miembro no se encuentra activo.” |
| `409` | Duplicado | “El miembro ya está asignado a esta actividad.” |
| `409` | Proyecto/actividad en estado no permitido | Mensaje con el estado |
| `409` | Sin agenda | “El miembro no tiene agenda configurada para verificar disponibilidad.” |
| `409` | Sin disponibilidad | Incluye `motivo`, `provisional`, `inicioEvaluado`, `finEvaluado` y `miembrosBloqueantes` |

Ejemplo de `409` por disponibilidad:

```json
{
  "message": "El miembro no tiene disponibilidad suficiente dentro del rango factible de la actividad.",
  "motivo": "SIN_HUECO",
  "provisional": true,
  "inicioEvaluado": "2026-11-02T09:00:00",
  "finEvaluado": "2026-11-02T18:00:00",
  "miembrosBloqueantes": [],
  "httpStatus": 409,
  "timestamp": 1790000000000
}
```

---

## 5. Flujo sugerido en frontend

1. `GET /proyectos/{idProyecto}/actividades` para elegir la actividad.
2. `GET /proyectos/{idProyecto}/miembros` para elegir el destinatario.
3. Opcional: `GET .../disponibilidad?idPerfil=...` para previsualizar.
4. Confirmar con `POST .../asignaciones`.
5. Refrescar planificación, cronograma, calendario y notificaciones.

---

## 6. Reglas que el frontend no debe duplicar

- No recalcular CPM, rangos, márgenes ni holguras.
- No decidir por sí solo si el miembro cubre un requerimiento.
- No persistir `inicioSeleccionado` ni `finSeleccionado` como horario.
- No tratar `provisional = true` como error.
- No asumir que `idRequerimientoActividad` siempre existe.
- No crear asignaciones por fuera del endpoint.
