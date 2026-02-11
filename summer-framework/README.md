# Summer Framework

Summer Framework, Spring benzeri temel konseptleri minimum bagimlilikla uygulayan hafif bir Java framework'udur.

## Moduller

- `core`: IoC container + lightweight REST runtime
- `example`: Framework kullanan ornek uygulama

## Ozellikler

### 1. IoC / DI
- `@Configuration`, `@Bean`
- `@Component`, `@ComponentScan`
- `@Autowired` constructor ve field injection
- `@Scope("singleton" | "prototype")`
- Circular dependency tespiti (bean olusturma asamasinda)

### 2. Lifecycle
- `BeanNameAware`
- `InitializingBean`
- `DisposableBean`
- `BeanPostProcessor` (before/after initialization)

### 3. Web Runtime
- JDK `HttpServer` tabanli (`TinyRestServer`)
- Annotation routing:
  - `@RestController`, `@RequestMapping`
  - `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`
- Route duplicate kontrolu

### 4. Request Binding
- `@RequestBody` -> record/POJO/string
- `@PathVariable`
- `@RequestParam` (required/default)
- `@RequestHeader` (required/default)
- Tekrarlayan query parametreleri `List<T>` olarak bind edilir

### 5. JSON ve API Cevabi
- Dahili `Json` parser/serializer (harici kutuphane yok)
- Basarili cevaplar:
  - `{"success":true,"path":"...","timestamp":"...","data":...}`
- Hatali cevaplar:
  - `{"success":false,"path":"...","timestamp":"...","status":...,"error":"..."}`

### 6. Performans ve Tuning
- Configurable thread pool (`coreThreads`, `maxThreads`, `queueCapacity`)
- Rejection policy (`ABORT`, `CALLER_RUNS`, `DISCARD_OLDEST`)
- Overload guard (`503`)
- Optional request timeout (`504`)
- Socket backlog ayari

### 7. Standart Bootstrap
- `SummerApplication.run(AppConfig.class, args)` ile uygulama baslatma
- `application.properties` uzerinden merkezi server/tuning ayarlari
- Komut satiri override destegi (`--key=value`)

## Calistirma

Gereksinimler:
- Java 17+
- Maven 3.9+

```bash
mvn -pl example -am -DskipTests compile exec:java
```

## Endpoint Ornekleri

```bash
curl -s http://localhost:8080/api/health

curl -s -X POST http://localhost:8080/api/welcome \
  -H 'Content-Type: application/json' \
  -d '{"name":"Seyda"}'

curl -s 'http://localhost:8080/api/users/42?verbose=true'

curl -s -X PUT http://localhost:8080/api/users/42 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada","active":true}'

curl -i -X DELETE http://localhost:8080/api/users/42

curl -s -X PATCH http://localhost:8080/api/users/42/status \
  -H 'Content-Type: application/json' \
  -H 'x-actor: admin' \
  -d '{"active":false}'

curl -s 'http://localhost:8080/api/search?tag=java&tag=framework&limit=5' \
  -H 'x-request-id: req-123'
```

## Tuning ile Calistirma

Konfigurasyon kaynagi:
- `example/src/main/resources/application.properties`

```bash
mvn -pl example -am -DskipTests compile exec:java \
  -Dexec.args="--server.port=8080 --summer.server.request-timeout-millis=1000 --summer.server.max-concurrent-requests=256 --summer.server.core-threads=8 --summer.server.max-threads=32 --summer.server.queue-capacity=500 --summer.server.rejection-policy=ABORT --summer.server.socket-backlog=1024"
```

Desteklenen property anahtarlari:
- `server.port`
- `summer.server.request-timeout-millis`
- `summer.server.max-concurrent-requests`
- `summer.server.core-threads`
- `summer.server.max-threads`
- `summer.server.queue-capacity`
- `summer.server.keep-alive-seconds`
- `summer.server.rejection-policy`
- `summer.server.socket-backlog`

Geriye donuk uyumluluk:
- Eski positional arg formati hala desteklenir:
  - `port requestTimeoutMillis maxConcurrentRequests coreThreads maxThreads queueCapacity rejectionPolicy socketBacklog`

## Kutuphane Paketleme

```bash
# core jar
mvn -pl core -am -DskipTests package

# local maven'e install
mvn -pl core -am -DskipTests install
```

Artifact:
- `core/target/summer-framework-core-0.1.1.jar`

## Dagitim / Publish

GitHub Actions workflow:
- `/.github/workflows/publish-summer-framework.yml`

Workflow, `core` artifact'ini GitHub Packages'a deploy eder.

## Tasarim Notu

Detayli container tasarimi:
- `CORE_CONTAINER_DESIGN.md`
