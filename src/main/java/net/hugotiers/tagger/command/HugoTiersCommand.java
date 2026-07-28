package net.hugotiers.tagger.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.hugotiers.tagger.gui.HugoTiersKeybinds;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Single client command: {@code /hugotiers} opens the settings screen. Everything that used to be
 * a subcommand (toggle, rainbow, mode) now lives in that screen.
 *
 * <p>The open is routed through {@link HugoTiersKeybinds#requestOpen()} rather than calling
 * {@code setScreen} here, because ChatScreen closes the screen again right after dispatch.
 */
public final class HugoTiersCommand {
    private HugoTiersCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("hugotiers").executes(context -> {
            HugoTiersKeybinds.requestOpen();
            return 1;
        }));
    }
}
