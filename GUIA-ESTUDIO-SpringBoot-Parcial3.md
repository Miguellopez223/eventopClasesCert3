# Guía de Estudio — Spring Boot (Tercer Parcial)

> Material basado en los **dos pull del repo del docente** que se integraron al proyecto **Eventop**. Explica cada tema con **teoría**, **el porqué** y **el código real** donde aparece.

## Índice
- [Pull 1 — Webhooks, HMAC, JSONObject y rediseño de Logs](#pull-1)
  1. [Webhook (HTTP entrante de sistema a sistema)](#1-webhook)
  2. [HMAC — verificación de firma](#2-hmac)
  3. [JSONObject (org.json)](#3-jsonobject)
  4. [Rediseño del sistema de Logs (enum + auditoría + paginación)](#4-logs)
  5. [Externalizar secretos con variables de entorno](#5-secretos)
- [Pull 2 — Métodos asíncronos con valor de retorno (Future) y Jobs con trabajadores](#pull-2)
  6. [`Future` / `CompletableFuture` — async que devuelve resultado](#6-future)
  7. [Patrón scatter-gather (trabajadores en paralelo)](#7-scatter-gather)
  8. [Paginación: `Page`, `Pageable`, `PageRequest`, `Sort`](#8-paginacion)
  9. [Job que coordina trabajadores](#9-job)
- [Pull 3 — Excepciones personalizadas y manejo de errores HTTP](#pull-3)
  10. [Excepciones personalizadas (`OperationException`)](#10-excepciones)
  11. [Traducir excepciones a HTTP (`ResponseStatusException`)](#11-responsestatus)
  12. [Problem Details (RFC 7807)](#12-problem-details)
- [Mapa de conceptos para el examen](#mapa)

---

# PULL 1
## Webhooks, HMAC, JSONObject y rediseño de Logs

Este primer pull agregó la **integración "Stereum"**: recibir notificaciones de un sistema de pagos externo de forma segura, y reescribió el subsistema de logs.

---

### 1. Webhook

#### Teoría
Un **webhook** es un endpoint HTTP que **expones para que OTRO sistema te llame automáticamente** cuando ocurre un evento (ej: "se completó un pago"). Es comunicación **máquina-a-máquina**, sin un humano ni un navegador.

- **API normal**: TÚ llamas a un servidor cuando necesitas algo (modelo *pull*).
- **Webhook**: el otro servidor te llama a TI cuando algo pasa (modelo *push*).

#### Práctica — `StereumController`
```java
@RestController
@RequestMapping("/api/v1/stereum")
public class StereumController {

    @Value("${stereum.secret.key:DASDASDADASDAS}")
    private String stereumApiKey;

    @PostMapping(...)
    public ResponseEntity<Void> outbound(
            @RequestHeader("X-Signature") String signature,   // firma que envía Stereum
            @RequestHeader("X-Timestamp") int xTimestamp,
            @RequestBody String body) throws Exception {
        ...
    }
}
```
- **`@RequestHeader`**: lee cabeceras HTTP que el sistema externo manda (aquí la firma `X-Signature` y el `X-Timestamp`).
- **`@RequestBody String body`**: recibe el cuerpo **como texto crudo** (no como objeto). Esto es **clave** para los webhooks firmados: la firma se calcula sobre los bytes *exactos* del body, así que hay que leerlo tal cual llegó, antes de convertirlo a objeto.
- Este endpoint es **público** (no requiere JWT) — se configuró en `WebSecurityConfiguration`:
  ```java
  .requestMatchers(HttpMethod.POST, "/api/v1/stereum").permitAll()
  ```
  Pero "público" no significa "inseguro": la seguridad la da la **firma HMAC** (siguiente tema), no el JWT.

#### Para el examen
- Webhook = endpoint que TÚ expones para recibir llamadas automáticas de otro sistema.
- Se lee el body como `String` crudo cuando hay verificación de firma.
- Suele ser público pero protegido por firma, no por token de usuario.

---

### 2. HMAC

#### Teoría
**HMAC (Hash-based Message Authentication Code)** responde a la pregunta: *"¿este mensaje vino realmente de quien dice, y nadie lo alteró en el camino?"*

Funciona con una **clave secreta compartida** que solo conocen los dos sistemas (Eventop y Stereum):
1. Stereum calcula `firma = HMAC_SHA256(cuerpo_del_mensaje, clave_secreta)` y la manda en la cabecera `X-Signature`.
2. Eventop **recalcula** la misma firma con su copia de la clave secreta.
3. Si ambas firmas coinciden → el mensaje es auténtico e íntegro. Si no → alguien lo falsificó o alteró → se rechaza.

> Es el **mismo concepto del JWT** (que viste en el parcial anterior): firma simétrica con clave secreta. La diferencia es **dónde** se aplica: en el JWT firma el token de login; aquí firma el cuerpo del webhook.

#### Práctica — la verificación en `StereumController`
```java
String hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
        stereumApiKey.getBytes(StandardCharsets.UTF_8))   // la clave secreta
        .hmacHex(body.getBytes(StandardCharsets.UTF_8));   // se firma el body

if (!signature.equals(hmac)) {                             // ¿la firma recibida == la calculada?
    throw new Exception("MessageCode.SIGN_REQUEST_INVALID");// no coincide → rechazar
}
```
- **`HmacUtils`** (de Apache Commons Codec): utilidad que calcula el HMAC.
- **`hmacHex(...)`**: devuelve la firma como texto hexadecimal (para comparar con la cabecera).
- Si la firma no cuadra, se lanza excepción y el pago **no** se procesa.

La clave secreta vive en `application.properties`:
```properties
stereum.secret.key = 7144779d3dec...e025b57f10dcd...
```

#### Para el examen
- HMAC = firma con **clave secreta simétrica** para garantizar **autenticidad + integridad**.
- `HMAC_SHA_256` = algoritmo HMAC usando SHA-256 como función hash.
- El receptor **recalcula** la firma y la compara; nunca "descifra" nada (HMAC no es cifrado).
- Se firma sobre el **body crudo** exacto.

---

### 3. JSONObject

#### Teoría
`org.json.JSONObject` es una clase para **construir y leer JSON manualmente**, clave por clave, sin necesidad de crear una clase DTO. Útil cuando el JSON es simple o dinámico.

#### Práctica — `SistemaA` (versión del docente)
El docente reescribió la llamada a Sistema 1 para usar `JSONObject` en vez de DTOs:
```java
// CONSTRUIR el JSON de la petición (objeto → JSON)
JSONObject jsonObject = new JSONObject();
jsonObject.put("username", request.getUsername());
jsonObject.put("password", request.getPassword());

ResponseEntity<String> response = restClient.post()
        .uri(urlBase + "/api/v1/auth")
        .body(jsonObject.toString())     // se envía el JSON como texto
        .retrieve()
        .toEntity(String.class);         // se recibe la respuesta como texto crudo

// LEER el JSON de la respuesta (JSON → valor)
JSONObject jsonResponse = new JSONObject(response.getBody());
String token = jsonResponse.getString("access_token");   // extrae solo el campo que interesa
```

#### `JSONObject` vs DTO + Jackson (las dos formas)
| | `JSONObject` (manual) | DTO + Jackson (automático) |
|---|---|---|
| Cómo | `.put("clave", valor)` / `.getString("clave")` | clase con campos + `@JsonProperty` |
| Tipado | Sin tipado (claves como texto) | Fuertemente tipado |
| Cuándo | JSON simple, dinámico, o extraer 1 campo | Estructuras grandes y estables |
| Riesgo | Errores en tiempo de ejecución si te equivocas de clave | El compilador te protege |

> En este proyecto conviven **ambos**: `SistemaA` usa `JSONObject` (extraer solo el token), mientras los DTOs de Stereum (`StereumNotificacionRequestDto`) usan Jackson.

#### Para el examen
- `JSONObject.put(...)` construye JSON; `new JSONObject(texto).getString(...)` lo lee.
- Es la alternativa **manual** a Jackson; sirve cuando solo necesitas un par de campos.

---

### 4. Rediseño del sistema de Logs

El docente reemplazó el log "de juguete" por uno con **enum de nivel**, **auditoría automática** y **consulta paginada**.

#### 4.1 Enum `LogLevel`
```java
public enum LogLevel {
    INFO, ERROR, WARNING, DEBUG, TRACE;
}
```
**Por qué un enum y no un `String`**: un enum restringe los valores posibles. No puedes guardar `"infoo"` por error; solo existen esos 5 niveles. El compilador te obliga a usar uno válido.

#### 4.2 Entidad `Log` con auditoría
```java
@Entity
@Table(name = "log")
public class Log extends AuditableEntity {     // <-- hereda createdDate, createdBy, etc.
    @Id @UuidGenerator
    private String id;

    @Column(name = "_level", length = 10)
    @Enumerated(EnumType.STRING)               // guarda "INFO" como texto, no el número 0
    private LogLevel level;

    @Column(name = "_message", length = 4000)
    private String message;
}
```
- **`extends AuditableEntity`**: el log gana automáticamente `createdDate` (cuándo se creó), `createdBy`, `@Version`, etc. **Esto es clave** porque la consulta de logs filtra y ordena por `createdDate`.
- **`@Enumerated(EnumType.STRING)`**: indica a JPA que guarde el enum como **texto** (`"INFO"`) en la BD, no como su posición numérica (más legible y robusto ante reordenamientos del enum).

#### 4.3 Repositorio paginado
```java
@Query("SELECT l FROM Log l WHERE l.createdDate BETWEEN :pInit AND :pEnd")
Page<Log> findAllByOrderByDateDesc(
        @Param("pInit") LocalDateTime pInit,
        @Param("pEnd") LocalDateTime pEnd,
        Pageable pageable);
```
Devuelve una **`Page<Log>`** (no una `List`): trae los logs **de a pedazos** (páginas) en vez de todos de golpe. Esencial cuando hay miles de registros (ver tema 8).

#### 4.4 `LogController` — exponerlo por REST
```java
@GetMapping()
public ResponseEntity<Page<Log>> logs(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(defaultValue = "createdDate") String sortBy,
        @RequestParam(defaultValue = "DESC") Sort.Direction sortDir,
        @RequestParam("from") @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
        @RequestParam("to")   @DateTimeFormat(pattern = "yyyy-MM-dd") Date to) {
    ...
}
```
- **`@RequestParam`**: lee parámetros de la URL (`/api/v1/logs?page=0&size=10&from=2026-01-01&to=2026-12-31`).
- **`@DateTimeFormat`**: convierte el texto `"2026-01-01"` en un objeto `Date`.
- **`defaultValue`**: si no mandas el parámetro, usa ese valor por defecto.

#### Para el examen
- Enum + `@Enumerated(EnumType.STRING)` para campos de valores fijos.
- Heredar de `AuditableEntity` da fecha de creación gratis (y permite filtrar/ordenar por ella).
- `@RequestParam` lee query params; `@RequestBody` lee el cuerpo.

---

### 5. Externalizar secretos (variables de entorno)

El docente movió la clave secreta del JWT fuera del código, a una **variable de entorno**:
```properties
security.jwt.token.secret-key=${EVENTOP_SECRET_KEY}
```
Y el valor real va en un archivo `local.env` (que **no se sube a git** en un proyecto real):
```
EVENTOP_SECRET_KEY=pQKOkAZ8j59Kb6QJC+LHL7viCOUvfhBZ7PjGeIznKbY=
```
- **`${NOMBRE}`** en `application.properties` significa "lee este valor de una variable de entorno o del sistema".
- **Por qué**: nunca subir claves secretas al repositorio. Cada entorno (tu PC, el servidor de producción) define su propia clave sin tocar el código.

> En *tu* versión se mantuvo la clave directa (hardcodeada) para que la app corra sin configurar variables. En producción se usaría la variable de entorno. **Las dos formas se evalúan en examen.**

#### Para el examen
- `${VAR}` en properties = inyectar desde variable de entorno.
- Secretos (claves, contraseñas) → fuera del código, en variables de entorno.

---

# PULL 2
## Métodos asíncronos con valor de retorno (Future) y Jobs con trabajadores

El segundo pull introdujo el concepto más importante: **async que devuelve un resultado** y **coordinar varios hilos en paralelo**.

---

### 6. `Future` / `CompletableFuture`

#### Teoría
Antes tus métodos `@Async` eran `void`: "dispara y olvida" (lanzas la tarea y no esperas nada). Pero a veces **sí necesitas el resultado**. Para eso el método async devuelve un **`Future<T>`**.

- **`Future<T>`** = una "promesa" de un valor de tipo `T` que estará disponible **en el futuro**. El método devuelve el `Future` de inmediato (sin bloquear), y el valor real se obtiene después con `future.get()`.
- **`future.get()`** = "dame el resultado; si todavía no está listo, **espérame aquí** hasta que lo esté" (operación **bloqueante**).
- **`CompletableFuture.completedFuture(valor)`** = empaqueta un valor ya calculado dentro de un `Future`.

#### Práctica — `LogService.delete`
```java
@Async
@Transactional
public Future<String> delete(String id) {
    repository.deleteById(id);
    try {
        Thread.sleep(5000);            // simula que borrar tarda 5 segundos
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return CompletableFuture.completedFuture(id);   // devuelve el id ya procesado
}
```
- El método corre en otro hilo (por `@Async`) y **devuelve un `Future<String>`** al instante.
- Quien lo llama recibe la promesa y puede seguir trabajando; recoge el resultado cuando quiera con `.get()`.

#### Para el examen
- `void` async = dispara y olvida. `Future<T>` async = dispara y **puedes recoger el resultado**.
- `future.get()` **bloquea** hasta que el resultado esté listo.
- `CompletableFuture.completedFuture(x)` envuelve un valor en un `Future`.

---

### 7. Patrón scatter-gather (trabajadores en paralelo)

#### Teoría
**Scatter-gather** ("dispersar y recolectar") es el patrón de:
1. **Scatter**: lanzar **muchas** tareas async a la vez (cada una un trabajador en su propio hilo).
2. **Gather**: esperar a que **todas** terminen y recoger sus resultados.

La ganancia es el **paralelismo**:
```
Sin async (secuencial):
  delete(1) 5s → delete(2) 5s → delete(3) 5s = 15 segundos

Con async + Future (paralelo):
  delete(1) ┐
  delete(2) ┤ todos a la vez = ~5 segundos
  delete(3) ┘
```

#### Práctica — en el job (ver código completo en tema 9)
```java
// SCATTER: lanzar un trabajador por cada log (no esperamos aquí)
List<Future<String>> trabajadores = new ArrayList<>();
for (Log log : page.getContent()) {
    trabajadores.add(logService.delete(log.getId()));   // arranca y guarda la promesa
}

// GATHER: ahora sí, esperar a que todos terminen
for (Future<String> future : trabajadores) {
    log.info(future.get());     // bloquea hasta que ESE trabajador termina
}
```
**El truco está en separar los dos bucles**:
- En el **primer for** se *arrancan* todos los trabajadores (no se espera ninguno) → corren en paralelo.
- En el **segundo for** se *recogen* los resultados → aquí se espera, pero como ya estaban corriendo juntos, el tiempo total es el del más lento, no la suma.

> Si llamaras `logService.delete(...).get()` dentro de un solo bucle, **perderías el paralelismo** (esperarías cada uno antes de lanzar el siguiente = secuencial otra vez).

#### Para el examen
- Scatter = lanzar todas las tareas; Gather = esperar todas con `.get()`.
- **Dos bucles separados** = paralelo. Un solo bucle con `.get()` dentro = secuencial.
- El pool de hilos (`taskLog`) es el que provee los "trabajadores".

---

### 8. Paginación: `Page`, `Pageable`, `PageRequest`, `Sort`

#### Teoría
**Paginar** = traer los datos **de a páginas** (ej: 10 registros por vez) en vez de todos de golpe. Imprescindible con tablas grandes (no cargas 1 millón de logs en memoria).

| Clase | Qué es |
|---|---|
| **`Pageable`** | La *petición* de página: "quiero la página 2, de tamaño 10, ordenada por fecha". |
| **`PageRequest.of(pagina, tamaño, sort)`** | La forma de **construir** un `Pageable`. |
| **`Sort`** | El criterio de ordenamiento (`Sort.by("createdDate").descending()`). |
| **`Page<T>`** | El *resultado*: los registros de esa página + metadatos (total de páginas, total de elementos, etc.). |

#### Práctica
```java
Pageable pageable = PageRequest.of(index, 2, Sort.by("createdDate").descending());
Page<Log> page = logService.findAllByOrderByDateDesc(pageable);

page.getContent();       // los logs de ESTA página (máx 2)
page.getTotalPages();    // cuántas páginas hay en total
```
El job recorre todas las páginas con:
```java
int index = 0;
do {
    Pageable pageable = PageRequest.of(index, 2, Sort.by("createdDate").descending());
    page = logService.findAllByOrderByDateDesc(pageable);
    // ... procesar page.getContent() ...
    index++;
} while (index < page.getTotalPages());   // repetir mientras queden páginas
```

#### Para el examen
- `Pageable` = lo que pides; `Page<T>` = lo que recibes.
- `PageRequest.of(nº, tamaño, Sort)` construye la petición.
- `page.getTotalPages()` / `page.getContent()` para recorrer todo.

---

### 9. Job que coordina trabajadores

Aquí se **juntan todos los conceptos**: un `@Scheduled` (job) que pagina (`Page`) y borra con trabajadores async (`Future`).

```java
@Scheduled(cron = "0 */1 * * * *")     // cada minuto (job)
public void listarEmpresas() throws ExecutionException, InterruptedException {

    int index = 0;
    Page<Log> page;
    do {
        // 1) PAGINAR: pedir la página actual de logs
        Pageable pageable = PageRequest.of(index, 2, Sort.by("createdDate").descending());
        page = logService.findAllByOrderByDateDesc(pageable);

        // 2) SCATTER: lanzar un trabajador async por cada log de la página
        List<Future<String>> trabajadores = new ArrayList<>();
        for (Log log : page.getContent()) {
            trabajadores.add(logService.delete(log.getId()));
        }

        // 3) GATHER: esperar a que todos los trabajadores terminen
        for (Future<String> future : trabajadores) {
            log.info(future.get());
        }
        index++;
    } while (index < page.getTotalPages());     // 4) siguiente página
}
```

#### Las 4 piezas trabajando juntas
1. **Job (`@Scheduled`)**: el reloj dispara el método cada minuto, en el hilo `Miguel-1`.
2. **Paginación (`Page`)**: procesa los logs de a 2, sin cargarlos todos.
3. **Scatter (`@Async` + `Future`)**: borra los 2 logs en paralelo (hilos `TaskLog-X`).
4. **Gather (`future.get()`)**: espera a que la página termine antes de pasar a la siguiente.

#### Detalle fino (puede caer en examen)
En `for (Log log : page.getContent())` la variable se llama `log`, lo que **oculta temporalmente** al logger `log` de `@Slf4j` dentro de ese bucle. Por eso los `log.info(...)` están **fuera** de ese for. Es Java válido, pero mala práctica de nombres.

#### Para el examen
- Un job puede combinar: scheduling + paginación + async con Future.
- El orden importa: paginar → lanzar trabajadores → esperar resultados → siguiente página.

---

# PULL 3
## Excepciones personalizadas y manejo de errores HTTP

Este pull mejoró **cómo la API le comunica los errores al cliente**. Antes, cualquier fallo devolvía un `500 Internal Server Error` genérico y sin pistas. Ahora se distingue entre "error por culpa del cliente" (datos inválidos → `400`) y "error por culpa del servidor" (algo inesperado → `500`), con un mensaje claro.

---

### 10. Excepciones personalizadas

#### Teoría
Una **excepción personalizada** es una clase de error **propia**, creada por ti, que representa una situación de negocio concreta (ej: "los datos enviados no son válidos"). Sirve para **distinguir** tus errores controlados de los errores genéricos e inesperados de Java/Spring.

`OperationException` hereda de **`RuntimeException`** (excepción *no checked* / *unchecked*):
```java
public class OperationException extends RuntimeException {
    public OperationException(String message) {
        super(message);
    }
}
```

**¿Por qué `RuntimeException` y no `Exception`?**

| | `extends Exception` (checked) | `extends RuntimeException` (unchecked) |
|---|---|---|
| ¿Obliga a `throws` o `try/catch`? | Sí, el compilador te obliga | No, es opcional |
| ¿Hace rollback por defecto en `@Transactional`? | **No** (necesita `rollbackFor`) | **Sí**, automáticamente |
| Uso típico | Errores recuperables previstos | Errores de lógica/validación |

Al ser `RuntimeException`, no ensucia las firmas de los métodos con `throws` y además **dispara el rollback de la transacción automáticamente** (recuerda el tema de transacciones del parcial 2).

#### Práctica — lanzarla en el servicio (`EmpresaService`)
```java
if (StringUtil.isNullOrEmpty(empresa.getNombre())) {
    log.error("Error al guardar empresa. El campo nombre null");
    logService.errorTx("Error al guardar empresa. El campo nombre null");
    throw new OperationException("El campo nombre es null");   // <-- error de negocio
}
```
El servicio **lanza** la excepción con un mensaje claro; **no decide** el código HTTP (eso es responsabilidad del controlador). Separación de responsabilidades: el servicio sabe de *negocio*, el controlador sabe de *HTTP*.

#### Para el examen
- Excepción personalizada = clase propia que `extends RuntimeException`.
- `RuntimeException` (unchecked): no obliga a `throws` y **sí** hace rollback automático.
- El servicio **lanza** la excepción; el controlador **decide qué HTTP devolver**.

---

### 11. Traducir excepciones a HTTP (`ResponseStatusException`)

#### Teoría
**`ResponseStatusException`** es la clase de Spring que convierte una excepción de Java en una **respuesta HTTP con un código de estado y un mensaje** específicos. Es el "traductor" entre el mundo de las excepciones y el mundo de los códigos HTTP.

#### Práctica — el controlador (`EmpresaController.guardar`)
```java
@PostMapping
public ResponseEntity<Void> guardar(@RequestBody EmpresaRequestDto empresa) {
    try {
        this.empresaService.save(empresa);
        return ResponseEntity.ok().build();                      // 200 OK
    } catch (OperationException e) {                             // error del CLIENTE
        log.error("Error al guardar empresa. Message: {}", e.getMessage());
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());          // 400
    } catch (Exception e) {                                     // error del SERVIDOR
        log.error("Error al guardar empresa", e);
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Se generó un error genérico al guardar empresa");                          // 500
    }
}
```

**Lo que ocurre, paso a paso:**
1. Si los datos son inválidos, el servicio lanza `OperationException`.
2. El primer `catch` la atrapa y la **re-lanza** como `ResponseStatusException` con código **400 Bad Request** y el mensaje original.
3. Cualquier otro fallo cae en el segundo `catch` → **500 Internal Server Error** con un mensaje genérico (sin filtrar detalles internos al cliente).

#### El orden de los `catch` IMPORTA (cae en examen)
`OperationException` **es una** `Exception` (por herencia). Por eso el `catch` específico (`OperationException`) debe ir **antes** que el genérico (`Exception`). Si los inviertes, el genérico atraparía todo primero y el `400` **nunca** se ejecutaría. Java obliga a ordenar **de lo más específico a lo más general**.

#### Tabla de códigos HTTP relevantes
| Código | Nombre | Significado | Cuándo |
|---|---|---|---|
| **200** | OK | Todo bien | Empresa guardada |
| **400** | Bad Request | "Tú mandaste algo mal" | Nombre/descripción vacíos (`OperationException`) |
| **401** | Unauthorized | "No estás autenticado" | Falta token JWT |
| **403** | Forbidden | "No tienes permiso" | Rol incorrecto (`@Secured`) |
| **500** | Internal Server Error | "Falló algo de mi lado" | Excepción inesperada |

#### Para el examen
- `ResponseStatusException(HttpStatus.X, "mensaje")` = devolver el código HTTP X con un mensaje.
- 4xx = culpa del cliente; 5xx = culpa del servidor.
- Ordena los `catch` de específico → genérico.

---

### 12. Problem Details (RFC 7807)

#### Teoría
**Problem Details** es un **estándar** (RFC 7807) para que TODOS los errores de una API tengan el **mismo formato JSON**, en lugar de cada error con su propia estructura. Así el frontend siempre sabe dónde leer el mensaje.

Se activa con una sola línea en `application.properties`:
```properties
spring.mvc.problem-details.enabled=true
```

#### Práctica — la respuesta que recibe el cliente
Cuando mandas un POST con el nombre vacío, en vez de un error sin formato, recibes:
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "El campo nombre es null",
  "instance": "/api/v1/empresas"
}
```
- **`status`** → el código (400).
- **`title`** → el nombre del código.
- **`detail`** → tu mensaje (el de la `OperationException`). **Aquí lee el frontend** para mostrarle algo útil al usuario.
- **`instance`** → la URL que falló.

#### Por qué importa
Sin esto, el frontend tendría que adivinar cómo viene cada error. Con Problem Details, **todos** los errores de la API tienen la misma estructura → el cliente los maneja de forma uniforme. Es una práctica profesional de diseño de APIs REST.

#### Para el examen
- Problem Details (RFC 7807) = formato JSON **estándar** para errores.
- Se activa con `spring.mvc.problem-details.enabled=true`.
- El campo `detail` lleva el mensaje que pusiste en la excepción.

---

### El flujo completo de un error (resumen del Pull 3)
```
POST /api/v1/empresas  con  { "nombre_empresa": "", "descripcion": "x" }
  └─ EmpresaController.guardar()
       └─ empresaService.save()
            └─ nombre vacío → throw new OperationException("El campo nombre es null")   [SERVICIO: detecta el error de negocio]
       └─ catch (OperationException)
            └─ throw new ResponseStatusException(400, "El campo nombre es null")        [CONTROLADOR: decide el HTTP]
  └─ Spring + problem-details
       └─ responde JSON 400 con { "status":400, "detail":"El campo nombre es null", ... } [SPRING: formatea estándar]
```
Tres capas, tres responsabilidades: **el servicio detecta**, **el controlador traduce a HTTP**, **Spring formatea la respuesta**.

---

# MAPA DE CONCEPTOS PARA EL EXAMEN

| Tema | Anotación / Clase clave | Para qué sirve |
|---|---|---|
| **Webhook** | `@RequestHeader`, `@RequestBody String` | Recibir llamadas HTTP automáticas de otro sistema |
| **HMAC** | `HmacUtils`, `HMAC_SHA_256` | Verificar autenticidad+integridad con clave secreta |
| **JSONObject** | `new JSONObject()`, `.put()`, `.getString()` | Construir/leer JSON manualmente sin DTO |
| **Enum en BD** | `@Enumerated(EnumType.STRING)` | Guardar valores restringidos como texto |
| **Auditoría** | `extends AuditableEntity`, `createdDate` | Fecha/autor de creación automáticos |
| **Variables de entorno** | `${EVENTOP_SECRET_KEY}` | Sacar secretos del código |
| **Async con resultado** | `Future<T>`, `CompletableFuture` | Método async que devuelve un valor |
| **Esperar resultado** | `future.get()` | Bloquear hasta que el async termine |
| **Scatter-gather** | dos bucles (lanzar / recoger) | Ejecutar N tareas en paralelo y esperarlas |
| **Paginación** | `Page`, `Pageable`, `PageRequest`, `Sort` | Traer datos de a páginas |
| **Job** | `@Scheduled(cron=...)` | Ejecutar código periódicamente |
| **Excepción personalizada** | `extends RuntimeException` | Distinguir errores de negocio propios |
| **Error → HTTP** | `ResponseStatusException(HttpStatus.X, msg)` | Traducir una excepción a un código HTTP |
| **Problem Details** | `spring.mvc.problem-details.enabled=true` | Formato JSON estándar (RFC 7807) para errores |

## Flujo mental para repasar (el job de borrado de logs)

```
Cada minuto (reloj → hilo Miguel-1)
  └─ listarEmpresas()
       └─ repite por cada página de logs:
            ├─ PageRequest.of(i, 2, Sort by createdDate desc)   ← paginación
            ├─ por cada log: logService.delete(id) ─→ Future     ← scatter (hilos TaskLog-X en paralelo)
            ├─ por cada Future: future.get()                     ← gather (espera a todos)
            └─ i++ mientras i < totalPages
```

Si puedes narrar ese diagrama y explicar **por qué los dos bucles van separados** (para que los borrados sean paralelos y no secuenciales), dominas el corazón de este parcial. 💪

---

### Conexión con el parcial anterior
- **HMAC** aquí = la misma idea de firma del **JWT** del parcial 2 (clave secreta simétrica), aplicada al body de un webhook.
- **`Future`** aquí = la evolución del **`@Async void`** del parcial 2 (ahora devuelve resultado).
- **Paginación** aquí = la evolución de los **repositorios** del parcial 2 (ahora devuelven `Page` en vez de `List`).
- **`OperationException` (unchecked)** se conecta con **transacciones** del parcial 2: al ser `RuntimeException`, dispara el **rollback automático** sin necesidad de `rollbackFor`.
- **`ResponseStatusException`** complementa la **seguridad** del parcial 2: igual que el filtro JWT devuelve `401/403`, ahora el controlador devuelve `400/500` con mensaje claro.
