package dev.example.legalvoice.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FollowUpPolicyTest {
    private final FollowUpPolicy policy = new FollowUpPolicy(Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void signedDeliveryNearDeadlineBecomesUrgentWhileIntakeStaysNormal() {
        MatterVoiceNote delivery = new MatterVoiceNote("n-1", "m-8", MatterVoiceNote.NoteKind.SIGNED_DOCUMENT_DELIVERY,
                "call-44", "Client signed; filing is due Monday.", true, LocalDate.parse("2026-09-21"));
        MatterVoiceNote intake = new MatterVoiceNote("n-2", "m-8", MatterVoiceNote.NoteKind.MATTER_INTAKE,
                "voice-12", "Initial tenancy intake.", false, null);

        assertEquals(FollowUpPolicy.Priority.URGENT, policy.priority(delivery));
        assertEquals(FollowUpPolicy.Priority.NORMAL, policy.priority(intake));
    }
}
