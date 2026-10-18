# DocMind — RAG-Based Enterprise Knowledge Assistant

A full-stack Java/React application that uses Retrieval-Augmented Generation (RAG) to answer questions grounded in uploaded documents. Reduces hallucination by constraining the LLM to only answer from retrieved context.

**Live app:** https://doc-mind-vp.vercel.app
**Live API:** https://docmind-r42d.onrender.com/api/health

> Backend runs on Render's free tier and spins down after inactivity — the first request after idling can take 30–50 seconds to wake up.

## Features

- **Document Ingestion**: Upload PDFs, extract text, chunk intelligently, and store embeddings in Pinecone
- **Semantic Search**: Query documents using vector similarity via Pinecone
- **Grounded Answer Generation**: Gemini LLM answers only based on retrieved document chunks (with source citations)
- **MCP Tool Calling**: Structured queries and metadata filters via Model Context Protocol
- **Conversation Sessions**: Multi-turn chat with full session history
- **Authentication**: JWT-based auth with role-based access control (ADMIN, STAFF, USER)

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21 + Spring Boot 4.1.0 |
| Frontend | React + Vite |
| Vector DB | Pinecone (serverless, AWS us-east-1) |
| Embeddings & Generation | Google Gemini API |
| Relational DB | PostgreSQL (Neon serverless) |
| Auth | Spring Security + JWT |
| HTTP Client | Spring WebClient (reactive) |
| Backend Hosting | Render (Docker) |
| Frontend Hosting | Vercel |

## Prerequisites

- Java 21+
- Maven Wrapper included (no system Maven install needed)
- Node.js 18+ (for the frontend)
- External accounts:
  - [Google AI Studio](https://aistudio.google.com) — Gemini API key
  - [Pinecone](https://pinecone.io) — serverless index (dimension: 768)
  - [Neon](https://neon.tech) — PostgreSQL database

## Getting Started

### 1. Clone the Repository
```bash
git clone git@github.com:vigneshpraveen-official/DocMind.git
cd DocMind/docmind-backend
```

### 2. Set Up Credentials
Copy `.env.example` to `.env` and fill in your credentials:
```bash
cp .env.example .env
# Edit .env with your Gemini API key, Pinecone index host, and Neon connection string
```

**Required environment variables:**
- `DB_URL` — Postgres connection string from Neon
- `DB_USERNAME`, `DB_PASSWORD` — Neon credentials
- `GEMINI_API_KEY` — From Google AI Studio
- `PINECONE_API_KEY` — From Pinecone console
- `PINECONE_INDEX_HOST` — Your Pinecone index host
- `JWT_SECRET` — Generated locally (included in `.env`)

### 3. Build & Run the Backend
```bash
./mvnw spring-boot:run
```

Server starts on `http://localhost:8080`.

### 4. Run the Frontend
```bash
cd ../docmind-frontend
npm install
npm run dev
```

Set `VITE_API_BASE_URL` in a `.env` file (or your shell) if the backend isn't on `http://localhost:8080`.

## API Endpoints

**Authentication**
- `POST /api/auth/register` — Register a new user
- `POST /api/auth/login` — Login and get JWT token

**Documents**
- `POST /api/documents/upload` — Upload and ingest a PDF (requires ADMIN/STAFF)
- `GET /api/documents` — List ingested documents

**Chat**
- `POST /api/chat/sessions` — Create a new conversation session
- `POST /api/chat/query` — Ask a question, get grounded answer
- `GET /api/chat/sessions/{id}/messages` — Retrieve conversation history

See `docmind_master.md` for full API specification and request/response examples.

## Project Structure

```
docmind-backend/
├── src/main/java/com/docmind/
│   ├── config/              (WebClient, Security, Database config)
│   ├── controller/          (REST endpoints)
│   ├── service/
│   │   ├── ingestion/       (PDF extraction, chunking)
│   │   ├── embedding/       (Gemini embeddings)
│   │   ├── retrieval/       (Pinecone search)
│   │   ├── generation/      (Grounded prompt building, Gemini generation)
│   │   └── mcp/             (MCP tool server & client)
│   ├── entity/              (JPA entities)
│   ├── repository/          (Data access)
│   ├── dto/                 (Request/response DTOs)
│   └── security/            (JWT utilities, filters)
├── src/main/resources/
│   ├── application.yml      (Spring Boot config)
│   └── db/migration/        (Flyway SQL migrations)
└── pom.xml
```

## Development Phases

The project followed a 9-day build timeline — all 9 days complete:

1. **Day 1**: Verify external API connections (Postgres, Pinecone, Gemini) ✅
2. **Days 2–3**: Document ingestion pipeline (PDF → chunks → embeddings → Pinecone) ✅
3. **Day 4**: Query pipeline (question → embedding → retrieve → generate) ✅
4. **Day 5**: Prompt optimization & evaluation (measure hallucination reduction) ✅
5. **Days 6–7**: MCP tool server integration (structured queries) ✅
6. **Day 8**: JWT auth + React chat UI frontend ✅
7. **Day 9**: Deployment (Render backend, Vercel frontend, Neon DB, Pinecone cloud) ✅

Status: **Live and deployed.**

## Running Tests

```bash
./mvnw test
```

## Deployment

The backend is containerized (`docmind-backend/Dockerfile`) and deployed on **Render** via `render.yaml`. The frontend is deployed on **Vercel**, pointed at the Render backend via `VITE_API_BASE_URL`. Postgres runs on **Neon** (serverless) and the vector index runs on **Pinecone** (serverless, AWS).

Required backend environment variables (set as secrets on Render): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GEMINI_API_KEY`, `PINECONE_API_KEY`, `PINECONE_INDEX_HOST`, `JWT_SECRET`, `CORS_ALLOWED_ORIGIN_PATTERNS` (set to the deployed frontend's origin).

## References

- **Full specification**: [docmind_master.md](docmind_master.md) — Architecture, API spec, execution timeline
- **Progress tracking**: [PROGRESS.md](PROGRESS.md) — Build decisions, credential status, phase log
- **User guide**: [guide.md](guide.md) — How to run and deploy at any phase

## License

Private portfolio project.
