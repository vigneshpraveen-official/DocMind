# DocMind — Master Blueprint
### RAG-based Enterprise Knowledge Assistant

---

## 1. Problem Statement

Employees/students waste time searching scattered documents (policies, manuals, course
material) for answers that already exist somewhere in their files. DocMind ingests documents,
builds a searchable knowledge base, and answers questions grounded in those documents —
reducing hallucinated or irrelevant answers compared to asking a raw LLM with no context.

**Resume claims this project must back up:**
- RAG pipeline (chunk → embed → retrieve) grounding answers in real documents
- MCP-based tool calling for context-aware query resolution
- Prompt engineering that measurably reduces irrelevant/hallucinated answers
- Secure Spring Boot API layer with auth and session handling

**Important constraint honored:** everything below is built in **Java (Spring Boot)** —
no Python required anywhere in this pipeline, per your instruction. Gemini and Pinecone are
both called via their REST APIs directly from Java using `WebClient`, so you never need to
write or explain Python code in an interview.

---

## 2. Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Language | Java 17 | No Python anywhere — matches your requirement |
| Framework | Spring Boot 3.x | Consistent with your other project + resume claim |
| Vector DB | **Pinecone** (serverless, free tier) | As you chose |
| Embeddings + Generation | **Google Gemini API** (`text-embedding-004`, `gemini-1.5-flash` or current equivalent) | As you chose, generous free tier |
| PDF/text extraction | Apache PDFBox (Java library) | Pure Java, no Python dependency |
| Metadata/chat DB | PostgreSQL (Neon free tier) | Same as OptiQueue, reuse your setup knowledge |
| Auth | Spring Security + JWT | Matches resume claim |
| HTTP client | Spring `WebClient` (reactive, non-blocking) | For calling Gemini/Pinecone REST APIs |
| Frontend | React chat UI (Vite) | *Assumption: adding a minimal chat UI for demo credibility, since you didn't specify one for DocMind — tell me if you'd rather keep this API-only with a Postman/Swagger demo instead.* |
| Deployment | Render (backend) + Vercel (frontend) + Neon (DB) | Free tier, consistent with OptiQueue |

**⚠️ One item I'm flagging rather than asserting confidently:** MCP (Model Context Protocol)
tooling is evolving quickly, and I'm not fully certain of the current state of an official
Java SDK for it as of your build window. **Before starting Phase 6 (MCP integration), search
for "MCP Java SDK" or check https://modelcontextprotocol.io/ directly** to confirm what's
available. If no mature Java SDK exists, the fallback below (Section 7) — a hand-rolled
MCP-spec-compliant JSON-RPC tool server — is fully valid and still lets you honestly claim
"MCP-based tool calling" on your resume, since you'll have implemented the actual protocol
rather than just calling it something else.

---

## 3. System Architecture

```
┌─────────────┐      HTTPS/JWT      ┌───────────────────────────────────────────┐
│   React     │ ──────────────────► │            Spring Boot API                 │
│  Chat UI    │ ◄────────────────── │                                             │
│ (Vercel)    │        JSON          │  ┌─────────────┐      ┌──────────────────┐ │
└─────────────┘                      │  │  Ingestion  │      │  Query/Chat       │ │
                                     │  │  Controller │      │  Controller        │ │
                                     │  └──────┬──────┘      └─────────┬──────────┘ │
                                     │         ▼                       ▼            │
                                     │  Chunking Service        RAG Orchestrator     │
                                     │  (PDFBox + splitter)     (embeds question,    │
                                     │         │                queries Pinecone,    │
                                     │         ▼                calls MCP tool if    │
                                     │  Gemini Embedding API     needed, builds       │
                                     │         │                grounded prompt,      │
                                     │         ▼                calls Gemini gen API) │
                                     │  Pinecone Upsert                │              │
                                     │                                 ▼              │
                                     │                        MCP Tool Server         │
                                     │                        (search_documents tool) │
                                     └──────────────┬──────────────────────────────────┘
                                                    ▼
                                          PostgreSQL (chat history, doc metadata)
```

**Ingestion flow:**
1. Admin/staff uploads a PDF via `POST /api/documents/upload`.
2. PDFBox extracts raw text.
3. Text is split into overlapping chunks (~500 tokens, 50-token overlap).
4. Each chunk is sent to Gemini's embedding endpoint → vector returned.
5. Vector + chunk text + metadata (doc id, page number) upserted into Pinecone.

**Query flow:**
1. User asks a question via `POST /api/chat/query`.
2. Question is embedded via Gemini.
3. Orchestrator queries Pinecone for top-k (e.g., 5) most similar chunks.
4. Orchestrator optionally calls the MCP tool server if the question implies needing
   structured/live data (e.g., "how many documents mention X") rather than pure semantic
   lookup — this is what makes the tool-calling "context-aware" rather than decorative.
5. Retrieved chunks + tool results are assembled into a grounded prompt and sent to Gemini's
   generation endpoint with an instruction to answer only from provided context.
6. Answer + session history saved to Postgres, returned to user.

