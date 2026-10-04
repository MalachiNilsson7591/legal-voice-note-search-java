package dev.example.legalvoice.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class InfraiVectorClient {
    private final HttpClient http;
    private final URI baseUrl;
    private final String apiKey;
    private final ObjectMapper mapper;

    public InfraiVectorClient(HttpClient http, URI baseUrl, String apiKey, ObjectMapper mapper) {
        this.http = http; this.baseUrl = baseUrl; this.apiKey = apiKey; this.mapper = mapper;
    }

    public void createCollection(String collection, int dimension) {
        post("/vector/collection/create", Map.of("collection", collection, "dimension", dimension,
                "metric", "cosine", "metadata", Map.of("domain", "legal-voice")), "collection:" + collection);
    }

    public void upsert(String collection, List<Map<String, Object>> vectors) {
        String writeKey = "upsert:" + collection + ":" + vectors.stream().map(v -> String.valueOf(v.get("id"))).sorted().toList();
        post("/vector/upsert", Map.of("collection", collection, "vectors", vectors), writeKey);
    }

    public List<Map<String, Object>> query(String collection, List<Double> embedding, int topK) {
        Map<String, Object> data = post("/vector/query", Map.of("collection", collection,
                "embedding", embedding, "top_k", topK, "filter", Map.of(), "include_metadata", true));
        return castList(data.get("items"));
    }

    public List<Map<String, Object>> rerank(String query, List<String> candidates, int topK) {
        Map<String, Object> data = post("/ai/rerank", Map.of("query", query, "candidates", candidates,
                "top_k", topK, "model", "auto", "vendor", "auto"));
        return castList(data.get("items"));
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        return post(path, body, null);
    }

    private Map<String, Object> post(String path, Map<String, Object> body, String idempotencyKey) {
        int attempt = 0;
        while (true) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(baseUrl.resolve(baseUrl.getPath() + path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json");
            if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey);
            RawEnvelope raw;
            try {
                HttpResponse<String> response = http.send(builder.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),
                        HttpResponse.BodyHandlers.ofString());
                Envelope envelope = mapper.readValue(response.body(), new TypeReference<>() {});
                raw = new RawEnvelope(response.statusCode(), response.headers().firstValue("Retry-After").orElse(null), envelope);
            } catch (IOException e) {
                throw new IllegalStateException("Could not exchange the Infrai request", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Request interrupted", e);
            }
            Envelope env = raw.envelope();
            if (raw.status() == 429 && attempt++ < 3) {
                sleep(retryDelay(raw.retryAfter(), attempt));
                continue;
            }
            if (env != null && !env.ok()) {
                ErrorBody error = env.error();
                throw new InfraiException(error == null ? "REQUEST_REJECTED" : error.code(),
                        error == null ? "Request rejected" : error.message(), raw.status());
            }
            if (raw.status() >= 500) throw new IllegalStateException("Upstream transport response " + raw.status());
            if (env == null) throw new IllegalStateException("Infrai response did not contain an envelope");
            return env.data() == null ? Map.of() : env.data();
        }
    }

    private static Duration retryDelay(String header, int attempt) {
        if (header != null && header.matches("\\d+")) return Duration.ofSeconds(Long.parseLong(header));
        return Duration.ofMillis(250L * (1L << attempt));
    }

    private static void sleep(Duration duration) {
        try { Thread.sleep(duration.toMillis()); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Retry interrupted", e); }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope(boolean ok, Map<String, Object> data, ErrorBody error, Map<String, Object> metadata) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorBody(String code, String message) {}
    private record RawEnvelope(int status, String retryAfter, Envelope envelope) {}
}
