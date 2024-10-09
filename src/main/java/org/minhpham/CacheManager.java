package org.minhpham;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    public void put(String key, CachedResponse response) {
        cache.put(key, response);
    }

    public Optional<CachedResponse> get(String key) {
        return Optional.ofNullable(cache.get(key));
    }

    public void clear() {
        cache.clear();
    }

}
