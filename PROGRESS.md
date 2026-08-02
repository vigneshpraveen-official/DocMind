# DocMind — Progress & Context Log

> **Purpose of this file:** this is the single source of truth for any AI agent (or human)
> picking up this project cold. It records what's done, what's decided, what's open, and why.
> Update it at the end of every work session — don't wait for a phase to fully finish.
> The full original spec is in [docmind_master.md](docmind_master.md); this file tracks
> *deviations from* and *progress against* that spec, not a restatement of it.

---

## How to use this file (for an agent picking this up)

1. Read `docmind_master.md` first (the spec), then this file (current reality vs. spec).
2. Check "Current Phase" below to know what's in flight.
3. Check "Decisions Log" for anything that changed from the original master doc.
4. Check "Open Questions" before assuming — if something's listed there, it hasn't been
   decided, don't guess.
5. Check "Environment / Credentials Status" before asking the user for something they've
   already provided.
6. Update this file yourself before ending your session.

---

## Current Phase

**Phase 1 — Day 5 Eval Set (complete).** Big milestone this session: the app was actually run
live for the first time (Days 1-4 had only ever been compile-checked, never booted) —
`./mvnw spring-boot:run` against the real Neon/Gemini/Pinecone credentials in `.env`. This
surfaced and fixed **six real bugs** that had been sitting undetected since nobody had run the
app before (full details in Decisions Log, all dated 2026-08-02):
1. `.env`'s `DB_URL` embedded `user:password@` in JDBC-URL style, which the Postgres JDBC driver
   rejects (that's libpq/psql connection-string syntax, not JDBC)
2. Two ambiguous `WebClient` beans caused a startup `NoUniqueBeanDefinitionException` — collapsed
   to one, since neither bean's `baseUrl`/default-header config was ever actually used by any
   caller (every service builds absolute URLs and sets its own auth header)
3. `PineconeService` sent `Authorization: Bearer <key>`; Pinecone's REST API actually wants
   `Api-Key: <key>` (the master doc's own sample code had this right — Day 1's implementation
   deviated from it without noticing)
4. `PineconeQueryResponse.usage` was typed `long` but Pinecone returns `{"readUnits": N}`, an
   object — field was unused anywhere in the codebase, so removed rather than modeled
5. `gemini-2.5-flash` (originally configured generation model) returns 404 "no longer available
   to new users" for this project's API key — switched to `gemini-3.5-flash`
6. `gemini-3.5-flash`'s free-tier daily quota (`limit: 20` generateContent requests/day) was
   exhausted partway through the eval run — switched to `gemini-3.5-flash-lite`, which has a
   separate, more generous free-tier quota and produced equally good answers in practice

With those fixed, `GET /api/health` reports all three connections healthy, and the full
ingest → embed → upsert → query → grounded-answer pipeline was verified working live end to end
against a real test document (`eval/sample-hr-policy.pdf`, a fabricated Northwind Analytics
employee handbook built specifically for this eval).

**Real eval results (20 questions, naive vs. grounded prompt, full writeup in
`eval/results.md`):** zero factual hallucinations occurred in *either* prompt variant — a
genuinely positive sign the retrieval pipeline is solid. The grounding instruction's clearly
measured win was **source citation: 52.9% (naive) → 100% (grounded)**, a +47 percentage point
improvement in answers that name a specific, checkable section/page. That citation-rate number,
not a fabricated "hallucination reduced by X%" claim, is the honest, defensible resume metric
from this project — see `eval/results.md` for the full scoring table and reasoning.

---

## Decisions Log (deviations from docmind_master.md)

| Date | Decision | Reason |
|---|---|---|
| 2026-07-22 | Frontend: React chat UI confirmed (not API-only) | User choice — see Section 14 open item in master doc, now resolved |
| 2026-07-22 | MCP: use official `modelcontextprotocol/java-sdk` (GA v2.0.0, Spring AI-backed) instead of the hand-rolled JSON-RPC fallback in master doc Section 7 | Researched during scaffolding — a mature, official Java SDK now exists. Stronger resume claim ("used the official SDK") than a hand-rolled server. |
| 2026-07-22 | Generation model: `gemini-2.5-flash`. Embedding model: `gemini-embedding-001` (text-only, output dimension set to 768) | `text-embedding-004` (named in master doc) was deprecated/shut down Jan 2026. Confirmed current model IDs via ai.google.dev docs. Both are free-tier. Model names are externalized to config (`application.yml` / env vars), not hardcoded, so this is a one-line change later if needed. |
| — | ⚠️ `gemini-2.5-flash` is scheduled for shutdown by Google on 2026-10-16 | Flagging so whoever deploys/demos this after that date checks `ai.google.dev/gemini-api/docs/changelog` and swaps the config value if needed. Not a code change. |
| 2026-07-22 | Git workflow: direct commits to `main`, small logical commits (not one-per-file), plain conventional-ish messages, no AI-attribution footers | User requirement: repo must read as normal human solo-dev history. No feature-branch/PR workflow requested. |
| 2026-07-22 | Spring Boot version: **4.1.0** (not 3.x as master doc assumed) | start.spring.io no longer offers 3.x at all (only 4.0.x/4.1.x) as of this build window; user chose to go with current GA rather than hand-building on an aging 3.x line. Note: Maven Central *does* still have 3.5.16 if this ever needs revisiting — 3.x isn't literally dead, just not offered via Initializr. Java target kept at 17 (Boot 4.1 still supports it). Real Spring Boot version strings on Maven Central have no `.RELEASE` suffix (e.g. `4.1.0`, not `4.1.0.RELEASE`) — start.spring.io's metadata API returns the old-style suffixed id, which does not resolve; had to correct the pom.xml parent version by hand after the first build failure. |
| 2026-07-22 | `MessageRole` enum uses uppercase `USER`/`ASSISTANT` constants (Java convention), not the lowercase `'user'/'assistant'` literal in the master doc's SQL | Trivial naming deviation, applied to both the Java enum and the `messages.role` CHECK constraint in `V1__init_schema.sql` for consistency. |
| 2026-07-22 | Added a `users` table (not in master doc's Section 4 DDL) with `id, username, email, password_hash, role, created_at` | Master doc's `documents.uploaded_by` and `conversation_sessions.user_id` reference `users(id)` but never defines the table — needed for the auth system described in Section 5. `role` is `ADMIN/STAFF/USER` per the API spec's role column. |
| 2026-07-22 | `.env` loaded via Spring Boot's `spring.config.import: optional:file:.env[.properties]` (parses the .env as a properties file) rather than a third-party dotenv library | No extra dependency needed; officially supported Spring Boot config-import mechanism. Only works when the JVM's working directory is `docmind-backend/` (true for `./mvnw spring-boot:run`). |
| 2026-07-22 | `SecurityConfig` currently permits all requests (stateless, CSRF disabled, no JWT yet) | Placeholder so the app can boot and the Postgres/Pinecone/Gemini connections can be verified before building full JWT auth (planned for the "Day 8" phase). **Not safe to deploy in this state** — flagged with a comment in the code itself too. Deployment (final phase) happens after auth is built, so this window never reaches production. |
| 2026-07-29 | Chunking uses word-boundary sliding window (split on whitespace, rejoin), not the master doc's raw character-substring example | Master doc's sample `chunkText()` slices by character offset, which can cut a word in half at every chunk boundary. Splitting on words first avoids that with no added dependency (no tokenizer library). Chunk size 500 words / 50 word overlap, approximating the spec's "~500 tokens, 50-token overlap" — words vs. tokens differ only in the constant factor, structurally identical. |
| 2026-07-29 | PDFBox 3.0.1: PDF loading uses `org.apache.pdfbox.Loader.loadPDF(byte[])`, not `PDDocument.load(InputStream)` | PDFBox 3.x moved static loaders out of `PDDocument` into a dedicated `Loader` class; the old `PDDocument.load(...)` overloads used in most online examples (written for PDFBox 2.x) no longer exist. Discovered via compile error, not upfront research — flagging in case future PDFBox-touching code hits the same surprise. |
| 2026-07-29 | Each `Chunk` gets a `pinecone_vector_id` (UUID) generated at chunk-creation time (Day 2), before any embedding exists | `chunks.pinecone_vector_id` is `NOT NULL` in the schema, but embedding/upsert doesn't happen until Day 3. Generating the UUID upfront avoids a schema/nullability change; Day 3 will reuse the same UUID as the Pinecone vector ID when it embeds and upserts, so Postgres and Pinecone stay linked by an ID that's stable from the moment the chunk is created. |
| 2026-07-29 | `Document.status` stays `PROCESSING` after Day 2 ingestion succeeds (not flipped to `PROCESSED`) | `PROCESSED` should mean "fully searchable" (i.e., embedded + upserted to Pinecone), which doesn't happen until Day 3. Only sets `FAILED` on extraction error or zero extractable chunks (e.g., scanned/image-only PDFs — OCR is out of scope). |
| 2026-07-29 | Ingestion services live in `service.document` (not `service.ingestion` / `service.embedding` split exactly as master doc's suggested tree shows) | Minor, harmless deviation from the master doc's illustrative package layout — `DocumentExtractionService`, `ChunkingService`, `DocumentIngestionService` all live together in `service/document/`. `GeminiEmbeddingService` (`service/embedding/`) and `PineconeService` (`service/retrieval/`) do match the master doc. Flagging only so a future agent doesn't go looking for a `service.ingestion` package that doesn't exist. |
| 2026-07-30 | **Bug fix:** `GeminiEmbedRequest` now sends `taskType` and `outputDimensionality` in the embedContent request body; previously sent neither | Without `outputDimensionality`, `gemini-embedding-001` returns its default 3072-dim vector, not the 768 dims our Pinecone index is provisioned for — every upsert would have failed with a dimension mismatch, and Day 1's Gemini health check (`embedding.length == 768`) would have silently returned `false` even with a working API key. Found while wiring Day 3, not by the user reporting it — worth a runtime re-check of Day 1's health endpoint since this changes its actual behavior. `taskType` also added since Gemini's embedding model is asymmetric: `RETRIEVAL_DOCUMENT` for chunks going into the index (Day 3, what's implemented now), `RETRIEVAL_QUERY` for the user's question at search time (Day 4, constant already defined as `GeminiEmbeddingService.TASK_TYPE_QUERY` — not yet called anywhere). Using the wrong task type doesn't error, it just quietly degrades retrieval relevance, so this was worth getting right now rather than patching later. |
| 2026-07-30 | Pinecone vector metadata includes the full chunk text (`documentId`, `chunkId`, `filename`, `pageNumber`, `text`) | Standard RAG pattern: storing chunk text directly in Pinecone metadata means Day 4's query step can build a grounded prompt straight from Pinecone's response, no extra Postgres round-trip needed. Chunk size (~500 words) is well under Pinecone's per-vector metadata limit. |
| 2026-07-30 | Removed `@Transactional` from `DocumentIngestionService.ingest()` | The method now makes sequential external HTTP calls (Gemini embed per chunk, Pinecone upsert) — wrapping that in a single Spring-managed DB transaction would hold a Postgres connection open for the entire external-call duration, which doesn't scale and isn't good practice. Each repository `save`/`saveAll` call still gets its own implicit transaction from Spring Data JPA; this project doesn't need cross-call atomicity badly enough to justify a manual transaction-scoping workaround (e.g. saving twice — once as PROCESSING, again as PROCESSED/FAILED — is an acceptable eventual-consistency window for a single-user portfolio project). |
| 2026-07-30 | Pinecone `upsert()` batches vectors in groups of 100 | Matches Pinecone's own guidance for upsert request size; irrelevant for small test PDFs but avoids a payload-size failure on a large document with hundreds of chunks. |
| 2026-07-31 | `POST /api/chat/query` takes only `{ question }` — no `sessionId`, nothing written to `conversation_sessions`/`messages` yet, unlike the master doc's sample request/response in Section 5 | Master doc's own timeline (Section 9) explicitly scopes "session/message persistence" to Day 8, alongside JWT auth. Sessions without auth have no real owner concept yet, so building session plumbing now means redoing it once users exist. Keeping Day 4 stateless avoids that throwaway work — Day 8 will add `sessionId` to the request and persist both the user's question and the answer as `Message` rows. |
| 2026-07-31 | Grounded prompt uses the master doc's system instruction (Section 6) verbatim, with one addition: each context chunk is labeled `[Source: {filename}, page {N}]` above its text | The master doc's instruction says "cite which section supports your answer" but the raw context chunks it shows have no labels to cite by. Adding the label lets Gemini's citation actually reference something concrete, and gives us a prompt worth using as Day 5's "refined" baseline (vs. a naive unlabeled/no-instruction prompt) rather than needing to invent one from scratch then. |
| 2026-07-31 | Generation temperature set to 0.2 (low, not default ~1.0) | Grounded factual Q&A benefits from low temperature — less creative rephrasing, less chance of drifting from the provided context. Not in the master doc's snippet, but directly serves the project's core "reduce hallucination" goal, so worth setting deliberately now rather than leaving at the API default. |
| 2026-07-31 | If Pinecone returns zero matches (empty index), skip the Gemini generation call entirely and return a canned "I don't have information on that" response | No context means there's nothing for Gemini to ground an answer in — calling it anyway would just burn an API call for a foregone conclusion. If Pinecone returns matches but they're weakly relevant, they're still passed through to Gemini rather than filtered by a score threshold — deciding "is this actually relevant" is left to the grounding instruction itself, which is exactly what Day 5's eval set is designed to test. |
| 2026-08-02 | **Bug fix:** `.env`'s `DB_URL` changed from `jdbc:postgresql://neondb_owner:PASSWORD@host/db?...` to `jdbc:postgresql://host/db?...` (credentials removed from the URL) | The Postgres JDBC driver rejects `user:password@` embedded in the URL authority — that's psql/libpq connection-string syntax, not JDBC. `application.yml` already sets `spring.datasource.username`/`password` separately from `DB_USERNAME`/`DB_PASSWORD`, so the URL never needed the credentials at all. This was the very first thing that broke on the first-ever live run; Days 1-4 had been built and "verified" purely by `mvn compile` until this session. |
| 2026-08-02 | **Bug fix:** collapsed `WebClientConfig`'s two `WebClient` beans (`geminiWebClient`, `pineconeWebClient`) into one plain `webClient()` bean | Caused a startup `NoUniqueBeanDefinitionException` the moment any service tried to autowire a bare `WebClient` (no `@Qualifier` anywhere). Root cause wasn't just ambiguity — the two beans' `baseUrl`/default-header setup was dead configuration: every real call site (`GeminiEmbeddingService`, `GeminiGenerationService`, `PineconeService`) builds a fully-qualified absolute URL and sets its own auth header per-request, and passing an absolute URL to `WebClient.uri()` ignores any configured base URL anyway. Removing the two specialized beans wasn't a workaround, it was deleting code that never did anything. |
| 2026-08-02 | **Bug fix:** `PineconeService` now sends header `Api-Key: <key>`, not `Authorization: Bearer <key>` | Pinecone's REST API wants `Api-Key`, confirmed via direct `curl` (200 with `Api-Key`, 401 with `Bearer`). The master doc's own Section 6 sample code had this right (`.header("Api-Key", pineconeApiKey)`); Day 1's implementation silently deviated from it without anyone noticing, since the app had never been run. |
| 2026-08-02 | **Bug fix:** removed `PineconeQueryResponse.usage` field entirely (was typed `long`) | Pinecone actually returns `"usage": {"readUnits": N}` — an object, not a number — which broke Jackson deserialization on every query call. The field wasn't read anywhere in the codebase, so removed rather than modeled as a nested `Usage` class. |
| 2026-08-02 | Generation model changed twice more: `gemini-2.5-flash` → `gemini-3.5-flash` → `gemini-3.5-flash-lite` | `gemini-2.5-flash` returns 404 "no longer available to new users" for this project's API key (confirmed via direct `curl` against the real `v1beta/models` list — it's still a listed model, just not usable by this key). Switched to `gemini-3.5-flash`, which worked — until its free-tier quota (`limit: 20` generateContent requests/day, confirmed from the actual 429 error body) was exhausted mid-eval. Switched again to `gemini-3.5-flash-lite`, which has a separate quota bucket (confirmed via `curl` before switching) and produced equally accurate answers in the Day 5 eval. Both switches are one-line config changes (`docmind.gemini.generation-model` in `application.yml`) — exactly why that value was externalized instead of hardcoded. Whoever revisits this project later should expect to repeat this dance; Gemini's free-tier model lineup and quotas change often. |
| 2026-08-02 | Built `EvalRunner` (`com.docmind.eval`, `@Profile("eval")`) as a resumable `CommandLineRunner`, not a REST endpoint | Day 5 needs to run every eval question through both a naive and a grounded prompt using the *same* retrieved context (to isolate the prompt's effect from retrieval variance) and dump results for scoring. A permanent `/api/eval/run` endpoint would be unnecessary production surface for a one-off dev task, so it's profile-gated (`-Dspring-boot.run.profiles=eval`) and exits via `SpringApplication.exit()` when done. Made it resumable (skips question IDs already present in `eval/results-raw.json`) after the first run got cut off by a rate limit partway through — re-running from scratch would have wasted already-consumed quota for no reason. Retry logic backs off on `RESOURCE_EXHAUSTED`/429 specifically and re-raises anything else immediately. |
| 2026-08-02 | Day 5's real finding is a citation-rate metric (52.9% → 100%), not a hallucination-rate metric, because hallucination rate was 0% in both prompt conditions | Manually scored all 40 answers (20 questions × naive + grounded) against the source document — zero fabricated facts in either condition, on both the 17 answerable and 3 deliberately-unanswerable questions. Reporting a hallucination-rate improvement would have meant inventing a number that isn't in the data. What *did* measurably differ: the grounding instruction's "cite which section supports your answer" pushed citation rate from 9/17 to 17/17. Full reasoning and the per-question scoring table are in `eval/results.md` — kept the writeup honest about the nuance (strong retrieval + strong model meant naive prompting also avoided hallucinating on this particular sample) rather than forcing a bigger-sounding but fabricated claim. |

---

## Environment / Credentials Status

Tracked here so we don't re-ask the user for things already provided. **Never write actual
secret values into this file or any committed file** — only track *whether* a given credential
has been provided and is sitting in the local (gitignored) `.env`.

| Credential | Status | Notes |
|---|---|---|
| Gemini API key | **Confirmed working** (2026-08-02) | In `.env`, verified live via `GET /api/health` and the Day 5 eval (dozens of real embed/generate calls) |
| Pinecone API key + index host | **Confirmed working** (2026-08-02) | In `.env`, verified live — upsert and query both confirmed against the real `docmind` index (dimension 768) |
| Neon Postgres connection string | **Confirmed working** (2026-08-02) | In `.env`; `DB_URL` had to be corrected (credentials were embedded in the URL, which the JDBC driver rejects — see Decisions Log). Flyway migration applies cleanly against it. |
| JWT secret | Generated, sitting in `.env` | Not yet used — auth isn't built until Day 8 |
| GitHub push access | Verified working | SSH key authenticated as `vigneshpraveen-official`. Remote `origin`: `git@github.com:vigneshpraveen-official/DocMind.git`. Days 1-4 commits already pushed. |

---

## Open Questions

None blocking right now. Resolved items moved to Decisions Log above.

---

## Environment Notes (this dev machine)

- OS: Ubuntu (Linux 7.0.0-27-generic), user runs as `vp`
- Java: OpenJDK 25 installed system-wide (no JAVA_HOME set explicitly). Spring Boot project
  will target Java 17 language level via Maven `<release>` for compatibility/portability —
  runs fine on the JDK 25 runtime since Java is backwards-compatible at bytecode level.
- Maven: **not installed system-wide**. Using the Maven Wrapper (`mvnw`) generated by Spring
  Initializr instead — this downloads its own Maven build under the hood, no `sudo` needed.
- Node: v22.22.1, npm 9.2.0 — sufficient for Vite + React frontend.
- No Docker installed. Not required — Postgres is Neon (cloud), no local DB container needed.
- No local `psql` client. Not required for app function; only useful for manual DB debugging.
  Can be installed later with `sudo apt install postgresql-client` if the user wants it — left
  to the user per their instruction that anything needing admin privilege is theirs to run.

---

## Phase Log

### Phase 1 — Day 5 Eval Set (completed 2026-08-02)
- [x] First-ever live run of the app (`./mvnw spring-boot:run` against real Neon/Gemini/Pinecone
      credentials) — found and fixed 6 real bugs, see Decisions Log entries dated 2026-08-02
- [x] `GET /api/health` confirmed all-green (`postgres: true, gemini: true, pinecone: true`)
- [x] Built `eval/sample-hr-policy.pdf` — a fabricated 3-page employee handbook (Northwind
      Analytics), generated from `eval/sample-hr-policy.txt` via `soffice --headless --convert-to pdf`
- [x] Uploaded and confirmed `PROCESSED` (3 chunks, all correctly extracted/embedded/indexed)
- [x] Manually verified Day 4's `/api/chat/query` end-to-end with real questions (accurate,
      correctly-sourced answer for an answerable question; correct decline for an unanswerable one)
- [x] `eval/questions.json` — 20 questions (17 answerable, 3 deliberately not covered)
- [x] `PromptBuilder.buildNaivePrompt()` added alongside the existing grounded one
- [x] `EvalRunner` (`com.docmind.eval`, `@Profile("eval")`) — resumable eval harness, retries
      with backoff on rate limits, writes `eval/results-raw.json`
- [x] Ran the eval live against real Gemini/Pinecone APIs (all 40 answers are real model output,
      not hand-written) — took 3 attempts across two generation models due to free-tier quota
      limits, see Decisions Log
- [x] Manually scored all 40 answers against the source document; wrote up methodology, full
      per-question scoring table, and honest findings in `eval/results.md`
- [x] All code compiles cleanly (`./mvnw compile`)
- [ ] Commit + push Day 5 code (in progress)

**Result:** 0% hallucination rate in both naive and grounded conditions (retrieval pipeline is
solid); grounding instruction raised verifiable source citation from 52.9% to 100% — the real,
defensible resume number from this project. Full detail in `eval/results.md`.

*(Next: Day 6-7 — research MCP Java tooling, build the MCP tool server (`search_documents` tool),
wire it into the orchestrator.)*

### Phase 1 — Day 4 Query + Grounded Generation (completed 2026-07-31)
- [x] New DTOs: `GeminiGenerateRequest`/`Response`, `ChatQueryRequest`/`Response`, `SourceReference`
- [x] `GeminiGenerationService.generate(prompt)` — calls `generateContent` on the configured
      generation model (`gemini-2.5-flash` originally; see Day 5 for why that changed twice),
      temperature 0.2
- [x] `RetrievedChunk` record + `PromptBuilder.buildGroundedPrompt(question, chunks)` — master
      doc's grounding instruction verbatim, plus `[Source: filename, page N]` labels per chunk
      (see Decisions Log for why)
- [x] `ChatService` orchestrator (`service/chat/`) — embeds question with `RETRIEVAL_QUERY`,
      queries Pinecone top-5, short-circuits to a canned response on zero matches, otherwise
      builds the prompt, calls generation, and returns deduped `sources`
- [x] `ChatController` — `POST /api/chat/query`, `@Valid`-checked `{ question }` body
- [x] All new code compiles cleanly (`./mvnw compile`)
- [x] Committed and pushed to `origin/main`
- [x] Manual end-to-end test: confirmed working live during Day 5 (see above) — accurate, sourced
      answers for answerable questions, correct declines for unanswerable ones

### Phase 1 — Day 3 Embedding + Vector Indexing (completed 2026-07-30)
- [x] **Bug fix:** `GeminiEmbedRequest` now includes `taskType` + `outputDimensionality`
      (previously missing — see Decisions Log 2026-07-30 for why this mattered)
- [x] `GeminiEmbeddingService.embed(text, taskType)` overload added; `TASK_TYPE_DOCUMENT` and
      `TASK_TYPE_QUERY` constants defined (`RETRIEVAL_DOCUMENT` used now, `RETRIEVAL_QUERY`
      reserved for Day 4's query embedding)
- [x] New DTOs: `PineconeVector`, `PineconeUpsertRequest`, `PineconeUpsertResponse`
- [x] `PineconeService.upsert(List<PineconeVector>)` — batches in groups of 100
- [x] `DocumentIngestionService.ingest()` now runs the full pipeline: extract → chunk → save
      chunks → embed each chunk (Gemini) → upsert to Pinecone (with `documentId`, `chunkId`,
      `filename`, `pageNumber`, `text` as metadata) → set `Document.status = PROCESSED`
- [x] `@Transactional` removed from `ingest()` (see Decisions Log — external HTTP calls shouldn't
      sit inside a DB transaction)
- [x] All new code compiles cleanly (`./mvnw compile`)
- [x] Committed and pushed to `origin/main`
- [ ] Manual end-to-end test: upload a real PDF, confirm status reaches `PROCESSED` and vectors
      are queryable in the Pinecone console — still not confirmed by an actual run; Day 4 built
      on top of this anyway (query side is independently testable once any doc is indexed)

### Phase 1 — Day 2 Document Ingestion (completed 2026-07-29)
- [x] Added PDFBox 3.0.1 + commons-lang3 to `pom.xml`
- [x] `DocumentExtractionService` — extracts text per-page from an uploaded PDF via PDFBox
      (`Loader.loadPDF`, PDFBox 3.x API)
- [x] `ChunkingService` — word-boundary sliding window chunking (500 words / 50 overlap,
      configurable), avoids the naive character-substring approach's mid-word cuts
- [x] `DocumentIngestionService` — orchestrates: validate upload (PDF only, non-empty) → save
      `Document` (status `PROCESSING`) → extract per-page text → chunk each page → save `Chunk`
      rows (each pre-assigned a UUID `pinecone_vector_id`) → `FAILED` on extraction error or zero
      extractable text (e.g. scanned PDFs)
- [x] `DocumentController` — `POST /api/documents/upload` (multipart), `GET /api/documents`,
      `GET /api/documents/{id}`, `GET /api/documents/{id}/chunks`
- [x] DTOs: `DocumentUploadResponse`, `DocumentSummaryResponse`, `ChunkResponse`
- [x] `application.yml` — multipart upload limits (20MB max file/request size)
- [x] All new code compiles cleanly (`./mvnw compile`)
- [x] Committed and pushed to `origin/main`
- [ ] Manual end-to-end test: upload a real PDF, confirm chunks appear correctly via
      `GET /api/documents/{id}/chunks` — still not confirmed by an actual run; Day 3 built on
      top of this anyway (chunking logic didn't change, low risk)

### Phase 1 — Day 1 Connection Verification (completed 2026-07-23, runtime confirmed 2026-08-02)
- [x] Created GeminiEmbeddingService (calls Gemini embedding API via WebClient)
- [x] Created PineconeService (queries Pinecone via REST API)
- [x] Created health check methods on both services
- [x] Created ConnectionVerificationService (runs on app startup, logs all three connection statuses)
- [x] Created HealthController (`GET /api/health`) for manual connection verification via HTTP
- [x] All new code compiles cleanly
- [x] Committed and pushed to `origin/main`
- [x] Runtime finally confirmed during Day 5's session — took 4 bug fixes (`DB_URL` format,
      duplicate `WebClient` beans, Pinecone auth header, `PineconeQueryResponse.usage` type) before
      `GET /api/health` actually returned all-green. See Decisions Log, 2026-08-02.

### Phase 0 — Scaffolding (completed 2026-07-23)
- [x] Explored environment, confirmed toolchain (Java 25, no Maven binary, Node 22, git configured)
- [x] Confirmed GitHub SSH auth works, remote repo exists and is empty
- [x] Researched MCP Java SDK and current Gemini model names (see Decisions Log)
- [x] Resolved master doc's open items with user (frontend choice, credential status)
- [x] `git init`, `.gitignore`, remote `origin` set
- [x] Created this file and `guide.md`
- [x] Scaffold `docmind-backend/` via Spring Initializr (Spring Boot 4.1.0, Java 17, deps:
      actuator, web, webflux, data-jpa, postgresql, security, validation, flyway, lombok,
      configuration-processor)
- [x] First commit (blueprint + tracking docs) pushed to local `main` — not yet pushed to
      `origin` (ask before first push, per risky-action norms — will do once Day 1 verified working)
- [x] `application.yml` written (env-var driven: DB, Gemini, Pinecone, JWT config namespaces)
- [x] `.env.example` + local `.env` created in `docmind-backend/` (gitignored); JWT_SECRET
      auto-generated, Gemini/Pinecone/Neon values still blank pending user input
- [x] `V1__init_schema.sql` Flyway migration written (users, documents, chunks,
      conversation_sessions, messages)
- [x] JPA entities: `User`, `Document`, `Chunk`, `ConversationSession`, `Message` (+ `Role`,
      `DocumentStatus`, `MessageRole` enums)
- [x] Repositories: `UserRepository`, `DocumentRepository`, `ChunkRepository`,
      `ConversationSessionRepository`, `MessageRepository`
- [x] Config: `GeminiProperties`, `PineconeProperties`, `JwtProperties` (record-based
      `@ConfigurationProperties`), `WebClientConfig` (two `WebClient` beans), `SecurityConfig`
      (temporary permit-all, see Decisions Log)
- [ ] **BLOCKED**: verify `./mvnw compile` succeeds — needs `openjdk-25-jdk` installed by user
      (JRE-only machine, no `javac` currently). Nothing above has been compile-checked yet.
- [ ] Once compiling: fill real credentials into `.env`, run `./mvnw spring-boot:run`, confirm
      app boots and hits Postgres (Flyway migration applies cleanly)
- [ ] Scaffold `docmind-frontend/` via Vite (later — Day 8 per timeline, not blocking backend work)
- [ ] Commit backend scaffold once verified; push to `origin/main` (ask user first)

*(Next entries append below as phases complete — keep each phase's entry, don't overwrite.)*
