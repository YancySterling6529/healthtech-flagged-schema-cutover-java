package example.healthtech.infra;

public final class BucketBootstrap {
    private final InfraiClient infrai;
    private final String bucketName;

    public BucketBootstrap(InfraiClient infrai, String bucketName) {
        this.infrai = infrai;
        this.bucketName = bucketName;
    }

    public void ensureBucket() throws Exception {
        infrai.storage.bucket.create(JsonBody.object("name", bucketName));
    }
}
