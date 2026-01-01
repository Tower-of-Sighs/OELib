package com.sighs.oelib.registry.action;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.Objects;

public record CommandRegisterAction(CommandRegistrar registrar, boolean clientOnly) implements RegistrationAction {
    public CommandRegisterAction {
        Objects.requireNonNull(registrar, "registrar");
    }

    @FunctionalInterface
    public interface CommandRegistrar {
        void register(CommandDispatcher<CommandSourceStack> dispatcher,
                      CommandBuildContext context,
                      Commands.CommandSelection environment);
    }
}