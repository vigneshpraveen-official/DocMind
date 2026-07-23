# DocMind — RAG-Based Enterprise Knowledge Assistant

A full-stack Java/React application that uses Retrieval-Augmented Generation (RAG) to answer questions grounded in uploaded documents. Reduces hallucination by constraining the LLM to only answer from retrieved context.

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
| Backend | Java 17 + Spring Boot 4.1.0 |
| Frontend | React + Vite (coming in next phase) |
| Vector DB | Pinecone (serverless) |
| Embeddings & Generation | Google Gemini API |
| Relational DB | PostgreSQL (Neon cloud) |
| Auth | Spring Security + JWT |
| HTTP Client | Spring WebClient (reactive) |

## Prerequisites

- Java 17+
- Maven Wrapper included (no system Maven install needed)
- Node.js 18+ (for frontend, later phase)
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

### 3. Build & Run
```bash
./mvnw spring-boot:run
```

Server starts on `http://localhost:8080`.

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

The project follows a 9-day build timeline:

1. **Day 1**: Verify external API connections (Postgres, Pinecone, Gemini)
2. **Days 2–3**: Document ingestion pipeline (PDF → chunks → embeddings → Pinecone)
3. **Day 4**: Query pipeline (question → embedding → retrieve → generate)
4. **Day 5**: Prompt optimization & evaluation (measure hallucination reduction)
5. **Days 6–7**: MCP tool server integration (structured queries)
6. **Day 8**: JWT auth + React chat UI frontend
7. **Day 9**: Deployment (Render backend, Vercel frontend, Neon DB)

Current phase: **Phase 0 (Scaffolding)** — Backend skeleton complete, ready for Day 1 connection verification.

## Running Tests

```bash
./mvnw test
```

## Deployment

Backend deployment targets **Render**, frontend targets **Vercel**, and Postgres targets **Neon**. Detailed deployment steps are in `docmind_master.md` Section 11.

## References

- **Full specification**: [docmind_master.md](docmind_master.md) — Architecture, API spec, execution timeline
- **Progress tracking**: [PROGRESS.md](PROGRESS.md) — Build decisions, credential status, phase log
- **User guide**: [guide.md](guide.md) — How to run and deploy at any phase

## License

Private portfolio project.
