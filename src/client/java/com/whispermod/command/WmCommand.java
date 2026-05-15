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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public class WmCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> wm = ClientCommands.literal("wm");

        // /wm help
        wm.then(ClientCommands.literal("help")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("").withStyle(ChatFormatting.GRAY));
                    ctx.getSource().sendFeedback(
                            Component.literal("--- Whisper Mod Commands ---").withStyle(ChatFormatting.AQUA)
                    );
                    ctx.getSource().sendFeedback(
                            Component.literal("/wm dm <player>").withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal(" — start an unencrypted DM session").withStyle(ChatFormatting.GRAY))
                    );
                    ctx.getSource().sendFeedback(
                            Component.literal("/wm em <player>").withStyle(ChatFormatting.GREEN)
                                    .append(Component.literal(" — start an end-to-end encrypted session").withStyle(ChatFormatting.GRAY))
                    );
                    ctx.getSource().sendFeedback(
                            Component.literal("/wm back").withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal(" — return to public chat").withStyle(ChatFormatting.GRAY))
                    );
                    ctx.getSource().sendFeedback(
                            Component.literal("/wm help").withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal(" — show this list").withStyle(ChatFormatting.GRAY))
                    );
                    ctx.getSource().sendFeedback(Component.literal("").withStyle(ChatFormatting.GRAY));
                    return 1;
                })
        );

        // /wm back
        wm.then(ClientCommands.literal("back")
                .executes(ctx -> {
                    if (WhisperMod.getDmTarget() == null && WhisperMod.getEmTarget() == null) {
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

        // /wm dm <player>
        wm.then(ClientCommands.literal("dm")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(
                            Component.literal("Usage: /wm dm <player>. Use /wm back to return to public chat.").withStyle(ChatFormatting.GRAY)
                    );
                    return 1;
                })
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
                                                .append(Component.literal(". Use /wm back to exit.").withStyle(ChatFormatting.GRAY))
                                );
                            } else {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[DM] Now chatting with ")
                                                .withStyle(ChatFormatting.YELLOW)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(". Use /wm back to exit.").withStyle(ChatFormatting.GRAY))
                                );
                            }

                            WhisperMod.setDmTarget(player);
                            return 1;
                        })
                )
        );

        // /wm em <player>
        wm.then(ClientCommands.literal("em")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(
                            Component.literal("Usage: /wm em <player>. Use /wm back to return to public chat.").withStyle(ChatFormatting.GRAY)
                    );
                    return 1;
                })
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
                                                .append(Component.literal(". Use /wm back to exit.").withStyle(ChatFormatting.GRAY))
                                );
                            } else {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[EM] Encrypted session started with ")
                                                .withStyle(ChatFormatting.GREEN)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(". Use /wm back to exit.").withStyle(ChatFormatting.GRAY))
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

        // /wm with no args — show help
        wm.executes(ctx -> {
            ctx.getSource().sendFeedback(
                    Component.literal("Whisper Mod — type /wm help for a list of commands.").withStyle(ChatFormatting.AQUA)
            );
            return 1;
        });

        dispatcher.register(wm);
        dispatcher.register(ClientCommands.literal("whispermod").redirect(wm.build()));
    }
}
