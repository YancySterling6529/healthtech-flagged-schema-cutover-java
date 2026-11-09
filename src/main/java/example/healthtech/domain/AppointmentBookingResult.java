package example.healthtech.domain;

import java.util.Optional;

public record AppointmentBookingResult(
        String appointmentId,
        String strategy,
        boolean notificationStored,
        Optional<String> notificationObjectKey,
        String patientMessage
) {
}
