package net.hugotiers.tagger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.hugotiers.tagger.command.HugoTiersCommand;
import net.hugotiers.tagger.config.ModConfig;
import net.hugotiers.tagger.gui.HugoTiersKeybinds;
import net.hugotiers.tagger.model.PlayerTiers;
import net.hugotiers.tagger.model.Rank;
import net.hugotiers.tagger.net.TierService;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HugoTiersClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("hugotiers-tagger");

    // Kit icons ship as a bundled bitmap font; each kit maps to one private-use glyph.
    private static final Style ICON_STYLE =
            Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("hugotiers", "icons")));
    // Tiers use a bundled small, condensed font (own look, not the vanilla one).
    private static final Style TIER_FONT =
            Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("hugotiers", "tiers")));
    private static final Map<String, String> KIT_ICON = Map.of(
            "cavekit", "",
            "crystalfight", "",
            "macefight", ""
    );
    // Order for the "all" mode; any further kits are appended after these, without an icon.
    private static final List<String> KIT_ORDER = List.of("cavekit", "crystalfight", "macefight");

    private static ModConfig config;
    private static TierService tierService;

    // Per-player cache of the styled badge, so it is NOT rebuilt every render frame (FPS win).
    private record CachedBadge(MutableText badge, PlayerTiers tiers, String gamemode) {}
    private static final Map<UUID, CachedBadge> BADGE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /** The live config instance. Mutate it — never replace it; TierService holds this reference. */
    public static ModConfig config() {
        return config;
    }

    /** Drops the per-player styled badges, so a changed display mode takes effect immediately. */
    public static void clearBadgeCache() {
        BADGE_CACHE.clear();
    }

    /** Drops both the looked-up tiers and the styled badges — for a changed API host or cache TTL. */
    public static void invalidateTierCache() {
        TierService svc = tierService;
        if (svc != null) {
            svc.invalidateAll();
        }
        BADGE_CACHE.clear();
    }

    @Override
    public void onInitializeClient() {
        config = ModConfig.load();
        tierService = new TierService(config);
        tierService.start();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (tierService != null) {
                tierService.stop();
            }
            if (config != null) {
                config.save(); // last resort: never lose a setting because the game was quit
            }
        });
        HugoTiersKeybinds.register(config);
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> HugoTiersCommand.register(dispatcher)
        );
        logEnvironment();
    }

    /**
     * One startup line describing the environment. It exists so a user who sees nothing can tell
     * "the mod never loaded" apart from "the mod loaded but something else draws the nametags" by
     * pasting a single log line — third-party clients replace the nametag renderer.
     */
    private static void logEnvironment() {
        FabricLoader loader = FabricLoader.getInstance();
        String mcVersion = loader.getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        boolean lunar = loader.isModLoaded("ichor");
        LOGGER.info("HUGOTiers Tagger initialised. minecraft={} loader={} enabled={} rainbow={} mode={} host={}{}",
                mcVersion,
                loader.getModContainer("fabricloader")
                        .map(container -> container.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown"),
                config.enabled, config.rainbow, config.gamemode, config.apiBaseUrl,
                lunar ? " client=lunar(ichor)" : "");
        if (lunar) {
            LOGGER.warn("Lunar Client detected. It replaces the vanilla nametag renderer, so tier "
                    + "badges may not appear. If they do not, please report it with this log line.");
        }
    }

    /**
     * Prepends one "&lt;kit-icon&gt;&lt;TIER&gt;" pair per ranked kit to the nametag. Runs on the
     * render thread — fast and null-safe; returns the original unchanged when there is nothing to show.
     */
    public static Text decorateLabel(UUID uuid, Text original) {
        ModConfig cfg = config;
        TierService svc = tierService;
        if (cfg == null || svc == null || !cfg.enabled || uuid == null || original == null) {
            return original;
        }
        // Idempotence: a client may feed us the same label through more than one path (the vanilla
        // render path and the getDisplayName fallback). Decorating twice would double the badge.
        if (isDecorated(original)) {
            return original;
        }

        PlayerTiers tiers = svc.getTiers(uuid);
        if (tiers == null || !tiers.found()) {
            return original;
        }

        // Reuse the styled badge across frames; it only changes when the tiers refresh or the mode changes.
        CachedBadge cached = BADGE_CACHE.get(uuid);
        MutableText badge;
        if (cached != null && cached.tiers() == tiers
                && java.util.Objects.equals(cached.gamemode(), cfg.gamemode)) {
            badge = cached.badge();
        } else {
            badge = buildBadge(cfg, tiers);
            if (badge == null) {
                return original;
            }
            BADGE_CACHE.put(uuid, new CachedBadge(badge, tiers, cfg.gamemode));
        }
        // Keep the badge as the FIRST sibling so its width is measurable at render time (for the frame).
        return Text.empty().append(badge).append(Text.literal(" ")).append(original);
    }

    /** True when this label already carries one of our kit glyphs. */
    private static boolean isDecorated(Text text) {
        String s = text.getString();
        return s.indexOf('\uE000') >= 0 || s.indexOf('\uE001') >= 0 || s.indexOf('\uE002') >= 0;
    }

    private static MutableText buildBadge(ModConfig cfg, PlayerTiers tiers) {
        MutableText badge = Text.empty();
        boolean any = false;
        for (String kit : kitsToShow(cfg.gamemode, tiers.ranks())) {
            Rank rank = tiers.ranks().get(kit);
            if (rank == null) {
                continue;
            }
            if (any) {
                badge.append(Text.literal(" "));
            }
            String glyph = KIT_ICON.get(kit);
            if (glyph != null) {
                badge.append(Text.literal(glyph).setStyle(ICON_STYLE));
            }
            badge.append(tierText(rank));
            any = true;
        }
        return any ? badge : null;
    }

    private static List<String> kitsToShow(String mode, Map<String, Rank> ranks) {
        if (mode == null || mode.equalsIgnoreCase("all") || mode.equalsIgnoreCase("overall")) {
            List<String> kits = new ArrayList<>(KIT_ORDER);
            for (String kit : ranks.keySet()) {
                if (!kits.contains(kit)) {
                    kits.add(kit);
                }
            }
            return kits;
        }
        return List.of(mode);
    }

    private static MutableText tierText(Rank rank) {
        return Text.literal(rank.label()).setStyle(TIER_FONT.withColor(TextColor.fromRgb(rank.rgb())));
    }

    /**
     * Called from LabelCommandRendererMixin after a nametag label is drawn: draws a thin, time-animated
     * rainbow rectangle outline around just the tier badge (kept as the first sibling) of our tagged
     * nametags. Uses the label's own matrix and MC's nametag-background render layer, so it sits on the
     * tag and only appears when the label itself is drawn.
     */
    public static void renderBadgeFrame(OrderedRenderCommandQueueImpl.LabelCommand command,
                                        VertexConsumerProvider.Immediate immediate,
                                        TextRenderer textRenderer) {
        ModConfig cfg = config;
        if (cfg == null || !cfg.enabled || !cfg.rainbow || command == null) {
            return;
        }
        Text text = command.text();
        if (text == null) {
            return;
        }
        String s = text.getString();
        if (s.indexOf('\uE000') < 0 && s.indexOf('\uE001') < 0 && s.indexOf('\uE002') < 0) {
            return; // not one of our tagged nametags
        }
        List<Text> siblings = text.getSiblings();
        Text badge = (siblings != null && !siblings.isEmpty()) ? siblings.get(0) : text;
        int badgeWidth = textRenderer.getWidth(badge);
        if (badgeWidth <= 0) {
            return;
        }

        float x0 = command.x();
        float y0 = command.y();
        float left = x0 - 1.0f;
        float right = x0 + badgeWidth + 1.0f;
        float top = y0 - 1.0f;
        float bottom = y0 + 9.0f;
        float th = 1.0f;
        int argb = 0xFF000000 | hsvToRgb((System.currentTimeMillis() % 2600L) / 2600.0f);
        int light = command.lightCoords();
        Matrix4f matrix = command.matricesEntry();
        VertexConsumer vc = immediate.getBuffer(RenderLayers.textBackgroundSeeThrough());

        frameQuad(vc, matrix, left, top, right, top + th, argb, light);       // top edge
        frameQuad(vc, matrix, left, bottom - th, right, bottom, argb, light); // bottom edge
        frameQuad(vc, matrix, left, top, left + th, bottom, argb, light);     // left edge
        frameQuad(vc, matrix, right - th, top, right, bottom, argb, light);   // right edge
    }

    private static void frameQuad(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2,
                                  int argb, int light) {
        vc.vertex(m, x1, y2, 0.0f).color(argb).light(light);
        vc.vertex(m, x2, y2, 0.0f).color(argb).light(light);
        vc.vertex(m, x2, y1, 0.0f).color(argb).light(light);
        vc.vertex(m, x1, y1, 0.0f).color(argb).light(light);
    }

    private static int hsvToRgb(float h) {
        int i = (int) (h * 6.0f) % 6;
        float f = h * 6.0f - (float) Math.floor(h * 6.0f);
        float v = 1.0f, p = 0.0f, q = 1.0f - f, t = f;
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
