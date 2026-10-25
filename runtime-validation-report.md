# Runtime Validation Report

**Generated**: 2026-10-21T21:10:00+05:30
**Target**: Microservices Hotel Review System

## Summary

| Step | Status | Exit Code | Details |
|------|--------|-----------|---------|
| Startup | PASS | n/a | Config Server (8888), Eureka (8761), Gateway (8084), and User/Hotel/Rating (8081-8083) were listening; Eureka reported all four registered applications as UP. |
| Config Client | PASS | 0 | Config Server returned shared and application-specific property sources for the gateway and each data service. |
| API E2E | PASS | 0 | Three PowerShell test flows completed with 38 passing assertions and no failures. |
| Resilience E2E | PASS | 0 | A 160-request burst returned HTTP 429 with Retry-After; five downstream failures opened the circuit; open-circuit calls returned 503; three successful half-open probes closed it. |
| Negative API cases | PASS | 0 | Missing User, Hotel, and Rating IDs returned HTTP 404. |
| Actuator | PASS | 0 | User and Hotel health, metrics, circuit-breaker, and rate-limiter endpoints returned HTTP 200; breakers were CLOSED after recovery. |
| Maven package | PASS | 0 | Corrected package commands for User, Hotel, and Rating completed successfully with tests skipped. |
| Unit/integration tests | FAIL | n/a | The test runner reported 0 passed / 3 failed: the existing User, Hotel, and Rating `contextLoads()` tests failed to initialize their Spring application contexts. Root cause was not determined. |
| Browser E2E | N/A | n/a | The project exposes JSON APIs and has no browser UI. |

**Overall**: PASS for startup, Config Client, API CRUD/aggregation, resilience behavior, negative API cases, and Actuator. Existing Maven context-load tests remain failing in the test runner.

## API Test Evidence

Test scripts are in `tests/api-smoke/`.

| Flow | Assertions | Result |
|------|------------|--------|
| User CRUD and cross-service aggregation | 12 | PASS — creates a hotel, rating, and user; verifies persistence and the user response includes its rating and hotel; deletes the user and confirms it is gone. |
| Hotel CRUD and rating aggregation | 14 | PASS — creates a hotel and rating; verifies collection, details, rating aggregation, and deletion. |
| Rating CRUD and filters | 12 | PASS — creates a rating; verifies collection, lookup by ID, filters by user and hotel, and deletion. |

All scripts use generated IDs and delete test records in `finally` cleanup blocks.

The resilience flow verified the `ratingService` 20 calls/second limiter using a 160-request concurrent burst (65 HTTP 200, 95 HTTP 429 in this run), including `Retry-After: 1`. It then generated five Hotel dependency failures, observed the User service's `hotelService` circuit open, confirmed calls through the open circuit return HTTP 503, and verified that three successful half-open probes close the circuit. Temporary test users, hotels, and ratings were deleted afterward.

## Endpoint Coverage

| Endpoint | Verified response |
|----------|-------------------|
| `GET /users` | 200; persisted records returned |
| `POST /adduser` | 200; success response body verified |
| `GET /users/{userid}` | 200; user and associated rating/hotel details verified |
| `DELETE /users/{userid}` | 204; deleted record absent from list |
| `GET /hotels` | 200; persisted record returned |
| `POST /hotels` | 201 |
| `GET /hotels/{hotel_id}` | 200; hotel and ratings verified |
| `GET /hotels/{hotel_id}/details` | 200 |
| `GET /hotels/{hotel_id}/ratings` | 200; matching rating verified |
| `DELETE /hotels/{hotel_id}` | 204; deleted record absent from list |
| `GET /ratings` | 200; persisted record returned |
| `POST /ratings` | 201 |
| `GET /ratings/{ratingId}` | 200; submitted fields verified |
| `GET /ratings/user/{userid}` | 200; matching rating verified |
| `GET /ratings/hotel/{hotelId}` | 200; matching rating verified |
| `DELETE /ratings/{ratingId}` | 204; deleted record absent from list |

Initial collection requests to `/users`, `/hotels`, and `/ratings` through the gateway also returned 200.

## Environment and Gaps

- Docker daemon: unavailable; these runtime tests used the already-running local MySQL instance.
- Node.js: available; Playwright was not applicable because this is an API-only project.
- Authentication: no authentication is configured for these API endpoints.
- Missing-resource 404 cases were exercised; malformed payload validation was not.
- Existing Maven context-load tests failed in the test runner. The test tool reported `Failed to load ApplicationContext` for each service; the underlying cause was not available in the report.

## Issues Found

None in the exercised startup, configuration, and API flows.
