# Southagame

A Spring Boot REST API for a South African trivia game.

## Features

* JWT authentication
* User registration & login
* 5-question timed quiz
* Automatic scoring
* Leaderboard
* Admin question management
* Flyway database migrations
* Docker support
* PostgreSQL (local & Neon)

## Tech Stack

* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA
* PostgreSQL
* Flyway
* Docker
* Maven

## Run Locally

```bash
docker compose up --build
```

Or run directly:

```bash
./mvnw spring-boot:run
```

For local development, configure `backend/.env`. The Maven run goal starts the
app from the backend directory, where Spring Dotenv loads `.env` automatically;
you do not need to copy or reload it on each start. The root `.env.example`
lists the backend variables and can be copied once to `backend/.env`. The `.env`
file is ignored by Git, and real operating-system environment variables take
precedence over values in it. The example database settings require a local
PostgreSQL database named `southadb` with the configured credentials.

## API

Main endpoints:

* `POST /api/v1/auth/register`
* `POST /api/v1/auth/login`
* `POST /api/v1/auth/refresh`
* `POST /api/v1/auth/logout`
* `POST /api/v1/auth/forgot-password`
* `POST /api/v1/auth/reset-password`
* `GET /api/v1/users/me`
* `PATCH /api/v1/users/me`
* `PATCH /api/v1/users/me/password`
* `POST /api/v1/users/avatar` (multipart field: `file`)
* `GET /api/v1/users/me/stats`
* `POST /api/v1/qna/attempts/start`
* `POST /api/v1/qna/attempts/{id}/submit`
* `GET /api/v1/leaderboard/qna`
* `POST /api/v1/thirty-seconds/games/start`
* `POST /api/v1/thirty-seconds/games/{id}/rounds/score`
* `POST /api/v1/thirty-seconds/games/{id}/complete`
* `GET /api/v1/admin/qna/questions` (Admin)
* `POST /api/v1/admin/qna/questions` (Admin)

Q&A attempt endpoints require an authenticated user. Thirty Seconds game endpoints
are available without signing in.

## Environment Variables

```
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
APP_EMAIL_FROM
BREVO_USERNAME
BREVO_PASSWORD
SEED_QUESTIONS
APP_CORS_ALLOWED_ORIGINS
APP_UPLOAD_DIR
APP_UPLOAD_URL_PREFIX
```

`APP_CORS_ALLOWED_ORIGINS` is a comma-separated list of exact frontend origins,
for example `https://app.example.com,http://localhost:5173`. The API uses bearer
tokens, so cookie credentials are not required.

Profile pictures are decoded and normalized server-side to WebP, limited to
512px per dimension and below 90 KB before storage.

## Status
Currently in active development.

Planned additions include:

* Web frontend
* Mobile app
* Profile pictures
* Cloud deployment
* CI/CD
* Additional game modes
