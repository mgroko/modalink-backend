# 4. Mi cuenta

La cuenta reúne tus datos personales, el estado de la suscripción y la agenda de tus perfiles. Desde el panel de usuario podés modificar tus datos, solicitar la baja de la cuenta y administrar tu calendario.

## 4.1. Modificar datos personales

**Objetivo:** actualizar tu información personal y tu ubicación.

**Quién puede hacerlo:** usuario con sesión iniciada y cuenta activa. No hace falta tener un perfil activo.

**Pasos:**

1. Abrí **Panel de usuario** y, en **Acciones**, hacé clic en **Modificar datos personales**. También podés llegar desde la barra lateral.
2. Editá **Nombre** y **Apellido**.
3. Elegí **Género**: **Mujer**, **Hombre**, **No binario** o **Prefiero no decirlo**.
4. Indicá tu **Fecha de nacimiento**.
5. En **Ubicación**, elegí **Provincia** y después **Localidad**. Podés buscar dentro de cada lista.
6. Si querés quitar la ubicación cargada, hacé clic en **Quitar ubicación**.
7. Hacé clic en **Guardar cambios**.

**Resultado esperado:** ves “Datos actualizados correctamente.” y volvés al panel de usuario. Si quitaste la ubicación, ves “Ubicación eliminada.”

**Reglas y restricciones:**

- El nombre y el apellido son obligatorios y deben tener entre 2 y 50 caracteres.
- Debés tener 18 años cumplidos o más.
- El país es fijo: Argentina.
- La localidad solo se habilita después de elegir una provincia.
- Si guardás sin localidad, el sistema pide confirmación: “Vas a guardar sin ubicación. ¿Continuar?”
- Al quitar la ubicación, el sistema pide confirmación: “¿Quitar tu ubicación?”.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “Debés tener al menos 18 años” | La fecha de nacimiento indica que sos menor de edad. | Corregí la fecha. |
| “Fecha inválida” | La fecha está incompleta o no existe. | Ingresá una fecha válida. |
| “No se pudieron cargar las provincias.” | Falló la carga del catálogo. | Recargá la página. |
| “No se pudieron cargar las localidades.” | Falló la carga del catálogo de la provincia. | Volvé a elegir la provincia. |
| “No se pudieron guardar los cambios. Intentá nuevamente.” | Problema de conexión o de validación. | Revisá los datos y volvé a intentar. |

[CAPTURA: modificar-datos-personales - formulario con datos y ubicación]

## 4.2. Solicitar la baja de la cuenta

**Objetivo:** pedir la baja de tu cuenta y de todos tus perfiles, con un plazo para arrepentirte.

**Quién puede hacerlo:** usuario con sesión iniciada que no haya solicitado la baja antes.

**Pasos:**

1. Abrí **Panel de usuario**.
2. En la barra lateral, hacé clic en **Solicitar baja del sistema**.
3. Leé el aviso y hacé clic en **Solicitar baja**.

**Resultado esperado:** la sesión se cierra y volvés a la pantalla de inicio de sesión con el aviso “Solicitud de baja registrada.” y la fecha límite para reactivar la cuenta.

**Reglas y restricciones:**

- El plazo para reactivar es el configurado por la administración (30 días por defecto).
- Durante el plazo, tus perfiles dejan de ser visibles para la comunidad y la cuenta no puede iniciar sesión.
- La reactivación se hace iniciando sesión y usando **Reactivar cuenta** (ver “Reactivar una cuenta pendiente de baja”).
- Si no reactivás la cuenta dentro del plazo, el sistema la elimina definitivamente.
- No se puede volver a solicitar la baja si ya hay una solicitud activa.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “Ya existe una solicitud de baja activa.” | Ya pediste la baja y el plazo sigue vigente. | Iniciá sesión y reactivá la cuenta. |
| “La cuenta ya fue dada de baja.” | El plazo venció y la cuenta ya está de baja. | Contactá a la administración. |
| “Error al solicitar la baja. Intentá nuevamente.” | Problema de conexión. | Volvé a intentar. |

[CAPTURA: confirmar-baja-cuenta - modal con el plazo y los botones Cancelar y Solicitar baja]

## 4.3. Consultar el calendario

**Objetivo:** ver tu disponibilidad semanal y las actividades de tus proyectos.

**Quién puede hacerlo:** usuario con perfil activo.

**Pasos:**

1. Abrí **Calendario** desde la barra lateral o desde el panel de usuario.
2. Navegá entre semanas con los botones **Semana anterior** y **Semana siguiente**, o volvé a la semana actual con **Hoy**.
3. Hacé clic en un día para ver el detalle.

**Resultado esperado:** ves una grilla semanal con:

- Tus **actividades de proyecto**.
- Tus **bloqueos manuales**.
- Las franjas **Disponible** y **No laborable**.
- El detalle del día con cada bloqueo o actividad.

**Reglas y restricciones:**

- La agenda es por perfil: cada perfil tiene su propia disponibilidad.
- En el calendario de tu propio perfil podés configurar la jornada y agregar bloqueos. En el calendario de un perfil ajeno solo se consulta.
- El botón **Agregar bloque** y la configuración de jornada solo aparecen si el perfil es propio y está activo.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “Este perfil no tiene una agenda configurada.” | El perfil no tiene jornada cargada. | Configurá la jornada laboral. |
| “No se pudo cargar el calendario.” | Problema de conexión. | Recargá la página. |

