# Doxa

[![CI](https://github.com/thabomoagi/doxa/actions/workflows/ci.yml/badge.svg)](https://github.com/thabomoagi/doxa/actions/workflows/ci.yml)

AI analytics layer for **How Southa Are You**.

Doxa reads gameplay data from the existing Neon PostgreSQL database and provides analytics through a FastAPI backend and Svelte frontend.

## Stack

* **Backend:** Python, FastAPI, SQLAlchemy
* **Frontend:** Svelte 5, TypeScript, Tailwind CSS
* **Database:** PostgreSQL (Neon)
* **AI:** Planned

## Architecture

```text
How Southa Are You
        ↓
Spring Boot API
        ↓
Neon PostgreSQL
        ↓
      Doxa
     ↙    ↘
Analytics  AI
     ↓
 Dashboard
```

Doxa is **read-only** and does not modify the How Southa Are You database schema.

## Current Features

### Analytics

* Overview statistics
* Most-played questions
* Hardest questions
* Easiest questions
* Most-missed questions
* Fastest responses
* Slowest responses

### Planned

* Category analytics
* Difficulty analytics
* Activity trends
* AI-powered natural-language insights

## Running Locally

### Backend

```bash
cd backend
uv sync
uv run uvicorn app.main:app --reload
```

API:

```text
http://127.0.0.1:8000
```

Health check:

```text
http://127.0.0.1:8000/health
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend runs on the Vite development server.

## Running Tests

Backend tests live in `backend/tests` and run against a temporary SQLite database, so they need no running Postgres instance and no network access.

```bash
cd backend
uv sync --group dev
uv run pytest
```

Lint with the same command CI uses:

```bash
uv run ruff check .
```

## Screenshot

![Doxa Overview](frontend/assets/homescreen.png)

## What I Learned

Building Doxa taught me how to build an analytics layer on top of a database I don't own or control.

* **Async FastAPI.** Every endpoint is `async` and the database session arrives through `Depends(get_db)` instead of being imported. Keeping the session as a dependency is what made the service testable later, because `app.dependency_overrides` can swap the real engine for a test one.
* **SQLAlchemy 2.0 async with Neon.** I used `create_async_engine` with the psycopg driver and `async_sessionmaker`. Neon is a standard PostgreSQL endpoint, so the work was mostly connection handling: `pool_pre_ping` is there because serverless Postgres closes idle connections.
* **Push the aggregation into SQL.** Each analytics endpoint runs one aggregate query (`count`, `sum`, `case`, `avg` with `group_by`) rather than pulling rows into Python and looping over them. Accuracy and average response time are computed in the database and rounded at the edge.
* **Read-only by design.** Doxa reads tables owned by the existing Spring Boot API. Schema changes and writes stay out of this service.
* **Testing against real SQL.** The tests swap the session dependency for SQLite instead of mocking the queries, so the same aggregate expressions that run against Neon are the ones under test.

## Project Status

Doxa is currently in the **core analytics dashboard** stage. Batch 1 is implemented and connected to the real Neon database.
