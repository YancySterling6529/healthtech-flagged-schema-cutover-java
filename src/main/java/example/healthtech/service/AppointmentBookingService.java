package example.healthtech.service;

import example.healthtech.domain.AppointmentBookingRequest;
import example.healthtech.domain.AppointmentBookingResult;
import example.healthtech.domain.ReleaseDecision;

import java.util.Optional;

public final class AppointmentBookingService {
    private final NotificationArchive notificationArchive;
    private final ReleaseControlService releaseControlService;

    public AppointmentBookingService(NotificationArchive notificationArchive, ReleaseControlService releaseControlService) {
        this.notificationArchive = notificationArchive;
        this.releaseControlService = releaseControlService;
    }

    public AppointmentBookingResult book(AppointmentBookingRequest request) throws Exception {
        ReleaseDecision decision = AppointmentReleasePolicy.decide(request, releaseControlService.isMigrationPathEnabled());
        if (decision.notificationPlan().required()) {
            String objectKey = notificationArchive.storePatientSafeNotice(request);
            return new AppointmentBookingResult(
                    request.appointmentId(),
                    decision.strategy(),
                    notificationArchive.noticeExists(objectKey),
                    Optional.of(objectKey),
                    "Your appointment stays confirmed while the record update completes."
            );
        }

        return new AppointmentBookingResult(
                request.appointmentId(),
                decision.strategy(),
                false,
                Optional.empty(),
                "Your appointment is confirmed."
        );
    }
}
