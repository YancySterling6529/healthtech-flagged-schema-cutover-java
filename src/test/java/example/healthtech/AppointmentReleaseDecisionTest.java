package example.healthtech;

import example.healthtech.domain.AppointmentBookingRequest;
import example.healthtech.domain.ReleaseDecision;
import example.healthtech.service.AppointmentReleasePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppointmentReleaseDecisionTest {
    @Test
    void keepsLegacyPathWhenMigrationIsRequestedButFlagIsDisabled() {
        AppointmentBookingRequest request = new AppointmentBookingRequest(
                "apt-200",
                "p-100",
                72,
                "dr-lee",
                true,
                "Medication review"
        );

        ReleaseDecision decision = AppointmentReleasePolicy.decide(request, false);

        assertEquals("LEGACY_SLOT_V1", decision.strategy());
        assertTrue(decision.notificationPlan().required());
        assertEquals("Flag disabled for migration path", decision.notificationPlan().reason());
    }
}
