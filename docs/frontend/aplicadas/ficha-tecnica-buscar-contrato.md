# Ficha técnica — UC-68 Buscar contrato

**Módulo:** Contratos  
**Actor:** Director de proyecto / Miembro de proyecto  
**Estado backend:** implementado y testeado con tests unitarios, MVC e integración con PostgreSQL (Testcontainers).

---

## 1. Resumen

El usuario autenticado busca contratos asociados a proyectos donde participa. La búsqueda es global, paginada y con filtros opcionales. El listado devuelve un resumen sin el texto legal completo; el texto se obtiene desde el detalle.

---

## 2. Visibilidad

- **Director activo del proyecto:** ve todos los contratos del proyecto, aunque no sea firmante.
- **Miembro del proyecto:** ve solo los contratos donde su perfil activo es parte firmante.
- **Miembro activo no firmante:** no ve contratos ajenos.
- **Usuario ajeno al proyecto:** no ve ningún contrato del proyecto.
- **Exmiembro que fue parte:** conserva visibilidad de sus contratos históricos porque la firma permanece.

La visibilidad se aplica en la búsqueda y en el detalle.

---

## 3. Endpoints

### 3.1. Búsqueda

```text
GET /contratos/buscar
```

Parámetros:

```text
idProyecto, estados, parte, fechaEmisionDesde, fechaEmisionHasta,
fechaFirmaDesde, fechaFirmaHasta, idTipoAcuerdo, nombreContrato,
page=0, size=20
```

Reglas:

- `estados` vacío aplica el conjunto por defecto: `PENDIENTE_FIRMA`, `ACTIVO`, `RECHAZADO_POR_PARTES`, `CANCELADO`, `ELIMINADO`.
- `ELIMINADO` se incluye por defecto para que las bajas lógicas sigan visibles. El cliente puede excluirlo enviando explícitamente la lista de estados.
- `BORRADOR` se excluye por defecto; se puede pedir explícitamente.
- `parte` busca por nombre artístico, nombre y apellido. No se usa DNI.
- `fechaEmisionDesde/Hasta` y `fechaFirmaDesde/Hasta` son rangos inclusivos por día.
- El filtro de firma matchea si cualquier firma `FIRMADO` cae en el rango.
- `size` máximo: 50. `page` negativo se ajusta a 0; `size <= 0` usa 20; `size > 50` se limita a 50.
- Orden por defecto: `fechaEmision DESC, idContrato DESC`.

Respuesta `200`:

