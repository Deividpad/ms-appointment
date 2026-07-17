# Appointment Service

Appointment Service is a Spring Boot API for managing doctors, patients, appointments, penalties, and available time slots for a clinic.

The application uses PostgreSQL, Liquibase for schema management, Spring Validation for request validation, and Springdoc OpenAPI for API documentation.

### Architecture Diagram

The high-level architecture diagram is available here:

                           Appointment Service

                    +--------------------------------+
                    |           REST Clients          |
                    | Swagger • Postman • Frontend   |
                    +---------------+----------------+
                                    |
                                    |
                           HTTP / JSON REST API
                                    |
                                    ▼
                    +--------------------------------+
                    |         REST Controllers        |
                    | Doctor • Patient • Appointment |
                    +---------------+----------------+
                                    |
                                    ▼
                    +--------------------------------+
                    |         Service Layer          |
                    | Business Rules & Orchestration |
                    +---------------+----------------+
                                    |
                 +------------------+------------------+
                 |                  |                  |
                 ▼                  ▼                  ▼
      +----------------+   +----------------+  +----------------+
      | Validation     |   | Penalty Logic  |  | Scheduler      |
      | Bussiness      |   | Appointment    |  | Unlock Patients|
      +----------------+   +----------------+  +----------------+
                                    |
                                    ▼
                    +--------------------------------+
                    |         Repository Layer        |
                    |         Spring Data JPA         |
                    +---------------+----------------+
                                    |
                                    ▼
                    +--------------------------------+
                    |          PostgreSQL            |
                    |    Liquibase Managed Schema    |
                    +--------------------------------+

                        Cross-cutting Components

        +----------------------------------------------------------+
        | MapStruct | GlobalExceptionHandler | Jollyday | Pageable |
        +----------------------------------------------------------+

The project follows a layered architecture:

- `controller`: REST endpoints and request/response handling
- `service`: business rules and orchestration
- `repository`: persistence access and queries
- `entity`: JPA entities
- `dto`: request and response contracts
- `mapper`: MapStruct mappers between entities and DTOs
- `exception`: centralized error handling and custom exceptions
- `util`: scheduling, pagination, date rules, and shared constants
- `annotation`: custom validation annotations


## Technology Stack

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Spring Validation
- PostgreSQL
- Liquibase
- MapStruct
- Lombok
- Springdoc OpenAPI
- JUnit 5, Mockito, Spring Boot Test
- Jollyday for holiday checks

## Architecture

### Main Domain Flow

1. A controller receives the HTTP request and validates the input.
2. The service layer applies business rules.
3. Repositories read or write data in PostgreSQL.
4. DTOs are returned to the client through MapStruct mappings.
5. Exceptions are translated by `GlobalExceptionHandler` into structured error responses.

## Business Flows

### Patients

- Register a patient
- Retrieve a patient by id
- Validate birth date and penalty status
- Automatically unlock blocked patients with the scheduled job

### Doctors

- Register a doctor
- Retrieve a doctor by id
- Validate specialty, phone, and email uniqueness

### Appointments

- Book an appointment
- Cancel an appointment
- Reschedule an appointment
- List appointments with filters and paging
- Check available slots by doctor and date range

### Penalties

- Create a penalty when a cancellation happens too close to the appointment time
- Block patients after 3 penalties in the last 30 days
- Automatically unlock blocked patients when the penalty window expires

## Validation and Rules

- Appointment times must follow 30-minute intervals.
- Appointments cannot be scheduled on Sundays.
- Holidays in Colombia are blocked.
- Weekdays and Saturdays have different allowed slot windows.
- Patient and doctor cannot be double-booked at the same time.
- Birth dates must be in the past.
- Sorting is limited to the fields allowed by the application.

## Database

Liquibase manages the initial schema and seed data.

Tables created by the migration:

- `specialties`
- `doctors`
- `patients`
- `appointments`
- `penalties`

The initial migration also seeds the specialty catalog.

## Configuration

### Local profile

Use the local profile for development:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The local profile expects PostgreSQL running on:

- host: `localhost`
- port: `5432`
- database: `clinic_db`
- username: `postgres`
- password: `postgres`

### Environment variables

The default profile reads database settings from environment variables:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

Example:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=clinic_db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

## Run the Application

### 1. Start PostgreSQL

Make sure a PostgreSQL instance is available and the database exists.

### 2. Run the service locally

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Run with Docker Compose

Use the same database variables defined in your `.env` file, then start the stack:

```bash
docker compose up --build
```

This starts both services:

- `postgres` on port `5432`
- `ms-appointment` on port `8080`

The application runs with the `default` Spring profile and reads database settings from the compose environment.

### 4. Build and run tests

```bash
mvn test
```

## API Documentation

Springdoc OpenAPI is included.

- Swagger UI: `http://localhost:8080/appointment/api/v1/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/appointment/api/v1/v3/api-docs`

Because the application uses a context path, all endpoints are exposed under:

`/appointment/api/v1`

### Request Collection

The repository includes an importable Postman collection with all request folders grouped by resource:

- [docs/collection.json](docs/collection.json)

Collection folders:

- `doctor`
- `patient`
- `appointments`

## REST Endpoints

### Patient

- `POST /patient`
- `GET /patient/{id}`

### Doctor

- `POST /doctor`
- `GET /doctor/{id}`

### Appointment

- `POST /appointment`
- `PUT /appointment/{id}/cancel`
- `PUT /appointment/{id}/reschedule`
- `GET /appointment`
- `GET /appointment/availability`

> Note: `GET /appointment` accepts a JSON request body for filtering and paging in the current implementation.

## Error Handling

The API returns structured error responses through `GlobalExceptionHandler`.

Typical response shape:

```json
{
  "timestamp": "2026-07-17T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation message",
  "path": "/appointment/api/v1/patient"
}
```

## Scheduled Jobs

The application includes a scheduled job that unlocks expired blocked patients.

- Cron: `0 0 0 * * *`
- Time zone: `America/Bogota`

## Testing

The project includes controller and service tests for the main flows and edge cases.

Recommended commands:

```bash
mvn test
```

If you want a full verification build:

```bash
mvn verify
```

## Project Notes

- JPA uses `ddl-auto: validate`, so the schema must already match the entities.
- Holidays are checked against Colombian holiday rules.
- Pagination and sorting are validated before repository queries are executed.
- MapStruct generates the entity-to-DTO mapping code at compile time.
- `docs/collection.json` is the canonical Postman export for this API.
- `docs/architecture.svg` shows the layered design and main runtime dependencies.
