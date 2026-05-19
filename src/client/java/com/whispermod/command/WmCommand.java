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
                    sendClickable(ctx.getSource(), "/wm dm <player>",       "Start an unencrypted DM session",                  ChatFormatting.YELLOW);
                    sendClickable(ctx.getSource(), "/wm em <player>",       "Send an encrypted session request (friends only)", ChatFormatting.GREEN);
                    sendClickable(ctx.getSource(), "/wm friend <player>",   "Send a friend request",                            ChatFormatting.AQUA);
                    sendClickable(ctx.getSource(), "/wm unfriend <player>", "Remove a friend",                                  ChatFormatting.RED);
                    sendClickable(ctx.getSource(), "/wm friends",           "View your friend list",                            ChatFormatting.AQUA);
                    sendClickable(ctx.getSource(), "/wm pending",           "View pending outgoing friend requests",            ChatFormatting.YELLOW);
                    sendClickable(ctx.getSource(), "/wm status",            "See which friends are online",                     ChatFormatting.AQUA);
                    sendClickable(ctx.getSource(), "/wm muted",             "View muted players and global mute status",        ChatFormatting.YELLOW);
                    sendClickable(ctx.getSource(), "/wm mute",              "Toggle muting all incoming requests",              ChatFormatting.RED);
                    sendClickable(ctx.getSource(), "/wm mute <player>",    "Mute incoming requests from a specific player",    ChatFormatting.RED);
                    sendClickable(ctx.getSource(), "/wm unmute",           "Unmute all incoming requests",                     ChatFormatting.YELLOW);
                    sendClickable(ctx.getSource(), "/wm unmute <player>",  "Unmute incoming requests from a specific player",  ChatFormatting.YELLOW);
                    sendClickable(ctx.getSource(), "/wm block <player>",   "Block someone from sending you requests",          ChatFormatting.RED);
                    sendClickable(ctx.getSource(), "/wm unblock <player>",  "Unblock someone",                                 ChatFormatting.YELLOW);
                    sendClickable(ctx.getSource(), "/wm back",              "Return to public chat",                           ChatFormatting.YELLOW);
                    sendClickable(ctx.getSource(), "/wm clearconfig",       "Clear all saved friends and blocked players",      ChatFormatting.RED);
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

                            if (!isOnline(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" is not online.").withStyle(ChatFormatting.RED))
                                );
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

                            if (!isOnline(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" is not online.").withStyle(ChatFormatting.RED))
                                );
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

                            if (SessionManager.hasOutgoing(player)) {
                                long secs = SessionManager.outgoingRemainingSeconds(player);
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal("You already sent an EM request to ").withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(secs > 0 ? ". Expires in " + secs + "s." : ".").withStyle(ChatFormatting.YELLOW))
                                );
                                return 1;
                            }

                            SessionManager.addOutgoing(player);
                            mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.REQ_PREFIX, mc.screen);

                            ctx.getSource().sendFeedback(
                                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                            .append(Component.literal("Encrypted session request sent to ").withStyle(ChatFormatting.GREEN))
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(". Waiting for them to accept...").withStyle(ChatFormatting.GREEN))
                            );

                            // 30 second timeout
                            new java.util.Timer().schedule(new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    if (SessionManager.hasOutgoing(player)) {
                                        SessionManager.removeOutgoing(player);
                                        mc.execute(() -> mc.player.sendSystemMessage(
                                                Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                        .append(Component.literal("No response from ").withStyle(ChatFormatting.RED))
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
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal("You and ").withStyle(ChatFormatting.GREEN))
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" are now friends!").withStyle(ChatFormatting.GREEN))
                                );
                                return 1;
                            }

                            ctx.getSource().sendFeedback(
                                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                            .append(Component.literal("No pending request from ").withStyle(ChatFormatting.RED))
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
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal("You declined ").withStyle(ChatFormatting.RED))
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
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal("You declined ").withStyle(ChatFormatting.RED))
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal("'s friend request.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            ctx.getSource().sendFeedback(
                                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                            .append(Component.literal("No pending request from ").withStyle(ChatFormatting.WHITE))
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(".").withStyle(ChatFormatting.WHITE))
                            );
                            return 1;
                        })
                )
        );

        // /wm friend <player>
        wm.then(ClientCommands.literal("friend")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("Usage: /wm friend <player>").withStyle(ChatFormatting.GRAY));
                    return 1;
                })
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestPlayers(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            Minecraft mc = Minecraft.getInstance();
                            String localName = mc.player != null ? mc.player.getName().getString() : "";

                            if (player.equalsIgnoreCase(localName)) {
                                ctx.getSource().sendFeedback(Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.literal("You can't friend yourself.").withStyle(ChatFormatting.RED)));
                                return 1;
                            }

                            if (!isOnline(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" is not online.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            if (FriendManager.isFriend(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal("You are already friends with ").withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(".").withStyle(ChatFormatting.YELLOW))
                                );
                                return 1;
                            }

                            if (FriendManager.hasOutgoing(player)) {
                                long secs = FriendManager.outgoingRemainingSeconds(player);
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal("You already sent a friend request to ").withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(secs > 0 ? ". Expires in " + secs + "s." : ".").withStyle(ChatFormatting.YELLOW))
                                );
                                return 1;
                            }

                            if (FriendManager.isBlocked(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal("You have blocked ").withStyle(ChatFormatting.RED))
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
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal("You and ").withStyle(ChatFormatting.GREEN))
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" are now friends!").withStyle(ChatFormatting.GREEN))
                                );
                                return 1;
                            }

                            FriendManager.addOutgoing(player);
                            String myName = mc.player != null ? mc.player.getName().getString() : "you";
                            mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.FRIEND_REQ_PREFIX + myName, mc.screen);

                            ctx.getSource().sendFeedback(
                                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                            .append(Component.literal("Friend request sent to ").withStyle(ChatFormatting.GREEN))
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
                            );

                            // 60 second timeout
                            new java.util.Timer().schedule(new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    if (FriendManager.hasOutgoing(player)) {
                                        FriendManager.removeOutgoing(player);
                                        mc.execute(() -> mc.player.sendSystemMessage(
                                                Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                        .append(Component.literal("Friend request to ").withStyle(ChatFormatting.RED))
                                                        .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal(" expired.").withStyle(ChatFormatting.RED))
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
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" is not in your friend list.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            if (!isOnline(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" is not online. You can only unfriend online players.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            FriendManager.removeFriend(player);

                            // If in EM session with them, end it
                            if (player.equalsIgnoreCase(WhisperMod.getEmTarget())) {
                                WhisperMod.exitAll();
                                mc.player.sendSystemMessage(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal("Encrypted session with ").withStyle(ChatFormatting.RED))
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" ended — you are no longer friends.").withStyle(ChatFormatting.RED))
                                );
                            }

                            // Notify them
                            String myName = mc.player != null ? mc.player.getName().getString() : "you";
                            mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.UNFRIEND_PREFIX + myName, mc.screen);

                            ctx.getSource().sendFeedback(
                                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                            .append(Component.literal("You unfriended ").withStyle(ChatFormatting.RED))
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

        // /wm status
        wm.then(ClientCommands.literal("status")
                .executes(ctx -> {
                    if (FriendManager.isMutedAll()) {
                        ctx.getSource().sendFeedback(Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                .append(Component.literal("⚠ All incoming requests are currently muted.").withStyle(ChatFormatting.RED)));
                    }
                    Set<String> friends = FriendManager.getFriends();
                    if (friends.isEmpty()) {
                        ctx.getSource().sendFeedback(Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                .append(Component.literal("You have no friends yet.").withStyle(ChatFormatting.GRAY)));
                        return 1;
                    }
                    ctx.getSource().sendFeedback(Component.literal("--- Friend Status ---").withStyle(ChatFormatting.LIGHT_PURPLE));
                    for (String friend : friends) {
                        boolean online = isOnline(friend);
                        ctx.getSource().sendFeedback(
                                Component.literal("• ").withStyle(online ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                                        .append(Component.literal(friend).withStyle(ChatFormatting.AQUA))
                                        .append(Component.literal(online ? " — Online" : " — Offline").withStyle(online ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                        );
                    }
                    return 1;
                })
        );

        // /wm mute [player]
        wm.then(ClientCommands.literal("mute")
                .executes(ctx -> {
                    if (FriendManager.isMutedAll()) {
                        ctx.getSource().sendFeedback(
                                Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                        .append(Component.literal("All incoming requests are already muted. Use ").withStyle(ChatFormatting.RED))
                                        .append(Component.literal("/wm unmute").withStyle(ChatFormatting.YELLOW))
                                        .append(Component.literal(" to unmute.").withStyle(ChatFormatting.RED))
                        );
                        return 1;
                    }
                    FriendManager.setMutedAll(true);
                    ctx.getSource().sendFeedback(
                            Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                    .append(Component.literal("All incoming requests muted.").withStyle(ChatFormatting.RED))
                    );
                    return 1;
                })
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestPlayers(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            if (FriendManager.isMuted(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" is already muted.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }
                            FriendManager.mute(player);
                            ctx.getSource().sendFeedback(
                                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                            .append(Component.literal("Muted incoming requests from ").withStyle(ChatFormatting.RED))
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(".").withStyle(ChatFormatting.RED))
                            );
                            return 1;
                        })
                )
        );

        // /wm unmute [player]
        wm.then(ClientCommands.literal("unmute")
                .executes(ctx -> {
                    if (!FriendManager.isMutedAll()) {
                        ctx.getSource().sendFeedback(Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                .append(Component.literal("Incoming requests are not globally muted.").withStyle(ChatFormatting.RED)));
                        return 1;
                    }
                    FriendManager.setMutedAll(false);
                    ctx.getSource().sendFeedback(Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                            .append(Component.literal("Incoming requests unmuted.").withStyle(ChatFormatting.GREEN)));
                    return 1;
                })
                .then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestMuted(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            if (!FriendManager.isMuted(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" is not muted.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }
                            FriendManager.unmute(player);
                            ctx.getSource().sendFeedback(
                                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                            .append(Component.literal("Unmuted incoming requests from ").withStyle(ChatFormatting.GREEN))
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
                            );
                            return 1;
                        })
                )
        );

        // /wm muted
        wm.then(ClientCommands.literal("muted")
                .executes(ctx -> {
                    Set<String> muted = FriendManager.getMuted();
                    if (FriendManager.isMutedAll()) {
                        ctx.getSource().sendFeedback(Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                .append(Component.literal("All incoming requests are currently muted.").withStyle(ChatFormatting.RED)));
                    }
                    if (muted.isEmpty() && !FriendManager.isMutedAll()) {
                        ctx.getSource().sendFeedback(Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                .append(Component.literal("You have no muted players.").withStyle(ChatFormatting.GRAY)));
                    } else if (!muted.isEmpty()) {
                        ctx.getSource().sendFeedback(Component.literal("--- Muted Players ---").withStyle(ChatFormatting.LIGHT_PURPLE));
                        for (String m : muted) {
                            ctx.getSource().sendFeedback(Component.literal("• " + m).withStyle(ChatFormatting.WHITE));
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
                                ctx.getSource().sendFeedback(Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.literal("You can't block yourself.").withStyle(ChatFormatting.RED)));
                                return 1;
                            }

                            if (FriendManager.isBlocked(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" is already blocked.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            if (!isOnline(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" is not online.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            // Unfriend if needed, then always notify they're blocked
                            if (FriendManager.isFriend(player)) {
                                FriendManager.removeFriend(player);
                                if (player.equalsIgnoreCase(WhisperMod.getEmTarget())) {
                                    WhisperMod.exitAll();
                                }
                                String myName = mc.player != null ? mc.player.getName().getString() : "";
                                mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.UNFRIEND_PREFIX + myName, mc.screen);
                            }
                            mc.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.FRIEND_BLOCKED_PREFIX, mc.screen);

                            FriendManager.block(player);
                            ctx.getSource().sendFeedback(
                                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                            .append(Component.literal("You blocked ").withStyle(ChatFormatting.RED))
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
                        .suggests((ctx, builder) -> suggestBlocked(builder))
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");

                            if (!FriendManager.isBlocked(player)) {
                                ctx.getSource().sendFeedback(
                                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                                .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal(" is not blocked.").withStyle(ChatFormatting.RED))
                                );
                                return 1;
                            }

                            Minecraft mc2 = Minecraft.getInstance();
                            if (mc2.getConnection() != null) {
                                mc2.getConnection().sendUnattendedCommand("w " + player + " " + MessageCrypto.UNBLOCK_PREFIX, mc2.screen);
                            }
                            FriendManager.unblock(player);
                            ctx.getSource().sendFeedback(
                                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                            .append(Component.literal("You unblocked ").withStyle(ChatFormatting.GREEN))
                                            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
                            );
                            return 1;
                        })
                )
        );

        // /wm clearconfig — disabled until offline notification is solved
        wm.then(ClientCommands.literal("clearconfig")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(
                            Component.literal("⚠ /wm clearconfig is currently disabled. Offline friends cannot be notified when you clear your config.")
                                    .withStyle(ChatFormatting.RED)
                    );
                    return 1;
                })
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
                Component.literal("• ").withStyle(color)
                        .append(Component.literal(command).withStyle(Style.EMPTY
                                .withColor(ChatFormatting.WHITE)
                                .withClickEvent(new ClickEvent.SuggestCommand(command))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to use")))))
                        .append(Component.literal(" — " + description).withStyle(ChatFormatting.GRAY))
        );
    }

    private static boolean isOnline(String player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return false;
        return mc.getConnection().getOnlinePlayers().stream()
                .anyMatch(info -> info.getProfile().name().equalsIgnoreCase(player));
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

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestMuted(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (String muted : FriendManager.getMuted()) {
            if (muted.toLowerCase().startsWith(remaining)) {
                builder.suggest(muted);
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestBlocked(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (String blocked : FriendManager.getBlocked()) {
            if (blocked.toLowerCase().startsWith(remaining)) {
                builder.suggest(blocked);
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestFriends(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (String friend : FriendManager.getFriends()) {
            if (friend.toLowerCase().startsWith(remaining) && isOnline(friend)) {
                builder.suggest(friend);
            }
        }
        return builder.buildFuture();
    }
}
