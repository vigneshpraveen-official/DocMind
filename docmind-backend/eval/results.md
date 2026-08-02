# Day 5 Eval Results — Naive vs. Grounded Prompt

## Methodology

- **Test document:** `eval/sample-hr-policy.pdf` — a fabricated employee handbook for a fictional
  company ("Northwind Analytics"), written specifically for this eval so the correct answers are
  known in advance and unambiguous. Uploaded and indexed through the real pipeline (Days 2-3).
- **Question set:** `eval/questions.json` — 20 questions: 17 answerable directly from the
  document, 3 deliberately not covered by it at all (to test whether the model correctly declines
  instead of guessing).
- **Same retrieval, two prompts.** For each question, the top-5 matching chunks are retrieved
  from Pinecone *once* and reused for both prompt variants — this isolates the effect of the
  prompt instruction itself rather than measuring retrieval variance.
  - **Naive prompt:** raw context + question, no instruction telling the model to stick to the
    context or admit when it doesn't know (`PromptBuilder.buildNaivePrompt`).
  - **Grounded prompt:** the master doc's system instruction — answer only from context, say "I
    don't have information on that" if it's not there, cite the section (`PromptBuilder.buildGroundedPrompt`).
- **Generation:** `gemini-3.5-flash-lite`, temperature 0.2, via the real `GeminiGenerationService`.
  (Started the run on `gemini-3.5-flash`; switched to the lite variant mid-eval after hitting its
  free-tier daily quota — see `PROGRESS.md` Decisions Log, 2026-08-02.)
- **Raw answers:** `eval/results-raw.json` — both prompts' answers for all 20 questions, generated
  live against the real Gemini/Pinecone APIs. Not hand-written, not simulated.
- **Scoring rubric** (applied per answer, by inspection against `expectedAnswer` and the source
  document):
  - **Correct** — matches the document's actual content (for answerable questions).
  - **Hallucinated** — states something as fact that is not in the document (fabricated specifics,
    wrong numbers, invented policy).
  - **Correctly declined** — for unanswerable questions, the model says it doesn't know instead
    of guessing.
  - **Cited** — the answer names a specific section and/or page number backing its claim, i.e. a
    claim a reader could actually go verify — not just "based on the handbook."

## Scoring Table

| # | Question | Answerable | Naive: correct? | Naive: cited? | Grounded: correct? | Grounded: cited? |
|---|---|---|---|---|---|---|
| 1 | Leave policy for interns | Yes | ✅ | ❌ | ✅ | ✅ |
| 2 | Full-time PTO days/year | Yes | ✅ | ❌ | ✅ | ✅ |
| 3 | Paid sick days/year | Yes | ✅ | ❌ | ✅ | ✅ |
| 4 | Interns eligible for remote work? | Yes | ✅ | ✅ | ✅ | ✅ |
| 5 | Home office stipend amount | Yes | ✅ | ❌ | ✅ | ✅ |
| 6 | Remote days/week + eligibility | Yes | ✅ | ❌ | ✅ | ✅ |
| 7 | Core working hours | Yes | ✅ | ❌ | ✅ | ✅ |
| 8 | Expense report submission window | Yes | ✅ | ✅ | ✅ | ✅ |
| 9 | Daily meal reimbursement cap | Yes | ✅ | ✅ | ✅ | ✅ |
| 10 | Alcohol reimbursable? | Yes | ✅ | ✅ | ✅ | ✅ |
| 11 | Hotel cost cap | Yes | ✅ | ❌ | ✅ | ✅ |
| 12 | Confidentiality agreement duration | Yes | ✅ | ✅ | ✅ | ✅ |
| 13 | Full-time review cycle | Yes | ✅ | ✅ | ✅ | ✅ |
| 14 | Intern review process | Yes | ✅ | ✅ | ✅ | ✅ |
| 15 | Password requirements | Yes | ✅ | ✅ | ✅ | ✅ |
| 16 | US paid holidays count | Yes | ✅ | ❌ | ✅ | ✅ |
| 17 | Parental leave at 8 months tenure | Yes | ✅ | ✅ | ✅ | ✅ |
| 18 | Company annual revenue | **No** | ✅ declined | n/a | ✅ declined | n/a |
| 19 | Head of HR's name | **No** | ✅ declined | n/a | ✅ declined | n/a |
| 20 | Parental leave for same-sex couples | **No** | ✅ declined* | n/a | ✅ declined | n/a |

\* Naive answer to Q20 correctly states the document has no same-sex-specific policy, but then
pads the response with the general parental leave policy anyway — technically doesn't fabricate
anything, but is a less clean refusal than the grounded prompt's direct one-line decline.

**Full-precision counts:**
- Hallucinations (wrong or fabricated facts): **0/17 naive, 0/17 grounded**
- Unanswerable questions wrongly answered instead of declined: **0/3 naive, 0/3 grounded**
- Answerable questions with a specific, checkable citation: **9/17 (52.9%) naive, 17/17 (100%) grounded**

## Result

**The honest finding is more specific than a single "% fewer hallucinations" number, so here it
is stated precisely rather than dressed up:**

On this eval, `gemini-3.5-flash-lite` did not fabricate a single fact in either condition — not
on the 17 answerable questions, and not on the 3 designed to be unanswerable. That's a genuine,
positive result: it means the retrieval pipeline (Days 2-4: chunking → embedding → Pinecone
top-5) is doing its job well enough that the model had solid grounding material regardless of
prompt wording, and a modern instruction-tuned model doesn't need to be told twice not to
confabulate. Reporting an invented "hallucination reduced by X%" here would be dishonest — there
was no hallucination to reduce in this sample.

What the grounding instruction *did* measurably change: **source citation went from 52.9% (naive)
to 100% (grounded)** — every grounded answer names the specific section and page backing its
claim; naive answers cite a specific section only about half the time, and several just say
"based on the employee handbook" with nothing a reader could actually go check. This is a
**+47 percentage point improvement**, and it's the property the master doc calls out as the whole
point of returning `sources` in the API response: not "does the LLM sound confident," but "can a
human verify this against the source in ten seconds." That's the real, defensible, resume-usable
number from this project — **grounded prompting raised verifiable source citation from 53% to
100%** on a 20-question eval against a real indexed document.

Caveat worth stating plainly: this used one document, one (strong, current) model, and 20
questions — a bigger document set with messier/more ambiguous content, or an older/weaker model,
would likely show a hallucination-rate gap too. This result says the *retrieval* half of the
pipeline is pulling its weight, and the *prompt* half's clearest measured win here is citation
discipline, not error correction. Both are legitimate, real findings from live data — not
fabricated to fit a narrative.
