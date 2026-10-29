# Ficha Técnica Frontend: Reportar Perfil (UC-15)

## 1. Resumen rápido

Un usuario con **perfil activo** puede reportar el perfil de otro usuario.
El reporte se registra con estado `PENDIENTE` y genera una notificación in-app
para todos los administradores activos.

- **Alta:** `POST /perfiles/{idPerfil}/reportes` → `204 No Content`
- **Bandeja admin:** `GET /admin/reportes-perfil`
- **Detalle admin:** `GET /admin/reportes-perfil/{idReporte}`
- **Cambio de estado:** `PATCH /admin/reportes-perfil/{idReporte}/estado`
- **Notificación:** tipo `PERFIL_REPORTADO`, prioridad `ALTA`
- **No se notifica** al perfil reportado.

## 2. Alta del reporte

### 2.1. Endpoint

```http
POST /perfiles/{idPerfil}/reportes
Content-Type: application/json
```

Requiere sesión y **perfil activo** en el contexto de autenticación.
El `idPerfil` del path es el perfil que se quiere reportar.

### 2.2. Request

```json
{
  "motivo": "SPAM",
  "descripcion": "Publica lo mismo en todos los proyectos."
}
```

| Campo | Tipo | Obligatorio | Reglas |
| :--- | :--- | :---: | :--- |
| `motivo` | `string` | Sí | `SPAM`, `CONTENIDO_OFENSIVO`, `SUPLANTACION`, `PERFIL_FALSO`, `OTRO` |
| `descripcion` | `string` | No | Máx. 200 caracteres. **Obligatoria y no vacía si `motivo = OTRO`** |

### 2.3. Motivos y textos sugeridos

| Código | Texto para UI |
| :--- | :--- |
| `SPAM` | Spam o publicidad engañosa |
| `CONTENIDO_OFENSIVO` | Contenido ofensivo o inapropiado |
| `SUPLANTACION` | Suplantación de identidad |
| `PERFIL_FALSO` | Perfil falso o fraudulento |
| `OTRO` | Otro |

### 2.4. Respuesta exitosa

`204 No Content`, sin body.

### 2.5. Reglas de negocio

- El autor debe tener un **perfil activo** propio.
- El perfil reportado debe estar `ACTIVO` y su usuario también `ACTIVO`.
- No se puede reportar un perfil del mismo usuario (autorreporte).
- Un **administrador no puede reportar**.
- Un mismo perfil autor no puede tener dos reportes `PENDIENTE` sobre la misma
  entidad. Si ya existe uno, responde `409`.
- Se puede reportar a varios perfiles distintos.
- No hay límite de reportes por día.

### 2.6. Errores

| Código | Caso |
| :--- | :--- |
| `400` | Motivo inválido, descripción > 200, `OTRO` sin descripción |
| `403` | El usuario autenticado es administrador |
| `404` | Sin perfil activo, perfil reportado inexistente o no activo |
| `409` | Autorreporte o reporte pendiente duplicado |
| `401` | Sin sesión |
| `500` | Error no controlado |

Formato de error:

```json
{
  "message": "No podés reportar un perfil propio.",
  "httpStatus": 409,
  "timestamp": 1793201700000
}
```

## 3. Bandeja del administrador

### 3.1. Autenticación

Todas las rutas `/admin/reportes-perfil/**` requieren **rol global
`ADMINISTRADOR`**. No requieren permiso global adicional ni perfil activo.

Si el usuario no tiene el rol, responde `403`.

### 3.2. Listado

```http
GET /admin/reportes-perfil
    ?estado=PENDIENTE      (opcional)
    &motivo=SPAM           (opcional)
    &fechaDesde=2026-10-01 (opcional)
    &fechaHasta=2026-10-31 (opcional)
    &page=0
    &size=20
    &todos=false
```

- Orden: `fechaReporte DESC`.
- Si no se envía `estado`, devuelve todos los estados. La UI de revisión
  debería enviar `estado=PENDIENTE` por defecto.
- Respuesta: `PaginaResponse<AdminReportePerfilResumenResponse>`.

```json
{
  "contenido": [
    {
      "idReporte": 1,
      "motivo": "SPAM",
      "motivoDescripcion": "Spam o publicidad engañosa",
      "descripcion": "Publica lo mismo en todos los proyectos.",
      "estado": "PENDIENTE",
      "fechaReporte": "2026-10-29T10:00:00",
      "idPerfilReportado": 20,
      "nombrePerfilReportado": "Perfil Reportado",
      "idUsuarioAutor": 1,
      "nombreUsuarioAutor": "Luna Pérez",
      "idPerfilAutor": 10,
      "nombrePerfilAutor": "Luna Diseños"
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

### 3.3. Detalle

```http
GET /admin/reportes-perfil/{idReporte}
```

```json
{
  "idReporte": 1,
  "entidadTipo": "PERFIL",
  "entidadId": 20,
  "motivo": "SPAM",
  "motivoDescripcion": "Spam o publicidad engañosa",
  "descripcion": "Publica lo mismo en todos los proyectos.",
  "estado": "PENDIENTE",
  "fechaReporte": "2026-10-29T10:00:00",
  "perfilReportado": {
    "idPerfil": 20,
    "nombreArtistico": "Perfil Reportado",
    "profesion": "Modelo",
    "estado": "ACTIVO"
  },
  "usuarioReportado": {
    "idUsuario": 2,
    "nombre": "Ana",
    "apellido": "Gómez",
    "correo": "ana@example.com",
    "dni": "30000002",
    "estado": "Activo"
  },
  "autor": {
    "idUsuario": 1,
    "nombre": "Luna",
    "apellido": "Pérez",
    "correo": "luna@example.com",
    "dni": "30000001",
    "idPerfil": 10,
    "nombreArtistico": "Luna Diseños"
  },
  "idAdminRevisor": null,
  "nombreAdminRevisor": null,
  "fechaResolucion": null
}
```

- Los datos del perfil y del usuario son los **actuales** al momento de la
  consulta; no hay snapshot histórico.
- Desde el detalle se puede navegar al detalle de usuario existente
  (`GET /admin/usuarios/{idUsuario}`) tanto del reportado como del autor.

### 3.4. Cambio de estado

```http
PATCH /admin/reportes-perfil/{idReporte}/estado
Content-Type: application/json

