# Sample Project (Using Summer Framework)

This project shows how to build a standalone REST API using `summer-framework-core`.

## Requirements

- Java 17+
- Maven 3.9+
- Access to GitHub Packages for `seydaozdmr/summer-framework`

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

If you already installed locally:

```bash
cd ../summer-framework
mvn -pl core -am -DskipTests install
```

then this sample can also resolve from local Maven cache.

## Run

```bash
mvn -DskipTests compile exec:java -Dexec.args="8081"
```

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
