# Diagrama de clases del backend

Este diagrama representa el diseño del backend de ModaLink según la metodología
UP y la arquitectura de capas utilizada por Spring Boot. La vista está
deliberadamente acotada a los componentes transversales y a los agregados
principales del dominio; los módulos restantes siguen el mismo patrón.

## Diagrama de clases UML

```mermaid
classDiagram
direction LR

namespace Presentacion {
  class AuthController {
    +registro(request)
    +login(request)
    +iniciarGoogle()
    +obtenerSesion()
  }
  class PerfilController {
    +obtenerPerfil(id)
    +crearPerfil(request)
    +modificarPerfil(id, request)
  }
  class ProyectoController {
    +crearProyecto(request)
    +obtenerProyecto(id)
    +modificarProyecto(id, request)
  }
  class ActividadController {
    +crearActividad(request)
    +modificarActividad(id, request)
    +eliminarActividad(id)
  }
  class PostulacionController {
    +listarPostulaciones(idProyecto)
    +postular(request)
    +gestionar(id, estado)
  }
  class ContratoController {
    +generar(request)
    +firmar(id)
    +rechazar(id)
  }
}

namespace Aplicacion {
  class AuthService {
    +registrar(request)
    +login(request)
    +obtenerUsuarioAutenticado()
  }
  class PerfilService {
    +crear(request)
    +buscar(id)
    +modificar(id, request)
  }
  class ProyectoService {
    +crear(request)
    +buscar(id)
    +modificar(id, request)
  }
  class ActividadService {
    +crear(request)
    +modificar(id, request)
    +eliminar(id)
  }
  class PostulacionService {
    +listar(idProyecto)
    +crear(request)
    +gestionar(id, estado)
  }
  class ContratoService {
    +generar(request)
    +firmar(id)
    +rechazar(id)
  }
  class CalendarioService {
    +obtenerAgenda(idPerfil)
    +crearBloqueo(request)
    +reprogramar(request)
  }
}

namespace Persistencia {
  class UsuarioRepository {
    <<interface>>
  }
  class PerfilRepository {
    <<interface>>
  }
  class ProyectoRepository {
    <<interface>>
  }
  class ActividadRepository {
    <<interface>>
  }
  class PostulacionActividadRepository {
    <<interface>>
  }
  class ContratoRepository {
    <<interface>>
  }
  class AgendaRepository {
    <<interface>>
  }
  class RepositoryBase {
    <<Spring Data JPA>>
    +save(entity)
    +findById(id)
    +delete(entity)
  }
}

namespace Dominio {
  class Usuario {
    +idUsuario : Long
    +nombre : String
    +correo : String
    +estado : EstadoUsuario
  }
  class Perfil {
    +idPerfil : Long
    +nombreArtistico : String
    +biografia : String
    +estado : EstadoPerfil
  }
  class Proyecto {
    +idProyecto : Long
    +nombre : String
    +estado : EstadoProyecto
    +privacidad : Privacidad
  }
  class MiembroProyecto {
    +idMiembro : Long
    +rol : RolProyecto
    +estado : EstadoMiembro
  }
  class Actividad {
    +idActividad : Long
    +nombre : String
    +estado : EstadoActividad
  }
  class RequerimientoActividad {
    +idRequerimiento : Long
    +cantidad : Integer
    +estado : EstadoRequerimiento
  }
  class PostulacionActividad {
    +idPostulacion : Long
    +fecha : LocalDateTime
    +estado : EstadoPostulacion
  }
  class Contrato {
    +idContrato : Long
    +estado : EstadoContrato
    +fechaCreacion : LocalDateTime
  }
  class Agenda {
    +idAgenda : Long
    +margenActividadMinutos : Integer
  }
  class BloqueoAgenda {
    +idBloqueo : Long
    +fechaHoraInicio : LocalDateTime
    +fechaHoraFin : LocalDateTime
  }
  class Publicacion {
    +idPublicacion : Long
    +contenido : String
    +fecha : LocalDateTime
  }
  class Conversacion {
    +idConversacion : Long
  }
  class Mensaje {
    +idMensaje : Long
    +contenido : String
    +fecha : LocalDateTime
  }
}

namespace Seguridad {
  class SecurityConfig {
    +securityFilterChain(http)
    +corsConfigurationSource()
  }
  class JwtService {
    +generarToken(usuario)
    +validarToken(token)
  }
  class JwtAuthenticationFilter {
    +doFilterInternal(request, response, chain)
  }
  class GlobalExceptionHandler {
    +handleException(exception)
  }
}

AuthController --> AuthService : delega
PerfilController --> PerfilService : delega
ProyectoController --> ProyectoService : delega
ActividadController --> ActividadService : delega
PostulacionController --> PostulacionService : delega
ContratoController --> ContratoService : delega

AuthService --> UsuarioRepository : consulta
PerfilService --> PerfilRepository : persiste
ProyectoService --> ProyectoRepository : persiste
ActividadService --> ActividadRepository : persiste
PostulacionService --> PostulacionActividadRepository : persiste
ContratoService --> ContratoRepository : persiste
CalendarioService --> AgendaRepository : persiste

UsuarioRepository --|> RepositoryBase : extiende
PerfilRepository --|> RepositoryBase : extiende
ProyectoRepository --|> RepositoryBase : extiende
ActividadRepository --|> RepositoryBase : extiende
PostulacionActividadRepository --|> RepositoryBase : extiende
ContratoRepository --|> RepositoryBase : extiende
AgendaRepository --|> RepositoryBase : extiende

AuthService ..> Usuario : crea y autentica
PerfilService ..> Perfil : administra
ProyectoService ..> Proyecto : administra
ActividadService ..> Actividad : administra
PostulacionService ..> PostulacionActividad : administra
ContratoService ..> Contrato : administra
CalendarioService ..> Agenda : administra

Usuario "1" --> "0..*" Perfil : posee
Perfil "1" --> "0..*" Publicacion : crea
Perfil "1" --> "0..1" Agenda : posee
Proyecto "1" o-- "1..*" MiembroProyecto : integra
Perfil "1" --> "0..*" MiembroProyecto : participa
Proyecto "1" o-- "0..*" Actividad : planifica
Actividad "1" o-- "0..*" RequerimientoActividad : requiere
Perfil "1" --> "0..*" PostulacionActividad : realiza
RequerimientoActividad "1" --> "0..*" PostulacionActividad : recibe
Proyecto "1" o-- "0..*" Contrato : formaliza
Agenda "1" o-- "0..*" BloqueoAgenda : contiene
Conversacion "1" o-- "0..*" Mensaje : contiene

SecurityConfig --> JwtAuthenticationFilter : registra
JwtAuthenticationFilter --> JwtService : valida
JwtAuthenticationFilter --> UsuarioRepository : carga usuario
GlobalExceptionHandler ..> Presentacion : transforma errores REST
```

