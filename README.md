# BookShelf — Full-Stack Library Management

A full-stack library application built with **Spring Boot 4** / **Java 21** and a **React 19** front end. Members can browse books, borrow and return them, and leave reviews; admins manage the catalog. The API is secured with **JWT authentication** and role-based access control.

The backend demonstrates a clean layered architecture with CRUD, pagination, JPA relationships, DTO mapping, projections, validation, global exception handling, auditing, AOP logging, HATEOAS, and OpenAPI docs — backed by 37 unit and web-layer tests.

## Tech Stack

**Backend**
- **Java 21**, **Spring Boot 4.1**
- **Spring Web** (REST), **Spring Data JPA** (Hibernate), **MySQL**
- **Spring Security** + **JWT** (jjwt), **BCrypt** password hashing
- **Bean Validation** (Jakarta), **MapStruct** (DTO mapping), **Lombok**
- **Spring HATEOAS**, **springdoc-openapi** (Swagger UI), **Spring Boot Actuator**
- **AOP** (AspectJ) for cross-cutting logging
- **JUnit 5**, **Mockito**, **MockMvc**, **JaCoCo** (coverage)
- **Maven**

**Frontend**
- **React 19**, **Vite**, **React Router**
- **Axios** (JWT interceptor + 401 handling)
- Plain **CSS3** (flexbox, grid, responsive breakpoints)

## Architecture

```
React (Vite)  --HTTP/JWT-->  Controller  ->  Service  ->  Repository  ->  MySQL
                                (REST)     (business)   (Spring Data JPA)
                                   |            |
                                DTOs <---- MapStruct ----> Entity
```

- **Controller** — REST endpoints, request validation, HTTP status codes, HATEOAS.
- **Service** — business rules (duplicate titles, borrow eligibility) and transaction boundaries.
- **Repository** — Spring Data JPA with derived queries, a JPQL projection, and pagination.
- **DTOs** — decouple the API contract from JPA entities (entities never leave the service layer).
- **Mapper** — MapStruct maps entity <-> DTO at compile time, including nested fields.
- **Security** — `JwtAuthFilter` authenticates each request; `SecurityConfig` authorizes by role.
- **Exception handling** — `@RestControllerAdvice` turns domain exceptions into clean HTTP responses.
- **Auditing** — `@CreatedBy` / `@CreatedDate` populated from the authenticated user.
- **Aspect** — logs method entry, exit, exceptions, and execution time.

### Domain model

```
Category 1---* Book *---1 Author (string)
                |
                |---* Review
                |
                *---* User   (through BorrowRecord)
```

A `BorrowRecord` links a user to a book. A record with `returnDate IS NULL` means the book is currently out — that single condition drives the borrow/return rules.

## Features

**Catalog**
- Full CRUD for books (`title`, `author`, `price`, `available`, `category`)
- Pagination and sorting on every list endpoint (`Pageable`)
- Search by author, title (contains), price (less-than / between), and availability
- Categories (`@ManyToOne`) and reviews (`@OneToMany` with cascade + orphan removal)
- Average-rating summaries computed in the database via a JPQL `AVG()` projection

**Borrowing**
- Borrow a book as the logged-in user (identified from the JWT, not the request body)
- A book already on loan cannot be borrowed again (409 Conflict)
- Return a book, and view your own borrow history

**Security**
- Register / login returning a signed JWT (15-minute expiry)
- Passwords hashed with BCrypt; registration always assigns the `USER` role
- Reads are public; writes require `ADMIN`; borrowing and reviewing require any authenticated user
- CORS configured for the React dev server

**Platform**
- Content negotiation — responses in JSON or XML
- HATEOAS links (`self`, `all`, `update`, `delete`) on every book resource
- Auto-generated API docs via Swagger UI, plus a Postman collection

## API Endpoints

Legend: 🔓 public · 🔒 authenticated · 👑 admin only

### Auth
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| `POST` | `/auth/register` | 🔓 | Create an account, returns a JWT |
| `POST` | `/auth/login` | 🔓 | Authenticate, returns a JWT |

