package com.whispermod;

import com.whispermod.command.WmCommand;
import com.whispermod.crypto.MessageCrypto;
import com.whispermod.crypto.SessionManager;
import com.whispermod.friends.FriendManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WhisperMod implements ClientModInitializer {

    private static String dmTarget = null;
    private static String emTarget = null;
    private static String reTarget = null;

    /** Tracks WM tokens we sent so echoes can be detected and cancelled. */
    private static final Set<String> sentTokens = Collections.synchronizedSet(new HashSet<>());

    public static void trackSent(String token) {
        sentTokens.add(token);
    }

    public static boolean consumeSent(String token) {
        return sentTokens.remove(token);
    }

    @Override
    public void onInitializeClient() {
        FriendManager.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                WmCommand.register(dispatcher));

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            Minecraft mc = Minecraft.getInstance();

            // Reply mode — send one whisper then clear
            if (reTarget != null) {
                String text = message.trim();
                if (text.isEmpty()) return false;
                mc.getConnection().sendUnattendedCommand("msg " + reTarget + " " + text, mc.screen);
                trackSent("DM:" + text);
                String myName = mc.player.getName().getString();
                mc.player.sendSystemMessage(
                        Component.literal("[DM] ").withStyle(ChatFormatting.YELLOW)
                                .append(Component.literal(myName + ": ").withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(text).withStyle(ChatFormatting.WHITE))
                );
                reTarget = null;
                return false;
            }

            if (emTarget != null) {
                String prefix = "[EM to " + emTarget + "] ";
                String text = message.startsWith(prefix) ? message.substring(prefix.length()) : message;
                text = text.trim();
                if (text.isEmpty()) return false;

                if (SessionManager.isReady(emTarget)) {
                    byte[] key = SessionManager.get(emTarget).getSharedKey();
                    String encrypted = MessageCrypto.encrypt(key, text);

                    // Track echo so we can cancel it when it comes back
                    trackSent(encrypted);

                    mc.getConnection().sendUnattendedCommand("msg " + emTarget + " " + encrypted, mc.screen);

                    // Show own message immediately — don't wait for echo
                    String myName = mc.player.getName().getString();
                    mc.player.sendSystemMessage(
                            Component.literal("[EM] ").withStyle(ChatFormatting.GREEN)
                                    .append(Component.literal(myName + ": ").withStyle(ChatFormatting.AQUA))
                                    .append(Component.literal(text).withStyle(ChatFormatting.WHITE))
                    );
                } else {
                    mc.player.sendSystemMessage(
                            Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                    .append(Component.literal("Warning: secure session not established yet. Message not sent.").withStyle(ChatFormatting.RED))
                    );
                }
                return false;
            }

            if (dmTarget != null) {
                String prefix = "[DM to " + dmTarget + "] ";
                String text = message.startsWith(prefix) ? message.substring(prefix.length()) : message;
                text = text.trim();
                if (text.isEmpty()) return false;

                mc.getConnection().sendUnattendedCommand("msg " + dmTarget + " " + text, mc.screen);

                // Track sent text so we can cancel the server echo
                trackSent("DM:" + text);

                // Show own message immediately
                String myName = mc.player.getName().getString();
                mc.player.sendSystemMessage(
                        Component.literal("[DM] ").withStyle(ChatFormatting.YELLOW)
                                .append(Component.literal(myName + ": ").withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(text).withStyle(ChatFormatting.WHITE))
                );
                return false;
            }

            return true;
        });
    }

    // --- DM ---
    public static void setDmTarget(String player) {
        dmTarget = player;
        if (player != null) emTarget = null;
    }

    public static String getDmTarget() {
        return dmTarget;
    }

    // --- EM ---
    public static void setEmTarget(String player) {
        emTarget = player;
        if (player != null) dmTarget = null;
    }

    public static String getEmTarget() {
        return emTarget;
    }

    // --- Reply (one-shot) ---
    public static void setReTarget(String player) { reTarget = player; }
    public static String getReTarget() { return reTarget; }
    public static void clearReTarget() { reTarget = null; }

    // --- Exit without notifying the other player (used when receiving WMEND) ---
    public static void exitAllSilent() {
        if (emTarget != null) SessionManager.remove(emTarget);
        dmTarget = null;
        emTarget = null;
        reTarget = null;
    }

    // --- /back — exit everything ---
    public static void exitAll() {
        Minecraft mc = Minecraft.getInstance();
        if (emTarget != null) {
            if (mc.getConnection() != null) {
                String endMsg = MessageCrypto.END_PREFIX;
                trackSent(endMsg);
                mc.getConnection().sendUnattendedCommand("msg " + emTarget + " " + endMsg, mc.screen);
            }
            SessionManager.remove(emTarget);
        }
        dmTarget = null;
        emTarget = null;
        reTarget = null;
    }

    public static String getChatPrefix() {
        if (reTarget != null) return "[Re to " + reTarget + "] ";
        if (dmTarget != null) return "[DM to " + dmTarget + "] ";
        if (emTarget != null) return "[EM to " + emTarget + "] ";
        return null;
    }
}
