# Ficha técnica — Historial de contratos del proyecto

**Módulo:** Contratos / Historial
**Actor:** Director activo o miembro activo del proyecto

---

## 1. Resumen

La pestaña Historial del proyecto incorpora una sección **Contratos** con los
eventos registrados en `contrato_evento` para los contratos del proyecto.

- **Director activo:** ve todos los eventos de contratos del proyecto.
- **Miembro activo:** ve únicamente los eventos de los contratos donde su
  perfil es o fue parte firmante.
- **Usuario ajeno:** recibe `403`.

La vista incluye contratos eliminados y contratos de proyectos finalizados o
cancelados. Es una vista de solo lectura: no habilita firmar, modificar ni
eliminar.

---

## 2. Endpoint

```text
GET /proyectos/{idProyecto}/historial/contratos
```

Parámetros:

```text
tipo, nombre, page=0, size=20
```

- `tipo`: valor de `TipoEventoContrato` (`CREADO`, `EMITIDO`, `MODIFICADO`,
  `FIRMADO`, `RECHAZADO`, `ACTIVADO`, `CANCELADO`, `ELIMINADO`,
  `FIRMA_CANCELADA`, `FIRMA_INVALIDADA`, `MIEMBRO_BAJA`).
- `nombre`: búsqueda parcial por nombre del contrato.
- `size` máximo: 100.

Respuesta `200`:

```json
{
  "contenido": [
    {
      "idEvento": 77,
      "idContrato": 55,
      "nombreContrato": "Préstamo de indumentaria - Colección",
      "idProyecto": 16,
      "nombreProyecto": "Colección Coquetas",
      "idTipoAcuerdo": 3,
      "codigoTipoAcuerdo": "PRESTAMO",
      "nombreTipoAcuerdo": "Préstamo de indumentaria y materiales",
      "tipoEvento": "ELIMINADO",
      "fechaHora": "2026-11-08T12:00:00",
      "idMiembro": null,
      "idPerfil": null,
      "nombreParte": null,
      "idUsuario": 1,
      "nombreOperador": "Ana Gómez",
      "version": 1,
      "motivo": null,
      "estadoContrato": "ELIMINADO",
      "estadoProyecto": "FINALIZADO",
      "fechaBaja": "2026-11-08T12:00:00"
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

---

## 3. Reglas

1. El perfil activo debe estar en estado `Activo`.
2. El proyecto debe existir.
3. El Director activo ve todos los eventos del proyecto.
4. Un miembro activo ve solo contratos donde su perfil figura en
   `firma_contrato`; la firma puede pertenecer a una membresía ya inactiva.
5. No se filtra por estado del contrato: los eliminados y los históricos
   también se muestran.
6. `tipo` inválido, `page < 0` o `size` fuera de rango devuelven `400`.
7. Un usuario sin membresía activa ni rol Director recibe `403`.

---

## 4. Frontend

- `HistorialProyecto.vue` es el contenedor con las secciones **Miembros** y
  **Contratos**.
- `HistorialMiembrosProyecto.vue` mantiene el historial de membresías y sigue
  siendo exclusivo del Director.
- `HistorialContratosProyecto.vue` consume el nuevo endpoint, muestra filtros,
  paginación, estado actual del contrato y enlace al detalle.
- La sección Contratos está disponible también para miembros activos.

---

## 5. Tests

- `HistorialProyectoControllerTest`: endpoint y parámetros.
- `HistorialContratosIntegrationTest`: visibilidad Director/miembro/ajeno,
  filtros, paginación y evento `ELIMINADO`.
- `BuscarContratoIntegrationTest`: `ELIMINADO` incluido por defecto.
