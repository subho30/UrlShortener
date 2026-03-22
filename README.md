# 🔗 URL Shortener

A safety-first URL shortening service with AI-powered link previews. Know where you're going before you click.

Built with Java, Spring Boot, Spring AI, MySQL, Redis, and Docker.

---

## 🚀 Features

- **Shorten URLs** — Unique 6-character Base62 short codes across 56 billion combinations
- **Custom Aliases** — Choose your own short code (e.g. `/my-github`)
- **AI Safety Check** — Every link scanned for malicious or spammy content before saving
- **AI Link Preview** — Visitors see title, summary, category, tags, and safety score before redirecting
- **Smart Expiry** — AI suggests expiry duration based on content type (news vs docs vs profiles)
- **Click Analytics** — Hit count, creation time, last accessed, expiry tracking per link
- **Redis Caching** — Sub-millisecond redirects for popular links
- **Rate Limiting** — 10 shorten requests per IP per minute via token bucket
- **Soft Delete** — Deactivate links without losing analytics history
- **Input Validation** — Rejects malformed or non-HTTP/HTTPS URLs
- **Global Error Handling** — Consistent JSON error responses

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| AI | Spring AI + Ollama (Llama 3.2) |
| Database | MySQL 8.0 |
| Cache | Redis 7 |
| ORM | Spring Data JPA / Hibernate |
| Templating | Thymeleaf |
| Containerisation | Docker + Docker Compose |
| API Docs | Swagger / OpenAPI |
| Build Tool | Maven |
| Utilities | Lombok |

---

## 🤖 How the AI Pipeline Works
```
POST /api/shorten
        ↓
Step 1 — Safety Check (Llama 3.2)
  "Is this URL malicious or spammy?"
  → safetyStatus: SAFE/UNSAFE
  → safetyScore: 0-100
        ↓ (UNSAFE → reject 400)
Step 2 — Content Enrichment (Llama 3.2)
  Fetch page → analyse content
  → title, summary, category, tags
  → suggestedExpiryDays + reason
        ↓
Saved to MySQL + cached in Redis + returned in response
```

---

## 👁️ Link Preview Experience

When someone visits a short link instead of blindly redirecting they see:
```
┌─────────────────────────────────┐
│  🔗 Link Preview                │
│  GitHub Profile                 │
│                                 │
│  🌐 github.com                  │
│  View subho30's GitHub profile  │
│                                 │
│  📂 Technology  👁️ 0 clicks    │
│  ⏳ Expires Apr 21, 2026        │
│                                 │
│  ✅ Safe to visit (95/100)      │
│                                 │
│  🏷️ Java  Spring Boot  AI      │
│                                 │
│  [Continue to site →] [Go Back] │
└─────────────────────────────────┘
```

---

## 📐 Architecture
```
src/main/java/com/subho/urlshortner/
├── controller/
│   └── UrlController.java           ← REST endpoints + preview page
├── service/
│   ├── UrlShortenerService.java     ← Core business logic
│   ├── AiEnrichmentService.java     ← Spring AI pipeline
│   ├── PageFetcherService.java      ← Page content fetcher
│   ├── RedisCacheService.java       ← Redis cache operations
│   └── RateLimiterService.java      ← IP-based rate limiting
├── repository/
│   └── UrlMappingRepository.java    ← Database queries
├── model/
│   └── UrlMapping.java              ← JPA entity
├── dto/
│   ├── ShortenRequest.java          ← Input DTO with validation
│   └── ShortenResponse.java         ← Output DTO
└── exception/
    ├── GlobalExceptionHandler.java
    ├── UrlNotFoundException.java
    ├── UrlUnsafeException.java
    ├── CustomAliasAlreadyTakenException.java
    └── RateLimitException.java

src/main/resources/templates/
└── preview.html                     ← Thymeleaf preview page

src/test/java/com/subho/urlshortner/
└── service/
    └── UrlShortenerServiceTest.java ← 12 unit tests
```

---

## 📡 API Reference

