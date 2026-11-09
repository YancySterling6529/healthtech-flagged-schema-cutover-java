package example.healthtech.infra;

public final class JsonBody {
    private JsonBody() {
    }

    public static String object(String key, String value) {
        return "{" + quote(key) + ":" + quote(value) + "}";
    }

    public static String bucketWriteNotice(String appointmentId, String patientId, String clinicianId, String visitReason) {
        return "{" +
                quote("data_base64") + ":" + quote(Base64Codec.encode(noticeJson(appointmentId, patientId, clinicianId, visitReason))) + "," +
                quote("content_type") + ":" + quote("application/json") + "," +
                quote("idempotency_key") + ":" + quote("notice-" + appointmentId) +
                "}";
    }

    public static String flagToggleBody(boolean enabled, String key) {
        return "{" +
                quote("enabled") + ":" + enabled + "," +
                quote("key") + ":" + quote(key) + "," +
                quote("version") + ":1" +
                "}";
    }

    public static String flagRolloutBody(String key) {
        return "{" +
                quote("key") + ":" + quote(key) + "," +
                quote("percentage") + ":100," +
                quote("salt") + ":" + quote(key) + "," +
                quote("sticky_unit") + ":" + quote("user_id") + "," +
                quote("version") + ":2" +
                "}";
    }

    private static String noticeJson(String appointmentId, String patientId, String clinicianId, String visitReason) {
        return "{" +
                quote("appointmentId") + ":" + quote(appointmentId) + "," +
                quote("patientId") + ":" + quote(patientId) + "," +
                quote("clinicianId") + ":" + quote(clinicianId) + "," +
                quote("visitReason") + ":" + quote(visitReason) + "," +
                quote("message") + ":" + quote("Your appointment record was updated safely. No action is needed.") +
                "}";
    }

    private static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
