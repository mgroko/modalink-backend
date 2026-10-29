# Ficha Técnica Frontend: Dar de Baja Postulación a Proyecto (UC-27)

## 1. Resumen rápido

El usuario autenticado puede retirar una postulación propia que todavía esté
`PENDIENTE`, tanto a un requerimiento general del proyecto como a un
requerimiento de actividad. La baja es **física**: la fila se elimina.

- Listado previo: `GET /postulaciones/mias`
- Baja general: `DELETE /proyectos/{idProyecto}/postulaciones/general/{idPostulacion}`
- Baja de actividad: `DELETE /proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}`
- Respuesta exitosa: `204 No Content`
- No se envía body ni motivo.

## 2. Endpoints y contexto de autenticación

Todos requieren JWT válido y **perfil activo** seleccionado en el contexto de
autenticación. Si no hay perfil activo, el backend responde `404`.

### 2.1 Listar mis postulaciones

```http
GET /postulaciones/mias
    ?idProyecto=5          (opcional)
    &estado=PENDIENTE      (opcional)
    &tipo=GENERAL          (opcional: GENERAL | ACTIVIDAD)
    &page=0                (opcional, default 0)
    &size=20               (opcional, default 20)
    &todos=false           (opcional; true ignora la paginación)
```

Respuesta `200 OK`:

```json
{
  "contenido": [
    {
      "idPostulacion": 88,
      "tipo": "ACTIVIDAD",
      "estado": "PENDIENTE",
      "fechaPostulacion": "2026-10-27T11:00:00",
      "idProyecto": 5,
      "nombreProyecto": "Desfile Primavera",
      "idRequerimiento": 50,
      "descripcionRequerimiento": "Una modelo para vestuario",
      "idActividad": 40,
      "nombreActividad": "Prueba de vestuario",
      "puedeDarseDeBaja": true
    },
    {
      "idPostulacion": 77,
      "tipo": "GENERAL",
      "estado": "RECHAZADA",
      "fechaPostulacion": "2026-10-25T09:00:00",
      "idProyecto": 6,
      "nombreProyecto": "Campaña Verano",
      "idRequerimiento": 31,
      "descripcionRequerimiento": "Un fotógrafo",
      "idActividad": null,
      "nombreActividad": null,
      "puedeDarseDeBaja": false
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 2,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

En `ACTIVIDAD` se completan `idActividad` y `nombreActividad`; en `GENERAL`
quedan en `null`.

### 2.2 Dar de baja

```http
DELETE /proyectos/5/postulaciones/general/77
DELETE /proyectos/5/postulaciones/actividad/88
```

- Sin body.
- `idProyecto` debe ser el proyecto al que pertenece la postulación.
- El backend valida que la postulación pertenezca al **perfil activo**; si no,
  responde `404` (no revela la existencia de postulaciones ajenas).

Respuesta exitosa: `204 No Content`.

## 3. Reglas de negocio

| Regla | Detalle |
| :--- | :--- |
| Sólo `PENDIENTE` | Si la postulación ya fue `ACEPTADA` o `RECHAZADA`, responde `409`. |
| Sólo el titular | El `idPostulacion` debe pertenecer al perfil activo del usuario autenticado. |
| Proyecto correcto | El `idProyecto` del path debe coincidir con el proyecto real de la postulación. |
| Estado del proyecto | No se restringe: la baja se permite mientras la postulación siga `PENDIENTE`, sin importar el estado del proyecto. |
| Concurrencia | Si el director acepta/rechaza al mismo tiempo, una de las dos operaciones gana y la otra recibe `409`. |
| Sin notificación | La baja no genera notificación al director. |
| Re-postulación | Al ser borrado físico, el usuario puede volver a postularse al mismo requerimiento. |

## 4. Flujo de pantalla sugerido

### 4.1 Entrada

1. El usuario abre "Mis postulaciones" (o el detalle de un proyecto con su
   postulación pendiente).
2. `GET /postulaciones/mias` y mostrar sólo las que tengan
   `puedeDarseDeBaja = true` con la acción habilitada.
3. La acción debe indicar el proyecto, el requerimiento y, si corresponde, la
   actividad, para evitar bajas por error.

### 4.2 Confirmación

Modal con texto explícito, por ejemplo:

> ¿Querés retirar tu postulación a **Una modelo** del proyecto **Desfile
> Primavera**? Esta acción no se puede deshacer.

Botones: "Cancelar" y "Retirar postulación". El segundo se deshabilita mientras
la request está en curso.

### 4.3 Después del `204`

- Quitar la fila de la lista (o refrescar `GET /postulaciones/mias`).
- Mostrar un toast de éxito.
- Si el usuario estaba en el detalle del proyecto, refrescar el proyecto para
  actualizar `puedePostularse`.

## 5. Errores

| Código | Caso | Acción sugerida en UI |
| :--- | :--- | :--- |
| `400` | Parámetros inválidos (por ejemplo `estado` o `tipo` desconocidos) | Corregir filtros y reintentar. |
| `404` | Sin perfil activo, postulación inexistente, de otro perfil o de otro proyecto | Cerrar el modal y refrescar el listado. |
| `409` | La postulación ya no está `PENDIENTE` | Informar que ya fue gestionada y refrescar. |
| `401` | Token ausente o vencido | Redirigir al login. |

Formato de error del backend:

```json
{
  "message": "La postulación ya fue gestionada. Actualizá el listado e intentá nuevamente.",
  "httpStatus": 409,
  "timestamp": 1793201700000
}
```

## 6. Integración sugerida

```ts
export type TipoPostulacion = 'GENERAL' | 'ACTIVIDAD';
export type EstadoPostulacion = 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA';

export interface MiPostulacion {
  idPostulacion: number;
  tipo: TipoPostulacion;
  estado: EstadoPostulacion;
  fechaPostulacion: string;
  idProyecto: number;
  nombreProyecto: string;
  idRequerimiento: number;
  descripcionRequerimiento: string | null;
  idActividad: number | null;
  nombreActividad: string | null;
  puedeDarseDeBaja: boolean;
}

export function darDeBajaPostulacion(postulacion: MiPostulacion): Promise<void> {
  const tipo = postulacion.tipo === 'GENERAL' ? 'general' : 'actividad';
  return api.delete(
    `/proyectos/${postulacion.idProyecto}/postulaciones/${tipo}/${postulacion.idPostulacion}`
  );
}
```

## 7. Checklist de aceptación

- [ ] El listado muestra postulaciones generales y de actividad del perfil activo.
- [ ] Sólo se habilita la baja cuando `puedeDarseDeBaja` es `true`.
- [ ] La confirmación identifica proyecto y requerimiento/actividad.
- [ ] El `DELETE` se arma con el `idProyecto` y `tipo` correctos.
- [ ] Un `204` quita la fila y muestra confirmación visual.
- [ ] Un `409` informa que la postulación ya fue gestionada y refresca.
- [ ] Un `404` refresca el listado sin romper la pantalla.

## 8. Notas

- No se pide motivo: el contrato de UC-27 no lo define.
- No se notifica al director.
- La baja es física; no hay historial de postulaciones retiradas.
