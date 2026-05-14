package com.whispermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.whispermod.WhisperMod;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class DmCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommands.literal("dm")
                        // /dm — exit DM, return to public chat
                        .executes(ctx -> {
                            String current = WhisperMod.getDmTarget();
                            if (current == null) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("You're not in a DM. Use /back to return to public chat from anywhere.").withStyle(ChatFormatting.GRAY)
                                );
                            } else {
                                WhisperMod.setDmTarget(null);
                                ctx.getSource().sendFeedback(
                                        Component.literal("[DM] Returned to public chat.").withStyle(ChatFormatting.YELLOW)
                                );
                            }
                            return 1;
                        })
                        // /dm <player> — start or switch DM
                        .then(ClientCommands.argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    if (mc.getConnection() != null) {
                                        String remaining = builder.getRemaining().toLowerCase();
                                        for (net.minecraft.client.multiplayer.PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                                            String name = info.getProfile().name();
                                            if (name.toLowerCase().startsWith(remaining)) {
                                                builder.suggest(name);
                                            }
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String player = StringArgumentType.getString(ctx, "player");
                                    String current = WhisperMod.getDmTarget();

                                    if (player.equalsIgnoreCase(current)) {
                                        ctx.getSource().sendFeedback(
                                                Component.literal("Already in a DM with ")
                                                        .withStyle(ChatFormatting.YELLOW)
                                                        .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal(".").withStyle(ChatFormatting.YELLOW))
                                        );
                                        return 1;
                                    }

                                    if (current != null) {
                                        ctx.getSource().sendFeedback(
                                                Component.literal("[DM] Switched to ")
                                                        .withStyle(ChatFormatting.YELLOW)
                                                        .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal(". Type /dm or /back to exit.").withStyle(ChatFormatting.GRAY))
                                        );
                                    } else {
                                        ctx.getSource().sendFeedback(
                                                Component.literal("[DM] Now chatting with ")
                                                        .withStyle(ChatFormatting.YELLOW)
                                                        .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal(". Type /dm or /back to exit.").withStyle(ChatFormatting.GRAY))
                                        );
                                    }

                                    WhisperMod.setDmTarget(player);
                                    return 1;
                                })
                        )
        );
    }
}
