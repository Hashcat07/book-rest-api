# Book REST API — Deep Design & Concepts Notes

This is not a summary of *what* the code does — it explains **why each piece is
built the way it is**, and just as importantly, **what breaks if you do it the
other way**. For every concept you'll find four things:

- **The idea** — what it is, in one line.
- **Why this way** — the reasoning.
- **What happens without it** — the concrete failure (many of these are bugs
  that actually showed up while building this project).
- **How the alternatives differ** — so you can defend the choice in an interview.

Read it top to bottom the first time; use it as a reference after that.

---

## Part 0 — The mental model: layers, and why they exist

```
Controller   -> HTTP only: read the request, return a ResponseEntity
   |
Service      -> business rules + transaction boundaries
   |
Repository   -> data access (Spring Data JPA)
   |
Entity       -> one row of a table, mapped to a Java object
```

### 0.1 Why separate these layers at all?

**Why this way.** Each layer has exactly one reason to change. The controller
changes when the URL or HTTP shape changes. The service changes when a business
rule changes. The repository changes when a query changes. When they're separated
you can test the service without HTTP, and swap the database without touching the
controller.

**What happens without it.** Put the SQL and the rules inside the controller (a
"fat controller") and every change touches one giant class. You can't unit-test
the borrow rule without spinning up a web server, and two endpoints that need the
same rule copy-paste it. Bugs multiply because there's no single place that owns
"how borrowing works."

**How it differs.** This layering is the standard Spring MVC / "onion" approach.
The alternative — logic in controllers — is faster to write for one endpoint and
miserable after five.

### 0.2 Why DTOs instead of returning entities

**The idea.** `BookResponse` / `BorrowResponse` are plain data classes the API
returns. The JPA entities (`Book`, `BorrowRecord`) never leave the service.

**Why this way.** Three concrete reasons:
1. **Security.** Once `User` has a `password` field, returning the entity would
   serialize the hash to the client. A DTO simply doesn't have that field.
2. **Lazy-loading crashes.** Entities carry relationships (`Book.reviews`,
   `BorrowRecord.user`). Serializing an entity after the transaction closes tries
   to lazy-load those and throws `LazyInitializationException`.
3. **Decoupling.** Rename a database column and your public API doesn't change —
   only the mapper does.

**What happens without it.** You return `List<BorrowRecord>` directly (this was a
real temptation in `getUserHistory`). Jackson walks the object graph: each record
drags in the whole `User` (name, email, **password**) and the whole `Book`
(every audit field). You leak data *and* risk a lazy-load exception at serialization
time.

**How it differs.** Some tiny apps expose entities directly for speed. It works
until the first security review or the first `@OneToMany` — then it's a rewrite.

---

## Part 1 — The Borrow feature, in depth

### 1.1 `@ManyToOne` and where the foreign key lives

```java
// in BorrowRecord
@ManyToOne @JoinColumn(name = "book_id", nullable = false)
private Book book;
```

**The idea.** Many borrow records point to one book. Read the annotation from the
class you're standing in: *this* is the "many" side.

**Why this way.** In a relational database the foreign key can only sit on one
table, and it has to sit on the "many" side. A `book_id` column on the
`borrow_record` row can hold exactly one book reference — which is correct, one
record is about one book. `@ManyToOne` + `@JoinColumn` puts the FK there.

**What happens without it / if you flip it.** Try to model this as `@OneToMany` on
the wrong side, or expect the `book` table to "hold" its borrow records in a
column, and it can't — a single column can't store a list of record ids. JPA would
either create an unexpected extra join table or fail to map. The direction isn't a
style choice; the database physically forces the FK onto the many side.

