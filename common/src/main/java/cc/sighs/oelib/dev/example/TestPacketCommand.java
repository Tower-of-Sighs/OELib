package cc.sighs.oelib.dev.example;

import cc.sighs.oelib.dev.example.net.BigPacketSender;
import cc.sighs.oelib.registry.extra.CommandRegister;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class TestPacketCommand {
    public static void register() {
        CommandRegister.registerServer(TestPacketCommand::registerCommand);
    }

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher,
                                       CommandBuildContext context,
                                       Commands.CommandSelection environment) {
        dispatcher.register(
                Commands.literal("oelib")
                        .then(Commands.literal("stress")
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                        .then(Commands.argument("interval", IntegerArgumentType.integer(10, 5000))
                                                .executes(ctx -> {
                                                    int count = IntegerArgumentType.getInteger(ctx, "count");
                                                    int interval = IntegerArgumentType.getInteger(ctx, "interval");
                                                    BigPacketSender.runStressTest(ctx.getSource().getPlayerOrException(), count, interval);
                                                    return 1;
                                                }))))
        );
    }
}
