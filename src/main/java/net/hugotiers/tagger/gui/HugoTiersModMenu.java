package net.hugotiers.tagger.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.hugotiers.tagger.HugoTiersClient;

/**
 * Adds the settings button to the mod's entry in ModMenu. ModMenu is an OPTIONAL dependency:
 * it is compiled against but never required at runtime — without it, ModMenu never looks up this
 * entrypoint and the class is simply never loaded.
 */
public final class HugoTiersModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new HugoTiersConfigScreen(parent, HugoTiersClient.config());
    }
}