[CAPTURA: calendario - grilla semanal con leyenda y acciones Configurar jornada y Agregar bloque]

## 4.4. Configurar la jornada laboral

**Objetivo:** definir en qué días y horarios trabajás, y cuánto margen necesitás entre actividades.

**Quién puede hacerlo:** perfil activo propio.

**Pasos:**

1. En **Calendario**, hacé clic en **Configurar jornada**.
2. Indicá el **Margen total entre actividades (min)**. La ayuda explica: “Separación total entre el fin de una actividad y el inicio de la siguiente.”
3. Marcá los días en los que trabajás: **Lunes** a **Domingo**.
4. Para cada día habilitado, completá **Inicio de jornada** y **Fin de jornada**.
5. Si trabajás en dos bloques, activá **Jornada partida** y completá **Inicio mañana**, **Fin mañana**, **Inicio tarde** y **Fin tarde**.
6. Hacé clic en **Guardar jornada**. Mientras se guarda, el botón muestra “Guardando y reprogramando…”.

**Resultado esperado:** ves “Jornada laboral actualizada correctamente.” o un resumen de **Reprogramación automática** con los proyectos que se movieron.

**Reglas y restricciones:**

- Tenés que habilitar al menos un día.
- El horario de fin debe ser posterior al de inicio.
- En jornada partida, el bloque de la tarde debe comenzar después de que termine el de la mañana.
- El margen se expresa en minutos y no puede ser negativo.
- Al guardar la jornada o el margen, el sistema reprograma automáticamente las actividades pendientes de los proyectos confirmados en los que participás.
- Si algún proyecto no se puede reprogramar, se muestra en **Proyectos que no se pudieron reprogramar** y podés usar **Reintentar reprogramación**.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “Debes habilitar al menos un día en la jornada laboral.” | No marcaste ningún día. | Marcá al menos un día. |
| “El horario de fin debe ser posterior al horario de inicio.” | El fin es anterior o igual al inicio. | Corregí los horarios. |
| “El fin de la mañana debe ser posterior a su inicio.” | El bloque de la mañana está invertido. | Corregí el horario. |
| “El bloque de la tarde debe comenzar después del fin del bloque de la mañana.” | Los bloques se superponen. | Ajustá el inicio de la tarde. |
| “No se puede guardar la jornada: falta definir el margen efectivo. Contactá al administrador.” | Falta una configuración del sistema. | Contactá a la administración. |

[CAPTURA: configurar-jornada - modal con margen, días y horarios]

## 4.5. Agregar un bloqueo de disponibilidad

**Objetivo:** marcar un período en el que no vas a estar disponible.

**Quién puede hacerlo:** perfil activo propio.

**Pasos:**

1. En **Calendario**, hacé clic en **Agregar bloque**.
2. Completá **Fecha y hora de inicio** y **Fecha y hora de fin**.
3. Escribí el **Motivo**. Ejemplo: “Vacaciones, compromiso personal...”.
4. Hacé clic en **Agregar bloqueo**.

**Resultado esperado:** ves “Bloqueo creado correctamente.” y el bloqueo aparece en la grilla y en el detalle del día.

**Reglas y restricciones:**

- El motivo es obligatorio y admite hasta 200 caracteres.
- La fecha y hora de fin deben ser posteriores a las de inicio.
- No se puede superponer con otro bloqueo manual.
- No se puede crear un bloqueo sobre un horario ya comprometido por una actividad de un proyecto activo.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “La fecha y hora de fin debe ser posterior a la de inicio.” | El rango está invertido. | Corregí las fechas. |
| “El periodo se superpone con un bloqueo existente de tu calendario.” | Ya hay un bloqueo en ese horario. | Elegí otro horario o eliminá el bloqueo existente. |
| “El periodo ya se encuentra bloqueado automáticamente por una actividad de un proyecto activo.” | Hay una actividad agendada en ese horario. | Elegí otro horario. |
| “No se pudo crear el bloqueo.” | Problema de conexión. | Volvé a intentar. |

[CAPTURA: agregar-bloqueo - modal con fechas, motivo y botón Agregar bloqueo]

## 4.6. Eliminar un bloqueo

**Objetivo:** volver a marcar como disponible un horario bloqueado manualmente.

**Quién puede hacerlo:** perfil activo dueño del bloqueo.

**Pasos:**

1. En **Calendario**, seleccioná el día y ubicá el bloqueo.
2. Hacé clic en el ícono de eliminar del bloqueo.
3. Confirmá con **Eliminar**.

**Resultado esperado:** ves “Bloqueo eliminado. Horario marcado como disponible.” y el bloqueo desaparece.

**Reglas y restricciones:**

- No se puede eliminar un bloqueo si el horario quedó comprometido con una actividad de un proyecto activo.
- La acción no se puede deshacer.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “No se puede marcar como disponible un horario comprometido con una actividad de un proyecto activo.” | Hay una actividad agendada en ese horario. | No elimines el bloqueo o reprogramá la actividad. |
| “El bloqueo no existe o no pertenece a tu calendario.” | El bloqueo ya fue eliminado. | Recargá la página. |

[CAPTURA: eliminar-bloqueo - modal de confirmación]
