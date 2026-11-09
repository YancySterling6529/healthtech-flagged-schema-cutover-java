package example.healthtech;

import example.healthtech.config.ServiceConfig;
import example.healthtech.domain.AppointmentBookingRequest;
import example.healthtech.domain.AppointmentBookingResult;
import example.healthtech.infra.BucketBootstrap;
import example.healthtech.infra.InfraiClient;
import example.healthtech.service.AppointmentBookingService;
import example.healthtech.service.NotificationArchive;
import example.healthtech.service.ReleaseControlService;

public final class AppointmentWorkflowCli {
    public static void main(String[] args) throws Exception {
        ServiceConfig config = ServiceConfig.fromEnv();
        InfraiClient infrai = new InfraiClient(config.apiKey(), config.baseUrl());

        BucketBootstrap bootstrap = new BucketBootstrap(infrai, config.bucketName());
        bootstrap.ensureBucket();

        ReleaseControlService releaseControlService = new ReleaseControlService(infrai, config.releaseFlagKey());
        releaseControlService.enableMigrationPath();

        NotificationArchive notificationArchive = new NotificationArchive(infrai, config.bucketName());
        AppointmentBookingService bookingService = new AppointmentBookingService(notificationArchive, releaseControlService);

        AppointmentBookingRequest request = new AppointmentBookingRequest(
                "apt-200",
                "p-100",
                68,
                "dr-lee",
                true,
                "Follow-up after imaging review"
        );

        AppointmentBookingResult result = bookingService.book(request);

        System.out.println("strategy=" + result.strategy());
        System.out.println("notificationKey=" + result.notificationObjectKey().orElse("none"));
        System.out.println("notificationStored=" + result.notificationStored());
    }
}
