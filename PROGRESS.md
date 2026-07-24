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

**Phase 1 — Day 1 Connection Verification (in progress).** Backend compiles cleanly.
Added Gemini embedding service, Pinecone retrieval service, and connection verification logic.
Services now available as Spring beans. Ready to test actual API connectivity:
- Health endpoint: `GET /api/health` (shows connection status for all three services)
- Startup verification: automatic health checks logged on application boot
- Requires `.env` to be populated with Gemini, Pinecone, and Neon credentials (already collected from user)

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

---

## Environment / Credentials Status

Tracked here so we don't re-ask the user for things already provided. **Never write actual
secret values into this file or any committed file** — only track *whether* a given credential
has been provided and is sitting in the local (gitignored) `.env`.

| Credential | Status | Notes |
|---|---|---|
| Gemini API key | User confirmed they have it | Not yet collected into `.env` |
| Pinecone API key + index host | User confirmed they have it | Not yet collected into `.env`; index must be created with dimension matching `gemini-embedding-001` output (768, per our config choice) |
| Neon Postgres connection string | User confirmed they have it | Not yet collected into `.env` |
| JWT secret | Not yet generated | Will be generated locally (random 256-bit value), not requested from user |
| GitHub push access | Verified working | SSH key already authenticated as `vigneshpraveen-official`, confirmed via `ssh -T git@github.com`. Remote `origin` set to `git@github.com:vigneshpraveen-official/DocMind.git`. Repo is currently empty on GitHub (no refs). |

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

### Phase 1 — Day 1 Connection Verification (in progress, started 2026-07-23)
- [x] Created GeminiEmbeddingService (calls Gemini embedding API via WebClient)
- [x] Created PineconeService (queries Pinecone via REST API)
- [x] Created health check methods on both services
- [x] Created ConnectionVerificationService (runs on app startup, logs all three connection statuses)
- [x] Created HealthController (`GET /api/health`) for manual connection verification via HTTP
- [x] All new code compiles cleanly
- [ ] Run the app and test actual connectivity (awaiting user to start with `./mvnw spring-boot:run`)
- [ ] Verify logs show all three services healthy
- [ ] (Optional) Test `GET /api/health` endpoint manually via curl/Postman

*(Next: Days 2-3 ingestion pipeline once connectivity confirmed.)*

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
