package example.healthtech.infra;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Base64Codec {
    private Base64Codec() {
    }

    public static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
