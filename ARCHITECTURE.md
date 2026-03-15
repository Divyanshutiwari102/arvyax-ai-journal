# ARCHITECTURE.md — ArvyaX Journal

## System Overview

```
User Browser
     │
     ▼
[React Frontend] ──HTTP──▶ [Spring Boot API] ──JPA──▶ [SQLite DB]
                                   │
                                   └──HTTPS──▶ [Anthropic Claude API]
```

---

## 1. How would you scale this to 100,000 users?

**Current bottlenecks** at scale:
- SQLite is single-writer; it cannot handle concurrent writes
- In-memory cache is local to one JVM instance
- The single Spring Boot process is a SPOF

**Migration plan:**

| Concern        | Solution                                                                                     |
|----------------|----------------------------------------------------------------------------------------------|
| Database        | Replace SQLite with **PostgreSQL** (e.g. AWS RDS / Supabase). Add read replicas for GET-heavy traffic. |
| Caching         | Move in-memory cache to **Redis** (shared across instances). Cache both analysis results and insight aggregations. |
| App servers     | Deploy multiple Spring Boot instances behind a **load balancer** (AWS ALB / nginx). Use stateless JWT auth. |
| Async LLM calls | Move LLM calls to an async job queue (**RabbitMQ / SQS**). Return `202 Accepted` immediately and push results via WebSocket or polling. |
| Auth            | Add **JWT / OAuth2** so `userId` is verified, not just user-supplied.                        |
| CDN             | Serve the React SPA via **CloudFront / Vercel** to reduce backend load.                      |

Rough target capacity with this design: ~50k RPS on insight queries, ~2k LLM analyses/min.

---

## 2. How would you reduce LLM cost?

| Strategy                  | Detail                                                                                              |
|---------------------------|-----------------------------------------------------------------------------------------------------|
| **Caching (done)**        | MD5 hash of normalized text → Redis cache. Identical/near-identical entries never hit the API twice. |
| **Smaller model**         | Use `claude-haiku` for routine analyses; escalate to `claude-sonnet` only for complex cases.        |
| **Prompt compression**    | Strip stop words and punctuation before sending. Shorter prompts = fewer input tokens.               |
| **Batch processing**      | Accumulate unanalyzed entries and send in batch during off-peak hours using Anthropic batch API.     |
| **Semantic dedup**        | Use embeddings to cluster similar entries; share one analysis across the cluster.                   |
| **User-triggered only**   | Don't auto-analyze every entry — let users click "Analyze" to avoid analyzing abandoned drafts.     |

At 100k users writing ~2 entries/week, with 70% cache hit rate and Haiku pricing, estimated cost: **~$40/month**.

---

## 3. How would you cache repeated analysis?

**Current implementation:**
- In-memory `ConcurrentHashMap<String, EmotionAnalysis>` keyed by `MD5(text.trim().toLowerCase())`
- Zero-latency cache hit for identical text
- Cache lives for the JVM lifetime (lost on restart)

**Production upgrade:**

```
Request ─▶ Check Redis (TTL 7 days) ─▶ Hit: return instantly
                │
              Miss
                │
                ▼
         Call Anthropic API
                │
                ▼
        Store in Redis + DB
                │
                ▼
          Return result
```

Redis key design: `analysis:{md5_of_normalized_text}` with 7-day TTL.

For near-duplicate detection, generate a sentence embedding and check cosine similarity > 0.95 before calling the API.

---

## 4. How would you protect sensitive journal data?

Journal entries are deeply personal health data. Layered protection:

**At rest:**
- Encrypt the SQLite/PostgreSQL database at the OS/cloud level (AES-256, e.g. AWS RDS encryption)
- Optionally encrypt the `text` column at the application layer (AES-GCM with per-user derived keys) — this prevents even DB admins from reading entries

**In transit:**
- HTTPS/TLS 1.3 enforced on all endpoints
- HSTS headers on the frontend

**Access control:**
- JWT authentication: every API request is verified; users can only access their own `userId`
- No admin "read all" endpoint; any bulk access requires the user's key

**LLM privacy:**
- Before sending to Anthropic, strip PII (names, locations) using a regex/NER pre-processor
- Do not log raw journal text in application logs
- Anthropic API data handling policy: https://www.anthropic.com/legal/privacy

**Compliance:**
- GDPR: expose `DELETE /api/journal/:userId` to support right-to-erasure
- Audit log: record who accessed which entries, stored separately from entry data
- Encryption key rotation schedule (annual minimum)

---

## Data Model

```
journal_entries
┌───────────────┬──────────────┬──────────────────────────────┐
│ Column        │ Type         │ Notes                        │
├───────────────┼──────────────┼──────────────────────────────┤
│ id            │ INTEGER PK   │ Auto-increment               │
│ user_id       │ VARCHAR      │ Indexed                      │
│ ambience      │ VARCHAR      │ forest/ocean/mountain/…      │
│ text          │ TEXT         │ Raw journal entry            │
│ emotion       │ VARCHAR      │ Populated by LLM             │
│ keywords      │ TEXT         │ JSON array string            │
│ summary       │ TEXT         │ One-sentence LLM summary     │
│ created_at    │ DATETIME     │ Auto-set on insert           │
└───────────────┴──────────────┴──────────────────────────────┘
```

---

## Request Flow

```
POST /api/journal/analyze
        │
        ▼
  RateLimiter.isAllowed()   ── 429 if exceeded
        │
        ▼
  LLMService.analyzeEmotion()
        │
        ├─▶ Check in-memory cache ─▶ HIT: return EmotionAnalysis
        │
        └─▶ MISS: build prompt → POST Anthropic API
                    │
                    ▼
              Parse JSON response
                    │
                    ▼
              Store in cache
                    │
                    ▼
              Return EmotionAnalysis
```
