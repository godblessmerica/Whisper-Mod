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
            // Hide all outgoing WM protocol echoes and DM echoes (we show our own message immediately)
            if (MessageCrypto.isRequest(raw) || MessageCrypto.isDecline(raw)
                    || MessageCrypto.isKeyExchange(raw) || MessageCrypto.isEnd(raw)
                    || MessageCrypto.isMessage(raw) || WhisperMod.getDmTarget() != null) {
                ci.cancel();
            }
            return;
        }

        // Fallback echo detection using sent-token cache (EM protocol + DM text)
        if (MessageCrypto.isMessage(raw) || MessageCrypto.isEnd(raw)) {
            String token = MessageCrypto.isMessage(raw)
                    ? MessageCrypto.extractToken(raw, MessageCrypto.MSG_PREFIX)
                    : MessageCrypto.END_PREFIX;
            if (token != null && WhisperMod.consumeSent(token)) {
                ci.cancel();
                return;
            }
        }

        // Fallback DM echo detection — check if raw ends with a DM text we sent
        if (WhisperMod.getDmTarget() != null) {
            String dmToken = extractDmSentToken(raw);
            if (dmToken != null && WhisperMod.consumeSent("DM:" + dmToken)) {
                ci.cancel();
                return;
            }
        }

        String sender = boundName.replaceAll("[\\[\\]()]+$", "").trim();

        // Incoming DM from our partner via disguised chat (raw is just the message text)
        String dmTarget = WhisperMod.getDmTarget();
        if (dmTarget != null && sender.equalsIgnoreCase(dmTarget)
                && !MessageCrypto.isMessage(raw) && !MessageCrypto.isRequest(raw)
                && !MessageCrypto.isKeyExchange(raw) && !MessageCrypto.isEnd(raw)
                && !MessageCrypto.isDecline(raw)) {
            Minecraft mc = Minecraft.getInstance();
            mc.player.sendSystemMessage(
                    Component.literal("[DM] ").withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal(sender + ": ").withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(raw).withStyle(ChatFormatting.WHITE))
            );
            ci.cancel();
            return;
        }

        handleIncoming(raw, sender, ci);
    }

    // Fallback for system messages
    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void onSystemMessage(Component message, boolean overlay, CallbackInfo ci) {
        String raw = message.getString();

        // Suppress outgoing DM echo in any format ("You whisper to player: ..." etc.)
        if (raw != null && WhisperMod.getDmTarget() != null) {
            String dmToken = extractDmSentToken(raw);
            if (dmToken != null && WhisperMod.consumeSent("DM:" + dmToken)) {
                ci.cancel();
                return;
            }
            // Also catch by outgoing format prefix regardless of text tracking
            if (raw.startsWith("You whisper to ") || raw.startsWith("You tell ")
                    || raw.startsWith("You msg ")) {
                ci.cancel();
                return;
            }
        }

        String sender = parseSender(raw);
        if (sender == null) return;

        // Ignore outgoing echoes (sender is "me" or our own name)
        Minecraft mc = Minecraft.getInstance();
        String localName = mc.player != null ? mc.player.getName().getString() : null;
        if ("me".equalsIgnoreCase(sender) || (localName != null && localName.equalsIgnoreCase(sender))) {
            if (MessageCrypto.isRequest(raw) || MessageCrypto.isDecline(raw)
                    || MessageCrypto.isKeyExchange(raw) || MessageCrypto.isMessage(raw)
                    || MessageCrypto.isEnd(raw) || WhisperMod.getDmTarget() != null) {
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

        // --- Incoming DM from our current DM partner ---
        String dmTarget = WhisperMod.getDmTarget();
        if (dmTarget != null && sender.equalsIgnoreCase(dmTarget)
                && !MessageCrypto.isMessage(raw) && !MessageCrypto.isRequest(raw)
                && !MessageCrypto.isKeyExchange(raw) && !MessageCrypto.isEnd(raw)
                && !MessageCrypto.isDecline(raw)) {
            String text = extractMessageText(raw, sender);
            mc.player.sendSystemMessage(
                    Component.literal("[DM] ").withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal(sender + ": ").withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(text != null ? text : raw).withStyle(ChatFormatting.WHITE))
            );
            ci.cancel();
            return;
        }

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
                    Component.literal("[EM] " + sender + ": ").withStyle(ChatFormatting.GREEN)
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

    /**
     * Extracts the trailing message text from an outgoing echo like
     * "You whisper to player: hello" or "[me -> player] hello" → "hello"
     */
    private static String extractDmSentToken(String raw) {
        if (raw == null) return null;
        // "You whisper to player: text"
        int colon = raw.indexOf(": ");
        if (colon > 0 && (raw.startsWith("You whisper to ") || raw.startsWith("You tell ")
                || raw.startsWith("You msg "))) {
            return raw.substring(colon + 2).trim();
        }
        // "[me -> player] text" or "[me -> player]: text"
        if (raw.startsWith("[") && raw.contains(" -> ")) {
            int end = raw.indexOf("] ");
            if (end > 0) return raw.substring(end + 2).replaceFirst("^:\\s*", "").trim();
        }
        return null;
    }

    /** Extracts just the message text from a full whisper string like "player whispers to you: hello" */
    private static String extractMessageText(String raw, String sender) {
        if (raw == null) return null;

        // "player whispers to you: text"
        String tag = sender + " whispers to you: ";
        if (raw.startsWith(tag)) return raw.substring(tag.length());

        // "[player -> me] text" or "(player -> me) text"
        int bracket = raw.indexOf("] ");
        if (bracket >= 0 && raw.indexOf(sender) >= 0) return raw.substring(bracket + 2).trim();

        int paren = raw.indexOf(") ");
        if (paren >= 0 && raw.indexOf(sender) >= 0) return raw.substring(paren + 2).trim();

        // "From player: text"
        String fromTag = sender + ": ";
        int idx = raw.lastIndexOf(fromTag);
        if (idx >= 0) return raw.substring(idx + fromTag.length());

        return raw;
    }
}
