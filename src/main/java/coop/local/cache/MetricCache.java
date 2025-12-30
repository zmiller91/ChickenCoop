package coop.local.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class MetricCache {

    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    // Reuse Gson (thread-safe for read/write)
    private static final Gson GSON = new GsonBuilder().create();

    // Prevent concurrent flush interleaving
    private final ReentrantLock flushLock = new ReentrantLock();

    private final Path cachePath;

    public MetricCache(String path) {
        this.cachePath = Path.of(path);
        hydrateCache();
    }

    // Make this public if you intend to call it
    public void put(String componentId, String metricName, Double value) {
        long now = System.currentTimeMillis();

        // Atomic per-key update
        CACHE.compute(componentId + "::" + metricName, (k, existing) -> {
            if (existing == null) return new CacheEntry(k, now, value);
            existing.setValue(value);
            existing.setTimestamp(now);
            return existing;
        });

        flushAtomic();
    }

    public Double get(String componentId, String metricName) {

        CacheEntry entry = CACHE.get(componentId + "::" + metricName);
        if(entry == null) {
            return null;
        }

        return entry.value;
    }

    private void hydrateCache() {
        // First run: no file yet
        if (!Files.exists(cachePath)) return;

        try (var lines = Files.lines(cachePath, StandardCharsets.UTF_8)) {
            lines.filter(line -> !line.isBlank())
                    .map(this::parseCacheEntry)
                    .filter(Objects::nonNull)
                    .forEach(entry -> CACHE.put(entry.key, entry));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Crash-safe flush:
     * write to temp file -> fsync (best-effort) -> atomic move over target
     */
    private void flushAtomic() {
        flushLock.lock();
        try {
            Path dir = cachePath.getParent();
            if (dir != null) Files.createDirectories(dir);

            // Snapshot to avoid iterating while map changes
            List<String> lines = CACHE.values().stream()
                    .map(GSON::toJson)
                    .toList();

            Path tmp = cachePath.resolveSibling(cachePath.getFileName() + ".tmp");

            Files.write(
                    tmp,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            // Atomic replace if supported by filesystem
            try {
                Files.move(tmp, cachePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, cachePath, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            flushLock.unlock();
        }
    }

    private CacheEntry parseCacheEntry(String line) {
        try {
            CacheEntry e = GSON.fromJson(line, CacheEntry.class);
            // minimal validation
            if (e == null || e.key == null || e.key.isBlank()) return null;
            return e;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Data
    @AllArgsConstructor
    public static class CacheEntry {
        private String key;
        private long timestamp;
        private Double value;
    }
}
