# Ficha técnica: Configurar términos y condiciones (UC-23)

## 1. Alcance

El usuario configura los términos y condiciones de su perfil. El texto se
versiona: cada cambio real cierra la versión vigente y crea una nueva. La
versión vigente es pública para la comunidad y se usa como referencia cuando un
proyecto incorpora al perfil.

- **Actor:** Usuario.
- **Precondición:** sesión iniciada y perfil en estado `Activo`.
- **Postcondición:** la nueva versión queda asociada al perfil y pasa a ser la
  vigente.

## 2. Endpoints

| Método | Endpoint | Descripción | Autorización |
|---|---|---|---|
| GET | `/perfiles/{idPerfil}/terminos-condiciones` | Devuelve la versión vigente o la plantilla base | Usuario autenticado |
| PUT | `/perfiles/{idPerfil}/terminos-condiciones` | Guarda una nueva versión | Dueño del perfil |

### 2.1. GET

Respuesta con versión vigente:

```json
{
  "idPerfil": 12,
  "idTyc": 7,
  "descripcion": "Mis condiciones de trabajo...",
  "fechaInicio": "2026-11-06T10:15:00",
  "fechaFin": null,
  "esPlantilla": false
}
```

Respuesta sin versión configurada:

```json
{
  "idPerfil": 12,
  "idTyc": null,
  "descripcion": "Mis condiciones de trabajo...",
  "fechaInicio": null,
  "fechaFin": null,
  "esPlantilla": true
}
```

La plantilla se muestra como texto base. No se persiste hasta que el usuario
presiona “Guardar”.

### 2.2. PUT

```json
{
  "descripcion": "Texto de los términos y condiciones..."
}
```

Reglas:

- `descripcion` obligatoria, no vacía, máximo **5000 caracteres**.
- Solo perfiles `Activo`. `PendienteBaja`, `Baja` y `Deshabilitado` responden
  `409`.
- Perfil ajeno o inexistente: `404`.
- Si el texto es igual a la versión vigente, no se crea una versión nueva.

## 3. Pantalla de configuración

1. Al entrar, llamar a `GET /perfiles/{idPerfil}/terminos-condiciones`.
2. Mostrar el texto en un editor/textarea.
3. Si `esPlantilla = true`, mostrar un aviso: “Estás usando la plantilla base
   del sistema. Guardá para convertirla en tus condiciones”.
4. Mostrar contador de caracteres con máximo 5000.
5. Al guardar, llamar a `PUT`.
6. Si el backend responde `400`, mostrar los errores por campo sin perder el
   texto editado.
7. Si responde `409`, mostrar el mensaje y recargar la versión vigente.

## 4. Visualización pública

`GET /perfiles/{idPerfil}` incluye:

```json
{
  "...": "...",
  "terminosCondiciones": {
    "idPerfil": 12,
    "idTyc": 7,
    "descripcion": "Mis condiciones de trabajo...",
    "fechaInicio": "2026-11-06T10:15:00",
    "fechaFin": null,
    "esPlantilla": false
  }
}
```

Si `esPlantilla = true`, la UI debe aclarar que se trata de la plantilla base
del sistema y no de condiciones configuradas por el perfil.

## 5. Aceptación en flujos de incorporación

Cuando un proyecto incorpora a un integrante que tiene T&C vigentes, el
director debe aceptar esa versión.

### 5.1. Invitación (UC-31)

Request individual:

```json
{
  "idPerfilDestinatario": 44,
  "idRequerimientoGral": null,
  "mensaje": "Te invitamos al proyecto",
  "aceptacionTerminos": {
    "idTyc": 7,
    "aceptaTerminos": true
  }
}
```

Request batch:

```json
{
  "idsPerfilDestinatario": [44, 45],
  "mensaje": "Te invitamos al proyecto",
  "aceptacionesTerminos": [
    { "idPerfil": 44, "idTyc": 7, "aceptaTerminos": true },
    { "idPerfil": 45, "idTyc": null, "aceptaTerminos": false }
  ]
}
```

Si el perfil no tiene T&C configurados, no se envía aceptación.

### 5.2. Aceptar postulación (UC-34)

```json
{
  "aceptacionTerminos": {
    "idTyc": 7,
    "aceptaTerminos": true
  }
}
```

### 5.3. Aprobar reemplazo (UC-38 bis)

```json
{
  "aceptacionTerminos": {
    "idTyc": 7,
    "aceptaTerminos": true
  }
}
```

### 5.4. Errores de aceptación

```json
{
  "codigo": "TERMINOS_DESACTUALIZADOS",
  "message": "Los términos y condiciones del perfil cambiaron. Revisá la versión vigente antes de continuar.",
  "idPerfil": 44,
  "idTycVigente": 8,
  "descripcionVigente": "Texto actualizado...",
  "esPlantilla": false,
  "httpStatus": 409
}
```

La UI debe:

1. Mostrar la versión vigente devuelta.
2. Pedir al director que la revise y acepte nuevamente.
3. Reintentar la operación con el nuevo `idTyc`.

## 6. DTOs expuestos

- `TerminosCondicionesResponse`
- `ActualizarTerminosCondicionesRequest`
- `AceptacionTerminosRequest`
- `AceptacionTerminosPorPerfilRequest`
- `InvitacionResponse.idTycAceptado`
- `InvitacionBusquedaResponse.idTycAceptado`
- `MiembroProyectoResponse.idTycAceptado`
- `PostulacionGestionadaResponse.idTycAceptado`
- `CandidatoReemplazoResponse.idTycAceptado`

## 7. Casos borde

- Perfil sin T&C: se muestra la plantilla y no se exige aceptación.
- Perfil `PendienteBaja`: no puede iniciar sesión ni configurar T&C.
- Cambio de T&C después de una incorporación: la membresía conserva la versión
  aceptada; no se exige re-aceptación.
- Invitaciones históricas sin `id_tyc_aceptado`: quedan con valor `null`.
- Si el candidato cambia sus T&C entre la invitación y la aceptación, la
  membresía conserva la versión que el director aceptó al invitar.
