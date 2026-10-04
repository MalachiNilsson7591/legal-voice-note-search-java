package dev.example.legalvoice.service;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import dev.example.legalvoice.domain.FollowUpPolicy;
import dev.example.legalvoice.domain.MatterVoiceNote;
import dev.example.legalvoice.infra.InfraiVectorClient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LegalVoiceSearchService {
    private final OpenAIClient ai;
    private final InfraiVectorClient vectors;
    private final FollowUpPolicy policy;
    private final String collection;
    private final int dimension;

    public LegalVoiceSearchService(OpenAIClient ai, InfraiVectorClient vectors,
            @Value("${infrai.collection}") String collection,
            @Value("${infrai.embedding-dimension}") int dimension) {
        this(ai, vectors, new FollowUpPolicy(java.time.Clock.systemUTC()), collection, dimension);
    }

    LegalVoiceSearchService(OpenAIClient ai, InfraiVectorClient vectors, FollowUpPolicy policy,
                            String collection, int dimension) {
        this.ai = ai; this.vectors = vectors; this.policy = policy;
        this.collection = collection; this.dimension = dimension;
    }

    public void prepareIndex() { vectors.createCollection(collection, dimension); }

    public IndexedNote index(MatterVoiceNote note) {
        List<Double> embedding = embed(note.transcript());
        FollowUpPolicy.Priority priority = policy.priority(note);
        Map<String, Object> metadata = Map.of(
                "matter_id", note.matterId(), "kind", note.kind().name(),
                "recording_ref", note.recordingRef(), "transcript", note.transcript(),
                "priority", priority.name(), "signed", note.signed());
        vectors.upsert(collection, List.of(Map.of("id", note.noteId(), "embedding", embedding, "metadata", metadata)));
        return new IndexedNote(note.noteId(), priority);
    }

    public List<SearchHit> search(String question, int limit) {
        List<Map<String, Object>> nearest = vectors.query(collection, embed(question), Math.max(limit * 3, limit));
        List<String> candidates = nearest.stream().map(this::transcriptOf).toList();
        List<Map<String, Object>> ranked = vectors.rerank(question, candidates, limit);
        List<SearchHit> hits = new ArrayList<>();
        for (Map<String, Object> item : ranked) {
            int index = ((Number) item.getOrDefault("index", 0)).intValue();
            if (index >= 0 && index < nearest.size()) {
                Map<String, Object> source = nearest.get(index);
                hits.add(new SearchHit(String.valueOf(source.get("id")), transcriptOf(source),
                        ((Number) item.getOrDefault("score", 0)).doubleValue()));
            }
        }
        hits.sort(Comparator.comparingDouble(SearchHit::score).reversed());
        return hits;
    }

    private List<Double> embed(String text) {
        EmbeddingCreateParams params = EmbeddingCreateParams.builder().model("auto").input(text).build();
        return ai.embeddings().create(params).data().get(0).embedding().stream()
                .map(Float::doubleValue)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private String transcriptOf(Map<String, Object> item) {
        Object metadata = item.get("metadata");
        return metadata instanceof Map<?, ?> map ? String.valueOf(((Map<String, Object>) map).get("transcript")) : "";
    }

    public record IndexedNote(String noteId, FollowUpPolicy.Priority priority) {}
    public record SearchHit(String noteId, String transcript, double score) {}
}
