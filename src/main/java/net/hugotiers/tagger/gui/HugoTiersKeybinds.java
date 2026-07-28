package net.hugotiers.tagger.gui;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.hugotiers.tagger.config.ModConfig;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

/**
 * Opens {@link HugoTiersConfigScreen} from a key bind, and from anywhere else via
 * {@link #requestOpen()}.
 *
 * <p>The open is deferred to the next client tick on purpose: calling {@code setScreen} straight
 * out of a command handler does nothing, because ChatScreen calls {@code setScreen(null)} right
 * after dispatching the command and would close the screen again immediately.
 */
public final class HugoTiersKeybinds {
    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("hugotiers", "main"));

    private static KeyBinding openSettings;
    private static volatile boolean openRequested;

    private HugoTiersKeybinds() {
    }

    /** Asks for the settings screen to open on the next client tick. */
    public static void requestOpen() {
        openRequested = true;
    }

    public static void register(ModConfig config) {
        // Unbound by default so the mod does not steal a key; users bind it in Options > Controls.
        openSettings = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hugotiers.open_settings",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettings.wasPressed()) {
                openRequested = true;
            }
            if (openRequested) {
                openRequested = false;
                client.setScreen(new HugoTiersConfigScreen(client.currentScreen, config));
            }
        });
    }
}
