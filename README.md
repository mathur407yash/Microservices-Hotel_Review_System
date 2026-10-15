# Microservices Hotel Review System

A Spring Boot microservices sample for managing users, hotels, and hotel reviews. Services register with Netflix Eureka, communicate through OpenFeign, and are exposed through a Spring Cloud API Gateway.

All included user, hotel, and review records are fictional demonstration data. No database contents are included in this repository; a new installation starts with empty tables.

## Architecture

- **Service Registry**: Netflix Eureka Server at `http://localhost:8761`
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
- Windows PowerShell for the commands below; each service also includes a Maven wrapper

The datasource username defaults to `root`; the password is read from the `DB_PASSWORD` environment variable. For a local MySQL account with a password, set it in each service terminal before starting that service:

```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
```

The services create their databases if they do not already exist and use Hibernate `ddl-auto=update` to create/update tables. Configure the database account with permission to create databases, or create these databases and grant access before launch:

- `user_microservice_db`
- `hotel_microservice_db`
- `rating_microservice_db`

To use a different database host, port, or database name, update the corresponding `spring.datasource.url` in that service's `src/main/resources/application.properties`.

## Run the services

Start each command in a separate terminal from the repository root, in this order. Keep each terminal open while using the system.

1. Start the Eureka registry:

```powershell
& .\Hotel\hotel_service\mvnw.cmd -f .\ServiceRegistry\service_registry\pom.xml spring-boot:run
```

2. Start Rating, User, and Hotel. Set `DB_PASSWORD` in each terminal first if needed:

```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
& .\Rating\rating_service\mvnw.cmd spring-boot:run
```

```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
& .\User\user_service\mvnw.cmd spring-boot:run
```

```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
& .\Hotel\hotel_service\mvnw.cmd spring-boot:run
```

3. Start the API Gateway in another terminal:

```powershell
& .\Hotel\hotel_service\mvnw.cmd -f .\APIGateway\api_gateway\pom.xml spring-boot:run
```

The Eureka dashboard is at `http://localhost:8761`. It should list `API-GATEWAY`, `USER-SERVICE`, `HOTEL-SERVICE`, and `RATING-SERVICE` when all processes are healthy. Stop a service with `Ctrl+C` in its terminal.

## API routes

Use the gateway base URL `http://localhost:8084`. The same routes are also available directly from each service at its port.

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
Invoke-RestMethod -Uri http://localhost:8084/adduser -Method Post -ContentType 'application/json' -Body $user
```

Create a hotel:

```powershell
$hotel = @{ hotel_id = "HTL-101"; name = "The Harbor House"; location = "Seattle"; about = "A fictional city hotel near the waterfront." } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8084/hotels -Method Post -ContentType 'application/json' -Body $hotel
```

Create a rating linking that user and hotel:

```powershell
$rating = @{ ratingId = "REV-101"; hotelId = "HTL-101"; userid = "USR-101"; rating = 5; feedback = "Friendly service and a comfortable stay." } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8084/ratings -Method Post -ContentType 'application/json' -Body $rating
```

Then query `http://localhost:8084/users/USR-101` or `http://localhost:8084/hotels/HTL-101` to see the aggregated responses.

## Build

Run these from the repository root:

```powershell
& .\Hotel\hotel_service\mvnw.cmd -f .\ServiceRegistry\service_registry\pom.xml -DskipTests package
& .\Hotel\hotel_service\mvnw.cmd -f .\APIGateway\api_gateway\pom.xml -DskipTests package
& .\Hotel\hotel_service\mvnw.cmd -f .\Hotel\hotel_service\pom.xml -DskipTests package
& .\Rating\rating_service\mvnw.cmd -DskipTests package
& .\User\user_service\mvnw.cmd -DskipTests package
```

## Technology

- Java 17
- Spring Boot 4.1.1
- Spring Cloud 2025.1.3
- Spring Cloud Netflix Eureka
- Spring Cloud OpenFeign
- Spring Cloud Gateway Server Web MVC
- Spring Data JPA and MySQL
