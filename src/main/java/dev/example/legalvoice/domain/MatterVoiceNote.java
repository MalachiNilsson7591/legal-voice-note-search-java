package dev.example.legalvoice.domain;

import java.time.LocalDate;

public record MatterVoiceNote(
        String noteId,
        String matterId,
        NoteKind kind,
        String recordingRef,
        String transcript,
        boolean signed,
        LocalDate deadline
) {
    public enum NoteKind { MATTER_INTAKE, SIGNED_DOCUMENT_DELIVERY, DEADLINE_FOLLOW_UP }
}
