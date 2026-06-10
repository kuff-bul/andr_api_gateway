# API Gateway

Spring Cloud Gateway routes the FlowManager API through Eureka service discovery.

## Prerequisites

- Java 17
- Eureka Server on `http://localhost:8761`
- FlowManager registered in Eureka as `flow-manager`

## Run

Start Eureka Server, FlowManager, and then the gateway:

```powershell
mvn spring-boot:run
```

The gateway listens on `http://localhost:8082`. FlowManager endpoints are available
without changing their paths:

```text
POST http://localhost:8082/api/v1/files
GET  http://localhost:8082/api/v1/files/{id}/status
GET  http://localhost:8082/api/v1/files/{id}/download
```

The Eureka URL and server port can be overridden:

```powershell
$env:EUREKA_DEFAULT_ZONE = "http://localhost:8761/eureka/"
$env:SERVER_PORT = "8082"
mvn spring-boot:run
```