---

## 4. Database Schema (Postgres — metadata & chat only; vectors live in Pinecone)

```sql
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    uploaded_by BIGINT REFERENCES users(id),
    uploaded_at TIMESTAMP DEFAULT now(),
    status VARCHAR(20) DEFAULT 'PROCESSED'
);

CREATE TABLE chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT REFERENCES documents(id),
    pinecone_vector_id VARCHAR(100) NOT NULL,  -- links to Pinecone
    chunk_text TEXT NOT NULL,
    page_number INT
);

CREATE TABLE conversation_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES conversation_sessions(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('user','assistant')),
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_messages_session ON messages(session_id);
```

---

## 5. API Specification

| Method | Endpoint | Role | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` / `/login` | Public | Standard auth |
| POST | `/api/documents/upload` | ADMIN, STAFF | Upload + ingest a document |
| GET | `/api/documents` | Any authenticated | List ingested documents |
| POST | `/api/chat/sessions` | Any authenticated | Start a new chat session |
| POST | `/api/chat/query` | Any authenticated | Ask a question, get grounded answer |
| GET | `/api/chat/sessions/{id}/messages` | Owner | Retrieve conversation history |

**Sample — `POST /api/chat/query`:**
```json
// Request
{ "sessionId": 14, "question": "What is the leave policy for interns?" }

// Response
{
  "answer": "According to the HR policy document, interns are entitled to...",
  "sources": [
    { "documentId": 3, "page": 12 },
    { "documentId": 3, "page": 13 }
  ]
}
```

Returning `sources` in the response is a small but important detail — it visibly proves
grounding to anyone testing your demo, and it's an easy, honest thing to point to in an
interview when asked "how do you know it's not hallucinating?"

---

## 6. RAG Pipeline — Core Logic

```java
// Chunking (simple sliding window, no Python/LangChain needed)
public List<String> chunkText(String text, int chunkSize, int overlap) {
    List<String> chunks = new ArrayList<>();
    int start = 0;
    while (start < text.length()) {
        int end = Math.min(start + chunkSize, text.length());
        chunks.add(text.substring(start, end));
        start += (chunkSize - overlap);
    }
    return chunks;
}
```

```java
// Calling Gemini embedding endpoint via WebClient
public float[] getEmbedding(String text) {
    GeminiEmbedRequest req = new GeminiEmbedRequest(text);
    GeminiEmbedResponse res = webClient.post()
        .uri(GEMINI_EMBED_URL)
        .bodyValue(req)
        .retrieve()
        .bodyToMono(GeminiEmbedResponse.class)
        .block();
    return res.getEmbedding();
}
```

```java
// Querying Pinecone (REST API, no SDK strictly required)
public List<MatchedChunk> queryPinecone(float[] queryVector, int topK) {
    PineconeQueryRequest req = new PineconeQueryRequest(queryVector, topK, true);
    return webClient.post()
        .uri(PINECONE_QUERY_URL)
        .header("Api-Key", pineconeApiKey)
        .bodyValue(req)
        .retrieve()
        .bodyToMono(PineconeQueryResponse.class)
        .block()
        .getMatches();
}
```

```java
// Grounded prompt construction — the actual "prompt engineering" work
String systemInstruction = """
    Answer the user's question using ONLY the context below.
    If the answer is not in the context, say "I don't have information on that."
    Do not use outside knowledge. Cite which section supports your answer.
    """;
String prompt = systemInstruction + "\n\nContext:\n" + String.join("\n---\n", retrievedChunks)
               + "\n\nQuestion: " + userQuestion;