### Books
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| `GET` | `/books` | 🔓 | List all books (paged) |
| `GET` | `/books/{id}` | 🔓 | Get a book by id |
| `GET` | `/books/summaries` | 🔓 | Title, author and average rating per book |
| `GET` | `/books/author?author=` | 🔓 | Find by author (paged) |
| `GET` | `/books/title?title=` | 🔓 | Find by title contains (paged) |
| `GET` | `/books/price?price=` | 🔓 | Find cheaper than a price (paged) |
| `GET` | `/books/between?min=&max=` | 🔓 | Find within a price range (paged) |
| `GET` | `/books/available` | 🔓 | List available books (paged) |
| `POST` | `/books` | 👑 | Create a book |
| `PUT` | `/books/{id}` | 👑 | Update a book |
| `DELETE` | `/books/{id}` | 👑 | Delete a book |

### Reviews, Categories & Borrowing
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| `GET` | `/books/{id}/reviews` | 🔓 | List reviews for a book |
| `POST` | `/books/{id}/reviews` | 🔒 | Add a review (rating 1–5) |
| `GET` | `/categories` | 👑 | List categories |
| `POST` | `/categories` | 👑 | Create a category |
| `POST` | `/borrow` | 🔒 | Borrow a book (`{ "bookId": 1 }`) |
| `POST` | `/return/{bookId}` | 🔒 | Return a book |
| `GET` | `/borrow/me` | 🔒 | Your borrow history |

## Running Locally

**Prerequisites:** Java 21, Node 18+, and a running MySQL instance.

### 1. Database

```sql
CREATE DATABASE bookdb;
```

### 2. Backend

```bash
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
export JWT_SECRET=a-random-string-of-at-least-32-characters

./mvnw spring-boot:run
```

Runs on `http://localhost:8080`. Tables are created automatically by Hibernate.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`.

### 4. Create an admin

Registration always creates a `USER`. To promote yourself:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';
```

Log in again to get a token carrying the new role.

## Configuration

All settings are externalized so nothing sensitive is committed:

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | MySQL credentials |
| `JWT_SECRET` | dev placeholder | HMAC signing key — **must be 32+ characters** |
| `CORS_ORIGINS` | `http://localhost:5173` | Comma-separated allowed origins |

## Testing

```bash
./mvnw test
```

**37 tests, all passing.**

- `BookServiceTest` — service-layer unit tests with Mockito, covering duplicate/not-found cases, category resolution, and the summary projection.
- `BorrowServiceTest` — borrow/return rules, including that a rejected borrow writes no record.
- `ReviewServiceTest` — review creation and the book-not-found path.
- `BookControllerTest` — web-layer tests with `@WebMvcTest` + MockMvc (status codes, JSON body, validation failures, security).
- JaCoCo coverage report at `target/site/jacoco/index.html`.

A **Postman collection** covering the full flow (including the 401/403/409 cases) is in [`postman/`](postman/).

## Project Structure

```
.
├── src/main/java/com/example/demo/
│   ├── config/        SecurityConfig, JpaAuditingConfig
│   ├── controller/    Book, Auth, Borrow, Review, Category
│   ├── dto/           request/response objects
│   ├── entity/        Book, User, Review, Category, BorrowRecord, Role
│   ├── exception/     custom exceptions + GlobalExceptionHandler
│   ├── mapper/        MapStruct mappers
│   ├── projection/    BookSummary
│   ├── repository/    Spring Data JPA repositories
│   ├── security/      JwtService, JwtAuthFilter, CustomUserDetailsService
│   └── service/       business logic
├── frontend/          React + Vite client
├── docs/              design and concepts notes
└── postman/           API collection
```

## Documentation

[`docs/PROJECT_NOTES.md`](docs/PROJECT_NOTES.md) is a deep write-up of the design decisions — why each pattern was chosen, what breaks without it, and how the alternatives compare (JPA relationships, transactions, projections, JWT vs sessions, and more).

## Deployment

The app is deploy-ready; all environment-specific values are externalized.

**Backend (e.g. Render)** — root directory `.`, build `./mvnw clean package -DskipTests`, start `java -jar target/*.jar`. Set `SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `CORS_ORIGINS` (your frontend URL).

**Frontend (e.g. Vercel)** — root directory `frontend`, framework Vite, build `npm run build`, output `dist`. Set `VITE_API_URL` to the deployed API URL.

**Database** — any managed MySQL 8 instance.

> Before going live, switch `spring.jpa.hibernate.ddl-auto` from `update` to `validate` and manage schema changes with migrations.

## Author

**Rohul Ray Edward S.** — [github.com/Hashcat07](https://github.com/Hashcat07)
