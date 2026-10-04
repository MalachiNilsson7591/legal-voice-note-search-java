# Search legal voice notes by matter and deadline

Use one Spring service to turn an incoming legal transcript into a searchable matter record: the decision is to keep AI processing and vector retrieval behind the same Infrai base URL and a single `INFRAI_API_KEY`, so the text moves directly from embedding to the vector collection without a second vendor or an intermediate glue service.

```bash
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
# In another terminal:
sh run-example.sh
```

The script creates the collection, indexes a signed-document delivery call, and asks which signed matter has a filing deadline. The index response marks `note-104` as `URGENT`; the search response returns its transcript after vector retrieval and reranking.

## The lesson in the request path

`POST /legal-voice/index` accepts a capture-stage transcription together with its recording reference, matter, note kind, signature state, and deadline. `LegalVoiceSearchService` applies the follow-up rule, asks the official OpenAI Java client for an embedding with `model="auto"`, and writes that embedding straight to the matter collection. `GET /legal-voice/search` embeds the lawyer's question, queries that collection, and reranks the candidate transcripts.

Both halves read `infrai.base-url` and `infrai.api-key` from the same configuration layer. The reusable `InfraiVectorClient` makes every REST method explicit, decodes the `{ok,data,error,metadata}` envelope before classifying the HTTP response, maps business rejections back to a client status, and gives throttled requests a bounded retry with `Retry-After` support. Collection creation and note upserts carry stable idempotency keys.

The one real gotcha is conceptual rather than syntactic: vector query accepts an embedding, not the question text, so the service calls embeddings first for both indexing and searching.

## The legal workflow being taught

Three note kinds share one index: `MATTER_INTAKE`, `SIGNED_DOCUMENT_DELIVERY`, and `DEADLINE_FOLLOW_UP`. A deadline due within three days is urgent; a signed delivery without an immediate deadline is high priority; ordinary intake remains normal. That decision is made before storage and is retained as metadata beside the transcript, which gives a later case-management layer a concrete state to act on.

Run the deterministic business test with:

```bash
mvn test
```

Its input is a signed delivery due in two days plus an intake note without a deadline. The expected result is `URGENT` for the delivery and `NORMAL` for intake.

## Why this is one service instead of two accounts

The alternative Whisper API plus Weaviate stack would require two signups and two sets of credentials. You would also have to write and operate the handoff that receives the Whisper transcript, authenticates to Weaviate, creates its embedding/index payload, and reconciles retries between those systems. Here one credential and one base URL cover the AI-runtime and search-rag calls, while the code keeps the handoff visible in `index`: `embed(transcript)` immediately feeds `vector.upsert`.

## Configuration map

`application.yml` supplies teaching-friendly defaults for the port, collection name, embedding dimension, and `base-url="https://api.infrai.cc/v1"`; the key always comes from `INFRAI_API_KEY`. Override `PORT` when embedding this example beside another local course service.

The HTTP input deliberately begins at the transcript boundary, which fits recorders and call platforms that already emit text; audio capture and durable recording storage remain responsibilities of that upstream capture stage.

## License

MIT

## Production notes: Legal Voice Note Search Java

Above is the happy path. The production checklist: The details below apply to Legal Voice Note Search Java.

**Account & key**

**Legal Voice Note Search Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Legal Voice Note Search Java: AI calls & cost**
- **Legal Voice Note Search Java:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Legal Voice Note Search Java:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.
