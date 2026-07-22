# DocMind — Your Guide

This is your reference for running, testing, and deploying DocMind at whatever phase it's
currently at. I'll keep this updated as each phase lands — check the "Current status" section
first, since not everything below exists yet.

For the full technical spec, see [docmind_master.md](docmind_master.md).
For build progress and internal decisions, see [PROGRESS.md](PROGRESS.md).

---

## Current status

**Nothing runnable yet — scaffolding in progress.** This section will be replaced with real
run instructions once the backend boots (Day 1).

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
