package net.hugotiers.tagger.net;

import net.hugotiers.tagger.config.ModConfig;
import net.hugotiers.tagger.model.PlayerTiers;
import net.hugotiers.tagger.model.Rank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class TierService {
    private static final Logger LOGGER = LoggerFactory.getLogger("hugotiers-tagger");
    private static final int MAX_BATCH = 200;
    private static final long RETRY_CACHE_MILLIS = 3_000L; // fast retry after a transient failure
    private static final long BURST_FLUSH_MILLIS = 40L;    // near-instant flush after the first new UUID

    private final ModConfig config;
    private final HttpClient httpClient;
    private final ConcurrentHashMap<UUID, Entry> cache = new ConcurrentHashMap<>();
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<UUID> queue = new ConcurrentLinkedQueue<>();
    private final java.util.concurrent.atomic.AtomicBoolean flushScheduled =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;

    public TierService(ModConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "hugotiers-tier-fetch");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        // periodic backup flush (retries of expired/failed entries); new sightings flush instantly via enqueue()
        scheduler.scheduleWithFixedDelay(this::flush, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    /**
     * Drops every cached lookup. Called when the API host or the cache lifetime changes in the
     * settings, because cached entries carry both the old host's data and an expiry computed from
     * the old TTL — without this, the new setting appears to do nothing for up to the old TTL.
     */
    public void invalidateAll() {
        cache.clear();
        queue.clear();
        pending.clear();
    }

    /** Non-blocking: returns the cached tiers (found or NOT_FOUND), or null while none are cached
     *  yet; a miss or an expired entry is enqueued for a background refresh (stale-while-refresh). */
    public PlayerTiers getTiers(UUID uuid) {
        Entry entry = cache.get(uuid);
        long now = System.currentTimeMillis();
        if (entry != null && entry.expiresAtMillis() > now) {
            return entry.tiers();
        }
        enqueue(uuid);
        return entry != null ? entry.tiers() : null;
    }

    private void enqueue(UUID uuid) {
        if (pending.add(uuid)) {
            queue.add(uuid);
            // Flush almost immediately after the first new UUID of a burst (batches the frame's players),
            // so a freshly seen player's tier shows in ~40 ms instead of waiting for the periodic tick.
            if (flushScheduled.compareAndSet(false, true)) {
                try {
                    scheduler.schedule(() -> {
                        flushScheduled.set(false);
                        flush();
                    }, BURST_FLUSH_MILLIS, TimeUnit.MILLISECONDS);
                } catch (java.util.concurrent.RejectedExecutionException ignored) {
                    flushScheduled.set(false); // scheduler is shutting down
                }
            }
        }
    }

    private void flush() {
        List<UUID> batch = new ArrayList<>(MAX_BATCH);
        while (batch.size() < MAX_BATCH) {
            UUID uuid = queue.poll();
            if (uuid == null) {
                break;
            }
            batch.add(uuid);
        }
        if (batch.isEmpty()) {
            return;
        }

        try {
            fetch(batch);
        } catch (Exception exception) {
            LOGGER.warn("Could not refresh player tiers.", exception);
            negativeCache(batch, RETRY_CACHE_MILLIS);
        } finally {
            pending.removeAll(batch);
        }
    }

    private void fetch(List<UUID> batch) throws Exception {
        String uuids = batch.stream()
                .map(TierService::normalizedUuid)
                .collect(Collectors.joining(","));
        URI uri = URI.create(
                config.apiBaseUrl + "/api/v1/players-by-uuid?uuids=" + uuids
        );
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", "HUGOTiers-Tagger/1.0 (+https://hugotiers.net)")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() != 200) {
            negativeCache(batch, RETRY_CACHE_MILLIS);
            return;
        }

        Map<UUID, PlayerTiers> parsed = TierResponseParser.parse(response.body(), batch);
        long now = System.currentTimeMillis();
        long ttlMillis = config.cacheTtlSeconds * 1000L;
        for (UUID uuid : batch) {
            cache.put(
                    uuid,
                    new Entry(
                            parsed.getOrDefault(uuid, PlayerTiers.NOT_FOUND),
                            now + ttlMillis
                    )
            );
        }
    }

    private void negativeCache(Collection<UUID> batch, long shortMillis) {
        long expiresAtMillis = System.currentTimeMillis() + shortMillis;
        for (UUID uuid : batch) {
            cache.put(uuid, new Entry(PlayerTiers.NOT_FOUND, expiresAtMillis));
        }
    }

    private static String normalizedUuid(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    private record Entry(PlayerTiers tiers, long expiresAtMillis) {
    }
}