**How the four relationships differ:**
| Annotation (from `BorrowRecord`'s view) | Meaning | When |
|---|---|---|
| `@ManyToOne Book` | many records → one book | ✅ correct here |
| `@OneToMany` | one record → many books | wrong — a record is about one book |
| `@OneToOne` | one record ↔ one book, exclusively | wrong — a book has many records over time |
| `@ManyToMany` | records and books both many-to-many | wrong — needs a join table you don't want |

### 1.2 `fetch = LAZY` vs `EAGER` (and the N+1 problem)

**The idea.** `@ManyToOne(fetch = FetchType.LAZY)` means the related entity isn't
loaded from the DB until you actually call its getter.

**Why this way.** `@ManyToOne` defaults to **EAGER**, which loads the related row
every single time — even when you don't need it.

**What happens without it (EAGER).** Fetch 100 books and each one eagerly loads its
category → 1 query for the books + 100 queries for categories = the **N+1 problem**.
Your endpoint quietly fires 101 queries instead of 1 or 2. It "works" in testing
with 3 rows and falls over in production.

**How it differs.** LAZY defers the load so you pay only when you use it; if you
know you'll always need the related data, you fetch it explicitly with a
`JOIN FETCH` query. EAGER-by-default is the classic hidden performance bug.

### 1.3 Modeling state with `returnDate IS NULL` (vs a status column)

**The idea.** There's no `status` column. A borrow is "active" while its
`returnDate` is `null`, and "returned" once it's stamped.

**Why this way.** The one fact that matters — is this book currently out? — becomes
a single condition (`return_date IS NULL`) and a single query
(`findByBookIdAndReturnDateIsNull`). Less state means fewer ways to be inconsistent.

**What happens without it (a status enum).** A `status` column can drift out of
sync with reality: you could have `status = RETURNED` but a null `returnDate`, or
vice versa — two sources of truth for one fact. Now every write must update both
and every read must trust the right one.

**How it differs.** A status column is worth it when there are genuinely many states
(PENDING/ACCEPTED/FULFILLED/CANCELLED). For a binary out/returned, a nullable date
is simpler and self-consistent.

### 1.4 Derived queries — how the method name becomes SQL

```java
Optional<BorrowRecord> findByBookIdAndReturnDateIsNull(Long bookId);
```

**The idea.** Spring Data reads the method name and generates the query:
`WHERE book_id = ? AND return_date IS NULL`.

**Why this way.** No SQL to write or keep in sync; the compiler and Spring validate
the property names at startup.

**⚠️ What went wrong here (a real bug).** The first version was
`findByIdAndReturnDateIsNull(Long id)`. `findById` filters on the record's **own
primary key**, but the code passed a **book id**. It compiled and ran, silently
querying the wrong column. `findByBookId` traverses `book.id`; `findById` means the
record's `id`. One missing word = a different query and a subtle, data-dependent
bug that only shows once you have more than one record.

**How it differs.** For anything complex you drop to `@Query` with JPQL (as the
projection does). Derived queries are perfect for simple, readable conditions and
dangerous only when the name doesn't say what you think.

### 1.5 `@Transactional` — all-or-nothing writes

```java
@Transactional
public String borrowBook(Long userId, Long bookId) { ... two writes ... }
```

**The idea.** Everything inside the method commits together or rolls back together.

**Why this way.** `borrowBook` does two writes: flip `book.available = false` and
insert a `BorrowRecord`. Those must both happen or neither.

**What happens without it.** No transaction, and the two writes are independent. If
the app crashes between them, you get a book marked unavailable with **no record
saying who has it** — a "ghost" borrow that nothing can return. `@Transactional`
makes that impossible: a failure on the second write rolls back the first.

**How it differs.** `@Transactional(readOnly = true)` on pure reads (like
`getUserHistory`) hints the DB that nothing will change, enabling optimizations and
preventing accidental writes. No annotation at all = auto-commit per statement = no
atomicity across statements.

### 1.6 `orElseThrow` — expression lambda vs statement lambda

```java
// CORRECT — throws
.orElseThrow(() -> new UserNotFoundException("User Not Found"));

// BUG — does nothing
.orElseThrow(() -> { new UserNotFoundException("User Not Found"); });
```

**⚠️ What went wrong here (a real bug).** The braces version *constructs* the
exception and throws it away — it never `throw`s or `return`s it. `orElseThrow`
expects the lambda to **return** the exception it should throw; the `{ }` form is a
code block that returns nothing, so `orElseThrow` throws a confusing
`NullPointerException` instead of your clean 404.

**Why this way.** `() -> new X()` is an *expression* lambda — its value is the new
exception, which `orElseThrow` then throws. `() -> { ... }` is a *statement* lambda
and needs an explicit `return`/`throw`.

**How it differs.** Same trap appears anywhere a lambda's return value matters
(`map`, `Supplier`). Rule of thumb: no braces when you want the value back.

### 1.7 Dependency injection — `@RequiredArgsConstructor` + `final`

```java
@RestController
@RequiredArgsConstructor
public class BorrowController {
    private final BorrowService borrowService;   // Spring injects this
}
```

**⚠️ What went wrong here (the NPE you hit).** The first controller had
`private BorrowService borrowService;` with no `@Autowired`, no constructor, and no
`@RequiredArgsConstructor`. Spring created the controller but **never set the
field**, so it stayed `null` → `Cannot invoke "...getUserHistory()" because
"this.borrowService" is null`.

**Why this way.** Lombok's `@RequiredArgsConstructor` generates a constructor for
every `final` field, and Spring injects dependencies through that constructor.

**What happens if you add the annotation but forget `final`.** Lombok only includes
`final` (or `@NonNull`) fields in the generated constructor. A non-final field is
skipped → the same NPE returns. `final` is load-bearing, not decoration.

**How the three injection styles differ:**
- **Constructor injection** (used here) — dependencies are set at construction, so
  the object is never in a half-built state, and the field can be `final`. Fails at
  startup if a bean is missing (loud, early).
- **Field injection** (`@Autowired` on the field, as the older `BookController`
  uses) — works, but you can't make it `final`, it hides dependencies, and it fails
  at *request* time, not startup.
- **Setter injection** — for genuinely optional dependencies; rare.

### 1.8 Flattening in the mapper (and why serialization breaks otherwise)

```java
@Mapping(source = "user.name",  target = "userName")
@Mapping(source = "book.title", target = "bookTitle")
BorrowResponse toResponse(BorrowRecord record);
```

**The idea.** Pull just the needed leaf fields out of nested entities into a flat
DTO.

**Why this way.** See 0.2 — it avoids leaking the `User`/`Book` graph and avoids
lazy-load exceptions. MapStruct writes the `record.getUser().getName()` calls for
you at compile time (look in `target/generated-sources` to see them).

**What happens without it.** Return the entity and you're back to leaking the
password and risking `LazyInitializationException` the moment Jackson touches
`reviews`.

---

## Part 2 — Reviews, Categories, Projections

### 2.1 `@OneToMany` + `mappedBy` — the owning side

```java
// Book
@OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Review> reviews = new ArrayList<>();
```

**The idea.** `Review` holds the FK (`@ManyToOne Book book`). `mappedBy = "book"`
tells JPA "the link is already defined on the `Review.book` field."

**Why this way.** It names `Review` as the **owning side** — the side with the
foreign key. This is the mirror of the `@ManyToOne` you saw in Part 1: one link,
described from both ends.

**⚠️ What happens without `mappedBy`.** JPA assumes *each* side owns the
relationship and creates a **third join table** (`book_reviews`) you never wanted —
plus extra queries to maintain it. `mappedBy` is what says "don't do that; the FK is
over there."

**How it differs.** Drop `mappedBy` only for a true `@ManyToMany`, where a join
table is actually correct.

### 2.2 `cascade` and `orphanRemoval`

**The idea.** `cascade = ALL` flows persistence operations from book to its reviews;
`orphanRemoval = true` deletes a review row when it's removed from the list.

**Why this way.** A review has no meaning without its book — the book owns the
review's lifecycle. Save a book with new reviews and they save too; delete the book
and its reviews go with it.

**What happens without them.** Without `cascade`, saving a book does **not** save its
new reviews — you'd have to save each review manually or get a "transient object"
error. Without `orphanRemoval`, removing a review from the list leaves an orphan row
in the DB pointing at nothing meaningful.

**How it differs.** You would *not* put `cascade = ALL` from `Book` to `Category` —
deleting a book must never delete its category (other books share it). Cascade
follows ownership, and a book does not own its category.

### 2.3 `@Getter/@Setter` vs `@Data` on entities

**⚠️ Why the entities use `@Getter/@Setter`, not `@Data`.** `@Data` also generates
`equals`, `hashCode`, and `toString`. On an entity with relationships
(`Book.reviews`, `BorrowRecord.user`) those generated methods walk the
relationships:
- `toString()` on `Book` prints its `reviews`, and each `Review.toString()` prints
  its `Book` → **infinite recursion / StackOverflowError**, or a flood of lazy
  loads.
- `equals`/`hashCode` that include a lazy collection trigger loading it just to
  compare.

**How it differs.** DTOs (`BookResponse`, `ReviewResponse`) *do* use `@Data` — they
have no relationships and no JPA lifecycle, so the generated methods are safe and
convenient. Rule: `@Getter/@Setter` for entities, `@Data` for DTOs.

### 2.4 Missing getters/setters — the silent empty `{}`

**⚠️ What went wrong here (a real bug).** `ReviewRequest` and `ReviewResponse` were
first written with **no Lombok annotations at all**. Two things broke at once:
- **MapStruct** needs getters on the source and setters on the target. With none, it
  couldn't read `ReviewRequest` or write `ReviewResponse`, so the mapping produced
  an empty object.
- **Jackson** serializes using getters. With none, `ReviewResponse` serialized to
  `{}` — the API returned an empty body even though the data existed.

**Why this matters.** "It compiled" is not "it works." No compiler error, just empty
JSON — the hardest kind of bug to spot. Adding `@Data` (or `@Getter/@Setter`) fixed
both at once.

### 2.5 `id` in the DTO, entity resolved in the service

```java
// BookRequest carries only:
private Long categoryId;

// BookService resolves it:
Category category = categoryRepository.findById(categoryId)
        .orElseThrow(() -> new CategoryNotFoundException(...));
book.setCategory(category);
```

**The idea.** The client sends a `categoryId` (a number). The service turns that
number into a managed `Category` entity and attaches it.

**Why this way.** The client shouldn't send a whole category object (it doesn't have
one, and you shouldn't trust it if it did). An id is the minimal, safe reference.

**⚠️ What went wrong here (a real bug).** The `categoryId` was added to
`BookRequest`, but `BookService.save` still used the old code and never resolved it —
so `category` was always `null` in the database, and `categoryName` always came back
null. Nothing errored; the feature just silently didn't work.

**Why the mapper can't do it.** MapStruct maps field-by-field by name/type. It has no
way to know a `Long categoryId` should become a DB `Category` — that requires a
repository lookup, which is a *service* concern. So the mapper `ignore`s `category`
and the service owns the resolution. This "id in, entity in service" pattern repeats
for every `@ManyToOne`.

### 2.6 `@Mapping(..., ignore = true)` for unmapped targets

**Why it's there.** `BookMapper.toEntity` marks `category`, `reviews`, `id`, and the
audit fields as ignored.

**What happens without it.** MapStruct sees target fields it can't map from the
source and emits "unmapped target property" warnings — and under a stricter
`unmappedTargetPolicy` it fails the build. Explicit `ignore` says "I'm handling this
elsewhere on purpose," which documents intent and keeps the build clean.

### 2.7 Interface projections — reading only what you need

```java
public interface BookSummary {
    String getTitle();
    String getAuthor();
    Double getAverageRating();
}

@Query("SELECT b.title AS title, b.author AS author, AVG(r.rating) AS averageRating " +
       "FROM Book b LEFT JOIN b.reviews r GROUP BY b.id, b.title, b.author")
List<BookSummary> findAllSummaries();
```

**The idea.** Return a lightweight view (title, author, average rating) computed in
the database, not full `Book` entities.

**Why this way.** You don't need the whole book graph to show a rating summary.
Spring builds objects that implement the interface by matching each getter to an
aliased column.

**⚠️ Two real bugs here.**
1. **Alias case.** The getter `getAverageRating` matches the alias `averageRating`,
   and `getAuthor` matches `author`. The first draft aliased `AS Author` (capital) —
   a mismatch that can leave the field unpopulated. Aliases must match the getter
   names.
2. **Return type.** `getAverageRating` first returned `String`, but `AVG(rating)` is
   a number. Returning `Double` matches what the database produces and avoids a
   conversion error.

**Why `LEFT JOIN`, not `INNER JOIN`.** `LEFT JOIN b.reviews` keeps books that have
**no** reviews (their average comes back `null`). An `INNER JOIN` would silently drop
every unreviewed book from the summary — a book with zero reviews would just vanish
from the list.

**How it differs.** A class-based (DTO) projection uses a constructor expression; an
interface projection (used here) is just an interface. Both avoid loading entities;
the interface form is the least boilerplate.

---

## Part 3 — Security (JWT), in depth

### 3.1 Authentication vs authorization

Two different questions, often confused:
- **Authentication** — *who are you?* Proven by logging in and carrying a token.
- **Authorization** — *what may you do?* Checked per request against your role.

You can be authenticated (a valid `USER` token) and still unauthorized (trying to
`POST /books`, which needs `ADMIN`) → that's a **403**, not a 401. No token at all on
a protected route → **401**.

### 3.2 BCrypt — why you hash, and why *this* hash

```java
user.setPassword(passwordEncoder.encode(rawPassword)); // BCrypt
```

**Why hash at all.** So a database leak doesn't hand attackers everyone's password.
You store a one-way hash; login hashes the attempt and compares.

**What happens without it (plaintext).** One leaked table = every account
compromised, and users who reuse passwords are compromised on *other* sites too.

**Why BCrypt and not MD5/SHA-256.** BCrypt is deliberately **slow** and **salted**.
Fast hashes (MD5, SHA) let an attacker try billions of guesses per second and
precompute "rainbow tables." BCrypt's built-in per-password salt defeats rainbow
tables, and its cost factor makes brute force expensive. A plain SHA-256 of a
password is barely better than plaintext against a serious attacker.

### 3.3 Stateless (JWT) vs stateful (server sessions)

**The idea.** `SessionCreationPolicy.STATELESS` — the server stores nothing about
who's logged in; the token *is* the proof, re-checked every request.

**Why this way.** No server-side session store to synchronize. Any instance of the
app can validate any token, so you can run many instances behind a load balancer —
which matters for the microservices/cloud sprints later.

**What happens with sessions instead.** The server keeps a session in memory keyed
by a cookie. Scale to two servers and request #2 may hit a server that doesn't have
your session → you're "logged out" randomly, unless you add sticky sessions or a
shared session store (Redis). More moving parts.

**How it differs (the trade-off).** Sessions can be invalidated instantly
server-side (logout = delete the session). A stateless JWT is valid until it
expires — you can't easily "un-issue" it, which is why tokens are short-lived
(15 min here) and why real systems add refresh tokens.

### 3.4 What a JWT actually is, and why the signature matters

A JWT is three base64 parts: `header.payload.signature`. The payload here carries the
email (`subject`) and an expiry. It is **signed**, not encrypted.

**Why the signature.** Anyone can *read* a JWT (it's just base64), but only the
server — which holds the secret — can produce a valid signature. On each request the
server recomputes the signature over the header+payload and compares.

**What happens if there were no signature (or you trusted the payload blindly).**
A client could edit the payload to say `role: ADMIN` and walk in. The signature is
what makes the claims trustworthy: change one byte of the payload and the signature
no longer matches → rejected.

**Why not store the token server-side.** You could (that's basically a session).
The whole point of a JWT is that the signature lets you trust it *without* a
lookup — that's what keeps it stateless (3.3).

**Why the secret must be long.** HS256 needs a ≥ 256-bit (32-byte) key. A short
secret is guessable, and a guessed secret means an attacker can forge any token,
including an admin one. That's why `jwt.secret` is externalized and length-checked.

### 3.5 The filter — `OncePerRequestFilter`, and its position

```java
.addFilterBefore(new JwtAuthFilter(...), UsernamePasswordAuthenticationFilter.class);
```

**The idea.** `JwtAuthFilter` runs on every request *before* the controller: read
the `Bearer` token, validate it, load the user, and put an `Authentication` into the
`SecurityContextHolder`.

**Why before `UsernamePasswordAuthenticationFilter`.** That built-in filter handles
form-login username/password. You want your token check to run first and establish
the identity, so by the time authorization rules are evaluated, the
`SecurityContext` already knows who's calling.

**What happens without setting the context.** If the filter validated the token but
didn't call `SecurityContextHolder.getContext().setAuthentication(...)`, Spring would
still consider the request anonymous → every protected route returns 401 even with a
valid token. Setting the context is the whole point of the filter.

**Why `OncePerRequestFilter`.** It guarantees the logic runs exactly once per
request (a plain filter can run multiple times per dispatch, e.g. on forwards),
which avoids double work and weird double-authentication.

### 3.6 `UserDetailsService` — the bridge, and why not use the entity

**The idea.** `CustomUserDetailsService.loadUserByUsername(email)` looks up your
`User` and returns a Spring `UserDetails` carrying the role as an authority.

**Why not hand Spring your `User` entity directly.** Spring Security speaks
`UserDetails`, not your domain model. Adapting keeps your entity free of framework
concerns and lets you expose exactly the fields security needs (username, password
hash, authorities) without coupling the two.

### 3.7 Why CSRF is disabled here (and why your test needed `csrf()`)

**Why disabled.** CSRF attacks rely on the browser automatically attaching a
**session cookie** to a forged request. A JWT API doesn't use session cookies — the
token is sent explicitly in a header the browser won't attach on its own — so CSRF
protection guards against an attack that can't happen, while blocking legitimate
POSTs.

**⚠️ Why the test needed `.with(csrf())`.** `@WebMvcTest` doesn't load your
`SecurityConfig`; it applies Spring Boot's **default** security, which has CSRF
**enabled**. So in that test context, POST/PUT/DELETE need a CSRF token, added via
`.with(csrf())`, and requests need an authenticated user via `@WithMockUser`. That's
a test-harness detail, not a contradiction of the disabled-CSRF production config.

### 3.8 Matcher order — the reviews-vs-create trap

```java
.requestMatchers(HttpMethod.POST, "/books/*/reviews").authenticated()  // must be first
.requestMatchers(HttpMethod.POST, "/books").hasRole("ADMIN")
```

**⚠️ Why order matters.** Rules are evaluated top-to-bottom, first match wins.
`POST /books/1/reviews` should be allowed for any logged-in user, but a broad
`POST /books/**` → `hasRole('ADMIN')` rule would catch it and demand admin. Listing
the specific reviews rule **first** (and scoping the create rule to exactly
`/books`, no wildcard) keeps a normal user able to review while only admins create
books.

**What happens with the wrong order.** Reviews would require `ADMIN`, so ordinary
users silently get 403 on a feature that's supposed to be theirs.

### 3.9 Role vs authority, and the `ROLE_` prefix gotcha

`.roles("ADMIN")` stores the authority as `ROLE_ADMIN`; `.hasRole("ADMIN")` checks
for `ROLE_ADMIN` (it adds the prefix for you). Mixing them up — e.g.
`hasAuthority("ADMIN")` when the stored value is `ROLE_ADMIN` — silently fails
authorization. Keep to `roles(...)` + `hasRole(...)` together, as this project does.

### 3.10 `AuditorAware` from the SecurityContext

**Before security**, the auditor was hardcoded to `"System"`. **After**, it reads
the logged-in user's email from the `SecurityContext`.

**Why.** `@CreatedBy` / `@LastModifiedBy` are only useful if they record *who*
actually did it. With no security there was no "who," so `"System"` was a placeholder.
Now every created/modified row is stamped with the real user — falling back to
`"System"` only for unauthenticated actions.

### 3.11 Registration always assigns `USER`

**Why.** `register` hardcodes `Role.USER`. If it read the role from the request, a
user could register themselves as `ADMIN` — **privilege escalation** in one POST.
Admin is granted out of band (a DB update). Least privilege by default is the safe
choice, and interviewers notice it.

---

## Part 4 — Testing, in depth

### 4.1 Mocks test logic; they do not test the database

**What the service tests prove.** With Mockito, the repositories and mappers are
mocked, so the tests exercise the *rules*: a borrowed book can't be borrowed again,
an invalid category id → 404, a rejected borrow writes **no** record
(`verify(repo, never()).save(...)`).

**What they cannot prove.** Because nothing hits MySQL, they don't verify the real
derived queries, the projection JPQL, or the category FK. That's the gap
integration tests (Testcontainers) fill — and why "37 tests pass" still needs a
manual run against the DB before you trust a feature.

### 4.2 Strict stubbing catches over-fetching

`MockitoExtension` uses strict stubs: if you stub a call the method never makes, the
test fails with `UnnecessaryStubbing`. That's a feature — it flags when a service
loads more than it needs. The tests here pass strict stubbing, which is a small
signal the services are tight.

### 4.3 Why the tests use setter-based factories, not positional constructors

**⚠️ What went wrong here.** The old tests built entities with
`new Book(1L, "Java", "Author", 500, true, null, null, null, null)` — positional
constructor calls. When `Book` gained two fields (`category`, `reviews`), **every one
of those calls broke**, because the argument list shifted.

**The fix.** Helper methods that build via setters:
```java
private Book book(Long id, String title, String author, double price, boolean available) {
    Book b = new Book();
    b.setId(id); b.setTitle(title); ...
    return b;
}
```
Now adding a field to `Book` doesn't touch a single test. **Why it matters:** tests
should break when *behavior* changes, not when an unrelated field is added. Positional
constructors couple your tests to field order; setters (or builders) don't.

### 4.4 Why `@WithMockUser` + `csrf()` appeared after security

See 3.7 — adding Spring Security to the classpath makes `@WebMvcTest` apply default
security, so the existing controller tests suddenly needed an authenticated user and
a CSRF token. The tests didn't change what they *assert*; they changed what they must
*set up*. That's the normal cost of adding a cross-cutting concern like security.

---

## Part 5 — One full request, traced end to end

An admin creating a book, showing every concept firing in order:

```
1. POST /auth/login {email, password}
     AuthService -> AuthenticationManager verifies BCrypt hash (3.2)
     JwtService signs a token carrying email + expiry (3.4)

2. POST /books  (Authorization: Bearer <token>)
     JwtAuthFilter (3.5): reads token, validates signature/expiry,
         loads user via CustomUserDetailsService (3.6),
         sets Authentication(ROLE_ADMIN) in the SecurityContext
     SecurityConfig (3.8): POST /books needs ROLE_ADMIN -> allowed
     BookController -> BookService.save:
         duplicate-title guard (business rule)
         resolve categoryId -> Category entity (2.5)
         save inside the transaction (1.5)
         AuditorAware stamps createdBy = admin's email (3.10)
     BookMapper -> BookResponse (0.2), + HATEOAS links -> 201 Created
```

Same request, **no token** → stops at step 2 with **401**.
Same request, **USER token** → reaches authorization, fails role check → **403**.

---

## Appendix — the "what breaks without it" quick table

| Concept | Skip it / do it wrong → |
|---|---|
| DTOs instead of entities | leak password, `LazyInitializationException` |
| `@ManyToOne` on the many side | wrong/failed FK mapping |
| `fetch = LAZY` | N+1 queries under load |
| `findByBookId` (not `findById`) | queries the wrong column, subtle bug |
| `@Transactional` | half-done writes, ghost borrows |
| `() -> new X()` (no braces) | exception silently discarded → NPE |
| `final` + `@RequiredArgsConstructor` | dependency stays null → NPE |
| `mappedBy` | unwanted extra join table |
| `cascade` / `orphanRemoval` | reviews not saved / orphan rows |
| `@Getter/@Setter` on entities (not `@Data`) | recursive `toString`, lazy-load storms |
| getters/setters on DTOs | empty `{}` responses, broken mapping |
| resolve `categoryId` in service | category silently null |
| `LEFT JOIN` in projection | unreviewed books vanish |
| BCrypt (not plaintext/MD5) | catastrophic on any leak |
| JWT signature | forge an admin token |
| set `SecurityContext` in filter | 401 even with a valid token |
| matcher order | reviews demand admin |
| register → `USER` only | privilege escalation |
| setter factories in tests | one new field breaks every test |
