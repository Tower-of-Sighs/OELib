package cc.sighs.oelib.bless;

import cc.sighs.oelib.bless.render.ChongYangOverlay;
import cc.sighs.oelib.bless.render.NewYearOverlay;
import cc.sighs.oelib.bless.render.ValentineOverlay;
import cc.sighs.oelib.platform.Platform;
import cc.sighs.oelib.registry.extra.CommandRegister;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class BlessToastCommand {
    public static void register() {
        CommandRegister.registerClient(BlessToastCommand::registerClientCommand);
    }

    private static void registerClientCommand(CommandDispatcher<CommandSourceStack> dispatcher,
                                              CommandBuildContext context,
                                              Commands.CommandSelection environment) {

        if (Platform.isDevelopmentEnv()) {
            dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("chongyang_toast")
                    .executes(ctx -> {
                        if (ShaderToastResources.getSpiritToastShader() != null) ChongYangOverlay.INSTANCE.show();
                        return Command.SINGLE_SUCCESS;
                    }));

            dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("new_year_toast")
                    .executes(ctx -> {
                        if (ShaderToastResources.getNewYearShader() != null) NewYearOverlay.INSTANCE.show();

                        return Command.SINGLE_SUCCESS;
                    }));

            dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("valentine_toast")
                    .executes(ctx -> {
                        if (ShaderToastResources.getValentineShader() != null) ValentineOverlay.INSTANCE.show();

                        return Command.SINGLE_SUCCESS;
                    }));
        }
    }
}