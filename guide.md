# DocMind — Your Guide

This is your reference for running, testing, and deploying DocMind at whatever phase it's
currently at. I'll keep this updated as each phase lands — check the "Current status" section
first, since not everything below exists yet.

For the full technical spec, see [docmind_master.md](docmind_master.md).
For build progress and internal decisions, see [PROGRESS.md](PROGRESS.md).

---

## Current status

**Baseline RAG is built, live-tested, and measured (through Day 5).** Upload a PDF (Day 2-3:
extracted, chunked, embedded, upserted to Pinecone), then ask a question about it (Day 4: your
question gets embedded, matched against Pinecone, and answered by Gemini using only the retrieved
chunks as context, with sources cited). No MCP tool-calling yet (Days 6-7), no chat history /
sessions yet (Day 8 — right now `/api/chat/query` is stateless, nothing is saved to Postgres
per-question).

**This session the app was actually run live for the first time** (Days 1-4 had only ever been
compile-checked). That surfaced 6 real bugs — a malformed DB URL, a Spring bean conflict, the
wrong Pinecone auth header, a bad response type, and two rounds of Gemini model swaps after
hitting availability/quota walls. All fixed; full detail in `PROGRESS.md`'s Decisions Log if
you're curious what broke and why. `GET /api/health` now genuinely returns all green.

**Day 5's real eval result:** ran 20 questions against a real indexed test document, once with a
naive prompt and once with the grounded one, scored all 40 live-generated answers by hand. Zero
hallucinations in either condition (good sign for retrieval quality) — the grounding instruction's
clear, measured win was **source citation: 52.9% → 100%**. Full writeup, methodology, and
per-question scoring table in `docmind-backend/eval/results.md`.

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

## Trying a chat query (Day 4)

Once at least one document has status `PROCESSED` (check via `GET /api/documents`), ask it a
question:
```bash
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the leave policy for interns?"}'
```

Response shape:
```json
{
  "answer": "According to the document, ...",
  "sources": [
    { "documentId": 1, "filename": "hr-policy.pdf", "page": 12 }
  ]
}
```

Things worth trying to get a feel for how grounded it actually is:
- Ask something the document clearly answers — check the answer is accurate and `sources` points
  at the right page.
- Ask something totally unrelated to your uploaded documents — it should say it doesn't have
  that information, not make something up.
- If no documents are indexed yet at all, it short-circuits to a canned "nothing indexed" answer
  without calling Gemini — that's intentional, not a bug.

This exact behavior is what Day 5's eval measured for real — see `docmind-backend/eval/results.md`
for the full 20-question scoring writeup.

---

## Day 5 eval set — reproducing or extending it

```bash
cd docmind-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=eval
```

This runs `eval/questions.json` through both a naive and a grounded prompt (reusing the same
Pinecone retrieval for both, so it's an apples-to-apples comparison) and writes
`eval/results-raw.json`. It's resumable — if it gets interrupted (e.g. a free-tier rate limit),
just run the same command again and it picks up where it left off, skipping question IDs already
in the results file. Needs a document already indexed first (see above) — the eval set assumes
`eval/sample-hr-policy.pdf` is what's loaded.

Scoring the results (correct / hallucinated / correctly-declined / cited) is manual — read
`eval/results-raw.json` against `eval/questions.json`'s `expectedAnswer` field and the source PDF.

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
