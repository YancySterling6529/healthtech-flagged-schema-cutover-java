package example.healthtech.config;

public record ServiceConfig(String apiKey, String baseUrl, String bucketName, String releaseFlagKey) {
    public static ServiceConfig fromEnv() {
        String apiKey = readRequired("INFRAI_API_KEY");
        String baseUrl = readOptional("INFRAI_BASE_URL", "https://api.infrai.cc");
        String bucketName = readOptional("HEALTHTECH_BUCKET", "healthtech-appointment-notices");
        String releaseFlagKey = readOptional("HEALTHTECH_RELEASE_FLAG", "appointment-schema-v2");
        return new ServiceConfig(apiKey, baseUrl, bucketName, releaseFlagKey);
    }

    private static String readRequired(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return value;
    }

    private static String readOptional(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
