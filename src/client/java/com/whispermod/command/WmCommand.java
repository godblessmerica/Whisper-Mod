package com.whispermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.whispermod.WhisperMod;
import com.whispermod.crypto.DmSession;
import com.whispermod.crypto.MessageCrypto;
import com.whispermod.crypto.SessionManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class WmCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> wm = ClientCommands.literal("wm");

        // /wm help
        wm.then(ClientCommands.literal("help")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("--- Whisper Mod Commands ---").withStyle(ChatFormatting.AQUA));
                    ctx.getSource().sendFeedback(
                            Component.literal("/wm dm <player>").withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal(" — start an unencrypted DM session").withStyle(ChatFormatting.GRAY))
                    );
                    ctx.getSource().sendFeedback(
                            Component.literal("/wm em <player>").withStyle(ChatFormatting.GREEN)
                                    .append(Component.literal(" — send an encrypted session request").withStyle(ChatFormatting.GRAY))
                    );
                    ctx.getSource().sendFeedback(
                            Component.literal("/wm em accept <player>").withStyle(ChatFormatting.GREEN)
                                    .append(Component.literal(" — accept an encrypted session request").withStyle(ChatFormatting.GRAY))
                    );
                    ctx.getSource().sendFeedback(
                            Component.literal("/wm em decline <player>").withStyle(ChatFormatting.RED)
                                    .append(Component.literal(" — decline an encrypted session request").withStyle(ChatFormatting.GRAY))
                    );
                    ctx.getSource().sendFeedback(
                            Component.literal("/wm back").withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal(" — return to public chat").withStyle(ChatFormatting.GRAY))
                    );
                    ctx.getSource().sendFeedback(
                            Component.literal("/wm help").withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal(" — show this list").withStyle(ChatFormatting.GRAY))
                    );
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
                        .suggests((ctx, builder) -> suggestPlayers(builder))
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

                            ctx.getSource().sendFeedback(
                                    Component.literal(current != null ? "[DM] Switched to " : "[DM] Now chatting with ")
                                            .withStyle(ChatFormatting.YELLOW)
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(". Use /wm back to exit.").withStyle(ChatFormatting.GRAY))
                            );

                            WhisperMod.setDmTarget(player);
                            return 1;
                        })
                )
        );

        // /wm em
        LiteralArgumentBuilder<FabricClientCommandSource> em = ClientCommands.literal("em");

        em.executes(ctx -> {
            ctx.getSource().sendFeedback(
                    Component.literal("Usage: /wm em <player>. Use /wm back to return to public chat.").withStyle(ChatFormatting.GRAY)
            );
            return 1;
        });

        // /wm em accept <player>
        em.then(ClientCommands.literal("accept")
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestPlayers(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            Minecraft mc = Minecraft.getInstance();

                            if (!SessionManager.hasIncoming(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("No pending EM request from ")
                                                .withStyle(ChatFormatting.RED)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                );
                                return 1;
                            }

                            SessionManager.removeIncoming(player);
                            WhisperMod.setEmTarget(player);

                            // Send our public key to initiate key exchange
                            DmSession session = SessionManager.getOrCreate(player);
                            String kxMessage = MessageCrypto.KX_PREFIX + session.getPublicKeyBase64();
                            mc.getConnection().sendCommand("w " + player + " " + kxMessage);

                            ctx.getSource().sendFeedback(
                                    Component.literal("[EM] Accepted. Establishing secure session with ")
                                            .withStyle(ChatFormatting.GREEN)
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal("...").withStyle(ChatFormatting.GRAY))
                            );
                            return 1;
                        })
                )
        );

        // /wm em decline <player>
        em.then(ClientCommands.literal("decline")
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestPlayers(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            Minecraft mc = Minecraft.getInstance();

                            if (!SessionManager.hasIncoming(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("No pending EM request from ")
                                                .withStyle(ChatFormatting.RED)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                );
                                return 1;
                            }

                            SessionManager.removeIncoming(player);
                            mc.getConnection().sendCommand("w " + player + " " + MessageCrypto.DECL_PREFIX);

                            ctx.getSource().sendFeedback(
                                    Component.literal("[EM] Declined request from ")
                                            .withStyle(ChatFormatting.RED)
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                            );
                            return 1;
                        })
                )
        );

        // /wm em <player> — send request
        em.then(ClientCommands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> suggestPlayers(builder))
                .executes(ctx -> {
                    String player = StringArgumentType.getString(ctx, "player");
                    Minecraft mc = Minecraft.getInstance();

                    if (player.equalsIgnoreCase(WhisperMod.getEmTarget())) {
                        ctx.getSource().sendFeedback(
                                Component.literal("Already in an encrypted session with ")
                                        .withStyle(ChatFormatting.YELLOW)
                                        .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                        .append(Component.literal(".").withStyle(ChatFormatting.YELLOW))
                        );
                        return 1;
                    }

                    // Send encrypted session request
                    SessionManager.addOutgoing(player);
                    mc.getConnection().sendCommand("w " + player + " " + MessageCrypto.REQ_PREFIX);

                    ctx.getSource().sendFeedback(
                            Component.literal("[EM] Encrypted session request sent to ")
                                    .withStyle(ChatFormatting.GREEN)
                                    .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                    .append(Component.literal(". Waiting for them to accept...").withStyle(ChatFormatting.GRAY))
                    );
                    return 1;
                })
        );

        wm.then(em);

        // /wm with no args
        wm.executes(ctx -> {
            ctx.getSource().sendFeedback(
                    Component.literal("Whisper Mod — type /wm help for a list of commands.").withStyle(ChatFormatting.AQUA)
            );
            return 1;
        });

        dispatcher.register(wm);
        dispatcher.register(ClientCommands.literal("whispermod").redirect(wm.build()));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestPlayers(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
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
    }
}
