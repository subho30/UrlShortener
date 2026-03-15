# 🔗 URL Shortener

A production-grade URL shortening service built with Java and Spring Boot.
Supports link expiry, click analytics, and IP-based rate limiting.

---

## 🚀 Features

- **Shorten URLs** — Generate a unique 6-character Base62 short code for any URL
- **Redirect** — Instantly redirect short URLs to their original destination
- **Link Expiry** — Links automatically expire after a configurable number of days
- **Click Analytics** — Track hit count, creation time, and last accessed time per link
- **Rate Limiting** — Max 10 shorten requests per IP per minute (in-memory, no Redis needed)
- **Input Validation** — Rejects malformed or non-HTTP/HTTPS URLs at the API layer
- **Global Error Handling** — Consistent JSON error responses across all failure scenarios

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Database | H2 (dev) / MySQL (prod) |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Validation |
| Build Tool | Maven |
| Utilities | Lombok |

---

## 📐 Architecture
```
src/main/java/com/subho/urlshortener/
├── controller/
│   └── UrlController.java         ← REST endpoints
├── service/
│   ├── UrlShortenerService.java   ← Core business logic
│   └── RateLimiterService.java    ← IP-based rate limiting
├── repository/
│   └── UrlMappingRepository.java  ← Database queries
├── model/
│   └── UrlMapping.java            ← JPA entity
├── dto/
│   ├── ShortenRequest.java        ← Input DTO with validation
│   └── ShortenResponse.java       ← Output DTO
└── exception/
    ├── GlobalExceptionHandler.java
    ├── UrlNotFoundException.java
    └── RateLimitException.java
```

---

## 📡 API Reference

### Shorten a URL
```
POST /api/shorten
```
**Request Body:**
```json
{
    "originalUrl": "https://www.example.com/some/very/long/url",
    "expiryDays": 7
}
```
**Response — 201 Created:**
```json
{
    "shortCode": "aB3xYz",
    "shortUrl": "http://localhost:8080/aB3xYz",
    "originalUrl": "https://www.example.com/some/very/long/url",
    "createdAt": "2026-03-15T10:30:00",
    "expiresAt": "2026-03-22T10:30:00",
    "hitCount": 0
}
```

---

### Redirect to Original URL
```
GET /{shortCode}
```
**Response — 302 Found**
Redirects to the original URL. Returns `404` if code not found or expired.

---

### Get Link Analytics
```
GET /api/analytics/{shortCode}
```
**Response — 200 OK:**
```json
{
    "shortCode": "aB3xYz",
    "shortUrl": "http://localhost:8080/aB3xYz",
    "originalUrl": "https://www.example.com/some/very/long/url",
    "createdAt": "2026-03-15T10:30:00",
    "expiresAt": "2026-03-22T10:30:00",
    "hitCount": 42
}
```

---

## ⚙️ Configuration

All configurable values live in `application.properties`:
```properties
app.base-url=http://localhost:8080
app.short-code-length=6
app.default-expiry-days=30
```

---

## 🏃 Running Locally

### Prerequisites
- Java 17+
- Maven 3.6+

### Steps
```bash
# Clone the repo
git clone https://github.com/subho30/urlshortener.git
cd urlshortener

# Run the app
./mvnw spring-boot:run
```

App starts at `http://localhost:8080`

H2 Console available at `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:urlshortenerdb`
- Username: `sa`
- Password: *(leave blank)*

---

## 🐳 Production Setup (MySQL + Docker)

*Coming soon — Docker Compose setup with MySQL for production use.*

---

## 🔒 Rate Limiting

The `POST /api/shorten` endpoint is rate limited per IP address:
- **Max requests:** 10 per minute
- **Response when exceeded:** `429 Too Many Requests`
- **Implementation:** In-memory sliding window via `ConcurrentHashMap`

---

## ❌ Error Responses

All errors return a consistent JSON structure:
```json
{
    "timestamp": "2026-03-15T10:30:00",
    "status": 404,
    "error": "Not Found",
    "message": "Short URL not found or expired: aB3xYz"
}
```

| Scenario | HTTP Status |
|----------|-------------|
| Short code not found / expired | 404 Not Found |
| Invalid URL format | 400 Bad Request |
| Rate limit exceeded | 429 Too Many Requests |
| Server error | 500 Internal Server Error |

---

## 🗺️ Roadmap

- [ ] MySQL + Docker Compose setup
- [ ] Redis caching for high-frequency redirects
- [ ] Custom aliases (user-defined short codes)
- [ ] Soft delete / deactivate links
- [ ] Swagger / OpenAPI documentation

---

## 👤 Author

**Subhadip Ghosh**
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=flat&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/subhadip-ghosh-2056b2179/)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=flat&logo=github&logoColor=white)](https://github.com/subho30)