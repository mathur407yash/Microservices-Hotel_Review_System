# Microservices Hotel Review System

A Spring Boot microservices sample for managing users, hotels, and hotel reviews. Services register with Netflix Eureka, communicate through OpenFeign, and are exposed through a Spring Cloud API Gateway.

All included user, hotel, and review records are fictional demonstration data. No database contents are included in this repository; a new installation starts with empty tables.

## Architecture

- **Service Registry**: Netflix Eureka Server at `http://localhost:8761`
- **Config Server**: Spring Cloud Config Server at `http://localhost:8888`
- **API Gateway**: Spring Cloud Gateway Server Web MVC at `http://localhost:8084`
- **User Service**: REST API and user-to-ratings aggregation at port `8081`
- **Hotel Service**: REST API and hotel-to-ratings aggregation at port `8082`
- **Rating Service**: review storage and query API at port `8083`
- **Database**: MySQL, with a separate database for each data service

User and Hotel use OpenFeign clients to resolve Rating and Hotel services by their Eureka service IDs. The gateway routes requests to registered service instances using Spring Cloud LoadBalancer.

## Prerequisites

- Java 17 or later
- MySQL 8.x, running on `localhost:3306` (or configure service-specific JDBC URLs)
- The MySQL account and password for the local database
- OpenSSL for generating the JWT signing key pair
- Windows PowerShell for the commands below; each service also includes a Maven wrapper

The datasource username defaults to `root`; the password is read from the `DB_PASSWORD` environment variable. For a local MySQL account with a password, set it in each service terminal before starting that service:

```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
```

The services create their databases if they do not already exist and use Hibernate `ddl-auto=update` to create/update tables. Configure the database account with permission to create databases, or create these databases and grant access before launch:

- `user_microservice_db`
- `hotel_microservice_db`
- `rating_microservice_db`

To use a different database host, port, or database name, update the corresponding `spring.datasource.url` in `config/user-service.properties`, `config/hotel-service.properties`, or `config/rating-service.properties`. Set `DB_USERNAME` and `DB_PASSWORD` in each service terminal to override the shared defaults.

## Shared configuration

`ConfigServer/config_server` runs the Spring Cloud Config Server. It serves the tracked properties in `config/` through the native filesystem backend. `config/application.properties` holds shared defaults, while `<application-name>.properties` files hold service-specific settings. All services, including the Eureka registry and API Gateway, import their configuration from the Config Server at startup. The gateway route paths and Eureka service IDs are defined in `config/API-GATEWAY.properties`.

## Authentication

Google OpenID Connect is used to authenticate users at the API Gateway. After Google sign-in, the gateway returns a short-lived (15-minute) RS256 JWT. Every business API route is protected by bearer-token authentication at both the gateway and the individual services, so calling a service port directly does not bypass authentication. User and Hotel also forward the caller's bearer token to downstream services through OpenFeign.

Create an OAuth 2.0 client in Google Cloud Console. Add `http://localhost:8084/login/oauth2/code/google` as an authorized redirect URI. Generate an RSA key pair outside the repository and keep the private key secret:

```powershell
$keyDirectory = Join-Path $env:USERPROFILE ".hotel-review-jwt"
New-Item -ItemType Directory -Force $keyDirectory | Out-Null
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out (Join-Path $keyDirectory "jwt-private.pem")
openssl pkey -in (Join-Path $keyDirectory "jwt-private.pem") -pubout -out (Join-Path $keyDirectory "jwt-public.pem")
```

Set these values in the process environment of each service terminal before starting it. All services need the public key; only the gateway needs the private key and Google OAuth client credentials:

```powershell
$keyDirectory = Join-Path $env:USERPROFILE ".hotel-review-jwt"
$env:JWT_PUBLIC_KEY = Get-Content (Join-Path $keyDirectory "jwt-public.pem") -Raw
```

In the gateway terminal, also set:

```powershell
$env:JWT_PRIVATE_KEY = Get-Content (Join-Path $keyDirectory "jwt-private.pem") -Raw
$env:GOOGLE_CLIENT_ID = "your-google-client-id"
$env:GOOGLE_CLIENT_SECRET = "your-google-client-secret"
```

Start Google sign-in by opening `http://localhost:8084/oauth2/authorization/google` in a browser. On successful sign-in, the callback responds with JSON containing `access_token`, `token_type`, and `expires_in`. Send the token in the `Authorization: Bearer <access_token>` header on API requests. For example:

```powershell
$token = @{ access_token = "paste-the-access_token-from-the-Google-login-response" }
$headers = @{ Authorization = "Bearer $($token.access_token)" }
$env:API_ACCESS_TOKEN = $token.access_token
```

The `/actuator/health` endpoint remains public for liveness checks on User and Hotel. Other actuator endpoints and all business endpoints require a valid JWT. The RSA public key, issuer, and audience are validated by each API service; do not commit OAuth credentials or the private key.

To verify anonymous access is rejected at both the gateway and direct service ports, run `.\tests\api-smoke\Security-E2E.ps1` after starting all services. The other API E2E scripts require a valid JWT in `$env:API_ACCESS_TOKEN`.

## Inter-service resilience

