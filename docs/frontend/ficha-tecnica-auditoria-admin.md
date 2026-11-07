# Ficha técnica — UC-67 Informe de auditoría

**Módulo:** Administración
**Actor:** Administrador
**Estado backend:** implementado y testeado con tests unitarios, MVC, integración con PostgreSQL (Testcontainers) y prueba de carga con 100.000 registros.
**Estado frontend:** pendiente de implementación.

---

## 1. Resumen

El administrador consulta el registro de eventos críticos de la plataforma en un
rango de fechas, con filtro multi-selección por tipo de evento.

La pantalla debe:

1. pedir un rango de fechas;
2. permitir seleccionar uno o varios tipos de evento (o ninguno = todos);
3. listar los eventos paginados, del más reciente al más antiguo;
4. permitir expandir un evento para ver el detalle campo a campo;
5. informar claramente cuando el rango supera el máximo permitido.

El backend no modifica datos: es una consulta de solo lectura.

---

## 2. Endpoints

### 2.1. Listado de auditoría

```http
GET /admin/auditoria
```

**Autenticación:** cookie `jwt` + header `X-XSRF-TOKEN`.
**Autorización:** solo rol `ADMINISTRADOR`.

#### Query params

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `fechaDesde` | `date` ISO (`YYYY-MM-DD`) | Sí | Inicio del rango, inclusive |
| `fechaHasta` | `date` ISO (`YYYY-MM-DD`) | Sí | Fin del rango, inclusive |
| `tipos` | `string` repetible o separado por comas | No | Códigos de evento. Si no se envía, devuelve todos los activos |
| `page` | `int` | No | Página, base 0. Default `0` |
| `size` | `int` | No | Tamaño, `1..100`. Default `20` |

Ejemplo:

```http
GET /admin/auditoria?fechaDesde=2026-11-01&fechaHasta=2026-11-07&tipos=USUARIO_ALTA,USUARIO_BAJA&page=0&size=20
```

#### Respuesta `200`

