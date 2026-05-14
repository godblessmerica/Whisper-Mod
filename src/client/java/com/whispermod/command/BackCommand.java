package com.whispermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.whispermod.WhisperMod;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class BackCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommands.literal("back")
                        .executes(ctx -> {
                            String dm = WhisperMod.getDmTarget();
                            String em = WhisperMod.getEmTarget();

                            if (dm == null && em == null) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("You're already in public chat.").withStyle(ChatFormatting.GRAY)
                                );
                            } else {
                                WhisperMod.exitAll();
                                ctx.getSource().sendFeedback(
                                        Component.literal("Returned to public chat.").withStyle(ChatFormatting.YELLOW)
                                );
                            }
                            return 1;
                        })
        );
    }
}