User and Hotel wrap their outbound Feign calls with Resilience4j circuit breakers and rate limiters using Spring AOP. Each downstream dependency is limited to 20 calls per second per service instance; excess calls fail fast with HTTP `429` and `Retry-After: 1`. Circuit breakers use a 10-call window, open after at least five calls when 50% fail, remain open for 10 seconds, and then allow three half-open probes. Calls blocked by an open circuit and failed downstream calls return HTTP `503`; responses are not replaced with empty success-shaped data.

User and Hotel expose Spring Actuator endpoints at `/actuator/health`, `/actuator/metrics`, `/actuator/circuitbreakers`, and `/actuator/ratelimiters`. The circuit breaker registry is available through Actuator without marking service health DOWN when a dependency circuit opens. Only `/actuator/health` is public; other Actuator endpoints require a valid bearer token.

The policy is configured centrally in `config/application.properties`. After changing shared configuration, restart the affected service to load the settings.

## Run the services

Start each command in a separate terminal from the repository root, in this order. Keep each terminal open while using the system.

1. Start the Config Server:

```powershell
& .\Hotel\hotel_service\mvnw.cmd -f .\ConfigServer\config_server\pom.xml spring-boot:run
```

2. Start the Eureka registry:

```powershell
& .\Hotel\hotel_service\mvnw.cmd -f .\ServiceRegistry\service_registry\pom.xml spring-boot:run
```

3. Start Rating, User, and Hotel. Set `DB_PASSWORD` in each terminal first if needed:

```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
& .\Rating\rating_service\mvnw.cmd -f .\Rating\rating_service\pom.xml spring-boot:run
```

```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
& .\User\user_service\mvnw.cmd -f .\User\user_service\pom.xml spring-boot:run
```

```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
& .\Hotel\hotel_service\mvnw.cmd -f .\Hotel\hotel_service\pom.xml spring-boot:run
```

4. Start the API Gateway in another terminal:

```powershell
& .\Hotel\hotel_service\mvnw.cmd -f .\APIGateway\api_gateway\pom.xml spring-boot:run
```

The Eureka dashboard is at `http://localhost:8761`. It should list `API-GATEWAY`, `USER-SERVICE`, `HOTEL-SERVICE`, and `RATING-SERVICE` when all processes are healthy. Stop a service with `Ctrl+C` in its terminal.

## API routes

Use the gateway base URL `http://localhost:8084` and attach the bearer token acquired above. The same routes are also available directly from each service at its port and require the same token.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/users` | List users |
| `POST` | `/adduser` | Create a user |
| `GET` | `/users/{userid}` | Get a user, their ratings, and hotel details for each rating |
| `DELETE` | `/users/{userid}` | Delete a user |
| `GET` | `/hotels` | List hotels |
| `POST` | `/hotels` | Create a hotel |
| `GET` | `/hotels/{hotel_id}` | Get a hotel and its ratings |
| `GET` | `/hotels/{hotel_id}/details` | Get a hotel without ratings (internal service lookup) |
| `DELETE` | `/hotels/{hotel_id}` | Delete a hotel |
| `GET` | `/ratings` | List ratings |
| `POST` | `/ratings` | Create a rating |
| `GET` | `/ratings/{ratingId}` | Get a rating |
| `GET` | `/ratings/user/{userid}` | List ratings by user |
| `GET` | `/ratings/hotel/{hotelId}` | List ratings by hotel |
| `DELETE` | `/ratings/{ratingId}` | Delete a rating |

### Example requests

Create a user:

```powershell
$user = @{ userid = "USR-101"; name = "Jordan Lee"; email = "jordan.lee@example.test" } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8084/adduser -Method Post -ContentType 'application/json' -Body $user -Headers $headers
```

Create a hotel:

```powershell
$hotel = @{ hotel_id = "HTL-101"; name = "The Harbor House"; location = "Seattle"; about = "A fictional city hotel near the waterfront." } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8084/hotels -Method Post -ContentType 'application/json' -Body $hotel -Headers $headers
```

Create a rating linking that user and hotel:

```powershell
$rating = @{ ratingId = "REV-101"; hotelId = "HTL-101"; userid = "USR-101"; rating = 5; feedback = "Friendly service and a comfortable stay." } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8084/ratings -Method Post -ContentType 'application/json' -Body $rating -Headers $headers
```

Then query `http://localhost:8084/users/USR-101` or `http://localhost:8084/hotels/HTL-101` with `-Headers $headers` to see the aggregated responses.

## Build

Run these from the repository root:

```powershell
& .\Hotel\hotel_service\mvnw.cmd -f .\ServiceRegistry\service_registry\pom.xml -DskipTests package
& .\Hotel\hotel_service\mvnw.cmd -f .\ConfigServer\config_server\pom.xml -DskipTests package
& .\Hotel\hotel_service\mvnw.cmd -f .\APIGateway\api_gateway\pom.xml -DskipTests package
& .\Hotel\hotel_service\mvnw.cmd -f .\Hotel\hotel_service\pom.xml -DskipTests package
& .\Rating\rating_service\mvnw.cmd -f .\Rating\rating_service\pom.xml -DskipTests package
& .\User\user_service\mvnw.cmd -f .\User\user_service\pom.xml -DskipTests package
```

## Technology

- Java 17
- Spring Boot 4.1.1
- Spring Cloud 2025.1.3
- Spring Cloud Netflix Eureka
- Spring Cloud OpenFeign
- Spring Cloud Gateway Server Web MVC
- Spring Data JPA and MySQL
