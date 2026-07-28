package net.hugotiers.tagger.model;

import java.util.Map;

public final class PlayerTiers {
    public static final PlayerTiers NOT_FOUND = new PlayerTiers(false, Map.of(), null, 0);

    private final boolean found;
    private final Map<String, Rank> ranks;
    private final Rank bestRank;
    private final int points;

    public PlayerTiers(boolean found, Map<String, Rank> ranks, Rank bestRank, int points) {
        this.found = found;
        this.ranks = Map.copyOf(ranks);
        this.bestRank = bestRank;
        this.points = points;
    }

    public boolean found() {
        return found;
    }

    public Map<String, Rank> ranks() {
        return ranks;
    }

    public Rank bestRank() {
        return bestRank;
    }

    public int points() {
        return points;
    }

    public Rank rankFor(String gamemode) {
        if (!found) {
            return null;
        }
        if (gamemode == null || gamemode.equalsIgnoreCase("overall")) {
            return bestRank;
        }
        return ranks.get(gamemode);
    }
}
