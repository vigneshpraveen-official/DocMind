# DocMind — Your Guide

This is your reference for running, testing, and deploying DocMind at whatever phase it's
currently at. I'll keep this updated as each phase lands — check the "Current status" section
first, since not everything below exists yet.

For the full technical spec, see [docmind_master.md](docmind_master.md).
For build progress and internal decisions, see [PROGRESS.md](PROGRESS.md).

---

## Current status

**Full ingestion pipeline works end to end (through Day 3).** Upload a PDF and it gets
extracted, chunked, embedded via Gemini, and upserted into Pinecone — the document's status
flips to `PROCESSED` once it's fully searchable. There's no search/chat endpoint yet — the
vectors are sitting in Pinecone ready to be queried, but the query side (Day 4) doesn't exist
yet.

Along the way I found and fixed a real bug: the Gemini embed request wasn't specifying an output
dimension, so it would have silently returned 3072-dim vectors instead of the 768 our Pinecone
index expects. If you tried the Day 1 health check before now and Gemini showed unhealthy, that
was probably why — worth re-checking `GET /api/health` now that it's fixed.

---

## Project layout (once scaffolded)

```
DocMind/
├── docmind_master.md      Original spec (tech stack, architecture, API design)
├── PROGRESS.md            Build progress + decisions log (for AI agents / future you)
├── guide.md                This file
├── docmind-backend/        Spring Boot API (Java 17)
└── docmind-frontend/       React chat UI (Vite) — added later, Day 8
```

---

## One-time setup you'll need to do

These need your action because they involve external accounts / secrets. I'll prompt you for
each when we reach the step that needs it — this is just a reference.

1. **Gemini API key** — from Google AI Studio (ai.google.dev). Used for embeddings + generation.
2. **Pinecone account** — a free serverless index. Dimension must be **768** (matches our
   embedding model's configured output size).
3. **Neon Postgres** — a free Postgres project, gives you a connection string.
4. Local `.env` file in `docmind-backend/` (created for you, gitignored) holding:
   - `GEMINI_API_KEY`
   - `PINECONE_API_KEY`
   - `PINECONE_INDEX_HOST`
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (from Neon)
   - `JWT_SECRET` (generated for you, no action needed)

**On secrets:** you can either paste key values to me directly in chat and I'll write them into
`.env`, or open `docmind-backend/.env` yourself in your editor and fill them in without telling
me the values — just say "done" and I'll proceed. The second option keeps secrets out of the
chat transcript, which is the safer habit. Your call.

---

## How to run the backend (once scaffolded)

```bash
cd docmind-backend
./mvnw spring-boot:run
```

No system-wide Maven install needed — `mvnw` (the Maven Wrapper) downloads what it needs on
first run.

On startup, check the logs for:
```
Postgres: ✅ OK
Gemini: ✅ OK
Pinecone: ✅ OK
```
Or hit `GET http://localhost:8080/api/health` any time to check the same thing manually.

---

## Trying document upload (Day 2 + 3)

Upload a PDF:
```bash
curl -F "file=@/path/to/your.pdf" http://localhost:8080/api/documents/upload
```
This now runs the whole pipeline synchronously — extraction, chunking, embedding every chunk via
Gemini, and upserting to Pinecone — so for a multi-page PDF the request may take a while (one
Gemini API call per chunk, sequentially). Returns the new document's id, final status, and how
many chunks were created. Status should be `PROCESSED` if everything worked, `FAILED` if
extraction found no text or embedding/upsert hit an error partway through.

List all uploaded documents (check status here too):
```bash
curl http://localhost:8080/api/documents
```

Inspect the chunks for one document (to sanity-check the extraction/chunking worked):
```bash
curl http://localhost:8080/api/documents/1/chunks
```

To confirm vectors actually landed in Pinecone, open the Pinecone console for the `docmind`
index and check the vector count — it should match the chunk count from the upload response.

Notes:
- Only `.pdf` files are accepted, max 20MB.
- Scanned/image-only PDFs with no extractable text will come back with status `FAILED` — that's
  expected, OCR isn't part of this project's scope.
- If status comes back `FAILED` after chunks were clearly created, check the server logs —
  it likely means the Gemini or Pinecone call failed partway (bad API key, rate limit, network).
  The chunks stay in Postgres either way; nothing needs to be re-uploaded once the underlying
  issue is fixed (Day 4+ could add a re-embed endpoint if that becomes annoying — not built yet).

---

## How to run the frontend (once scaffolded, Day 8+)

```bash
cd docmind-frontend
npm install
npm run dev
```

---

## How to run tests

```bash
cd docmind-backend
./mvnw test
```

---

## Deployment (Day 9 — not yet reached)

Deployment targets from the master doc: Render (backend), Vercel (frontend), Neon (already
your DB). **Per your instruction, I will not touch Vercel** (shared account/plugin access) —
when we reach this phase, I'll give you exact steps to run yourself, or you deploy the frontend
manually while I handle backend deployment guidance.

Detailed steps will be written here once we're actually at Day 9.

---

## Where things are tracked

- **Stuck or confused about what's been built?** → read `PROGRESS.md`.
- **Want to know the original design intent?** → read `docmind_master.md`.
- **Want to know how to *use* what's built right now?** → this file.
