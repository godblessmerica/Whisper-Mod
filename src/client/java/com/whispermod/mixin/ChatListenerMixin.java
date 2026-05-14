package com.whispermod.mixin;

import com.whispermod.crypto.DmSession;
import com.whispermod.crypto.MessageCrypto;
import com.whispermod.crypto.SessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public class ChatListenerMixin {

    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void onSystemMessage(Component message, boolean overlay, CallbackInfo ci) {
        String raw = message.getString();
        Minecraft mc = Minecraft.getInstance();

        // --- Key exchange handshake ---
        if (MessageCrypto.isKeyExchange(raw)) {
            String sender = parseSender(raw);
            if (sender == null) return;

            String token = MessageCrypto.extractToken(raw, MessageCrypto.KX_PREFIX);
            if (token == null) return;
            String theirPublicKey = token.substring(MessageCrypto.KX_PREFIX.length());

            DmSession session = SessionManager.getOrCreate(sender);

            if (!session.isReady()) {
                session.completeExchange(theirPublicKey);

                if (!session.wasInitiated()) {
                    // we're the responder — send our public key back
                    String kxReply = MessageCrypto.KX_PREFIX + session.getPublicKeyBase64();
                    mc.getConnection().sendUnattendedCommand("w " + sender + " " + kxReply, null);
                }

                mc.player.sendSystemMessage(
                        Component.literal("[DM] Secure session established with ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(sender).withStyle(ChatFormatting.AQUA))
                );
            }

            ci.cancel(); // hide raw KX message
            return;
        }

        // --- Encrypted message ---
        if (MessageCrypto.isMessage(raw)) {
            String sender = parseSender(raw);
            if (sender == null) return;

            DmSession session = SessionManager.get(sender);
            if (session == null || !session.isReady()) return; // no session, show as-is

            String token = MessageCrypto.extractToken(raw, MessageCrypto.MSG_PREFIX);
            if (token == null) return;

            String decrypted = MessageCrypto.decrypt(session.getSharedKey(), token);
            if (decrypted == null) return; // decryption failed, show as-is

            String display = raw.replace(token, decrypted);
            mc.player.sendSystemMessage(Component.literal(display).withStyle(ChatFormatting.WHITE));
            ci.cancel(); // hide the encrypted version
        }
    }

    private static String parseSender(String raw) {
        int idx = raw.indexOf(" whispers to you: ");
        if (idx > 0) return raw.substring(0, idx);
        return null;
    }
}
