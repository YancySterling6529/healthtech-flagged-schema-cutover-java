package example.healthtech.infra;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InfraiClient {
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;

    public final StorageNamespace storage = new StorageNamespace();
    public final FlagsNamespace flags = new FlagsNamespace();

    public InfraiClient(String apiKey, String baseUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public final class StorageNamespace {
        public final BucketNamespace bucket = new BucketNamespace();
        public final ObjectNamespace object = new ObjectNamespace();

        public final class BucketNamespace {
            public JsonNode create(String bodyJson) throws Exception {
                return call("POST", "/v1/storage/bucket/create", bodyJson, true);
            }
        }

        public final class ObjectNamespace {
            public JsonNode put(String bucket, String key, String bodyJson) throws Exception {
                return call("PUT", "/v1/storage/object/put/" + PathCodec.encode(bucket) + "/" + PathCodec.encode(key), bodyJson, true);
            }

            public JsonNode head(String bucket, String key) throws Exception {
                return call("GET", "/v1/storage/object/head/" + PathCodec.encode(bucket) + "/" + PathCodec.encode(key), null, false);
            }
        }
    }

    public final class FlagsNamespace {
        public JsonNode toggle(String key, String bodyJson) throws Exception {
            return call("POST", "/v1/flags/toggle/" + PathCodec.encode(key), bodyJson, true);
        }

        public JsonNode rollout(String key, String bodyJson) throws Exception {
            return call("POST", "/v1/flags/rollout/" + PathCodec.encode(key), bodyJson, true);
        }
    }

    private JsonNode call(String method, String path, String bodyJson, boolean sendJson) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json");

        if (sendJson) {
            builder.header("Content-Type", "application/json");
        }

        if (bodyJson == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8));
        }

        int maxAttempts = 4;
        long delayMillis = 250L;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode envelope = JsonNode.parse(response.body());
            if (!envelope.bool("ok")) {
                if (response.statusCode() == 429 && attempt < maxAttempts) {
                    delayMillis = sleepForRetry(response.headers(), delayMillis);
                    continue;
                }
                throw new InfraiException(
                        envelope.at("error.code").asText("INFRAI_ERROR"),
                        envelope.at("error.message").asText("Request failed"),
                        response.statusCode()
                );
            }
            return envelope.at("data");
        }
        throw new IllegalStateException("Retry budget exhausted");
    }

    private long sleepForRetry(HttpHeaders headers, long currentDelayMillis) throws InterruptedException {
        Optional<String> retryAfter = headers.firstValue("Retry-After");
        long waitMillis = retryAfter.map(value -> {
            try {
                return Long.parseLong(value) * 1000L;
            } catch (NumberFormatException ex) {
                return currentDelayMillis;
            }
        }).orElse(currentDelayMillis);
        Thread.sleep(waitMillis);
        return Math.min(waitMillis * 2L, 4_000L);
    }

    public static final class JsonNode {
        private final Object value;

        private JsonNode(Object value) {
            this.value = value;
        }

        static JsonNode parse(String json) {
            return new JsonNode(new JsonParser(json).parseValue());
        }

        public JsonNode at(String path) {
            String[] parts = path.split("\\.");
            Object current = value;
            for (String part : parts) {
                if (!(current instanceof Map<?, ?> map)) {
                    return new JsonNode(null);
                }
                current = map.get(part);
            }
            return new JsonNode(current);
        }

        public boolean bool(String key) {
            if (!(value instanceof Map<?, ?> map)) {
                return false;
            }
            Object raw = map.get(key);
            return raw instanceof Boolean b && b;
        }

        public String asText(String fallback) {
            if (value == null) {
                return fallback;
            }
            return String.valueOf(value);
        }

        public boolean asBoolean(boolean fallback) {
            if (value instanceof Boolean b) {
                return b;
            }
            return fallback;
        }
    }

    static final class JsonParser {
        private final String json;
        private int index;

        JsonParser(String json) {
            this.json = json;
        }

        Object parseValue() {
            skipWhitespace();
            char c = current();
            if (c == '{') {
                return parseObject();
            }
            if (c == '"') {
                return parseString();
            }
            if (c == 't' || c == 'f') {
                return parseBoolean();
            }
            if (c == 'n') {
                parseNull();
                return null;
            }
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (current() == '}') {
                index++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                char c = current();
                if (c == '}') {
                    index++;
                    return map;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (true) {
                char c = current();
                if (c == '"') {
                    index++;
                    return builder.toString();
                }
                if (c == '\\') {
                    index++;
                    char escaped = current();
                    switch (escaped) {
                        case '"', '\\', '/' -> builder.append(escaped);
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> {
                            String hex = json.substring(index + 1, index + 5);
                            builder.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> throw new IllegalArgumentException("Bad escape sequence");
                    }
                    index++;
                    continue;
                }
                builder.append(c);
                index++;
            }
        }

        private Boolean parseBoolean() {
            if (json.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (json.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean");
        }

        private void parseNull() {
            if (!json.startsWith("null", index)) {
                throw new IllegalArgumentException("Invalid null");
            }
            index += 4;
        }

        private Number parseNumber() {
            int start = index;
            while (index < json.length()) {
                char c = json.charAt(index);
                if ((c >= '0' && c <= '9') || c == '-' || c == '.') {
                    index++;
                    continue;
                }
                break;
            }
            String token = json.substring(start, index);
            if (token.contains(".")) {
                return Double.parseDouble(token);
            }
            return Long.parseLong(token);
        }

        private void skipWhitespace() {
            while (index < json.length()) {
                char c = json.charAt(index);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    index++;
                    continue;
                }
                break;
            }
        }

        private char current() {
            if (index >= json.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            return json.charAt(index);
        }

        private void expect(char expected) {
            if (current() != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "'");
            }
            index++;
        }
    }
}
