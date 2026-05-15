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
        String boundName = bound.name().getString();

        // Detect outgoing echo: bound name contains " -> " meaning we sent this
        if (boundName.contains(" -> ")) {
            // Hide all outgoing WM protocol echoes
            if (MessageCrypto.isRequest(raw) || MessageCrypto.isDecline(raw)
                    || MessageCrypto.isKeyExchange(raw) || MessageCrypto.isEnd(raw)
                    || MessageCrypto.isMessage(raw)) {
                ci.cancel();
            }
            return;
        }

        // Fallback echo detection using sent-token cache (for servers that don't use " -> " format)
        if (MessageCrypto.isMessage(raw) || MessageCrypto.isEnd(raw)) {
            String token = MessageCrypto.isMessage(raw)
                    ? MessageCrypto.extractToken(raw, MessageCrypto.MSG_PREFIX)
                    : MessageCrypto.END_PREFIX;
            if (token != null && WhisperMod.consumeSent(token)) {
                ci.cancel();
                return;
            }
        }

        String sender = boundName.replaceAll("[\\[\\]()]+$", "").trim();
        handleIncoming(raw, sender, ci);
    }

    // Fallback for system messages
    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void onSystemMessage(Component message, boolean overlay, CallbackInfo ci) {
        String raw = message.getString();

        // "You whisper to player: ..." — outgoing echo on some servers
        if (raw != null && raw.startsWith("You whisper to ")) {
            if (MessageCrypto.isRequest(raw) || MessageCrypto.isDecline(raw)
                    || MessageCrypto.isKeyExchange(raw) || MessageCrypto.isEnd(raw)
                    || MessageCrypto.isMessage(raw)) {
                ci.cancel();
            }
            return;
        }

        String sender = parseSender(raw);
        if (sender == null) return;

        // Ignore outgoing echoes (sender is "me" or our own name)
        Minecraft mc = Minecraft.getInstance();
        String localName = mc.player != null ? mc.player.getName().getString() : null;
        if ("me".equalsIgnoreCase(sender) || (localName != null && localName.equalsIgnoreCase(sender))) {
            if (MessageCrypto.isRequest(raw) || MessageCrypto.isDecline(raw)
                    || MessageCrypto.isKeyExchange(raw) || MessageCrypto.isMessage(raw)
                    || MessageCrypto.isEnd(raw)) {
                ci.cancel();
            }
            return;
        }

        // Fallback echo detection using sent-token cache
        if (MessageCrypto.isMessage(raw) || MessageCrypto.isEnd(raw)) {
            String token = MessageCrypto.isMessage(raw)
                    ? MessageCrypto.extractToken(raw, MessageCrypto.MSG_PREFIX)
                    : MessageCrypto.END_PREFIX;
            if (token != null && WhisperMod.consumeSent(token)) {
                ci.cancel();
                return;
            }
        }

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

        // --- Session ended by other player ---
        if (MessageCrypto.isEnd(raw)) {
            WhisperMod.exitAllSilent();
            SessionManager.remove(sender);
            mc.player.sendSystemMessage(
                    Component.literal("[EM] ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(sender).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" ended the encrypted session.").withStyle(ChatFormatting.GRAY))
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
            if (token == null) { ci.cancel(); return; }
            String theirPublicKey = token.substring(MessageCrypto.KX_PREFIX.length());

            DmSession session = SessionManager.getOrCreate(sender);

            // Ignore if this key is our own (echo of our own WMKX)
            if (theirPublicKey.equals(session.getPublicKeyBase64())) {
                ci.cancel();
                return;
            }

            if (!session.isReady()) {
                session.completeExchange(theirPublicKey);

                // If we sent the original request, send our key back
                if (SessionManager.hasOutgoing(sender)) {
                    SessionManager.removeOutgoing(sender);
                    session.markInitiated();
                    String kxReply = MessageCrypto.KX_PREFIX + session.getPublicKeyBase64();
                    mc.getConnection().sendUnattendedCommand("w " + sender + " " + kxReply, mc.screen);
                }

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
                    Component.literal("[EM] ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(sender + ": ").withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(decrypted).withStyle(ChatFormatting.WHITE))
            );
            ci.cancel();
        }
    }

    private static String parseSender(String raw) {
        if (raw == null) return null;

        // Vanilla: "player whispers to you: message"
        int idx = raw.indexOf(" whispers to you: ");
        if (idx > 0) return raw.substring(0, idx);

        // EssentialsX / common: "[player -> me] message" or "[player -> you] message"
        if (raw.startsWith("[")) {
            int arrowIdx = raw.indexOf(" -> ");
            if (arrowIdx > 0) return raw.substring(1, arrowIdx);
        }

        // Parentheses variant: "(player -> me) message"
        if (raw.startsWith("(")) {
            int arrowIdx = raw.indexOf(" -> ");
            if (arrowIdx > 0) return raw.substring(1, arrowIdx);
        }

        // "From player: message" or "[From player]: message" or "(From player) message"
        if (raw.startsWith("From ") || raw.startsWith("[From ") || raw.startsWith("(From ")) {
            String stripped = raw.replaceAll("^[\\[(]?From ", "");
            int end = stripped.indexOf("]");
            if (end == -1) end = stripped.indexOf(")");
            if (end == -1) end = stripped.indexOf(":");
            if (end > 0) return stripped.substring(0, end).trim();
        }

        // "[PC] player --> YOU: message"
        if (raw.contains(" --> ")) {
            int start = raw.indexOf("] ");
            int end = raw.indexOf(" --> ");
            if (start >= 0 && end > start) return raw.substring(start + 2, end).trim();
        }

        return null;
    }
}
