package net.hugotiers.tagger.gui;

import net.hugotiers.tagger.HugoTiersClient;
import net.hugotiers.tagger.config.ModConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * The mod's settings screen. Reached from ModMenu, from the key bind in Options &gt; Controls,
 * or via /hugotiers.
 *
 * <p>Mutates the live {@link ModConfig} instance rather than replacing it: {@code TierService}
 * holds the same reference and reads {@code apiBaseUrl} and {@code cacheTtlSeconds} at runtime.
 */
public final class HugoTiersConfigScreen extends Screen {
    private static final List<String> MODES =
            List.of("all", "cavekit", "crystalfight", "macefight");
    private static final List<Long> TTL_CHOICES = List.of(60L, 300L, 900L, 3600L);

    private static final int ROW_WIDTH = 158;
    private static final int ROW_HEIGHT = 20;
    private static final int COLOUR_OK = 0xE0E0E0;
    private static final int COLOUR_INVALID = 0xFF5555;

    private final Screen parent;
    private final ModConfig config;
    private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

    // Captured before any edit, so we only invalidate caches when something actually changed.
    private final String originalApiBaseUrl;
    private final long originalCacheTtlSeconds;

    private TextFieldWidget apiField;

    public HugoTiersConfigScreen(Screen parent, ModConfig config) {
        super(Text.translatable("hugotiers.config.title"));
        this.parent = parent;
        this.config = config;

        // The widgets can only display values from their fixed choice lists. Write the normalised
        // value back immediately, otherwise the screen would show one value while the mod uses
        // another — and saving would persist the value the user never saw.
        this.config.gamemode = normalizeMode(config.gamemode);
        this.config.cacheTtlSeconds = nearestTtl(config.cacheTtlSeconds);

        this.originalApiBaseUrl = this.config.apiBaseUrl;
        this.originalCacheTtlSeconds = this.config.cacheTtlSeconds;
    }

    @Override
    protected void init() {
        this.layout.addHeader(this.title, this.textRenderer);

        GridWidget grid = new GridWidget();
        grid.setColumnSpacing(8);
        grid.setRowSpacing(6);
        GridWidget.Adder rows = grid.createAdder(2);

        rows.add(CyclingButtonWidget.onOffBuilder(this.config.enabled)
                .tooltip(value -> Tooltip.of(Text.translatable("hugotiers.config.enabled.tooltip")))
                .build(0, 0, ROW_WIDTH, ROW_HEIGHT,
                        Text.translatable("hugotiers.config.enabled"),
                        (button, value) -> this.config.enabled = value));

        rows.add(CyclingButtonWidget.onOffBuilder(this.config.rainbow)
                .tooltip(value -> Tooltip.of(Text.translatable("hugotiers.config.rainbow.tooltip")))
                .build(0, 0, ROW_WIDTH, ROW_HEIGHT,
                        Text.translatable("hugotiers.config.rainbow"),
                        (button, value) -> this.config.rainbow = value));

        rows.add(CyclingButtonWidget.<String>builder(
                        mode -> Text.translatable("hugotiers.config.mode." + mode),
                        this.config.gamemode)
                .values(MODES)
                .tooltip(mode -> Tooltip.of(Text.translatable("hugotiers.config.mode." + mode + ".tooltip")))
                .build(0, 0, ROW_WIDTH, ROW_HEIGHT,
                        Text.translatable("hugotiers.config.mode"),
                        (button, value) -> {
                            this.config.gamemode = value;
                            HugoTiersClient.clearBadgeCache();
                        }));

        rows.add(CyclingButtonWidget.<Long>builder(
                        ttl -> Text.translatable("hugotiers.config.ttl.value", ttl),
                        this.config.cacheTtlSeconds)
                .values(TTL_CHOICES)
                .tooltip(ttl -> Tooltip.of(Text.translatable("hugotiers.config.ttl.tooltip")))
                .build(0, 0, ROW_WIDTH, ROW_HEIGHT,
                        Text.translatable("hugotiers.config.ttl"),
                        (button, value) -> this.config.cacheTtlSeconds = value));

        rows.add(new TextWidget(Text.translatable("hugotiers.config.api"), this.textRenderer), 2,
                rows.copyPositioner().marginTop(8).alignLeft());

        this.apiField = new TextFieldWidget(
                this.textRenderer, ROW_WIDTH * 2 + 8, ROW_HEIGHT,
                Text.translatable("hugotiers.config.api"));
        this.apiField.setMaxLength(200);
        this.apiField.setText(this.config.apiBaseUrl);
        this.apiField.setTooltip(Tooltip.of(Text.translatable("hugotiers.config.api.tooltip")));
        // Live feedback: the field turns red while the address is unusable, so the mistake is
        // visible before it silently breaks every lookup.
        this.apiField.setChangedListener(text ->
                this.apiField.setEditableColor(sanitizeUrl(text) != null ? COLOUR_OK : COLOUR_INVALID));
        rows.add(this.apiField, 2);

        this.layout.addBody(grid);

        this.layout.addFooter(ButtonWidget
                .builder(ScreenTexts.DONE, button -> this.close())
                .width(ButtonWidget.DEFAULT_WIDTH)
                .build());

        this.layout.forEachChild(this::addDrawableChild);
        this.refreshWidgetPositions();
    }

    @Override
    protected void refreshWidgetPositions() {
        this.layout.refreshPositions();
    }

    /**
     * Commit and persist here rather than in {@link #close()}: {@code close()} only runs when the
     * user leaves deliberately, while {@code removed()} runs on every screen replacement — being
     * kicked from a server, losing the connection, or any other forced screen change. Saving in
     * {@code close()} alone would silently discard the settings in those cases.
     */
    @Override
    public void removed() {
        if (this.apiField != null) {
            String sanitized = sanitizeUrl(this.apiField.getText());
            if (sanitized != null) {
                this.config.apiBaseUrl = sanitized;
            }
            // An unusable address is dropped on purpose: keeping the previous, working one beats
            // persisting a value that would break every lookup with no in-game feedback.
        }

        boolean hostChanged = !this.originalApiBaseUrl.equals(this.config.apiBaseUrl);
        boolean ttlChanged = this.originalCacheTtlSeconds != this.config.cacheTtlSeconds;
        if (hostChanged || ttlChanged) {
            // Cached entries hold the old host's data and an expiry computed from the old TTL.
            HugoTiersClient.invalidateTierCache();
        }

        this.config.save();
        super.removed();
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    /**
     * Returns a usable base URL, or null when the input cannot work. Requires an absolute http(s)
     * URI with a host, and strips trailing slashes so the path is not doubled when the endpoint is
     * appended.
     */
    static String sanitizeUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String url = raw.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.isEmpty()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            if (!uri.isAbsolute() || uri.getHost() == null) {
                return null;
            }
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return null;
            }
            return url;
        } catch (Exception exception) {
            return null;
        }
    }

    private static String normalizeMode(String mode) {
        return (mode != null && MODES.contains(mode)) ? mode : "all";
    }

    private static long nearestTtl(long seconds) {
        long best = TTL_CHOICES.get(0);
        for (long choice : TTL_CHOICES) {
            if (Math.abs(choice - seconds) < Math.abs(best - seconds)) {
                best = choice;
            }
        }
        return best;
    }
}