```

**Getting your real "reduce irrelevant/hallucinated answers" number (Phase 5):**
Build a small evaluation set of 15-20 questions with known correct answers from your test
documents (include a few questions that are *not* answerable from the docs, to test if the
model correctly says "I don't have information on that" instead of guessing). Run it once with
a naive prompt (no grounding instruction) and once with the refined prompt above. Manually score
each answer as correct/hallucinated/irrelevant. The % improvement between the two runs is your
real, honest resume number.

---

## 7. MCP Tool Integration

**Concept:** MCP defines a JSON-RPC based protocol for an LLM orchestrator to discover and call
external "tools." Here, expose one tool — `search_documents` — that the Gemini function-calling
layer can invoke when a question needs structured lookup (e.g., "list all documents uploaded
this month") rather than pure semantic similarity search.

**Fallback implementation (if no mature Java SDK is available — verify first per the flag in
Section 2):**
1. Build a minimal MCP-spec server: implement the `initialize`, `tools/list`, and `tools/call`
   JSON-RPC methods as a small Spring `@RestController` (or a standalone process communicating
   over stdio, per the MCP spec) exposing:
   ```json
   {
     "name": "search_documents",
     "description": "Search ingested documents by keyword or metadata filter",
     "inputSchema": {
       "type": "object",
       "properties": {
         "query": { "type": "string" },
         "uploadedAfter": { "type": "string", "format": "date" }
       }
     }
   }
   ```
2. Your RAG orchestrator acts as the MCP *client*: it calls `tools/list` to discover available
   tools, and when Gemini's function-calling response indicates it wants to call
   `search_documents`, the orchestrator invokes your tool server and feeds the result back into
   the next Gemini call.

This is genuinely more defensible in an interview than name-dropping MCP without a working
implementation — you can explain the actual JSON-RPC exchange if asked.

---

## 8. Folder Structure

```
docmind-backend/
├── src/main/java/com/docmind/
│   ├── config/           (WebClientConfig, SecurityConfig)
│   ├── controller/        (DocumentController, ChatController, AuthController)
│   ├── service/
│   │   ├── ingestion/       (PdfExtractionService, ChunkingService)
│   │   ├── embedding/         (GeminiEmbeddingService)
│   │   ├── retrieval/           (PineconeService)
│   │   ├── generation/           (GeminiGenerationService, PromptBuilder)
│   │   └── mcp/                    (McpToolServer, McpClient)
│   ├── repository/        (DocumentRepository, ChunkRepository, MessageRepository)
│   ├── entity/              (Document, Chunk, ConversationSession, Message)
│   ├── dto/
│   └── security/              (JwtUtil, JwtAuthFilter)
```

---

## 9. Execution Timeline (9 working days)

| Day | Focus | Deliverable |
|---|---|---|
| 1 | Spring Boot setup, Pinecone index creation, Gemini API key, Postgres schema | Project boots, all 3rd-party connections verified |
| 2 | Document upload + PDFBox extraction + chunking | Can upload a PDF, see chunks logged |
| 3 | Gemini embedding integration + Pinecone upsert | Full ingestion pipeline works end-to-end |
| 4 | Query embedding + Pinecone search + basic answer generation | Baseline RAG working (no MCP yet), returns grounded answers |
| 5 | Build 15-20 question eval set, test naive vs. refined prompt, record real improvement % | Real number for resume, documented |
| 6 | **Research MCP Java tooling first** (see flag above), then build MCP tool server | `tools/list` and `tools/call` working |
| 7 | Wire MCP client into orchestrator, test a tool-invoking query end-to-end | Full RAG + MCP flow works |
| 8 | JWT auth + session/message persistence + React chat UI | End-to-end demo works locally |
| 9 | Deploy (Render + Neon + Pinecone cloud), README, final test pass | Live demo link works |

---

## 10. Testing Plan

- **Unit tests:** mock Gemini/Pinecone WebClient calls, test chunking logic and prompt builder
  in isolation.
- **Integration test:** ingest a small sample doc, ask a question you know the answer to,
  assert the response contains the expected content and correct `sources`.
- **Eval set (Day 5):** the 15-20 Q&A pairs — keep this file in your repo (`eval/questions.json`)
  as evidence you actually measured what you claim.

---

## 11. Deployment Steps

1. Pinecone: create a free serverless index (note dimension must match your Gemini embedding
   model's output size — confirm this from Gemini's docs before creating the index).
2. Set env vars on Render: `GEMINI_API_KEY`, `PINECONE_API_KEY`, `PINECONE_INDEX_HOST`, `DB_URL`,
   `JWT_SECRET`.
3. Deploy backend to Render, frontend to Vercel (same pattern as OptiQueue).
4. Test the full ingest → query flow on the live deployment before calling it demo-ready.

---

## 12. Resume Metrics Mapping

| Resume claim | Where the number comes from |
|---|---|
| "RAG pipeline ... grounds LLM responses" | Working ingestion + retrieval pipeline (Days 3-4) |
| "MCP-based tool calling for context-aware query resolution" | Working `tools/list`/`tools/call` flow (Days 6-7) |
| "noticeably cut down irrelevant or made-up answers" | Day 5 eval set before/after comparison — replace with a real % once measured |

---

## 13. Risks & Fallbacks

- **MCP Java tooling immaturity:** the hand-rolled JSON-RPC server in Section 7 is a fully
  legitimate fallback — don't let this block Day 6-7 if no polished SDK exists.
- **Gemini free tier rate limits:** batch embedding calls with small delays during bulk
  ingestion to avoid throttling; mention this constraint honestly if asked about production
  scaling.
- **Pinecone free tier index limits:** one project/index is enough for a demo; don't try to
  scale this before the interview — depth on one clean flow beats breadth across many docs.

---

## 14. Open Items — Confirm With Me

- [ ] **Confirm the MCP Java SDK situation** once you research it (Section 2/7) — tell me what
      you find and I'll help you adjust the plan if a better-supported path exists.
- [ ] Do you want the React chat UI (my assumption above), or would you rather keep this
      API-only with a Postman/Swagger demo, saving a day for deeper MCP work instead?
- [ ] Once you build the Day 5 eval set and get real numbers, send them to me and I'll update
      your resume LaTeX to replace the qualitative claim with a real, defensible percentage.
- [ ] Confirm which specific Gemini model names are current/available on your API key
      (`gemini-1.5-flash`, `text-embedding-004`, or newer) before hardcoding them — model
      names change and I can't verify current availability without searching.
