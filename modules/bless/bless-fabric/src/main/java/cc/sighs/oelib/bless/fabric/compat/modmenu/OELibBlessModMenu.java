package cc.sighs.oelib.bless.fabric.compat.modmenu;

import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class OELibBlessModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<? extends Screen> getModConfigScreenFactory() {
        return parent -> new ConfigScreen(parent, "oelib");
    }
}
