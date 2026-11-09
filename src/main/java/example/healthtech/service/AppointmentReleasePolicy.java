package example.healthtech.service;

import example.healthtech.domain.AppointmentBookingRequest;
import example.healthtech.domain.NotificationPlan;
import example.healthtech.domain.ReleaseDecision;

public final class AppointmentReleasePolicy {
    private AppointmentReleasePolicy() {
    }

    public static ReleaseDecision decide(AppointmentBookingRequest request, boolean releaseFlagEnabled) {
        if (!request.migrationRequested()) {
            return new ReleaseDecision("LEGACY_SLOT_V1", new NotificationPlan(false, "No migration requested"));
        }
        if (!releaseFlagEnabled) {
            return new ReleaseDecision("LEGACY_SLOT_V1", new NotificationPlan(true, "Flag disabled for migration path"));
        }
        if (request.patientAge() >= 65) {
            return new ReleaseDecision("FHIR_SLOT_V2", new NotificationPlan(true, "Senior patient migration reviewed"));
        }
        return new ReleaseDecision("FHIR_SLOT_V2", new NotificationPlan(true, "Standard migration"));
    }
}