{ "estado": "RESUELTO" }
```

o

```json
{ "estado": "DESCARTADO" }
```

Reglas:

- Solo se admite `PENDIENTE → RESUELTO` o `PENDIENTE → DESCARTADO`.
- `PENDIENTE` no es un estado destino válido.
- Si el reporte ya fue gestionado, responde `409`.
- El backend guarda el administrador revisor y la fecha de resolución.
- Respuesta `200 OK` con el detalle actualizado.
- La acción queda registrada en auditoría.

> La deshabilitación del perfil o del usuario **no forma parte de UC-15**.
> El administrador puede usar los endpoints administrativos existentes para
> el usuario. En esta iteración no se agregó un endpoint admin para
> deshabilitar perfiles.

## 4. Notificación al administrador

Se genera **después del commit** del reporte. Destinatarios: todos los usuarios
con rol `Administrador` y estado `Activo`.

Si no hay administradores activos, el reporte se guarda igual y se registra un
warning; no se crea ninguna notificación.

### 4.1. Contrato en `NotificacionResponse`

```json
{
  "idNotificacion": 100,
  "tipo": "PERFIL_REPORTADO",
  "titulo": "Perfil reportado",
  "mensaje": "Nuevo reporte de Luna Pérez (Luna Diseños) contra el perfil 'Perfil Reportado'. Motivo: Spam o publicidad engañosa. Descripción: Publica lo mismo en todos los proyectos.",
  "entidadTipo": "REPORTE_PERFIL",
  "entidadId": 1,
  "leida": false,
  "idPerfilContexto": null,
  "idProyecto": null
}
```

- `entidadTipo`: `REPORTE_PERFIL`.
- `entidadId`: `idReporte`.
- `idProyecto`: `null`.
- `idPerfilContexto`: `null` (el administrador no tiene perfiles).
- No se agrega un campo `idReporte`; el front debe usar `entidadId` cuando
  `entidadTipo === "REPORTE_PERFIL"`.

### 4.2. Listado y resumen

El administrador debe consultar sin `idPerfilContexto`:

```http
GET /notificaciones
GET /notificaciones/resumen
```

Así se incluyen las notificaciones con `idPerfilContexto = null`.

### 4.3. Navegación sugerida

| Condición | Destino |
| :--- | :--- |
| `tipo = PERFIL_REPORTADO` y `entidadTipo = REPORTE_PERFIL` | `/admin/reportes-perfil/{entidadId}` |
| `idProyecto != null` | No aplica a este tipo |

No mostrar link si `entidadId` es `null`.

## 5. Flujo de UI sugerido

### 5.1. Usuario que reporta

1. En el detalle de un perfil de tercero, mostrar “Reportar perfil”.
2. Abrir modal con select de motivo y textarea de descripción.
3. Si `motivo = OTRO`, exigir descripción no vacía y máximo 200.
4. Enviar `POST /perfiles/{idPerfil}/reportes`.
5. En `204`, cerrar modal y mostrar confirmación.
6. En `409`, informar que ya existe un reporte pendiente o que no puede
   reportarse a sí mismo.
7. En `403`, ocultar la acción para administradores.

### 5.2. Administrador

1. En la bandeja, cargar `GET /admin/reportes-perfil?estado=PENDIENTE`.
2. Abrir detalle con `GET /admin/reportes-perfil/{idReporte}`.
3. Revisar perfil y usuario reportado, autor y descripción.
4. Decidir y ejecutar `PATCH .../estado` con `RESUELTO` o `DESCARTADO`.
5. Refrescar la bandeja.
6. Si corresponde, usar el detalle de usuario existente para deshabilitar.

## 6. Checklist de aceptación

- [ ] El botón “Reportar perfil” no aparece en el perfil propio.
- [ ] El formulario exige motivo y descripción cuando el motivo es `OTRO`.
- [ ] La descripción no supera los 200 caracteres.
- [ ] Un `204` cierra el modal y confirma el envío.
- [ ] Un `409` informa duplicado o autorreporte.
- [ ] Un `403` se muestra solo si el usuario es administrador.
- [ ] La bandeja admin filtra por estado, motivo y fechas.
- [ ] El detalle muestra perfil reportado, usuario reportado, autor y fecha.
- [ ] Solo se ofrecen `RESUELTO` y `DESCARTADO` desde `PENDIENTE`.
- [ ] Un reporte ya gestionado no permite otra transición.
- [ ] La notificación `PERFIL_REPORTADO` navega a
      `/admin/reportes-perfil/{entidadId}`.
- [ ] El resumen de notificaciones del admin se consulta sin `idPerfilContexto`.

## 7. Notas

- No se notifica al perfil reportado.
- No hay observaciones del administrador en esta iteración.
- No hay snapshot: el detalle muestra datos actuales.
- Después de resolver o descartar, el mismo perfil autor puede volver a
  reportar la misma entidad.
