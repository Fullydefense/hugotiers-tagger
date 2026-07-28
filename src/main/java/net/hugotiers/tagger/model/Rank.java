package net.hugotiers.tagger.model;

import java.util.Locale;

public enum Rank {
    HT1(1, true, 0xFFD34D),
    LT1(1, false, 0xC9A133),
    HT2(2, true, 0xE6EBF3),
    LT2(2, false, 0xAEB6C2),
    HT3(3, true, 0xD98E50),
    LT3(3, false, 0xA06A38),
    HT4(4, true, 0x8F939C),
    LT4(4, false, 0x75797F),
    HT5(5, true, 0x7D8189),
    LT5(5, false, 0x63666D);

    private final int tier;
    private final boolean high;
    private final int rgb;

    Rank(int tier, boolean high, int rgb) {
        this.tier = tier;
        this.high = high;
        this.rgb = rgb;
    }

    public int tier() {
        return tier;
    }

    public boolean high() {
        return high;
    }

    public int rgb() {
        return rgb;
    }

    public String label() {
        return name();
    }

    public static Rank fromCode(String code) {
        if (code == null) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
