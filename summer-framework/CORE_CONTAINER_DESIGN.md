# Core Container Design (Spring-like from scratch)

## Goal
Build a minimal IoC/DI container that mimics Spring core behavior without using Spring libraries.

## Scope v0.1
- Bean definition registry
- Constructor + field injection
- Singleton and prototype scope
- Annotation-driven scanning
- `@Configuration` + `@Bean` factory methods
- Bean lifecycle callbacks
- Bean post-processor extension point

## Architecture

### 1) Metadata layer
- `BeanDefinition`
  - bean name
  - bean class
  - scope (`singleton`, `prototype`)
  - optional factory method metadata (`configurationClass`, `method`)

### 2) Container layer
- `BeanFactory`
  - `getBean(String)`
  - `getBean(Class<T>)`
- `AnnotationApplicationContext`
  - registry (`beanDefinitions`)
  - singleton cache (`singletons`)
  - lifecycle orchestration (`refresh`, `close`)

### 3) Annotation model
- `@Component`
- `@Configuration`
- `@Bean`
- `@Autowired`
- `@Scope`
- `@ComponentScan`

### 4) Lifecycle model
Creation pipeline:
1. Resolve bean definition
2. Instantiate (constructor injection)
3. Inject fields (`@Autowired`)
4. Aware callback (`BeanNameAware`)
5. `BeanPostProcessor#postProcessBeforeInitialization`
6. Init callback (`InitializingBean#afterPropertiesSet`)
7. `BeanPostProcessor#postProcessAfterInitialization`
8. Cache singleton (if singleton scope)

### 5) Extension points
- `BeanPostProcessor`
- `InitializingBean`
- `DisposableBean`
- `BeanNameAware`

## Non-goals for v0.1
- AOP/proxying
- Circular dependency resolution
- Profiles/conditions
- Advanced type conversion
- Web MVC

## Next milestones
- v0.2: simple event bus
- v0.3: interceptor/proxy based AOP
- v0.4: minimal dispatcher + controller model

## Current lightweight web layer (implemented)
- Runtime: JDK `HttpServer` (no servlet container)
- Route discovery:
  - `@RestController` classes from container
  - `@RequestMapping` + `@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`/`@PatchMapping`
- Invocation:
  - Reflection-based handler invocation
  - Supports `@RequestBody`, `@PathVariable`, `@RequestParam`, `@RequestHeader`
  - Multi-value query params (`?tag=a&tag=b`) -> `List<T>`
- Serialization:
  - Built-in JSON parser/serializer (no external libs)
- Runtime protections:
  - Thread pool tuning (`core/max/queue/rejection`)
  - Socket backlog tuning
  - Overload guard with `503`
  - Optional per-request timeout with `504`
