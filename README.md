# Summer Framework

Summer Framework, Spring'den ilham alan ve sifirdan yazilmis hafif bir Java framework'udur.

Bu repo yalnizca Summer Framework kodunu icerir. Spring Boot, Tomcat veya Servlet API zorunlulugu yoktur.

## Proje Yapisi

```text
summer-framework/
  core/      -> Framework cekirdegi (IoC + web runtime)
  example/   -> Ornek uygulama ve endpoint'ler
sample-project/ -> Framework'u disaridan kullanan bagimsiz ornek
```

## One Cikan Ozellikler

- Annotation tabanli IoC/DI container
  - `@Configuration`, `@Bean`, `@Component`, `@ComponentScan`
  - `@Autowired` (constructor + field injection)
  - `@Scope("singleton" | "prototype")`
- Lifecycle ve extension noktasi
  - `BeanPostProcessor`, `BeanNameAware`, `InitializingBean`, `DisposableBean`
- Hafif REST runtime (JDK `HttpServer`)
  - `@RestController`, `@RequestMapping`
  - `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`
- Parametre binding
  - `@RequestBody`, `@PathVariable`, `@RequestParam`, `@RequestHeader`
  - Multi-query (`?tag=a&tag=b`) -> `List<T>`
- Dahili JSON parser/serializer (ek kutuphane olmadan)
- Thread tuning katmani
  - configurable thread pool (core/max/queue/rejection policy)
  - overload guard (`503 Service Unavailable`)
  - request timeout (`504 Gateway Timeout`)
- Standart JSON API envelope (success/error, timestamp, path)
- Standart bootstrap
  - `SummerApplication.run(...)`
  - `application.properties` tabanli ayar + CLI override

## Hizli Baslangic

Gereksinimler:
- Java 17+
- Maven 3.9+

```bash
cd summer-framework
mvn -pl example -am -DskipTests compile exec:java
```

Sunucu kalktiktan sonra test:

```bash
curl -s http://localhost:8080/api/health

curl -s -X POST http://localhost:8080/api/welcome \
  -H 'Content-Type: application/json' \
  -d '{"name":"Seyda"}'

curl -s 'http://localhost:8080/api/search?tag=java&tag=framework&limit=5' \
  -H 'x-request-id: req-123'
```

## Runtime Tuning

Varsayilan konfigurasyon:
- `summer-framework/example/src/main/resources/application.properties`

Ornek:

```bash
cd summer-framework
mvn -pl example -am -DskipTests compile exec:java \
  -Dexec.args="--server.port=8080 --summer.server.request-timeout-millis=1000 --summer.server.max-concurrent-requests=256 --summer.server.core-threads=8 --summer.server.max-threads=32 --summer.server.queue-capacity=500 --summer.server.rejection-policy=ABORT --summer.server.socket-backlog=1024"
```

## Kutuphane Olarak Paketleme

Core artifact'i olustur:

```bash
cd summer-framework
mvn -pl core -am -DskipTests package
```

Lokal Maven repo'ya kur:

```bash
cd summer-framework
mvn -pl core -am -DskipTests install
```

Dependency:

```xml
<dependency>
  <groupId>io.summerframework</groupId>
  <artifactId>summer-framework-core</artifactId>
  <version>0.1.1</version>
</dependency>
```

## Notlar

- Bu framework Servlet API ile birebir uyumlu degildir.
- Amac: minimum bagimlilikla, egitsel ve genisletilebilir hafif bir cekirdek sunmaktir.

Detaylar icin: `summer-framework/README.md`
