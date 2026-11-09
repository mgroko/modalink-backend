# Ficha técnica — Reemplazo automático

Módulo aislado en `org.mgroko.backend.reemplazos`. Se activa cuando un
integrante deja un proyecto `CONFIRMADO` por baja voluntaria o eliminación.
No se activa por desasignaciones de actividad.

## Flujo

1. Se detectan las vacantes de actividades y de requerimientos generales.
2. El sistema calcula candidatos ordenados por disponibilidad, ubicación
   (misma ciudad, misma provincia, otra) y concordancia.
3. El director confirma qué candidatos invitar.
4. Se envían invitaciones masivas.
5. El primero que acepta queda pendiente de aprobación del director.
6. Si el director aprueba, se crea la membresía/asignación y se eliminan las
   demás invitaciones de ese proceso.

## Endpoints del director

```http
GET  /proyectos/{idProyecto}/reemplazos
GET  /proyectos/{idProyecto}/reemplazos/{idReemplazo}
POST /proyectos/{idProyecto}/reemplazos/{idReemplazo}/confirmar
POST /proyectos/{idProyecto}/reemplazos/{idReemplazo}/regenerar
POST /proyectos/{idProyecto}/reemplazos/{idReemplazo}/cancelar
POST /proyectos/{idProyecto}/reemplazos/{idReemplazo}/candidatos/{idCandidato}/aprobar
POST /proyectos/{idProyecto}/reemplazos/{idReemplazo}/candidatos/{idCandidato}/rechazar
```

Confirmar:

```json
{
  "idsCandidatos": [101, 102, 103],
  "mensaje": "Te invitamos a cubrir la vacante."
}
```

## Endpoints del profesional

```http
GET  /reemplazos/invitaciones
POST /reemplazos/invitaciones/{idCandidato}/aceptar
POST /reemplazos/invitaciones/{idCandidato}/rechazar
```

`GET /reemplazos/invitaciones` puede devolver varias invitaciones del mismo
`idProyecto` cuando corresponden a actividades o requerimientos distintos.
La clave de cada ítem es `idCandidato`, no `idProyecto`.

La restricción de duplicados es por actividad o requerimiento, no por
proyecto: un perfil no puede tener dos invitaciones activas para la misma
actividad ni para el mismo requerimiento general, pero sí para actividades
distintas del mismo proyecto.

## Estados

Proceso: `PENDIENTE_CONFIRMACION_DIRECTOR`, `INVITACIONES_ENVIADAS`,
`PENDIENTE_APROBACION`, `CUBIERTO`, `SIN_CANDIDATOS`, `CANCELADO`, `EXPIRADO`.

Candidato: `PROPUESTO`, `INVITADO`, `ACEPTADO_PENDIENTE_APROBACION`,
`APROBADO`, `RECHAZADO`, `RECHAZADO_ADMIN`, `EXPIRADO`, `CANCELADO`.

## Cancelación automática por actividad cancelada

Cuando una actividad pasa a `CANCELADA`, los procesos de reemplazo asociados
que sigan activos pasan a `CANCELADO` y sus candidatos activos también pasan a
`CANCELADO`. Las invitaciones dejan de aparecer en la bandeja activa del
profesional, pero se conservan para trazabilidad. El proceso muestra
`motivoCierre = "Actividad cancelada"` en el panel del director.

## Reglas de orden

- Primero disponibilidad exacta; para actividades `PENDIENTE` también se
  admiten candidatos que entran con reprogramación.
- Luego misma ciudad, misma provincia, otra.
- Luego mayor puntaje de concordancia.
- Las habilidades y características no excluyen: generan alternativos con
  menor puntaje.
- En características numéricas, dentro del rango se prioriza el valor más alto
  y fuera del rango los más cercanos al borde.