### Shorten a URL
```
POST /api/shorten
```
**Request:**
```json
{
    "originalUrl": "https://www.example.com/some/long/url",
    "customAlias": "my-link",
    "expiryDays": 7
}
```
**Response — 201 Created:**
```json
{
    "shortCode": "my-link",
    "shortUrl": "http://localhost:8080/my-link",
    "originalUrl": "https://www.example.com/some/long/url",
    "createdAt": "2026-03-22T10:30:00",
    "expiresAt": "2026-03-29T10:30:00",
    "hitCount": 0,
    "title": "Example Domain",
    "summary": "A simple example webpage.",
    "category": "Technology",
    "tags": "Example,Web,Demo",
    "safetyStatus": "SAFE",
    "safetyScore": 97,
    "suggestedExpiryDays": 30,
    "expiryReason": "Static content remains relevant long-term"
}
```

### Preview Page
```
GET /preview/{shortCode}
```
Returns HTML preview page with AI-generated link information.

### Redirect (via preview)
```
GET /{shortCode}
```
Redirects to preview page first.

### Direct Redirect (skip preview)
```
GET /go/{shortCode}
```
Skips preview and redirects directly. Use for programmatic access.

### Get Analytics
```
GET /api/analytics/{shortCode}
```

### Deactivate a Link
```
DELETE /api/links/{shortCode}
```
Soft deletes a link — stops working but analytics and history preserved.

---

## 🏃 Running Locally

### Prerequisites
- Docker + Docker Compose
- [Ollama](https://ollama.com) with Llama 3.2

### Setup Ollama
```bash
# Mac
brew install ollama
ollama pull llama3.2
brew services start ollama
```

### Run the App
```bash
git clone https://github.com/subho30/UrlShortener.git
cd UrlShortener
docker-compose up --build
```

App starts at `http://localhost:8080`
Swagger UI at `http://localhost:8080/swagger-ui/index.html`

---

## ⚙️ Configuration
```properties
app.base-url=http://localhost:8080
app.short-code-length=6
app.default-expiry-days=30
spring.ai.ollama.base-url=http://host.docker.internal:11434
spring.ai.ollama.chat.model=llama3.2
```

---

## 🔒 Rate Limiting

- Max 10 requests/minute per IP on `POST /api/shorten`
- Returns `429 Too Many Requests` when exceeded
- In-memory token bucket via `ConcurrentHashMap`

---

## ❌ Error Responses
```json
{
    "timestamp": "2026-03-22T10:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "URL flagged as unsafe: Suspicious domain, potential phishing site"
}
```

| Scenario | HTTP Status |
|----------|-------------|
| Short code not found / expired | 404 |
| Invalid URL format | 400 |
| Unsafe URL detected | 400 |
| Custom alias already taken | 409 |
| Rate limit exceeded | 429 |
| Server error | 500 |

---

## 🧪 Tests
```bash
./mvnw test -Dspring.profiles.active=test
```

12 unit tests covering:
- URL shortening with AI enrichment
- Custom alias — success + conflict detection
- Unsafe URL rejection
- AI suggested expiry
- Cache hit — skips database
- Cache miss — hits database + populates cache
- Unknown short code → 404
- Soft delete + cache eviction
- Short code length and format validation
- Short code collision retry logic

All tests run in under 500ms with H2 in-memory database and mocked Redis/AI — no external dependencies needed.

---

## 🗺️ Roadmap

- [x] Core URL shortening — Base62, expiry, analytics
- [x] Rate limiting
- [x] MySQL + Docker Compose
- [x] Swagger / OpenAPI documentation
- [x] Spring AI — safety check, summarisation, categorisation
- [x] AI-powered link preview page
- [x] Redis caching
- [x] Custom aliases
- [x] Soft delete
- [x] Unit tests — 12 tests, <500ms
- [ ] Deploy to Railway
- [ ] React dashboard UI

---

## 👤 Author

**Subhadip Ghosh**
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=flat&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/subhadip-ghosh-2056b2179/)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=flat&logo=github&logoColor=white)](https://github.com/subho30)
