package com.whispermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.whispermod.WhisperMod;
import com.whispermod.crypto.DmSession;
import com.whispermod.crypto.MessageCrypto;
import com.whispermod.crypto.SessionManager;
import com.whispermod.friends.FriendManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class WmCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> wm = ClientCommands.literal("wm");

        // /wm help
        wm.then(ClientCommands.literal("help")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("--- Whisper Mod Commands ---").withStyle(ChatFormatting.AQUA));
                    sendClickable(ctx.getSource(), "/wm dm <player>", "Start an unencrypted DM session", ChatFormatting.YELLOW);
                    sendClickable(ctx.getSource(), "/wm em <player>", "Send an encrypted session request (friends only)", ChatFormatting.GREEN);
                    sendClickable(ctx.getSource(), "/wm accept <player>", "Accept a friend or EM request", ChatFormatting.GREEN);
                    sendClickable(ctx.getSource(), "/wm decline <player>", "Decline a friend or EM request", ChatFormatting.RED);
                    sendClickable(ctx.getSource(), "/wm addfriend <player>", "Send a friend request", ChatFormatting.AQUA);
                    sendClickable(ctx.getSource(), "/wm unfriend <player>", "Remove a friend", ChatFormatting.RED);
                    sendClickable(ctx.getSource(), "/wm friends", "View your friend list", ChatFormatting.AQUA);
                    sendClickable(ctx.getSource(), "/wm pending", "View pending outgoing friend requests", ChatFormatting.YELLOW);
                    sendClickable(ctx.getSource(), "/wm block <player>", "Block someone from sending you requests", ChatFormatting.RED);
                    sendClickable(ctx.getSource(), "/wm unblock <player>", "Unblock someone", ChatFormatting.YELLOW);
                    sendClickable(ctx.getSource(), "/wm back", "Return to public chat", ChatFormatting.YELLOW);
                    ctx.getSource().sendFeedback(Component.literal("Note: Friend list is stored locally — reinstalling the mod will clear it.").withStyle(ChatFormatting.DARK_GRAY));
                    return 1;
                })
        );

        // /wm back
        wm.then(ClientCommands.literal("back")
                .executes(ctx -> {
                    if (WhisperMod.getDmTarget() == null && WhisperMod.getEmTarget() == null) {
                        ctx.getSource().sendFeedback(Component.literal("You're already in public chat.").withStyle(ChatFormatting.GRAY));
                    } else {
                        WhisperMod.exitAll();
                        ctx.getSource().sendFeedback(Component.literal("Returned to public chat.").withStyle(ChatFormatting.YELLOW));
                    }
                    return 1;
                })
        );

        // /wm dm <player>
        wm.then(ClientCommands.literal("dm")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("Usage: /wm dm <player>").withStyle(ChatFormatting.GRAY));
                    return 1;
                })
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestPlayers(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            Minecraft mc = Minecraft.getInstance();
                            String localName = mc.player != null ? mc.player.getName().getString() : "";

                            if (player.equalsIgnoreCase(localName)) {
                                ctx.getSource().sendFeedback(Component.literal("You can't DM yourself.").withStyle(ChatFormatting.RED));
                                return 1;
                            }

                            if (player.equalsIgnoreCase(WhisperMod.getDmTarget())) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("Already in a DM with ").withStyle(ChatFormatting.YELLOW)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(".").withStyle(ChatFormatting.YELLOW))
                                );
                                return 1;
                            }

                            String current = WhisperMod.getDmTarget();
                            ctx.getSource().sendFeedback(
                                    Component.literal(current != null ? "[DM] Switched to " : "[DM] Now chatting with ").withStyle(ChatFormatting.YELLOW)
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(". Use /wm back to exit.").withStyle(ChatFormatting.GRAY))
                            );
                            WhisperMod.setDmTarget(player);
                            return 1;
                        })
                )
        );

        // /wm em <player>
        wm.then(ClientCommands.literal("em")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("Usage: /wm em <player> (must be friends first)").withStyle(ChatFormatting.GRAY));
                    return 1;
                })
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestFriends(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            Minecraft mc = Minecraft.getInstance();
                            String localName = mc.player != null ? mc.player.getName().getString() : "";

                            if (player.equalsIgnoreCase(localName)) {
                                ctx.getSource().sendFeedback(Component.literal("You can't EM yourself.").withStyle(ChatFormatting.RED));
                                return 1;
                            }

                            if (!FriendManager.isFriend(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("You must be friends with ").withStyle(ChatFormatting.RED)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" before starting an encrypted session.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            if (player.equalsIgnoreCase(WhisperMod.getEmTarget())) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("Already in an encrypted session with ").withStyle(ChatFormatting.YELLOW)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(".").withStyle(ChatFormatting.YELLOW))
                                );
                                return 1;
                            }

                            if (FriendManager.isBlocked(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("You have blocked ").withStyle(ChatFormatting.RED)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(". Unblock them first.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            SessionManager.addOutgoing(player);
                            mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.REQ_PREFIX, mc.screen);

                            ctx.getSource().sendFeedback(
                                    Component.literal("[EM] Encrypted session request sent to ").withStyle(ChatFormatting.GREEN)
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(". Waiting for them to accept...").withStyle(ChatFormatting.GRAY))
                            );

                            // 30 second timeout
                            new java.util.Timer().schedule(new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    if (SessionManager.hasOutgoing(player)) {
                                        SessionManager.removeOutgoing(player);
                                        mc.execute(() -> mc.player.sendSystemMessage(
                                                Component.literal("[EM] No response from ").withStyle(ChatFormatting.RED)
                                                        .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal(" — they may not have the mod installed.").withStyle(ChatFormatting.RED))
                                        ));
                                    }
                                }
                            }, 30_000);

                            return 1;
                        })
                )
        );

        // /wm accept <player>
        wm.then(ClientCommands.literal("accept")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("Usage: /wm accept <player>").withStyle(ChatFormatting.GRAY));
                    return 1;
                })
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestPlayers(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            Minecraft mc = Minecraft.getInstance();

                            // Accept EM request
                            if (SessionManager.hasIncoming(player)) {
                                if (!FriendManager.isFriend(player)) {
                                    ctx.getSource().sendFeedback(
                                            Component.literal("You must be friends with ").withStyle(ChatFormatting.RED)
                                                    .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal(" to accept an EM session.").withStyle(ChatFormatting.RED))
                                    );
                                    return 1;
                                }
                                SessionManager.removeIncoming(player);
                                WhisperMod.setEmTarget(player);
                                DmSession session = SessionManager.getOrCreate(player);
                                String kxMessage = MessageCrypto.KX_PREFIX + session.getPublicKeyBase64();
                                mc.getConnection().sendUnattendedCommand("w " + player + " " + kxMessage, mc.screen);
                                ctx.getSource().sendFeedback(
                                        Component.literal("[EM] Accepted. Establishing secure session with ").withStyle(ChatFormatting.GREEN)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal("...").withStyle(ChatFormatting.GRAY))
                                );
                                return 1;
                            }

                            // Accept friend request
                            if (FriendManager.hasIncoming(player)) {
                                FriendManager.removeIncoming(player);
                                FriendManager.addFriend(player);
                                String myName = mc.player != null ? mc.player.getName().getString() : "you";
                                // Notify sender via protocol message
                                mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.FRIEND_ACCEPT_PREFIX + myName, mc.screen);
                                ctx.getSource().sendFeedback(
                                        Component.literal("You and ").withStyle(ChatFormatting.GREEN)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" are now friends!").withStyle(ChatFormatting.GREEN))
                                );
                                return 1;
                            }

                            ctx.getSource().sendFeedback(
                                    Component.literal("No pending request from ").withStyle(ChatFormatting.RED)
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(".").withStyle(ChatFormatting.RED))
                            );
                            return 1;
                        })
                )
        );

        // /wm decline <player>
        wm.then(ClientCommands.literal("decline")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("Usage: /wm decline <player>").withStyle(ChatFormatting.GRAY));
                    return 1;
                })
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestPlayers(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            Minecraft mc = Minecraft.getInstance();

                            // Decline EM request
                            if (SessionManager.hasIncoming(player)) {
                                SessionManager.removeIncoming(player);
                                mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.DECL_PREFIX, mc.screen);
                                ctx.getSource().sendFeedback(
                                        Component.literal("You declined ").withStyle(ChatFormatting.RED)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal("'s encrypted session request.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            // Decline friend request
                            if (FriendManager.hasIncoming(player)) {
                                FriendManager.removeIncoming(player);
                                String myName = mc.player != null ? mc.player.getName().getString() : "you";
                                mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.FRIEND_DECLINE_PREFIX + myName, mc.screen);
                                ctx.getSource().sendFeedback(
                                        Component.literal("You declined ").withStyle(ChatFormatting.RED)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal("'s friend request.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            ctx.getSource().sendFeedback(
                                    Component.literal("No pending request from ").withStyle(ChatFormatting.RED)
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(".").withStyle(ChatFormatting.RED))
                            );
                            return 1;
                        })
                )
        );

        // /wm addfriend <player>
        wm.then(ClientCommands.literal("addfriend")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("Usage: /wm addfriend <player>").withStyle(ChatFormatting.GRAY));
                    return 1;
                })
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestPlayers(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            Minecraft mc = Minecraft.getInstance();
                            String localName = mc.player != null ? mc.player.getName().getString() : "";

                            if (player.equalsIgnoreCase(localName)) {
                                ctx.getSource().sendFeedback(Component.literal("You can't friend yourself.").withStyle(ChatFormatting.RED));
                                return 1;
                            }

                            if (FriendManager.isFriend(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("You are already friends with ").withStyle(ChatFormatting.YELLOW)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(".").withStyle(ChatFormatting.YELLOW))
                                );
                                return 1;
                            }

                            if (FriendManager.hasOutgoing(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("You already sent a friend request to ").withStyle(ChatFormatting.YELLOW)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(".").withStyle(ChatFormatting.YELLOW))
                                );
                                return 1;
                            }

                            if (FriendManager.isBlocked(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("You have blocked ").withStyle(ChatFormatting.RED)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(". Unblock them first.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            // If they already sent us a request, auto-accept
                            if (FriendManager.hasIncoming(player)) {
                                FriendManager.removeIncoming(player);
                                FriendManager.addFriend(player);
                                String myName = mc.player != null ? mc.player.getName().getString() : "you";
                                mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.FRIEND_ACCEPT_PREFIX + myName, mc.screen);
                                ctx.getSource().sendFeedback(
                                        Component.literal("You and ").withStyle(ChatFormatting.GREEN)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" are now friends!").withStyle(ChatFormatting.GREEN))
                                );
                                return 1;
                            }

                            FriendManager.addOutgoing(player);
                            String myName = mc.player != null ? mc.player.getName().getString() : "you";
                            mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.FRIEND_REQ_PREFIX + myName, mc.screen);

                            ctx.getSource().sendFeedback(
                                    Component.literal("Friend request sent to ").withStyle(ChatFormatting.AQUA)
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(".").withStyle(ChatFormatting.AQUA))
                            );

                            // 60 second timeout
                            new java.util.Timer().schedule(new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    if (FriendManager.hasOutgoing(player)) {
                                        FriendManager.removeOutgoing(player);
                                        mc.execute(() -> mc.player.sendSystemMessage(
                                                Component.literal("Your friend request to ").withStyle(ChatFormatting.YELLOW)
                                                        .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal(" expired.").withStyle(ChatFormatting.YELLOW))
                                        ));
                                    }
                                }
                            }, 60_000);

                            return 1;
                        })
                )
        );

        // /wm unfriend <player>
        wm.then(ClientCommands.literal("unfriend")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("Usage: /wm unfriend <player>").withStyle(ChatFormatting.GRAY));
                    return 1;
                })
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestFriends(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            Minecraft mc = Minecraft.getInstance();

                            if (!FriendManager.isFriend(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal(player).withStyle(ChatFormatting.AQUA)
                                                .append(Component.literal(" is not in your friend list.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            FriendManager.removeFriend(player);

                            // If in EM session with them, end it
                            if (player.equalsIgnoreCase(WhisperMod.getEmTarget())) {
                                WhisperMod.exitAll();
                                mc.player.sendSystemMessage(
                                        Component.literal("Your encrypted session with ").withStyle(ChatFormatting.RED)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" ended because you are no longer friends.").withStyle(ChatFormatting.RED))
                                );
                            }

                            // Notify them
                            String myName = mc.player != null ? mc.player.getName().getString() : "you";
                            mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.UNFRIEND_PREFIX + myName, mc.screen);

                            ctx.getSource().sendFeedback(
                                    Component.literal("You unfriended ").withStyle(ChatFormatting.RED)
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(".").withStyle(ChatFormatting.RED))
                            );
                            return 1;
                        })
                )
        );

        // /wm friends
        wm.then(ClientCommands.literal("friends")
                .executes(ctx -> {
                    Set<String> friends = FriendManager.getFriends();
                    if (friends.isEmpty()) {
                        ctx.getSource().sendFeedback(Component.literal("Your friend list is empty.").withStyle(ChatFormatting.GRAY));
                    } else {
                        ctx.getSource().sendFeedback(Component.literal("--- Friends ---").withStyle(ChatFormatting.AQUA));
                        for (String f : friends) {
                            ctx.getSource().sendFeedback(Component.literal("• " + f).withStyle(ChatFormatting.WHITE));
                        }
                    }
                    ctx.getSource().sendFeedback(Component.literal("Note: Friend list is stored locally — reinstalling the mod will clear it.").withStyle(ChatFormatting.DARK_GRAY));
                    return 1;
                })
        );

        // /wm pending
        wm.then(ClientCommands.literal("pending")
                .executes(ctx -> {
                    Set<String> pending = FriendManager.getOutgoing();
                    if (pending.isEmpty()) {
                        ctx.getSource().sendFeedback(Component.literal("You have no pending friend requests.").withStyle(ChatFormatting.GRAY));
                    } else {
                        ctx.getSource().sendFeedback(Component.literal("--- Pending Friend Requests ---").withStyle(ChatFormatting.AQUA));
                        for (String p : pending) {
                            ctx.getSource().sendFeedback(Component.literal("• " + p).withStyle(ChatFormatting.WHITE));
                        }
                    }
                    return 1;
                })
        );

        // /wm block <player>
        wm.then(ClientCommands.literal("block")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("Usage: /wm block <player>").withStyle(ChatFormatting.GRAY));
                    return 1;
                })
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestPlayers(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            Minecraft mc = Minecraft.getInstance();
                            String localName = mc.player != null ? mc.player.getName().getString() : "";

                            if (player.equalsIgnoreCase(localName)) {
                                ctx.getSource().sendFeedback(Component.literal("You can't block yourself.").withStyle(ChatFormatting.RED));
                                return 1;
                            }

                            if (FriendManager.isBlocked(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal(player).withStyle(ChatFormatting.AQUA)
                                                .append(Component.literal(" is already blocked.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            // Unfriend if needed
                            if (FriendManager.isFriend(player)) {
                                FriendManager.removeFriend(player);
                                if (player.equalsIgnoreCase(WhisperMod.getEmTarget())) {
                                    WhisperMod.exitAll();
                                }
                            }

                            FriendManager.block(player);
                            ctx.getSource().sendFeedback(
                                    Component.literal("You blocked ").withStyle(ChatFormatting.RED)
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(". They can no longer send you requests.").withStyle(ChatFormatting.RED))
                            );
                            return 1;
                        })
                )
        );

        // /wm unblock <player>
        wm.then(ClientCommands.literal("unblock")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("Usage: /wm unblock <player>").withStyle(ChatFormatting.GRAY));
                    return 1;
                })
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestPlayers(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");

                            if (!FriendManager.isBlocked(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal(player).withStyle(ChatFormatting.AQUA)
                                                .append(Component.literal(" is not blocked.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            FriendManager.unblock(player);
                            ctx.getSource().sendFeedback(
                                    Component.literal("You unblocked ").withStyle(ChatFormatting.YELLOW)
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(".").withStyle(ChatFormatting.YELLOW))
                            );
                            return 1;
                        })
                )
        );

        // /wm with no args
        wm.executes(ctx -> {
            ctx.getSource().sendFeedback(Component.literal("Whisper Mod — type /wm help for a list of commands.").withStyle(ChatFormatting.AQUA));
            return 1;
        });

        dispatcher.register(wm);
        dispatcher.register(ClientCommands.literal("whispermod").redirect(wm.build()));
    }

    private static void sendClickable(FabricClientCommandSource source, String command, String description, ChatFormatting color) {
        source.sendFeedback(
                Component.literal(command).withStyle(Style.EMPTY
                        .withColor(color)
                        .withClickEvent(new ClickEvent.SuggestCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to use"))))
                        .append(Component.literal(" — " + description).withStyle(ChatFormatting.GRAY))
        );
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestPlayers(
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

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestFriends(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (String friend : FriendManager.getFriends()) {
            if (friend.toLowerCase().startsWith(remaining)) {
                builder.suggest(friend);
            }
        }
        return builder.buildFuture();
    }
}