```json
{
  "contenido": [
    {
      "idAuditoria": 10,
      "fechaHora": "2026-11-07T10:15:30",
      "tipoEvento": "USUARIO_DESHABILITADO",
      "tipoEventoNombre": "Deshabilitación de usuario",
      "entidadAfectada": "usuario",
      "entidadClave": null,
      "idAfectado": 45,
      "idUsuario": 1,
      "usuarioNombre": "Admin ModaLink",
      "usuarioBd": "modalink_api",
      "ipUsuario": "203.0.113.10",
      "detalle": [
        {
          "campo": "estado",
          "valorAnterior": "ACTIVO",
          "valorNuevo": "DESHABILITADO"
        },
        {
          "campo": "motivo_deshabilitacion",
          "valorAnterior": null,
          "valorNuevo": "Incumplimiento"
        }
      ]
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

#### Campos de la respuesta

| Campo | Descripción |
|---|---|
| `idAuditoria` | Identificador del evento |
| `fechaHora` | Fecha y hora del evento (`timestamp` sin zona) |
| `tipoEvento` | Código del catálogo |
| `tipoEventoNombre` | Nombre legible del tipo |
| `entidadAfectada` | Tabla o entidad afectada (`usuario`, `proyecto`, `postulacion_gral`, etc.) |
| `entidadClave` | Clave textual para entidades sin ID numérico (`configuracion_sistema`) |
| `idAfectado` | ID de la entidad afectada; `0` para configuración |
| `idUsuario` | Operador real que ejecutó la acción; `null` si fue un scheduler |
| `usuarioNombre` | Nombre y apellido del operador, si existe |
| `usuarioBd` | Rol de BD (`modalink_api` en producción) |
| `ipUsuario` | IP registrada por la conexión a BD |
| `detalle` | Lista de cambios `campo` / `valorAnterior` / `valorNuevo` |

### 2.2. Catálogo de tipos

```http
GET /admin/auditoria/tipos
```

**Autorización:** solo rol `ADMINISTRADOR`.

Respuesta `200`:

```json
[
  {
    "codigo": "USUARIO_ALTA",
    "nombre": "Alta de usuario",
    "descripcion": "Registro de un nuevo usuario",
    "modulo": "USUARIOS"
  }
]
```

La UI puede agrupar las opciones por `modulo`.

---

## 3. Catálogo de eventos

| Código | Nombre | Módulo |
|---|---|---|
| `USUARIO_ALTA` | Alta de usuario | USUARIOS |
| `USUARIO_BAJA` | Baja de usuario | USUARIOS |
| `USUARIO_DESHABILITADO` | Deshabilitación de usuario | USUARIOS |
| `USUARIO_HABILITADO` | Habilitación de usuario | USUARIOS |
| `USUARIO_ROL_CAMBIADO` | Cambio de rol de usuario | USUARIOS |
| `PROYECTO_ESTADO_CAMBIADO` | Cambio de estado de proyecto | PROYECTOS |
| `POSTULACION_ESTADO_CAMBIADO` | Cambio de estado de postulación | POSTULACIONES |
| `POSTULACION_ELIMINADA` | Baja de postulación | POSTULACIONES |
| `INVITACION_ESTADO_CAMBIADO` | Cambio de estado de invitación | INVITACIONES |
| `INVITACION_ELIMINADA` | Baja de invitación | INVITACIONES |
| `CONFIGURACION_ACTUALIZADA` | Cambio de configuración | ADMINISTRACION |
| `REPORTE_ESTADO_CAMBIADO` | Alta o resolución de reporte | REPORTES |

---

## 4. Errores

Todos los errores usan el cuerpo estándar:

```json
{
  "codigo": "RANGO_FECHAS_EXCEDIDO",
  "message": "El rango máximo permitido es de 90 días.",
  "httpStatus": 400,
  "timestamp": 1762500000000
}
```

| HTTP | `codigo` | Cuándo |
|---|---|---|
| `400` | `FECHAS_OBLIGATORIAS` | Falta `fechaDesde` o `fechaHasta` |
| `400` | `RANGO_FECHAS_INVALIDO` | `fechaDesde > fechaHasta` |
| `400` | `RANGO_FECHAS_EXCEDIDO` | El rango supera `AUDITORIA_RANGO_MAX_DIAS` |
| `400` | `TIPO_EVENTO_INVALIDO` | Algún código de `tipos` no existe o está inactivo |
| `400` | `PAGINACION_INVALIDA` | `page < 0`, `size <= 0` o `size > 100` |
| `401` | — | Sesión ausente o expirada |
| `403` | `ROL_NO_PERMITIDO` | El usuario no es Administrador |

### 4.1. Tabla de decisión de filtros

| Caso | Resultado esperado |
|---|---|
| Sin fechas | Mostrar error `FECHAS_OBLIGATORIAS` |
| Solo una fecha | Mostrar error `FECHAS_OBLIGATORIAS` |
| `fechaDesde > fechaHasta` | Mostrar error `RANGO_FECHAS_INVALIDO` |
| Rango mayor al máximo | Mostrar error `RANGO_FECHAS_EXCEDIDO` con el máximo vigente |
| Sin tipos seleccionados | Consultar todos los tipos activos |
| Un tipo seleccionado | Filtrar por ese código |
| Varios tipos | Filtrar por la unión, sin duplicados |
| Tipo inexistente | Mostrar error `TIPO_EVENTO_INVALIDO` |
| Página fuera de rango | Mostrar página vacía |
| `size > 100` o `size <= 0` | Mostrar error `PAGINACION_INVALIDA` |

---

## 5. Recomendaciones de UI

### 5.1. Filtros

- Dos `date picker`: **Desde** y **Hasta**.
- Selector multi-selección de tipos, agrupado por `modulo`.
- Botón **Limpiar filtros** que restablece el rango a los últimos 7 días y deja los tipos vacíos.
- Mostrar el rango máximo vigente como texto de ayuda: “Máximo 90 días”.
- Validar en el front antes de enviar: fechas obligatorias, desde ≤ hasta, rango ≤ máximo.

### 5.2. Tabla

Columnas sugeridas:

| Columna | Fuente |
|---|---|
| Fecha y hora | `fechaHora` |
| Evento | `tipoEventoNombre` (fallback `tipoEvento`) |
| Entidad | `entidadAfectada` + `idAfectado` |
| Operador | `usuarioNombre` (fallback `usuarioBd` o “Sistema”) |
| IP | `ipUsuario` |
| Detalle | Botón/chevron para expandir `detalle[]` |

- Orden fijo del backend: más reciente primero.
- Paginación con tamaño 20 por defecto.
- Estado vacío: “No hay eventos de auditoría para los filtros seleccionados.”
- Estado de carga: skeleton o spinner.
- En móvil, priorizar fecha, evento y operador; el detalle se abre en un panel.

### 5.3. Detalle expandido

Mostrar una tabla de dos columnas:

| Campo | Valor anterior | Valor nuevo |
|---|---|---|
| `estado` | `ACTIVO` | `DESHABILITADO` |
| `motivo_deshabilitacion` | — | `Incumplimiento` |

- Los valores `null` se muestran como “—”.
- Los textos largos se truncan con opción de copiar.
- No mostrar `password_hash`, tokens ni secretos: el backend no los envía.

### 5.4. Acceso

- La pantalla debe estar dentro del área de administración.
- Si el usuario no es Administrador, el backend responde `403`; la UI debe redirigir o mostrar “No tenés permiso”.
- No hay exportación en el MVP.

---

## 6. Ejemplos de uso

### 6.1. Últimos 7 días, todos los eventos

```http
GET /admin/auditoria?fechaDesde=2026-11-01&fechaHasta=2026-11-07&page=0&size=20
```

### 6.2. Solo usuarios deshabilitados

```http
GET /admin/auditoria?fechaDesde=2026-11-01&fechaHasta=2026-11-07&tipos=USUARIO_DESHABILITADO
```

### 6.3. Varios tipos

```http
GET /admin/auditoria?fechaDesde=2026-11-01&fechaHasta=2026-11-07&tipos=PROYECTO_ESTADO_CAMBIADO,POSTULACION_ESTADO_CAMBIADO,INVITACION_ESTADO_CAMBIADO
```

---

## 7. Fuera de alcance

- Exportación CSV/PDF.
- Auditoría de consultas o exportaciones.
- Login/logout y accesos denegados.
- Retención y purga.
- Edición o borrado de eventos (el log es inmutable).

---

## 8. Referencias

- `docs/3_diseño/auditoria_mvp.md`
- `docs/calidad/controles_bd.md`
- `docs/2_analisis/contratos.md` (UC-67)
- `docs/5_estado_sistema/estado_uc.md` (UC-67)
