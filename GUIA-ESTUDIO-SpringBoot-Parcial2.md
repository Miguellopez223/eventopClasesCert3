# Guía de Estudio — Spring Boot (Segundo Parcial)

> Material basado en el proyecto **Eventop**. Cada concepto se explica con **teoría**, **el porqué**, y **el código real del proyecto** donde aparece.

## Índice
0. [Panorama general del proyecto](#0-panorama-general-del-proyecto)
1. [HMAC & JWT](#1-hmac--jwt)
2. [JSONObject (y cómo lo hace este proyecto: Jackson + DTOs)](#2-jsonobject)
3. [Stereum (la sesión de autenticación)](#3-stereum)
4. [Webhook](#4-webhook)
5. [Transactions & métodos asíncronos](#5-transactions--métodos-asíncronos)
6. [Logs](#6-logs)
7. [Seguridad](#7-seguridad)
8. [Jobs (tareas programadas)](#8-jobs)
9. [Glosario rápido para el examen](#9-glosario-rápido)

---

## 0. Panorama general del proyecto

Eventop es un proyecto **Maven multi-módulo** (Java 17, Spring Boot 4). Tiene 3 módulos en capas:

```
eventop-api  →  core  →  data
(arrancable)    (lógica)  (entidades, repos, DTOs)
```

- **`data`**: entidades JPA (`User`, `Empresa`, `Eventos`, `Log`), repositorios, DTOs y enums. **Sin lógica de negocio**.
- **`core`**: servicios (`EmpresaService`, `UserService`, `LogService`, ...) + integración externa (`SistemaA`).
- **`eventop-api`**: la app Spring Boot, controladores REST, configuración de seguridad, JWT, e inicialización.

**Regla de dependencias**: una capa solo conoce a la de abajo. El controlador no toca el repositorio directamente; pasa por el servicio.

### Las anotaciones "estereotipo" de Spring (esto cae siempre)
Spring crea y administra objetos llamados **beans**. Para decirle "administra esta clase", se usan anotaciones estereotipo:

| Anotación | Para qué | Ejemplo en el proyecto |
|---|---|---|
| `@Component` | Bean genérico | `JwtTokenProvider`, `JwtTokenFilter`, `DataInitializer` |
| `@Service` | Lógica de negocio | `EmpresaService`, `LogService`, `SistemaA` |
| `@Repository` | Acceso a datos | `EmpresaRepository`, `UserRepository` |
| `@Controller` / `@RestController` | Endpoints HTTP | `EmpresaController`, `AuthController` |
| `@Configuration` | Define otros beans con `@Bean` | `InjectConfiguration`, `WebSecurityConfiguration` |

**Inyección de dependencias (DI)**: en vez de hacer `new EmpresaService()`, Spring te lo "inyecta". Aquí se usa **inyección por constructor** generada por Lombok (`@AllArgsConstructor` / `@RequiredArgsConstructor`):

```java
@AllArgsConstructor   // Lombok genera el constructor con todos los campos final
@Service
public class EmpresaService {
    private final EmpresaRepository repository;   // Spring inyecta esto solo
    private final LogService logService;
}
```

---

## 1. HMAC & JWT

### 1.1 Teoría — ¿Qué es un JWT?
**JWT (JSON Web Token)** es un texto que representa la identidad de un usuario de forma **autocontenida** y **firmada**. Tiene 3 partes separadas por puntos:

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiJyb290IiwiaWQiOiIuLi4ifQ . k3rT9x_FIRMA
└─── HEADER (base64) ──┘ └────── PAYLOAD / CLAIMS (base64) ──┘ └─ SIGNATURE ─┘
```

- **Header**: el algoritmo de firma (aquí `HS256`).
- **Payload (claims)**: los datos. En este proyecto: `subject` (username), `id` (user id), `issuedAt`, `expiration`.
- **Signature**: la **firma** que garantiza que nadie alteró el token.

> ⚠️ El payload **NO está cifrado**, solo codificado en Base64. Cualquiera puede leerlo. Lo que NO puede hacer es **modificarlo sin invalidar la firma**. Por eso nunca pongas contraseñas en el payload.

### 1.2 Teoría — ¿Qué es HMAC?
**HMAC (Hash-based Message Authentication Code)** es el mecanismo de **firma simétrica**: se usa **la misma clave secreta** para firmar y para verificar. `HS256` = **HMAC + SHA-256**.

```
firma = HMAC_SHA256( base64(header) + "." + base64(payload),  CLAVE_SECRETA )
```

- **Simétrico**: la misma clave firma y verifica (a diferencia de RSA, que es asimétrico con clave pública/privada).
- Si alguien cambia el payload, tendría que **recalcular la firma**, pero no puede porque **no conoce la clave secreta**. Así se detecta cualquier manipulación.

### 1.3 Práctica — dónde vive en el proyecto
**Clave secreta** en `application.properties`:
```properties
security.jwt.token.secret-key=n70iaIlY02WbW61O09zJ8WiAvcZ+i/qZZKEyRlD5ovg=
security.jwt.token.expire-length=480   # minutos
```

**Creación del token** — `JwtTokenProvider.createToken()`:
```java
Claims claims = Jwts.claims()
        .subject(user.getUsername())   // a quién pertenece
        .id(user.getId())              // el claim que luego se usa para recargar al user
        .issuedAt(new Date())
        .expiration(validity)          // cuándo expira
        .build();

SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyByte);  // <-- HMAC con la clave
String token = Jwts.builder()
        .claims(claims)
        .signWith(secretKey)           // <-- aquí se FIRMA (HS256)
        .compact();
```

**Validación del token** — `JwtTokenProvider.validateToken()`:
```java
SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyByte);
Jws<Claims> claims = Jwts.parser()
        .verifyWith(secretKey)         // <-- verifica la FIRMA con la misma clave
        .build()
        .parseSignedClaims(token);     // si la firma no cuadra, lanza excepción

if (claims.getBody().getExpiration().after(new Date())) {   // ¿no expiró?
    User authUser = userService.findByUserIdToValidateSession(getId(token))...
    return Optional.of(new UsernamePasswordAuthenticationToken(authUser, "", authUser.getAuthorities()));
}
```

**Detalle clave del proyecto**: en cada petición se vuelve a **cargar el usuario desde la BD** usando el `id` del token (no se confía solo en lo que dice el token). Eso permite revocar/desactivar usuarios.

### 1.4 El detalle del Base64 de la clave
```java
@PostConstruct
protected void init() {
    secretKeyByte = Base64.getDecoder().decode(secretKey);  // la clave está en Base64
}
```
`@PostConstruct` = "ejecuta esto **una vez**, justo después de que Spring construya el bean". Se usa para decodificar la clave a bytes una sola vez.

### Preguntas típicas de examen
- *¿JWT es cifrado?* → No, es **firmado** (Base64 + firma HMAC). Es legible pero no manipulable.
- *¿Por qué HMAC y no solo un hash?* → Un hash simple (SHA-256 sin clave) cualquiera lo recalcula. HMAC mete una **clave secreta**, así solo el servidor puede generar/validar firmas.
- *¿Qué pasa si cambias un carácter del token?* → La firma deja de cuadrar → `parseSignedClaims` lanza `JwtException` → 401.

---

## 2. JSONObject

### 2.1 Teoría
`JSONObject` (de la librería `org.json`) es una forma **manual** de construir/leer JSON clave-valor:
```java
JSONObject obj = new JSONObject();
obj.put("access_token", token);
obj.put("expires_in", 480);
String json = obj.toString();   // {"access_token":"...","expires_in":480}
```
Es flexible pero **propenso a errores** (claves como texto, sin tipado).

### 2.2 Cómo lo resuelve ESTE proyecto: Jackson + DTOs
Spring Boot usa **Jackson** por defecto: convierte automáticamente entre **objetos Java ↔ JSON** (serialización / deserialización). En vez de `JSONObject` manual, defines una **clase DTO** y Jackson hace el mapeo.

**Ejemplo — la respuesta del login** (`OKAuthDto`):
```java
public class OKAuthDto implements Serializable {
    @JsonProperty("access_token")   // <-- el JSON usará "access_token"
    private String accessToken;     //     aunque el campo Java sea camelCase
    @JsonProperty("id_token")
    private String idToken;
    @JsonProperty("expires_in")
    private int expiresIn;
}
```
Cuando el controlador devuelve este objeto, Jackson produce:
```json
{ "access_token": "...", "id_token": "...", "expires_in": 480 }
```

- **`@JsonProperty("snake_case")`**: traduce el nombre del campo Java (camelCase) al nombre del JSON (snake_case). Es **el puente** entre la convención de Java y la del API.
- **`@JsonIgnore`**: excluye un campo del JSON. Ejemplo en `User`: el `password` lleva `@JsonIgnore` para que **nunca se serialice** en una respuesta.

**Uso directo de `ObjectMapper`** (el motor de Jackson) en `JwtTokenFilter`:
```java
servletResponse.getWriter().write(
    new ObjectMapper().writeValueAsString(HttpStatus.UNAUTHORIZED)  // objeto → JSON
);
```

**`@RequestBody`** hace el camino inverso (JSON entrante → objeto Java) en los controladores:
```java
@PostMapping
public ResponseEntity<Void> guardar(@RequestBody EmpresaRequestDto empresa) { ... }
```
El JSON `{"nombre_empresa":"X","descripcion":"Y"}` se convierte en un `EmpresaRequestDto` gracias a `@JsonProperty("nombre_empresa")`.

### Para el examen
- **Serializar** = objeto Java → JSON (salida). **Deserializar** = JSON → objeto Java (entrada).
- `JSONObject` es manual; **DTO + Jackson** es lo idiomático en Spring y es lo que usa este proyecto.
- `@JsonProperty` resuelve la diferencia camelCase (Java) vs snake_case (JSON).

---

## 3. Stereum

En este proyecto, **"Stereum"** es el nombre que se le da a la **sesión de autenticación** del usuario. Aparece literalmente en el log del `AuthController`:

```java
public OKAuthDto auth(AuthenticationDto data) {
    String username = data.username();
    log.info("Getting Stereum Session for username: {}", username);   // <-- aquí
    ...
}
```

"Getting Stereum Session" = "obteniendo la sesión (Stereum) del usuario". Conceptualmente representa **el proceso completo de iniciar sesión**:

1. Buscar el usuario por username (`userService.findByUsername`).
2. Validar las credenciales con el `AuthenticationManager` (compara la contraseña contra el hash bcrypt de la BD).
3. Si todo está bien, **emitir el JWT** (`jwtTokenProvider.createToken`) que será la "sesión" del usuario.

```java
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(data.username(), data.password()));  // valida credenciales
SecurityContextHolder.getContext()
    .setAuthentication(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()));
return jwtTokenProvider.createToken(user);   // emite el token (la "sesión Stereum")
```

> Como esta app es **stateless** (sin sesión en servidor), la "sesión Stereum" **no se guarda en memoria del servidor**: vive enteramente dentro del JWT que se entrega al cliente. Cada petición posterior re-presenta ese token.

### Para el examen
- Stereum = la **sesión/identidad autenticada** del usuario, materializada como JWT.
- Es **stateless**: no hay `HttpSession`; el token ES la sesión.

---

## 4. Webhook

### 4.1 Teoría
Un **Webhook** es una **llamada HTTP de sistema a sistema**: en vez de que un humano use el navegador, **un servidor llama a otro servidor** por HTTP para notificar o pedir algo. Hay dos lados:

- **Saliente (cliente)**: TU app llama a otra. → Necesitas un cliente HTTP.
- **Entrante (servidor/receptor)**: otra app llama a un endpoint TUYO. → Es un controlador normal que expones para que lo invoquen.

### 4.2 Práctica — la integración con "Sistema 1"
Este proyecto se comunica con un sistema peer ("Sistema 1") usando **`RestClient`** (el cliente HTTP moderno de Spring). Está en `SistemaA`:

```java
@Service
public class SistemaA {
    public Sistema1AuthResponse auth(Sistema1AuthRequest request) throws Exception {
        RestClient restClient = create();
        ResponseEntity<Sistema1AuthResponse> response = restClient.post()
                .uri(urlBase + "/api/v1/auth")                       // llama a OTRO servidor
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(request)                                        // envía JSON
                .retrieve()
                .toEntity(Sistema1AuthResponse.class);                // recibe y mapea la respuesta
        ...
    }

    private RestClient create() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofMillis(connectTimeout));   // timeout de conexión
        f.setReadTimeout(Duration.ofMillis(readTimeout));         // timeout de lectura
        return RestClient.builder().requestFactory(f).build();
    }
}
```

**Conceptos importantes aquí:**
- **Timeouts**: `connect-timeout` (cuánto esperar para *establecer* la conexión) y `read-timeout` (cuánto esperar la *respuesta*). Sin esto, un sistema caído colgaría tu app indefinidamente. (De hecho viste el `Connect timed out` cuando Sistema 1 no estaba accesible).
- **DTOs de integración** (`Sistema1AuthRequest`/`Sistema1AuthResponse`) con `@JsonProperty` snake_case — mismo patrón Jackson de la sección 2.

**El disparo al arrancar** está en `EventopApplication` (que es un `CommandLineRunner`):
```java
@Override
public void run(String... args) {           // se ejecuta UNA vez al arrancar
    try {
        Sistema1AuthResponse response = sistemaA.auth(request);   // webhook saliente
    } catch (Exception e) {
        log.warn("No se pudo autenticar contra Sistema 1 (...)"); // no tumba el arranque
    }
}
```

**El lado entrante**: tu propio `POST /api/v1/auth` (en `AuthController`) es justamente el tipo de endpoint que **Sistema 1 llamaría** sobre ti. Es decir, este mismo proyecto es a la vez **cliente** (vía `SistemaA`) y **receptor** (vía `AuthController`).

### Para el examen
- Webhook = comunicación HTTP **máquina a máquina**.
- En Spring 4 el cliente recomendado es **`RestClient`** (reemplaza al viejo `RestTemplate`).
- **Siempre** configurar timeouts en integraciones externas.

---

## 5. Transactions & métodos asíncronos

### 5.1 Teoría — Transacción
Una **transacción** es un bloque de operaciones de BD que es **todo-o-nada (atómico)**:
- Si todo sale bien → **COMMIT** (se confirman los cambios).
- Si algo falla → **ROLLBACK** (se deshacen todos los cambios).

En Spring se marca con **`@Transactional`** sobre un método. El proxy abre la transacción al entrar y hace commit/rollback al salir.

```java
@Transactional
public void save(EmpresaRequestDto empresa) { ... }
```

**Punto crítico que cae en examen — `rollbackFor`:**
```java
@Transactional(rollbackFor = Exception.class)
public void save(...) throws Exception { ... }
```
Por defecto, Spring **solo hace rollback ante `RuntimeException` y `Error`**, NO ante excepciones *checked* (`Exception`). Para que una `Exception` checked también revierta, hay que poner `rollbackFor = Exception.class`.

**`readOnly = true`** (optimización para consultas):
```java
@Transactional(readOnly = true)
public List<EmpresaDto> listar() { return repository.findByNombreAux("Empresa 1"); }
```
Le dice a Hibernate "esta transacción solo lee" → no hace falta tracking de cambios → más eficiente.

### 5.2 Teoría — Propagación
La **propagación** define qué pasa cuando un método transaccional llama a otro:
- **`REQUIRED`** (por defecto): si ya hay una transacción, se une a ella. Si no, crea una.
- **`REQUIRES_NEW`**: **siempre crea una transacción nueva e independiente**, suspendiendo la del padre. Su commit/rollback es propio.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void infoTxAsync(...) { ... }    // el log se confirma aunque el padre haga rollback
```

### 5.3 Teoría — Método asíncrono (`@Async`)
Normalmente un método se ejecuta **síncronamente**: quien lo llama **espera** a que termine. Con **`@Async`**, el método se ejecuta en **otro hilo** (de un pool) y quien lo llama **sigue de inmediato sin esperar**.

**Para qué sirve**: tareas que no deben bloquear la respuesta (logs, correos, notificaciones).

### 5.4 Práctica — el patrón completo del proyecto

**Paso 1 — habilitar** (en `EventopApplication`):
```java
@EnableAsync
```

**Paso 2 — definir el pool de hilos** (`InjectConfiguration`, leyendo parámetros de `application.properties`):
```java
@Value("${async.core-pool-size}")  private int corePoolSize;   // 5  → hilos siempre activos
@Value("${async.max-pool-size}")   private int maxPoolSize;    // 10 → máximo si hay carga
@Value("${async.queue-capacity}")  private int queueCapacity;  // 20 → tareas en cola

@Bean(name = "taskLog")
public ThreadPoolTaskExecutor myTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("TaskLog-");
    executor.initialize();
    return executor;
}
```

> **Analogía del "pool de Migueles"**: tienes 5 trabajadores (Migueles) siempre listos. Si llega mucho trabajo, clonas hasta 10. Si aún hay más, 20 tareas esperan en cola. Orden real de Spring: llena los **5 core** → **encola hasta 20** → recién entonces crea hilos extra hasta **10**.

**Paso 3 — el método async, EN OTRO BEAN** (`LogService`):
```java
@Async("taskLog")                                       // corre en un hilo del pool taskLog
@Transactional(propagation = Propagation.REQUIRES_NEW)  // con transacción propia
public void infoTxAsync(String mensaje, String usuario) {
    guardar("INFO", mensaje, usuario);
}
```

**Paso 4 — uso desde el método transaccional** (`EmpresaService.save`):
```java
String usuario = getUsuarioActual();   // se captura ANTES (el contexto no viaja al hilo async)
this.logService.infoTxAsync("Inicio guardado: " + empresa.getNombre(), usuario);  // no bloquea
```

### 5.5 Los DOS "gotchas" que SIEMPRE caen

1. **El proxy y la auto-invocación**: `@Async` y `@Transactional` solo funcionan si el método se llama **desde OTRO bean**. Si `LogService` se llamara a sí mismo con `this.metodo()`, las anotaciones **se ignoran** (porque no pasa por el proxy de Spring). Por eso `LogService` es un servicio aparte de `EmpresaService`.

2. **El contexto NO viaja al hilo async**: `SecurityContextHolder` es `ThreadLocal` (vive atado al hilo). Al saltar a un hilo `TaskLog-X`, el usuario autenticado se pierde. Solución: capturar el dato en el hilo original y pasarlo como parámetro.

### 5.6 El experimento mental que demuestra todo
En `save()` hay un `Thread.sleep(30000)` dentro del log async y un `throw new Exception(...)` al final:

```
Hilo http-nio (tu POST)                Hilo TaskLog-1 (pool)
────────────────────────              ──────────────────────
llama infoTxAsync() ──────encola──→   recibe la tarea
sigue de inmediato                     sleep(30s)...
guarda empresa                         (sigue durmiendo)
throw Exception → ROLLBACK empresa     INSERT log + COMMIT (independiente)
responde 500 (NO espera 30s)
```
**Resultado**: la empresa **no** se inserta (rollback), pero el log **sí** queda en la tabla `logs`. Y la respuesta es inmediata pese al `sleep` de 30s → prueba de que el log no impacta el tiempo del método.

---

## 6. Logs

### 6.1 Teoría
Un **log** es un registro de lo que hace la aplicación. Spring Boot usa **SLF4J** (la fachada/API) + **Logback** (la implementación) por defecto. Lombok da la anotación **`@Slf4j`** que genera automáticamente un objeto `log`.

```java
@Slf4j
public class EmpresaService {
    public void save(...) {
        log.info("...");    // informativo
        log.warn("...");    // advertencia
        log.error("...");   // error
    }
}
```

**Niveles de log** (de más a menos verboso): `TRACE < DEBUG < INFO < WARN < ERROR`. En producción se suele dejar `INFO` y subir a `DEBUG` solo para diagnosticar.

**Buenas prácticas que se ven en el proyecto:**
- **Placeholders `{}`** en vez de concatenar (más eficiente, no construye el string si el nivel está apagado):
  ```java
  log.info("USuario:{} ", user.getUsername());          // ✅
  log.error("Error al autentificar el usuario: {}", data.username(), e);  // ✅ + stacktrace
  ```
- **Nunca loguear contraseñas ni tokens completos.**

### 6.2 Dos tipos de log en este proyecto
1. **Log técnico (consola/archivo)** vía `@Slf4j` → para el desarrollador. Ej: `log.error("Error al validar el TOKEN", e)`.
2. **Log de negocio (tabla `logs` en BD)** vía `LogService` → para soporte/auditoría. Permite ver el **ciclo de vida de una operación** desde la misma base de datos, sin ir a otra herramienta.

La entidad `Log` (en `data`):
```java
@Entity @Table(name = "logs")
public class Log {
    @Id @UuidGenerator private String id;
    private String nivel;     // INFO / ERROR
    private String mensaje;
    private String usuario;
    private LocalDateTime fecha;
}
```

> El profe planteó: *"el logging no debe impactar el tiempo del método"* → la solución fue el **logging asíncrono** de la sección 5. Esa es la conexión entre "Logs" y "métodos asíncronos".

### Para el examen
- SLF4J = API, Logback = implementación. `@Slf4j` es de Lombok.
- Usa `{}` en vez de `+` para los mensajes.
- Niveles: TRACE/DEBUG/INFO/WARN/ERROR.

---

## 7. Seguridad

### 7.1 La cadena de filtros (lo más importante)
Spring Security funciona como una **cadena de filtros** que toda petición atraviesa **antes** de llegar al controlador. Se configura en `WebSecurityConfiguration`:

```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration ... {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ...) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)                  // sin CSRF (API stateless)
            .authorizeHttpRequests(reg -> reg
                .requestMatchers("/swagger-ui/**", ...).permitAll() // público
                .requestMatchers(HttpMethod.POST, "/api/v1/auth").permitAll()  // login público
                .requestMatchers("/error").anonymous()
                .anyRequest().authenticated())                      // TODO lo demás requiere token
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // sin sesión
            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);      // nuestro filtro JWT
        return http.build();
    }
}
```

Puntos clave:
- **`STATELESS`**: no se crea `HttpSession`. Cada petición se autentica **solo** con su token. Esto encaja con el JWT.
- **`permitAll()`** vs **`authenticated()`**: el único endpoint público (además de swagger/error) es `POST /api/v1/auth` (el login). Todo lo demás exige token.
- **`addFilterBefore(jwtTokenFilter, ...)`**: inserta nuestro filtro JWT en la cadena.

### 7.2 El filtro JWT — `JwtTokenFilter`
Extiende `OncePerRequestFilter` (se ejecuta **una vez por petición**):
```java
protected void doFilterInternal(req, res, chain) {
    String token = jwtTokenProvider.resolveToken(req.getHeader("Authorization"));  // saca "Bearer xxx"
    if (token == null) { chain.doFilter(req, res); return; }                       // sin token → sigue (y fallará si el endpoint exige auth)

    Optional<Authentication> auth = jwtTokenProvider.validateToken(token);         // valida firma + expiración
    if (auth.isPresent()) {
        SecurityContextHolder.getContext().setAuthentication(auth.get());          // marca al usuario como autenticado
        chain.doFilter(req, res);                                                  // deja pasar
    }
    // si no, responde 401 UNAUTHORIZED
}
```

### 7.3 El modelo de usuario — `UserDetails`
La entidad `User` implementa `UserDetails` (el contrato de Spring Security):
```java
public class User extends AuditableEntity implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));   // ej: "ROLE_ROOT"
    }
    @Override public boolean isAccountNonLocked() { return this.status == UserStatus.ACTIVO; }
}
```
- **Authorities/roles**: el rol del usuario (`ROLE_ROOT`) se convierte en una "autoridad" que Spring usa para autorizar.

### 7.4 Autenticación vs Autorización
- **Autenticación** (¿quién eres?): el login en `AuthController` valida credenciales con `AuthenticationManager` y emite el JWT.
- **Autorización** (¿qué puedes hacer?): se controla con **seguridad a nivel de método**.

**Method Security** — habilitada en el main:
```java
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
```
Y aplicada en el controlador:
```java
@Secured({"ROLE_ADMIN", "ROLE_ROOT"})   // solo estos roles pueden entrar
@Controller
@RequestMapping("/api/v1/empresas")
public class EmpresaController { ... }
```

### 7.5 Contraseñas — bcrypt
Las contraseñas **nunca** se guardan en texto plano. Se usa un `PasswordEncoder` (bcrypt por defecto):
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();  // {bcrypt}...
}
```
En `DataInitializer`:
```java
.password(passwordEncoder.encode("Abc123**"))   // se guarda el HASH, no el texto
```
El `DelegatingPasswordEncoder` guarda un prefijo `{bcrypt}` para saber con qué algoritmo verificar. Bcrypt incluye **salt** automático (dos hashes de la misma clave son distintos).

### 7.6 Auditoría JPA
Cada entidad extiende `AuditableEntity`, que registra automáticamente quién y cuándo:
```java
@CreatedDate     LocalDateTime createdDate;
@LastModifiedDate LocalDateTime modifiedDate;
@CreatedBy       String createdBy;     // ← lo provee el AuditorAware
@LastModifiedBy  String modifiedBy;
@Version         Integer version;       // ← bloqueo optimista
```
El `AuditorAware` (en `InjectConfiguration`) saca el usuario actual del `SecurityContextHolder` para llenar `createdBy`/`modifiedBy`. `@Version` da **bloqueo optimista** (evita que dos updates simultáneos se pisen).

### Para el examen
- Cadena de filtros → JWT filter → SecurityContext.
- **Stateless** + JWT (sin sesión de servidor).
- Autenticación (login) ≠ Autorización (`@Secured` por rol).
- Contraseñas con **bcrypt** (hash + salt), nunca en claro.

---

## 8. Jobs

### 8.1 Teoría
Un **Job** (tarea programada) es código que se ejecuta **automáticamente cada cierto tiempo**, sin que nadie lo pida. Lo dispara **el reloj del servidor**, no una petición HTTP. En Spring se hace con **`@Scheduled`**.

**Diferencia clave con `@Async`:**
| | `@Async` | `@Scheduled` |
|---|---|---|
| Lo dispara | una **llamada** de código | el **reloj** |
| Repetición | una vez por llamada | periódica (cada X) |

### 8.2 Práctica — el patrón
**Paso 1 — habilitar** (en `EventopApplication`):
```java
@EnableScheduling
```

**Paso 2 — el método programado** (`InjectConfiguration`):
```java
@Scheduled(cron = "0 */1 * * * *")
public void listarEmpresas() {
    log.info("INFO: Listando Empresas");
}
```

**Cómo se lee el CRON** (6 campos: `segundo minuto hora díaMes mes díaSemana`):
```
0       */1      *      *      *      *
↑        ↑       ↑      ↑      ↑      ↑
seg=0   cada    cualquier hora / día / mes / día-semana
        minuto
```
→ "en el segundo 0 de cada minuto". Por eso el log salió en `09:10:00`, `09:11:00`, ...

Otras opciones de `@Scheduled`:
- `@Scheduled(fixedRate = 60000)` → cada 60s (cuenta desde el **inicio** de la ejecución anterior).
- `@Scheduled(fixedDelay = 60000)` → 60s **después de terminar** la ejecución anterior.
- `@Scheduled(cron = "...")` → expresión cron (más flexible, horarios concretos).

### 8.3 Nombrar el hilo del scheduler (el "Miguel reloj")
Por defecto el hilo se llama `scheduling-1`. Para personalizarlo, se define un `TaskScheduler`:
```java
@Bean
public TaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("Miguel-");   // el hilo será "Miguel-1"
    scheduler.initialize();
    return scheduler;
}
```
Spring detecta automáticamente el bean de tipo `TaskScheduler` y lo usa para **todos** los `@Scheduled`.

**Cómo distinguir los hilos en consola:**
```
[   http-nio-8081-exec-1]  → atendiendo una petición HTTP
[              TaskLog-1 ]  → un @Async (logging)
[              Miguel-1  ]  → un @Scheduled (job)
[                  main  ]  → el arranque / CommandLineRunner
```

### 8.4 Usos reales de Jobs
- Depurar (borrar) logs viejos de la tabla `logs` cada noche.
- Generar reportes diarios.
- Re-sincronizar con Sistema 1 periódicamente.
- Cambiar de estado eventos vencidos.

### Para el examen
- `@EnableScheduling` + `@Scheduled(cron=...)`.
- Cron de 6 campos (Spring incluye **segundos**, a diferencia del cron de Unix que tiene 5).
- `fixedRate` vs `fixedDelay` vs `cron`.
- El `TaskScheduler` personalizado controla el pool y el nombre del hilo.

---

## 9. Glosario rápido

| Término | Definición en una línea |
|---|---|
| **Bean** | Objeto que Spring crea y administra por ti |
| **DI (Inyección de dependencias)** | Spring te entrega las dependencias en vez de hacer `new` |
| **JWT** | Token firmado (header.payload.signature) que representa la identidad |
| **HMAC / HS256** | Firma simétrica con clave secreta (HMAC + SHA-256) |
| **Claims** | Los datos dentro del payload del JWT (subject, id, expiration...) |
| **Jackson / `@JsonProperty`** | Convierte objeto Java ↔ JSON (y mapea camelCase ↔ snake_case) |
| **Stereum** | Nombre que da el proyecto a la sesión autenticada (el JWT) |
| **Webhook / RestClient** | Comunicación HTTP servidor-a-servidor |
| **`@Transactional`** | Hace un método atómico (commit/rollback) |
| **`REQUIRES_NEW`** | Transacción nueva e independiente del padre |
| **`rollbackFor`** | Hace rollback también ante excepciones checked |
| **`@Async`** | Ejecuta el método en otro hilo (no bloquea) |
| **Pool de hilos** | Conjunto de hilos reutilizables ("Migueles") |
| **`@Scheduled`** | Ejecuta un método periódicamente (job) |
| **`OncePerRequestFilter`** | Filtro que corre una vez por petición (el JWT filter) |
| **`UserDetails`** | Contrato de Spring Security para representar al usuario |
| **`@Secured`** | Restringe un método/controlador por rol |
| **bcrypt** | Algoritmo de hash de contraseñas (con salt) |
| **`@Slf4j` / SLF4J** | Logging (API SLF4J + implementación Logback) |
| **Auditoría JPA** | `@CreatedBy`, `@CreatedDate`, etc. automáticos |
| **STATELESS** | Sin sesión en servidor; el token lleva todo |

---

### Cómo estudiar este proyecto (sugerencia)
Sigue una petición de punta a punta y di en voz alta qué hace cada capa:

```
POST /api/v1/empresas  (con Bearer token)
  → JwtTokenFilter        valida el JWT (HMAC), setea el usuario en el SecurityContext
  → @Secured              comprueba que el rol sea ROLE_ADMIN/ROLE_ROOT
  → EmpresaController     recibe el JSON (@RequestBody → DTO vía Jackson)
  → EmpresaService.save   @Transactional(rollbackFor=...); dispara logs @Async
  → LogService            @Async("taskLog") + REQUIRES_NEW → guarda log en otro hilo
  → EmpresaRepository     INSERT en la tabla empresas
  → (commit o rollback)   según haya o no excepción
Y en paralelo, cada minuto:
  → @Scheduled            el job listarEmpresas() corre en el hilo "Miguel-1"
```

Si puedes explicar ese diagrama con tus palabras, dominas el parcial. 💪
```
