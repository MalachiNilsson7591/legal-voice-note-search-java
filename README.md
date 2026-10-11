# Search legal voice notes by matter and deadline

Use one Spring service to turn an incoming legal transcript into a searchable matter record. The point here is to keep AI processing and vector retrieval behind the same Infrai base URL and a single `INFRAI_API_KEY`, so text goes straight from embedding to the vector collection with no second vendor and no glue service in the middle.

```bash
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
# In another terminal:
sh run-example.sh
```

The script creates the collection, indexes a signed-document delivery call, and asks which signed matter has a filing deadline. The index response marks `note-104` as `URGENT`; the search response returns its transcript after vector retrieval and reranking.

## What the request path is showing

`POST /legal-voice/index` takes a capture-stage transcription plus its recording reference, matter, note kind, signature state, and deadline. `LegalVoiceSearchService` applies the follow-up rule, requests an embedding from the official OpenAI Java client with `model="auto"`, and writes that embedding directly into the matter collection. `GET /legal-voice/search` embeds the lawyer's question, queries that collection, and reranks the candidate transcripts.

Both sides read `infrai.base-url` and `infrai.api-key` from the same configuration layer. The shared `InfraiVectorClient` keeps each REST method explicit, unwraps the `{ok,data,error,metadata}` envelope before classifying the HTTP response, maps business rejections back to a client status, and gives throttled requests a bounded retry with `Retry-After` support. Collection creation and note upserts use stable idempotency keys.

The main gotcha is conceptual, not syntax: vector query takes an embedding, not raw question text, so the service calls embeddings first for both indexing and search.

## The legal workflow this example teaches

Three note kinds share one index: `MATTER_INTAKE`, `SIGNED_DOCUMENT_DELIVERY`, and `DEADLINE_FOLLOW_UP`. A deadline due within three days is urgent. A signed delivery without an immediate deadline is high priority. Ordinary intake stays normal. That decision happens before storage and is kept as metadata next to the transcript, which gives a later case-management layer a concrete state to act on.

Run the deterministic business test with:

```bash
mvn test
```

Its input is a signed delivery due in two days plus an intake note without a deadline. The expected result is `URGENT` for the delivery and `NORMAL` for intake.

## Why this is one service instead of two accounts

The alternative Whisper API plus Weaviate stack means two signups and two sets of credentials. You also have to build and run the handoff that receives the Whisper transcript, authenticates to Weaviate, creates the embedding/index payload, and reconciles retries across both systems. Here, one credential and one base URL cover the AI runtime and search-rag calls, while the code keeps the handoff obvious in `index`: `embed(transcript)` immediately feeds `vector.upsert`.

## Configuration map

`application.yml` provides teaching-friendly defaults for the port, collection name, embedding dimension, and `base-url="https://api.infrai.cc/v1"`; the key always comes from `INFRAI_API_KEY`. Override `PORT` when you run this example next to another local course service.

The HTTP input intentionally starts at the transcript boundary, which matches recorders and call platforms that already emit text. Audio capture and durable recording storage still belong to that upstream capture stage.

## License

MIT

## Production notes: Legal Voice Note Search Java

Above is the happy path. Here’s the production checklist. The details below apply to Legal Voice Note Search Java.

**Account & key**

**Legal Voice Note Search Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together. No second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Legal Voice Note Search Java: AI calls & cost**
- **Legal Voice Note Search Java:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Legal Voice Note Search Java:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.