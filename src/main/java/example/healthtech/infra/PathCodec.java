package example.healthtech.infra;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class PathCodec {
    private PathCodec() {
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
