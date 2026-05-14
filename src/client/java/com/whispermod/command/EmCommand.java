package com.whispermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.whispermod.WhisperMod;
import com.whispermod.crypto.DmSession;
import com.whispermod.crypto.MessageCrypto;
import com.whispermod.crypto.SessionManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class EmCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommands.literal("em")
                        // /em — exit encrypted session
                        .executes(ctx -> {
                            String current = WhisperMod.getEmTarget();
                            if (current == null) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("You're not in an encrypted session.").withStyle(ChatFormatting.GRAY)
                                );
                            } else {
                                WhisperMod.setEmTarget(null);
                                ctx.getSource().sendFeedback(
                                        Component.literal("[EM] Returned to public chat.").withStyle(ChatFormatting.YELLOW)
                                );
                            }
                            return 1;
                        })
                        // /em <player> — start or switch encrypted session
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
                                    String current = WhisperMod.getEmTarget();

                                    if (player.equalsIgnoreCase(current)) {
                                        ctx.getSource().sendFeedback(
                                                Component.literal("Already in an encrypted session with ")
                                                        .withStyle(ChatFormatting.YELLOW)
                                                        .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal(".").withStyle(ChatFormatting.YELLOW))
                                        );
                                        return 1;
                                    }

                                    if (current != null) {
                                        ctx.getSource().sendFeedback(
                                                Component.literal("[EM] Switched to ")
                                                        .withStyle(ChatFormatting.YELLOW)
                                                        .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal(". Type /em or /back to exit.").withStyle(ChatFormatting.GRAY))
                                        );
                                    } else {
                                        ctx.getSource().sendFeedback(
                                                Component.literal("[EM] Encrypted session started with ")
                                                        .withStyle(ChatFormatting.GREEN)
                                                        .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal(". Type /em or /back to exit.").withStyle(ChatFormatting.GRAY))
                                        );
                                    }

                                    WhisperMod.setEmTarget(player);

                                    // Initiate ECDH key exchange
                                    DmSession session = SessionManager.getOrCreate(player);
                                    session.markInitiated();
                                    String kxMessage = MessageCrypto.KX_PREFIX + session.getPublicKeyBase64();
                                    Minecraft mc = Minecraft.getInstance();
                                    mc.getConnection().sendUnattendedCommand("w " + player + " " + kxMessage, mc.screen);

                                    ctx.getSource().sendFeedback(
                                            Component.literal("[EM] Establishing secure session with ")
                                                    .withStyle(ChatFormatting.GRAY)
                                                    .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal("...").withStyle(ChatFormatting.GRAY))
                                    );

                                    return 1;
                                })
                        )
        );
    }
}
