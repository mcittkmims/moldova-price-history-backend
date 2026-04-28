# moldova-price-history-backend

Spring Boot 4 backend for `api.pricehistory.md`.

This service is intentionally small for now:

- CORS for `https://pricehistory.md` and local Vite development.
- Public search endpoints that proxy `moldova-stores-api-node` at `https://scraper.pricehistory.md`.
- Response mapping into the current `tum-web-lab6` frontend `Product` JSON shape.
- PostgreSQL-backed product catalog persistence with per-product price history.
- Username/password auth endpoints that issue signed JWTs with a `permissions` claim.
- Existing catalog endpoints remain public for now. The scraper API key is still used only server-side.

## Requirements

- Java 17 or newer. It was verified locally with Java 25.
- Maven 3.9+

Spring Boot `4.0.6` pulls Spring Framework `7.0.7`.

## Configuration

Set these environment variables in production:

```bash
SCRAPER_BASE_URL=https://scraper.pricehistory.md
SCRAPER_API_KEY=...
CORS_ALLOWED_ORIGINS=https://pricehistory.md,https://www.pricehistory.md
DATABASE_URL=jdbc:postgresql://db-host:5432/pricehistory
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=...
PORT=8080
```

In the `prod` profile, the backend generates its JWT signing key in-app at startup and defaults tokens to a `1h` lifetime. Local development still uses `JWT_SECRET`.

The backend runs Flyway migrations automatically on startup and expects PostgreSQL in normal runtime environments.

For local frontend development, the default CORS list also allows:

```text
http://localhost:5173
http://127.0.0.1:5173
```

Local auth defaults:

```bash
JWT_SECRET=replace-with-a-long-random-secret
JWT_EXPIRATION=1m
```

## Run

```bash
mvn spring-boot:run
```

The default Spring profile is `local`, which points the backend at:

```text
http://localhost:8000
```

For local PostgreSQL development, the default datasource settings are:

```text
jdbc:postgresql://localhost:5432/pricehistory
username: postgres
password: postgres
```

You can start a matching local database with:

```bash
docker compose up -d
```

For local full-stack testing with `moldova-stores-api-node`, run the scraper with a development-only API key and pass the same value to the backend:

```bash
NODE_ENV=development DEV_API_KEY=local-pricehistory-dev npm start
SCRAPER_API_KEY=local-pricehistory-dev SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

The default `local` profile can also run fully in memory with H2, which avoids Docker/PostgreSQL for frontend integration work. In that mode the backend bootstraps its schema on startup and seeds the store catalog automatically.

Production should use:

```bash
SPRING_PROFILES_ACTIVE=prod
```

To keep Maven dependencies inside the project directory:

```bash
mvn -Dmaven.repo.local=.m2/repository spring-boot:run
```

## Test

```bash
mvn test
```

## Endpoints

```http
GET /products/search?q=iphone&page=1&page_size=24
GET /api/products/search?q=iphone&page=1&page_size=24
GET /stores
GET /categories
POST /auth/register
POST /auth/login
GET /actuator/health
```

Search supports these optional query parameters:

```text
store=Enter
stores=Enter,Darwin
category=Phones
sort=relevance|price-low|price-high|drop
```

`/products/search` and `/api/products/search` return a JSON array matching the current frontend `Product` type.