## Correspondencia con UP

| Estereotipo UP | Implementación Spring Boot | Responsabilidad |
|---|---|---|
| `<<boundary>>` | Clases `*Controller` anotadas con `@RestController` | Recibir solicitudes HTTP, validar DTOs y devolver respuestas REST. |
| `<<control>>` | Clases `*Service` anotadas con `@Service` | Coordinar cada caso de uso, aplicar reglas y controlar transacciones. |
| `<<entity>>` | Clases de `modelo` anotadas con `@Entity` | Representar el dominio persistente y sus asociaciones JPA. |
| `<<repository>>` | Interfaces de `repositorio` | Encapsular consultas y operaciones de persistencia mediante Spring Data JPA. |

## Stack tecnológico

| Capa | Tecnología | Uso |
|---|---|---|
| Aplicación | Java 25 | Lenguaje y runtime objetivo. |
| Framework | Spring Boot 4.1.0 | Inyección de dependencias, configuración y ciclo de vida. |
| API | Spring Web MVC | Controladores REST y serialización JSON. |
| Persistencia | Spring Data JPA + Hibernate | Mapeo objeto-relacional y repositorios. |
| Base de datos | PostgreSQL 17 | Persistencia relacional; el esquema se valida con `ddl-auto=validate`. |
| Seguridad | Spring Security + JWT | Autenticación stateless, cookies HttpOnly, CSRF y autorización por roles. |
| Identidad externa | OAuth2 Client Google | Inicio de sesión y registro mediante Google. |
| Validación | Spring Boot Validation | Validación de requests con anotaciones Jakarta Validation. |

## Patrones y decisiones de diseño

- Se utiliza una arquitectura por capas y por módulos funcionales
  (`controlador`, `servicio`, `repositorio` y `modelo`).
- Los controladores delegan los casos de uso a servicios; no acceden
  directamente a los repositorios.
- Las entidades JPA modelan asociaciones del dominio con `@ManyToOne`,
  `@OneToMany` y `@ManyToMany`; los repositorios abstraen el acceso a
  PostgreSQL.
- La autenticación es stateless: `JwtAuthenticationFilter` valida el token y
  carga el usuario mediante `UsuarioRepository`.
- `GlobalExceptionHandler` centraliza la conversión de excepciones de dominio y
  validación a respuestas HTTP.

## Trazabilidad

- [pom.xml](../../pom.xml): versión de Java, Spring Boot y dependencias.
- [modelo/](../../src/main/java/org/mgroko/backend/modelo): entidades JPA.
- [repositorio/](../../src/main/java/org/mgroko/backend/repositorio): repositorios Spring Data.
- [auth/controlador/AuthController.java](../../src/main/java/org/mgroko/backend/auth/controlador/AuthController.java): entrada REST de autenticación.
- [security/SecurityConfig.java](../../src/main/java/org/mgroko/backend/security/SecurityConfig.java): seguridad, CORS y autorización.
- [common/exception/GlobalExceptionHandler.java](../../src/main/java/org/mgroko/backend/common/exception/GlobalExceptionHandler.java): manejo global de errores.
