# Summer Framework Guide

Bu dokuman, Summer Framework'un nasil calistigini, HTTP istegini nasil aldigini ve hangi mekanizmalari kullandigini adim adim aciklar.

## 1. Genel Mimari

Framework uc ana katmandan olusur:

1. Boot katmani
- `summer-framework/core/src/main/java/io/summerframework/core/boot/SummerApplication.java`

2. IoC/DI container katmani
- `summer-framework/core/src/main/java/io/summerframework/core/context/AnnotationApplicationContext.java`

3. Web/REST katmani
- `summer-framework/core/src/main/java/io/summerframework/core/web/TinyRestServer.java`

## 2. Uygulama Baslatma Akisi

Ornek uygulama:
- `sample-project/src/main/java/io/summerframework/sample/Application.java`

Baslatma akisi:

1. `SummerApplication.run(AppConfig.class, args)` cagrilir.
2. Classpath'ten `application.properties` okunur.
3. Konfigurasyon birlestirilir:
- framework default degerleri
- `application.properties` degerleri
- komut satiri override degerleri
4. `AnnotationApplicationContext` olusturulur ve bean'ler initialize edilir.
5. `TinyRestServer` olusturulur ve HTTP server start edilir.

## 3. Konfigurasyon Mekanizmasi

Desteklenen konfigurasyon kaynaklari:

1. `application.properties` (varsayilan)
2. CLI override
- Yeni format: `--key=value`
- Geriye donuk format: positional arg listesi

Onemli property anahtarlari:

- `server.port`
- `summer.server.request-timeout-millis`
- `summer.server.max-concurrent-requests`
- `summer.server.core-threads`
- `summer.server.max-threads`
- `summer.server.queue-capacity`
- `summer.server.keep-alive-seconds`
- `summer.server.rejection-policy`
- `summer.server.socket-backlog`

## 4. IoC/DI Container Mekanizmasi

Container su annotation ve mekanizmalari destekler:

- `@Configuration`, `@Bean`
- `@Component`, `@ComponentScan`
- `@Autowired` (constructor ve field injection)
- `@Scope("singleton" | "prototype")`

Lifecycle ve extension noktasi:

- `BeanPostProcessor` (before/after initialization)
- `BeanNameAware`
- `InitializingBean`
- `DisposableBean`

Ek koruma:

- Circular dependency tespiti (`beansInCreation` uzerinden)
- Duplicate bean name engeli

## 5. HTTP Istegi Nasil Isleniyor

Istek yasam dongusu:

1. JDK `HttpServer` istekleri `TinyRestServer.handle(...)` metoduna getirir.
2. `OverloadGuard` kontrolu yapilir.
- Limit asilmis ise `503` donulur.
3. Router method + path ile endpoint bulur.
- Endpoint yoksa `404`.
4. Body/query/header parse edilir.
5. Route invoke edilir.
- Timeout aktifse invocation ayri executor'da kosar.
- Sure asimi olursa `504`.
6. Sonuc envelope edilip JSON response donulur.

## 6. Routing ve Parametre Binding

Router tarafinda desteklenen annotation'lar:

- `@RestController`, `@RequestMapping`
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`

Parametre binding:

- `@RequestBody`
- `@PathVariable`
- `@RequestParam` (required/default)
- `@RequestHeader` (required/default)

Ek davranislar:

- Multi query parametre (`?tag=a&tag=b`) -> `List<T>` bind edilir.
- `@RequestBody` tek parametre ile sinirlidir.
- Path variable adi route template ile uyusmak zorundadir.

## 7. JSON ve Veri Donusumu

Framework harici JSON kutuphanesi kullanmaz.

JSON sinifi:
- `summer-framework/core/src/main/java/io/summerframework/core/web/Json.java`

Binding sinifi:
- `summer-framework/core/src/main/java/io/summerframework/core/web/BodyBinder.java`

Desteklenen tipler:

- primitive/wrapper tipler
- `String`
- `Map`, `List`
- record
- POJO

Hata durumlarinda `BadRequestException` uretilebilir ve `400` donulur.

## 8. Response ve Hata Modeli

Framework standart envelope doner:

Basarili cevap:
- `{"success":true,"path":"...","timestamp":"...","data":...}`

Hatali cevap:
- `{"success":false,"path":"...","timestamp":"...","status":...,"error":"..."}`

Yaygin status kodlari:

- `200` OK
- `201` Created
- `204` No Content
- `400` Bad Request
- `404` Not Found
- `503` Overloaded
- `504` Gateway Timeout
- `500` Internal Server Error

## 9. Threading ve Performans Mekanizmasi

Konfigurasyon sinifi:
- `summer-framework/core/src/main/java/io/summerframework/core/web/ServerTuningProperties.java`

Calisma modeli:

- I/O icin thread pool executor
- Timeout aktifse invocation icin ayri executor
- `OverloadGuard` ile ayni anda kabul edilen istek limiti

Rejection policy:

- `ABORT`
- `CALLER_RUNS`
- `DISCARD_OLDEST`

Bu sayede framework yuk altinda:

- fail-fast (`503`) davranisi gosterebilir
- timeout politikasi uygulayabilir (`504`)
- thread ve kuyruk kapasitesini kontrollu yonetebilir

## 10. Spring'e Gore Farklar

Summer Framework:

- Servlet API/Tomcat zorunlu degildir (JDK HttpServer kullanir)
- Cok daha hafiftir, daha az soyutlama icerir
- Auto-configuration/starter ekosistemi yoktur
- AOP, interceptor/filter zinciri, validation gibi gelismis katmanlar sinirlidir

Bu tasarim, ogrenme odakli ve kolay genisletilebilir bir cekirdek hedefler.
