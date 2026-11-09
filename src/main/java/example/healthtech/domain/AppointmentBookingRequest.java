package example.healthtech.domain;

public record AppointmentBookingRequest(
        String appointmentId,
        String patientId,
        int patientAge,
        String clinicianId,
        boolean migrationRequested,
        String visitReason
) {
}
