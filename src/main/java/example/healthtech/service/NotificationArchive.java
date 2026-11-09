package example.healthtech.service;

import example.healthtech.domain.AppointmentBookingRequest;
import example.healthtech.infra.InfraiClient;
import example.healthtech.infra.JsonBody;

public final class NotificationArchive {
    private final InfraiClient infrai;
    private final String bucketName;

    public NotificationArchive(InfraiClient infrai, String bucketName) {
        this.infrai = infrai;
        this.bucketName = bucketName;
    }

    public String storePatientSafeNotice(AppointmentBookingRequest request) throws Exception {
        String key = "notifications/" + request.patientId() + "/" + request.appointmentId() + ".json";
        infrai.storage.object.put(
                bucketName,
                key,
                JsonBody.bucketWriteNotice(request.appointmentId(), request.patientId(), request.clinicianId(), request.visitReason())
        );
        return key;
    }

    public boolean noticeExists(String key) throws Exception {
        return infrai.storage.object.head(bucketName, key).at("found").asBoolean(false);
    }
}
