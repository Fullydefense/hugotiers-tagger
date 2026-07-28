package net.hugotiers.tagger.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.hugotiers.tagger.model.PlayerTiers;
import net.hugotiers.tagger.model.Rank;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TierResponseParser {
    private TierResponseParser() {
    }

    public static Map<UUID, PlayerTiers> parse(String jsonBody, Collection<UUID> batch) {
        Map<UUID, PlayerTiers> result = notFoundBatch(batch);
        JsonObject players;
        try {
            JsonObject root = JsonParser.parseString(jsonBody).getAsJsonObject();
            JsonElement playersElement = root.get("players");
            if (playersElement == null || !playersElement.isJsonObject()) {
                return result;
            }
            players = playersElement.getAsJsonObject();
        } catch (RuntimeException exception) {
            return result;
        }

        for (UUID uuid : batch) {
            try {
                String key = normalizedUuid(uuid);
                JsonElement entryElement = players.get(key);
                if (entryElement == null || !entryElement.isJsonObject()) {
                    continue;
                }
                JsonObject entry = entryElement.getAsJsonObject();
                JsonElement foundElement = entry.get("found");
                if (foundElement == null
                        || !foundElement.isJsonPrimitive()
                        || !foundElement.getAsJsonPrimitive().isBoolean()
                        || !foundElement.getAsBoolean()) {
                    continue;
                }

                JsonElement ranksElement = entry.get("ranks");
                JsonElement pointsElement = entry.get("points");
                if (ranksElement == null
                        || !ranksElement.isJsonObject()
                        || pointsElement == null
                        || !pointsElement.isJsonPrimitive()
                        || !pointsElement.getAsJsonPrimitive().isNumber()) {
                    continue;
                }

                Map<String, Rank> ranks = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> rankEntry
                        : ranksElement.getAsJsonObject().entrySet()) {
                    Rank rank = parseRank(rankEntry.getValue());
                    if (rank != null) {
                        ranks.put(rankEntry.getKey(), rank);
                    }
                }

                Rank bestRank = parseRank(entry.get("bestRank"));
                int points = pointsElement.getAsInt();
                result.put(uuid, new PlayerTiers(true, ranks, bestRank, points));
            } catch (RuntimeException exception) {
                result.put(uuid, PlayerTiers.NOT_FOUND);
            }
        }
        return result;
    }

    private static Map<UUID, PlayerTiers> notFoundBatch(Collection<UUID> batch) {
        Map<UUID, PlayerTiers> result = new LinkedHashMap<>();
        for (UUID uuid : batch) {
            result.put(uuid, PlayerTiers.NOT_FOUND);
        }
        return result;
    }

    private static Rank parseRank(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            return Rank.fromCode(element.getAsString());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String normalizedUuid(UUID uuid) {
        return uuid.toString().replace("-", "");
    }
}
