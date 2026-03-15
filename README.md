# 🌿 ArvyaX Journal — AI-Assisted Nature Journal

A full-stack journaling application where users reflect on immersive nature sessions and get real-time AI-powered emotion analysis.

---

## 🖥️ Demo

![Entries View](https://i.imgur.com/placeholder.png)

- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080

---

## 🛠️ Tech Stack

| Layer     | Technology                     |
|-----------|--------------------------------|
| Backend   | Java 17 + Spring Boot 3        |
| Database  | SQLite (via Hibernate/JPA)     |
| Frontend  | React 18 + Vite                |
| LLM       | Groq API (Llama 3.1 - Free)    |
| Container | Docker + Docker Compose        |

---

## 📁 Project Structure

```
arvyax-journal/
├── backend/
│   └── src/main/java/com/arvyax/journal/
│       ├── controller/        # REST API endpoints
│       ├── service/           # Business logic + LLM calls
│       ├── model/             # JPA entities + DTOs
│       ├── repository/        # Spring Data JPA queries
│       └── config/            # CORS, Rate Limiter
├── frontend/
│   └── src/
│       ├── App.jsx            # Full single-page UI
│       └── main.jsx
├── Dockerfile.backend
├── Dockerfile.frontend
├── docker-compose.yml
├── README.md
└── ARCHITECTURE.md
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 20+
- Groq API Key (free) → https://console.groq.com

---

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/arvyax-journal.git
cd arvyax-journal
```

### 2. Set your Groq API Key

Open `backend/src/main/resources/application.properties`:
```properties
groq.api.key=YOUR_GROQ_API_KEY_HERE
```

### 3. Run the Backend

```bash
cd backend
mvn spring-boot:run
```
Server starts at → http://localhost:8080

### 4. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```
App opens at → http://localhost:5173

---

## 🐳 Docker Setup (Optional)

```bash
# Set your API key
echo "GROQ_API_KEY=your_key_here" > .env

# Build and run
docker-compose up --build
```

- Frontend → http://localhost:3000
- Backend → http://localhost:8080

---

## 📡 API Reference

### POST `/api/journal`
Create a new journal entry.

**Request:**
```json
{
  "userId": "123",
  "ambience": "forest",
  "text": "I felt calm today after listening to the rain."
}
```

**Response `201`:**
```json
{
  "id": 1,
  "userId": "123",
  "ambience": "forest",
  "text": "I felt calm today after listening to the rain.",
  "emotion": null,
  "createdAt": "2026-03-15T18:30:00"
}
```

---

### GET `/api/journal/:userId`
Get all entries for a user (newest first).

**Response `200`:**
```json
[
  {
    "id": 1,
    "userId": "123",
    "ambience": "forest",
    "text": "I felt calm today...",
    "emotion": "calm",
    "keywords": "[\"rain\",\"nature\",\"peace\"]",
    "summary": "User experienced relaxation during the forest session.",
    "createdAt": "2026-03-15T18:30:00"
  }
]
```

---

### POST `/api/journal/analyze`
Analyze emotion of any text using LLM (stateless).

**Request:**
```json
{
  "text": "I felt calm today after listening to the rain"
}
```

**Response `200`:**
```json
{
  "emotion": "calm",
  "keywords": ["rain", "nature", "peace"],
  "summary": "User experienced relaxation during the forest session"
}
```

---

### POST `/api/journal/analyze/:entryId`
Analyze a saved entry and persist results to the database.

---

### GET `/api/journal/insights/:userId`
Get aggregated mental health insights for a user.

**Response `200`:**
```json
{
  "totalEntries": 8,
  "topEmotion": "calm",
  "mostUsedAmbience": "forest",
  "recentKeywords": ["focus", "nature", "rain"]
}
```

---

## ✨ Features

| Feature | Status |
|---------|--------|
| Journal Entry CRUD | ✅ |
| LLM Emotion Analysis (Real) | ✅ |
| Insights Dashboard | ✅ |
| Analysis Caching (MD5-based) | ✅ Bonus |
| Rate Limiting (10 req/min) | ✅ Bonus |
| Docker Setup | ✅ Bonus |
| CORS Configuration | ✅ |

---

## 🗄️ Data Model

```
journal_entries
┌──────────────┬─────────────┬──────────────────────────────┐
│ Column       │ Type        │ Notes                        │
├──────────────┼─────────────┼──────────────────────────────┤
│ id           │ INTEGER PK  │ Auto-increment               │
│ user_id      │ VARCHAR     │ Indexed                      │
│ ambience     │ VARCHAR     │ forest/ocean/mountain/...    │
│ text         │ TEXT        │ Raw journal entry            │
│ emotion      │ VARCHAR     │ Populated by LLM             │
│ keywords     │ TEXT        │ JSON array string            │
│ summary      │ TEXT        │ One-sentence LLM summary     │
│ created_at   │ DATETIME    │ Auto-set on insert           │
└──────────────┴─────────────┴──────────────────────────────┘
```

---

## 📄 License

MIT License — free to use and modify.

Author :Divyanshu Tiwari