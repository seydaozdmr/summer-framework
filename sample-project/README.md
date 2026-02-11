# Sample Project (Using Summer Framework)

This project shows how to build a standalone REST API using `summer-framework-core`.

## Requirements

- Java 17+
- Maven 3.9+
- Access to GitHub Packages for `seydaozdmr/summer-framework` or local core install

## Maven Auth for GitHub Packages

Add a `github` server entry to `~/.m2/settings.xml`:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_PAT_WITH_read_packages</password>
    </server>
  </servers>
</settings>
```

Install core locally (recommended while developing this repository):

```bash
cd ../summer-framework
mvn -pl core -am -DskipTests install
```

Then run this sample. This ensures latest local framework changes are used.

## Run

```bash
mvn -DskipTests compile exec:java
```

Default startup config is read from:
- `src/main/resources/application.properties`

## Endpoints

```bash
curl -s http://localhost:8081/api/health

curl -s -X POST http://localhost:8081/api/todos \
  -H 'Content-Type: application/json' \
  -d '{"title":"Write docs","note":"Add API examples"}'

curl -s http://localhost:8081/api/todos

curl -s http://localhost:8081/api/todos/1

curl -s -X PATCH http://localhost:8081/api/todos/1/completed \
  -H 'Content-Type: application/json' \
  -d '{"completed":true}'

curl -i -X DELETE http://localhost:8081/api/todos/1
```

## Postman

Import these files:

- `postman/Summer Framework Todo Sample.postman_collection.json`
- `postman/Summer Framework Local.postman_environment.json`

Then select environment `Summer Framework Local` and run requests in order.
`Create Todo` test script automatically sets collection variable `todoId`.

## Config Override

You can override settings from command line:

```bash
mvn -DskipTests compile exec:java \
  -Dexec.args="--server.port=8090 --summer.server.max-concurrent-requests=2000 --summer.server.rejection-policy=CALLER_RUNS"
```

Supported properties:
- `server.port`
- `summer.server.request-timeout-millis`
- `summer.server.max-concurrent-requests`
- `summer.server.core-threads`
- `summer.server.max-threads`
- `summer.server.queue-capacity`
- `summer.server.keep-alive-seconds`
- `summer.server.rejection-policy` (`ABORT`, `CALLER_RUNS`, `DISCARD_OLDEST`)
- `summer.server.socket-backlog`