```json
{
  "contenido": [
    {
      "idContrato": 55,
      "nombre": "Cesión de derechos de imagen - Editorial Primavera",
      "idProyecto": 10,
      "nombreProyecto": "Editorial Primavera",
      "estadoProyecto": "PUBLICADO",
      "idTipoAcuerdo": 1,
      "codigoTipoAcuerdo": "CESION_DERECHOS_IMAGEN",
      "nombreTipoAcuerdo": "Cesión de derechos de imagen",
      "estado": "PENDIENTE_FIRMA",
      "fechaCreacion": "2026-11-07T12:00:00",
      "fechaEmision": "2026-11-07T12:00:00",
      "fechaModificacion": null,
      "fechaBaja": null,
      "versionActual": 1,
      "emisor": {
        "idMiembro": 3,
        "idPerfil": 7,
        "nombre": "Ana Pérez"
      },
      "partes": [
        {
          "idMiembro": 3,
          "idPerfil": 7,
          "nombre": "Ana Pérez",
          "rolParte": "DIRECTOR",
          "estadoFirma": "PENDIENTE",
          "fechaFirma": null
        }
      ],
      "miEstadoFirma": "PENDIENTE",
      "miFechaFirma": null,
      "puedeModificar": true
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

Sin resultados: `200` con página vacía. El frontend muestra el estado vacío.

### 3.2. Proyectos con contratos visibles (filtro)

```text
GET /contratos/proyectos
```

Devuelve las opciones del filtro "Proyecto" para el perfil activo, ordenadas por nombre:

```json
[
  {
    "idProyecto": 10,
    "nombre": "Editorial Primavera"
  }
]
```

Reglas:

- Solo incluye proyectos con al menos un contrato visible para el perfil activo: como director activo o como parte firmante.
- Incluye proyectos históricos donde el perfil ya no tiene membresía activa pero conserva una firma de contrato.
- No incluye proyectos sin contratos visibles, aunque el perfil sea miembro activo no firmante.
- Requiere perfil activo; si no hay, responde `404 PERFIL_ACTIVO_NO_SELECCIONADO`.

### 3.3. Detalle

```text
GET /proyectos/{idProyecto}/contratos/{idContrato}
```

Devuelve el texto legal, partes, firmas, emisor, estado, versión y flags:

- `puedeModificarse`
- `motivoNoModificable`: `SIN_PERMISO`, `CONTRATO_NO_PENDIENTE_FIRMA`, `PROYECTO_NO_APTO_PARA_CONTRATO`, `CONTRATO_CON_FIRMAS_REGISTRADAS`.
- `miEstadoFirma`, `miFechaFirma`.

### 3.3. Historial de eventos

```text
GET /proyectos/{idProyecto}/contratos/{idContrato}/eventos
```

Devuelve los eventos de `contrato_evento` ordenados cronológicamente:

```json
[
  {
    "idEvento": 1,
    "tipoEvento": "FIRMADO",
    "fechaHora": "2026-11-08T10:00:00",
    "idMiembro": 11,
    "idPerfil": 7,
    "nombreParte": "Ana Pérez",
    "idUsuario": 1,
    "nombreOperador": "Ana Gómez",
    "version": 1,
    "motivo": null
  }
]
```

Visibilidad: director activo del proyecto o parte firmante, incluido un exmiembro que conserva su firma. Un usuario ajeno recibe `403 ACCESO_DENEGADO_PROYECTO`.

---

## 4. Errores

| HTTP | Código | Situación |
|---|---|---|
| `400` | `RANGO_FECHAS_CONTRATO_INVALIDO` | `desde > hasta` |
| `403` | `ACCESO_DENEGADO_PROYECTO` | El perfil no puede ver el contrato |
| `404` | `PERFIL_ACTIVO_NO_SELECCIONADO` | Falta perfil activo |
| `404` | `CONTRATO_NO_ENCONTRADO` | Contrato inexistente o de otro proyecto |
| `409` | `PERFIL_EN_BAJA` | Perfil no activo |

---

## 5. Implementación

- `ContratoBusquedaRepository` + `ContratoBusquedaRepositoryImpl` con Criteria API.
- `ContratoSpecifications` con visibilidad y filtros mediante `EXISTS`.
- `ContratoSpecifications.conVisibilidad` se reutiliza para `GET /contratos/proyectos`, de modo que el combo del filtro y la búsqueda no diverjan.
- Proyección `ContratoResumenProyeccion` sin `texto`.
- Carga batch de partes con `FirmaContratoRepository.findConPartesPorContratos`.
- Cálculo batch de `puedeModificar` con `MiembroProyectoRepository.findIdsProyectosDondeEsDirectorActivo`.
- `BuscarContratoService` y `ObtenerContratoService`.
- `ListarEventosContratoService` + `ContratoEventoRepository.findByContratoConDetalle`.
- Ruta `/contratos/**` habilitada para rol `USUARIO` en `SecurityConfig`.

---

## 6. Pruebas

- `BuscarContratoIntegrationTest`: visibilidad director/miembro/ajeno, filtros, estados por defecto, `BORRADOR`/`ELIMINADO`, firma, firma parcial, detalle, errores, proyectos visibles con histórico de parte firmante, espejo en auditoría e historial visible para partes y exmiembros.
- `ContratoControllerTest`: `GET /contratos/buscar`, `GET /contratos/proyectos`, detalle e historial de eventos.
- Regresión de UC-39 y de los triggers de contrato.

---

## 7. Notas

- El listado no carga el texto legal; el detalle sí.
- La búsqueda exige perfil activo, consistente con el resto de la API.
- Si se necesita permitir perfiles en baja para consultar historial, se ajusta la validación de perfil.
