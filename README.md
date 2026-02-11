# Summer Framework

A lightweight Spring Boot extension framework that provides:

- Standard API response envelope
- Centralized exception handling
- Request id propagation via header
- Zero-config auto setup through a starter

## Modules

- `summer-autoconfigure`: Auto-configuration and web infrastructure.
- `summer-spring-boot-starter`: Starter dependency to enable framework features.
- `sample-app`: Example application that uses the starter.

## Response shape

All controller responses are wrapped into:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-02-10T18:34:11.301Z",
  "requestId": "4bc8d4e9-8863-4b73-92b6-f61f231167f7"
}
```

On errors:

```json
{
  "success": false,
  "data": null,
  "error": {
    "message": "text must not be blank",
    "status": 400
  },
  "timestamp": "2026-02-10T18:35:03.412Z",
  "requestId": "4bc8d4e9-8863-4b73-92b6-f61f231167f7"
}
```

## Quick start

```bash
mvn test
mvn -pl sample-app spring-boot:run
```

Then call:

- `GET /api/hello`
- `POST /api/echo`
- `GET /api/fail`

## Configuration

```yaml
summer:
  framework:
    enabled: true
    include-timestamp: true
    request-id-header: X-Request-Id
```
