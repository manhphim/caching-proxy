package org.minhpham;

import java.time.Instant;
import java.util.Map;


public class CachedResponse {
    private final byte[] body;
    private final int statusCode;
    private final Map<String, String> headers;
    private final Instant timestamp;

    public CachedResponse(byte[] body, int statusCode, Map<String, String> headers) {
        this.body = body;
        this.statusCode = statusCode;
        this.headers = headers;
        this.timestamp = Instant.now();
    }

    public byte[] getBody() {
        return body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    // Check if the response is still fresh
    public boolean isFresh(long maxAgeSeconds) {
        return Instant.now().minusSeconds(maxAgeSeconds).isBefore(timestamp);
    }
}
