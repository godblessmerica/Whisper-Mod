package com.whispermod;

import com.whispermod.command.WmCommand;
import com.whispermod.crypto.MessageCrypto;
import com.whispermod.crypto.SessionManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class WhisperMod implements ClientModInitializer {

    private static String dmTarget = null; // unencrypted session
    private static String emTarget = null; // encrypted session

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                WmCommand.register(dispatcher));

        // Intercept outgoing chat — strip prefix, route to the active session
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            Minecraft mc = Minecraft.getInstance();

            if (emTarget != null) {
                // strip prefix if present
                String prefix = "[EM to " + emTarget + "] ";
                String text = message.startsWith(prefix) ? message.substring(prefix.length()) : message;
                text = text.trim();
                if (text.isEmpty() || text.equals(prefix.trim())) return false;

                if (SessionManager.isReady(emTarget)) {
                    byte[] key = SessionManager.get(emTarget).getSharedKey();
                    String encrypted = MessageCrypto.encrypt(key, text);
                    mc.getConnection().sendUnattendedCommand("w " + emTarget + " " + encrypted, mc.screen);
                } else {
                    mc.player.sendSystemMessage(
                            Component.literal("[EM] Warning: secure session not established yet. Message not sent.")
                                    .withStyle(ChatFormatting.RED)
                    );
                }
                return false;
            }

            if (dmTarget != null) {
                // strip prefix if present
                String prefix = "[DM to " + dmTarget + "] ";
                String text = message.startsWith(prefix) ? message.substring(prefix.length()) : message;
                text = text.trim();
                if (text.isEmpty() || text.equals(prefix.trim())) return false;

                mc.getConnection().sendUnattendedCommand("w " + dmTarget + " " + text, mc.screen);
                return false;
            }

            return true;
        });
    }

    // --- DM ---
    public static void setDmTarget(String player) {
        dmTarget = player;
        if (player != null) emTarget = null; // only one mode active at a time
    }

    public static String getDmTarget() {
        return dmTarget;
    }

    // --- EM ---
    public static void setEmTarget(String player) {
        emTarget = player;
        if (player != null) dmTarget = null; // only one mode active at a time
    }

    public static String getEmTarget() {
        return emTarget;
    }

    // --- /back — exit everything ---
    public static void exitAll() {
        dmTarget = null;
        emTarget = null;
    }

    /** Returns the plain text prefix shown in the chat input, or null if no session is active. */
    public static String getChatPrefix() {
        if (dmTarget != null) return "[DM to " + dmTarget + "] ";
        if (emTarget != null) return "[EM to " + emTarget + "] ";
        return null;
    }
}
