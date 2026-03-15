# ArvyaX Journal — AI-Assisted Nature Journal

A full-stack journaling app where users reflect on immersive nature sessions and get AI-powered emotion analysis.

---

## Tech Stack

| Layer     | Technology               |
|-----------|--------------------------|
| Backend   | Java 17 + Spring Boot 3  |
| Database  | SQLite (via Hibernate)   |
| Frontend  | React 18 + Vite          |
| LLM       | Anthropic Claude API     |
| Container | Docker + Docker Compose  |

---

## Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 20+
- An Anthropic API key → https://console.anthropic.com

---

## Quick Start

### 1. Set your API key



### 2. Run the backend

```bash
cd backend
mvn spring-boot:run
# Server starts on http://localhost:8080
```

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
# App opens on http://localhost:5173
```

---

## Docker (recommended)

```bash
cp .env.example .env
# Edit .env and paste your ANTHROPIC_API_KEY

docker-compose up --build
# Backend: http://localhost:8080
# Frontend: http://localhost:3000
```

---

## API Reference

### POST /api/journal
Create a new journal entry.

```json
// Request
{ "userId": "123", "ambience": "forest", "text": "I felt calm today after listening to the rain." }

// Response 201
{ "id": 1, "userId": "123", "ambience": "forest", "text": "...", "emotion": null, "createdAt": "..." }
```

### GET /api/journal/:userId
Return all entries for a user (newest first).

### POST /api/journal/analyze
Analyze any text with LLM (stateless, does not persist).

```json
// Request
{ "text": "I felt calm today after listening to the rain" }

// Response 200
{ "emotion": "calm", "keywords": ["rain","nature","peace"], "summary": "User experienced relaxation during the forest session" }
```

### POST /api/journal/analyze/:entryId
Analyze a saved entry and persist the results back to the database.

### GET /api/journal/insights/:userId
Return aggregate insights for a user.

```json
{
  "totalEntries": 8,
  "topEmotion": "calm",
  "mostUsedAmbience": "forest",
  "recentKeywords": ["focus","nature","rain"]
}
```

---

## Project Structure

```
arvyax-journal/
├── backend/
│   └── src/main/java/com/arvyax/journal/
│       ├── controller/   # REST endpoints
│       ├── service/      # Business logic + LLM calls
│       ├── model/        # JPA entities + DTOs
│       ├── repository/   # Spring Data JPA queries
│       └── config/       # CORS, Rate limiter
├── frontend/
│   └── src/
│       ├── App.jsx       # Full single-page UI
│       └── main.jsx
├── Dockerfile.backend
├── Dockerfile.frontend
├── docker-compose.yml
├── README.md
└── ARCHITECTURE.md
```

---

## Bonus Features Implemented

- ✅ **Analysis caching** — MD5-keyed in-memory cache prevents duplicate LLM calls
- ✅ **Rate limiting** — Sliding-window limiter (10 req/min per key)
- ✅ **Docker setup** — Multi-stage builds for backend and frontend
- ✅ **Persist analysis** — `POST /analyze/:entryId` saves results to DB

---
