package dev.example.legalvoice.web;

import dev.example.legalvoice.domain.MatterVoiceNote;
import dev.example.legalvoice.service.LegalVoiceSearchService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/legal-voice")
public class LegalVoiceController {
    private final LegalVoiceSearchService service;
    public LegalVoiceController(LegalVoiceSearchService service) { this.service = service; }

    @PostMapping("/index")
    public LegalVoiceSearchService.IndexedNote index(@RequestBody MatterVoiceNote note) { return service.index(note); }

    @PostMapping("/collection")
    @ResponseStatus(HttpStatus.CREATED)
    public void prepareCollection() { service.prepareIndex(); }

    @GetMapping("/search")
    public List<LegalVoiceSearchService.SearchHit> search(@RequestParam String query,
                                                          @RequestParam(defaultValue = "5") int limit) {
        return service.search(query, limit);
    }
}
