# Summer Framework (Spring-like core)

This directory contains a Spring-like IoC/DI framework written from scratch.

## Implemented in v0.1
- Annotation-based component discovery (`@Component`, `@ComponentScan`)
- Java config + factory methods (`@Configuration`, `@Bean`)
- Constructor injection + field injection (`@Autowired`)
- Bean scopes (`singleton`, `prototype`)
- Bean lifecycle callbacks
  - `BeanNameAware`
  - `InitializingBean`
- `DisposableBean`
- Container extension point (`BeanPostProcessor`)

## Implemented in v0.2 (lightweight REST)
- JDK HTTP server based runtime (`TinyRestServer`)
- REST annotations:
  - `@RestController`
  - `@RequestMapping`
  - `@GetMapping`
  - `@PostMapping`
  - `@PutMapping`
  - `@DeleteMapping`
  - `@PatchMapping`
  - `@RequestBody`
  - `@PathVariable`
  - `@RequestParam`
  - `@RequestHeader`
- Lightweight JSON parser + serializer (no external dependency)
- JSON request body binding to records/POJOs
- Thread tuning layer:
  - configurable executor pool
  - overload guard (`503`)
  - optional request timeout (`504`)

## Design
- `/Users/seydaozdemir/Documents/GitHub/summer-framework/summer-framework/CORE_CONTAINER_DESIGN.md`

## How to run
```bash
mkdir -p summer-framework/out
javac -d summer-framework/out \
  $(find summer-framework/core/src/main/java -name "*.java") \
  $(find summer-framework/example/src/main/java -name "*.java")

java -cp summer-framework/out io.summerframework.example.Main
```

## Package as reusable framework (Maven)
From `/Users/seydaozdemir/Documents/GitHub/summer-framework/summer-framework`:

```bash
# build only core artifact
mvn -pl core -am -DskipTests package

# install core artifact to local Maven repository
mvn -pl core -am -DskipTests install
```

Produced artifact:
- `core/target/summer-framework-core-0.1.0.jar`

Use in another project:
```xml
<dependency>
  <groupId>io.summerframework</groupId>
  <artifactId>summer-framework-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Package without internet (JDK only)
If Maven plugin download is unavailable, package core jar directly:

```bash
./scripts/package-core.sh 0.1.0
```

Generated jar:
- `core/target/summer-framework-core-0.1.0.jar`

## Publish to GitHub Packages
Repository workflow:
- `/Users/seydaozdemir/Documents/GitHub/summer-framework/.github/workflows/publish-summer-framework.yml`

How it publishes:
- Trigger manually with `workflow_dispatch` or by creating a GitHub Release.
- Workflow runs:
  - `mvn -B -DskipTests -pl core -am deploy`
  - in `/Users/seydaozdemir/Documents/GitHub/summer-framework/summer-framework`
- Uses built-in `GITHUB_TOKEN` with `packages:write` permission.

If you want to publish from local machine:
```xml
<!-- ~/.m2/settings.xml -->
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>GITHUB_USERNAME</username>
      <password>GITHUB_PAT_WITH_packages_write</password>
    </server>
  </servers>
</settings>
```

Then:
```bash
cd /Users/seydaozdemir/Documents/GitHub/summer-framework/summer-framework
GITHUB_REPOSITORY=OWNER/REPO mvn -B -DskipTests -pl core -am deploy
```

## Run REST server
```bash
mkdir -p summer-framework/out
javac -d summer-framework/out \
  $(find summer-framework/core/src/main/java -name "*.java") \
  $(find summer-framework/example/src/main/java -name "*.java")

java -cp summer-framework/out io.summerframework.example.RestMain 8080
```

Or via Maven example module:
```bash
mvn -pl example -am -DskipTests compile exec:java -Dexec.args="8080"
```

## Test endpoints
```bash
curl -s http://localhost:8080/api/health

curl -s -X POST http://localhost:8080/api/welcome \
  -H 'Content-Type: application/json' \
  -d '{"name":"Seyda"}'

curl -s "http://localhost:8080/api/users/42?verbose=true"

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

## Run with tuning
`RestMain` args:
- arg1: `port` (default `8080`)
- arg2: `requestTimeoutMillis` (default `0`, disabled)
- arg3: `maxConcurrentRequests` (default `512`)
- arg4: `coreThreads` (optional)
- arg5: `maxThreads` (optional)
- arg6: `queueCapacity` (optional)
- arg7: `rejectionPolicy` (`ABORT`, `CALLER_RUNS`, `DISCARD_OLDEST`)
- arg8: `socketBacklog` (optional, default `1024`)

Example:
```bash
java -cp summer-framework/out io.summerframework.example.RestMain 8080 1000 64
```

Then:
```bash
curl -s http://localhost:8080/api/slow
```
With `requestTimeoutMillis=1000`, this returns `504` because `/api/slow` sleeps for 1500ms.

## High-concurrency profile (example)
```bash
java -cp summer-framework/out io.summerframework.example.RestMain \
  8080 \
  0 \
  2000 \
  32 \
  128 \
  2000 \
  ABORT \
  4096
```
This profile is tuned to fail fast (`503`) under overload instead of slowing down the whole server.

## Quick load test
```bash
# healthy high-load profile
java -cp summer-framework/out io.summerframework.example.RestMain 8080 0 2000 32 128 2000 ABORT 4096

# in another terminal
seq 1 400 | xargs -n1 -P100 -I{} sh -c "curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/health" | sort | uniq -c
```
