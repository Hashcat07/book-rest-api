# Book REST API

A production-style REST API for managing a book catalog, built with **Spring Boot 4** and **Java 21**. It demonstrates a clean layered architecture with full CRUD, pagination, DTO mapping, validation, global exception handling, JPA auditing, AOP logging, HATEOAS links, and OpenAPI documentation — backed by unit and web-layer tests.

## Tech Stack

- **Java 21**, **Spring Boot 4.1**
- **Spring Web** (REST), **Spring Data JPA** (Hibernate), **MySQL**
- **Bean Validation** (Jakarta), **MapStruct** (DTO mapping), **Lombok**
- **Spring HATEOAS**, **springdoc-openapi** (Swagger UI), **Spring Boot Actuator**
- **AOP** (AspectJ) for cross-cutting logging
- **JUnit 5**, **Mockito**, **MockMvc**, **JaCoCo** (coverage)
- **Maven**

## Architecture

```
Controller  ->  Service  ->  Repository  ->  MySQL
   (REST)      (business)    (Spring Data JPA)
      |            |
   DTOs <----- MapStruct -----> Entity
```

- **Controller** (`BookController`) — REST endpoints, request validation, HTTP status codes, HATEOAS.
- **Service** (`BookService`) — business rules (e.g. reject duplicate titles), transaction boundary.
- **Repository** (`BookRepository`) — Spring Data JPA with derived query methods.
- **DTOs** (`BookRequest`, `BookResponse`) — decouple the API contract from the JPA entity.
- **Mapper** (`BookMapper`) — MapStruct maps entity <-> DTO at compile time.
- **Exception handling** (`GlobalExceptionHandler`) — `@RestControllerAdvice` turns exceptions into clean HTTP responses.
- **Auditing** (`JpaAuditingConfig`, `Book`) — auto-populates created/modified date & user.
- **Aspect** (`LoggingAspect`) — logs method entry, exit, exceptions, and execution time.

## Features

- Full CRUD for books (`title`, `author`, `price`, `available`)
- Pagination and sorting on every list endpoint (`Pageable`)
- Search by author, title (contains), price (less-than / between), and availability
- Duplicate-title prevention (409 Conflict) and not-found handling (404)
- Request validation (400 Bad Request on invalid input)
- Content negotiation — responses in JSON or XML
- HATEOAS links (`self`, `all`, `update`, `delete`) on every resource
- Auto-generated API docs via Swagger UI

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/books` | Create a book |
| `GET` | `/books` | List all books (paged) |
| `GET` | `/books/{id}` | Get a book by id |
| `PUT` | `/books/{id}` | Update a book |
| `DELETE` | `/books/{id}` | Delete a book |
| `GET` | `/books/author?author=` | Find by author (paged) |
| `GET` | `/books/title?title=` | Find by title contains (paged) |
| `GET` | `/books/price?price=` | Find cheaper than a price (paged) |
| `GET` | `/books/between?min=&max=` | Find within a price range (paged) |
| `GET` | `/books/available` | List available books (paged) |

## Running Locally

**Prerequisites:** Java 21, Maven (or the bundled `mvnw`), and a running MySQL instance.

1. Create the database:
   ```sql
   CREATE DATABASE bookdb;
   ```
2. Set your DB credentials (defaults to `root`/`root` if unset):
   ```bash
   export DB_USERNAME=root
   export DB_PASSWORD=yourpassword
   ```
3. Run the app:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Explore:
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - Health: `http://localhost:8080/actuator/health`

## Testing

```bash
./mvnw test
```

- `BookServiceTest` — unit tests for the service layer with Mockito (happy paths + duplicate/not-found cases).
- `BookControllerTest` — web-layer tests with `@WebMvcTest` + MockMvc (status codes, JSON body, validation failures).
- JaCoCo coverage report is generated at `target/site/jacoco/index.html`.

## Author

**Rohul Ray Edward S.** — [github.com/Hashcat07](https://github.com/Hashcat07)
