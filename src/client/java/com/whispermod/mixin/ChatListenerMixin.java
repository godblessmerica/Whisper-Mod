package com.whispermod.mixin;

import com.whispermod.WhisperMod;
import com.whispermod.crypto.DmSession;
import com.whispermod.crypto.MessageCrypto;
import com.whispermod.crypto.SessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public class ChatListenerMixin {

    // Whispers in 26.1.2 come through handleDisguisedChatMessage
    @Inject(method = "handleDisguisedChatMessage", at = @At("HEAD"), cancellable = true)
    private void onDisguisedChatMessage(Component message, ChatType.Bound bound, CallbackInfo ci) {
        String raw = message.getString();
        String sender = bound.name().getString();
        handleIncoming(raw, sender, ci);
    }

    // Fallback for system messages
    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void onSystemMessage(Component message, boolean overlay, CallbackInfo ci) {
        String raw = message.getString();
        String sender = parseSender(raw);
        if (sender == null) return;
        handleIncoming(raw, sender, ci);
    }

    private static void handleIncoming(String raw, String sender, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        // --- EM session request ---
        if (MessageCrypto.isRequest(raw)) {
            SessionManager.addIncoming(sender);
            mc.player.sendSystemMessage(
                    Component.literal("[EM] ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(sender).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" wants to start an encrypted session. ").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal("/wm em accept " + sender).withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" or ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("/wm em decline " + sender).withStyle(ChatFormatting.RED))
            );
            ci.cancel();
            return;
        }

        // --- Decline ---
        if (MessageCrypto.isDecline(raw)) {
            SessionManager.removeOutgoing(sender);
            SessionManager.remove(sender);
            mc.player.sendSystemMessage(
                    Component.literal("[EM] ").withStyle(ChatFormatting.RED)
                            .append(Component.literal(sender).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" declined your encrypted session request.").withStyle(ChatFormatting.RED))
            );
            ci.cancel();
            return;
        }

        // --- Key exchange handshake ---
        if (MessageCrypto.isKeyExchange(raw)) {
            String token = MessageCrypto.extractToken(raw, MessageCrypto.KX_PREFIX);
            if (token == null) return;
            String theirPublicKey = token.substring(MessageCrypto.KX_PREFIX.length());

            DmSession session = SessionManager.getOrCreate(sender);

            if (!session.isReady()) {
                session.completeExchange(theirPublicKey);

                // If we sent the original request, we need to send our key back too
                if (SessionManager.hasOutgoing(sender)) {
                    SessionManager.removeOutgoing(sender);
                    session.markInitiated();
                    String kxReply = MessageCrypto.KX_PREFIX + session.getPublicKeyBase64();
                    mc.getConnection().sendUnattendedCommand("w " + sender + " " + kxReply, mc.screen);
                }

                // Auto-start encrypted chat on both sides once session is ready
                WhisperMod.setEmTarget(sender);

                mc.player.sendSystemMessage(
                        Component.literal("[EM] Secure session established with ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(sender).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" — you are now in encrypted chat.").withStyle(ChatFormatting.GRAY))
                );
            }

            ci.cancel();
            return;
        }

        // --- Encrypted message ---
        if (MessageCrypto.isMessage(raw)) {
            DmSession session = SessionManager.get(sender);
            if (session == null || !session.isReady()) return;

            String token = MessageCrypto.extractToken(raw, MessageCrypto.MSG_PREFIX);
            if (token == null) return;

            String decrypted = MessageCrypto.decrypt(session.getSharedKey(), token);
            if (decrypted == null) return;

            mc.player.sendSystemMessage(
                    Component.literal("[EM] " + sender + ": ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(decrypted).withStyle(ChatFormatting.WHITE))
            );
            ci.cancel();
        }
    }

    private static String parseSender(String raw) {
        int idx = raw.indexOf(" whispers to you: ");
        if (idx > 0) return raw.substring(0, idx);
        return null;
    }
}
