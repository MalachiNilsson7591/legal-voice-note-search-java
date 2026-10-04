package dev.example.legalvoice.domain;

import java.time.Clock;
import java.time.LocalDate;

public final class FollowUpPolicy {
    private final Clock clock;

    public FollowUpPolicy(Clock clock) { this.clock = clock; }

    public Priority priority(MatterVoiceNote note) {
        if (note.deadline() != null && !note.deadline().isAfter(LocalDate.now(clock).plusDays(3))) {
            return Priority.URGENT;
        }
        if (note.kind() == MatterVoiceNote.NoteKind.SIGNED_DOCUMENT_DELIVERY && note.signed()) {
            return Priority.HIGH;
        }
        return Priority.NORMAL;
    }

    public enum Priority { URGENT, HIGH, NORMAL }
}
