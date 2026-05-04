# Let's Play API

RESTful CRUD API built with **Spring Boot 3**, **MongoDB**, and **JWT** authentication.

---

## Requirements

| Tool | Version |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |
| MongoDB | 6+ (local or Atlas) |

---

## Quick start

```bash
# 1. Clone and enter the project
git clone <repo-url> && cd letsplay

# 2. Start MongoDB locally (or point to Atlas in application.properties)
mongod --dbpath /data/db

# 3. Run the app
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.

---

## Configuration (`src/main/resources/application.properties`)

| Key | Description |
|---|---|
| `spring.data.mongodb.uri` | MongoDB connection string |
| `app.jwt.secret` | HS256 signing secret (min 32 chars, base64) |
| `app.jwt.expiration-ms` | Token validity in milliseconds (default 24 h) |
| `app.rate-limit.capacity` | Max requests per window per IP |
| `app.rate-limit.refill-seconds` | Window duration in seconds |

> **Production checklist**: replace the default JWT secret, use environment variables, enable HTTPS.

---

## Authentication

All protected endpoints require the `Authorization` header:

```
Authorization: Bearer <jwt-token>
```

---

## Endpoints

### Auth — public

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/auth/register` | `{ name, email, password }` | 201 `{ token, user }` |
| POST | `/api/auth/login` | `{ email, password }` | 200 `{ token, user }` |

**Register example**
```json
POST /api/auth/register
{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "strongpass123"
}
```

---

### Products

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/products` | None | List all products |
| GET | `/api/products/{id}` | None | Get product by ID |
| POST | `/api/products` | Any user | Create product |
| PUT | `/api/products/{id}` | Owner or ADMIN | Update product |
| DELETE | `/api/products/{id}` | Owner or ADMIN | Delete product |

**Create product example**
```json
POST /api/products
Authorization: Bearer <token>
{
  "name": "Wireless Controller",
  "description": "Bluetooth gamepad, 20h battery",
  "price": 49.99
}
```

---

### Users — ADMIN only

| Method | Path | Description |
|---|---|---|
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get user by ID |
| PUT | `/api/users/{id}` | Update name / email |
| DELETE | `/api/users/{id}` | Delete user |

---

## HTTP status codes

| Code | Meaning |
|---|---|
| 200 | OK |
| 201 | Created |
| 204 | No content (DELETE) |
| 400 | Validation error — details in `details` map |
| 401 | Missing or invalid token |
| 403 | Insufficient permissions |
| 404 | Resource not found |
| 409 | Email already registered |
| 429 | Rate limit exceeded |
| 500 | Unexpected server error (always returns JSON, never a stack trace) |

**Error response format**
```json
{
  "status": 400,
  "message": "Validation failed",
  "details": {
    "email": "must be a well-formed email address",
    "password": "size must be between 8 and 100"
  },
  "timestamp": "2024-05-20T14:32:00Z"
}
```

---

## Roles

| Role | Capabilities |
|---|---|
| `USER` | Create products, update/delete own products |
| `ADMIN` | Full access — all users and all products |

A user registers with `USER` role by default. To promote a user to `ADMIN`, update the `role` field directly in MongoDB:
```js
db.users.updateOne({ email: "admin@example.com" }, { $set: { role: "ADMIN" } })
```

---

## Security checklist

- Passwords hashed with BCrypt (strength 10)
- JWT signed with HMAC-SHA256
- MongoDB injection prevented — inputs sanitised and Spring Data query methods used (no raw queries)
- Sensitive fields (`password`) excluded from all API responses via DTO projection
- Global `@ControllerAdvice` ensures no 5xx errors leak stack traces
- Rate limiter: 60 requests / 60 s per IP (configurable)
- CORS restricted to declared origins
- Stateless sessions (no server-side session storage)

---

## Project structure

```
src/main/java/com/letsplay/
├── LetsPlayApplication.java
├── config/
│   └── SecurityConfig.java          Spring Security + CORS + filter chain
├── controller/
│   ├── AuthController.java          POST /api/auth/**
│   ├── ProductController.java       CRUD /api/products
│   └── UserController.java          CRUD /api/users  (ADMIN)
├── dto/
│   ├── request/                     Incoming payloads with @Valid constraints
│   └── response/                    Outgoing views (no password field)
├── exception/
│   ├── ApiExceptions.java           Domain exceptions
│   └── GlobalExceptionHandler.java  @RestControllerAdvice
├── model/
│   ├── User.java
│   └── Product.java
├── repository/
│   ├── UserRepository.java
│   └── ProductRepository.java
├── security/
│   ├── JwtService.java              Token generation & validation
│   ├── JwtAuthFilter.java           Bearer token extraction per request
│   ├── RateLimitFilter.java         Bucket4j per-IP throttling
│   └── MongoUserDetailsService.java Spring Security UserDetailsService
└── service/
    ├── AuthService.java
    ├── ProductService.java
    └── UserService.java
```
./mvnw spring-boot:run

# java-advanced
