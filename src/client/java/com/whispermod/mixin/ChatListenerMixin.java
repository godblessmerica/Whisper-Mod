package com.whispermod.mixin;

import com.whispermod.WhisperMod;
import com.whispermod.crypto.DmSession;
import com.whispermod.crypto.MessageCrypto;
import com.whispermod.crypto.SessionManager;
import com.whispermod.friends.FriendManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
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
            if (MessageCrypto.isAnyProtocol(raw) || WhisperMod.getDmTarget() != null) {
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

        // Format all incoming whispers as [DM] (disguised chat — raw is just the message text)
        if (!MessageCrypto.isAnyProtocol(raw)) {
            Minecraft mc = Minecraft.getInstance();
            boolean isFriend = FriendManager.isFriend(sender);
            mc.player.sendSystemMessage(
                    Component.literal(isFriend ? "[DM ★] " : "[DM] ").withStyle(ChatFormatting.YELLOW)
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
            if (MessageCrypto.isAnyProtocol(raw) || WhisperMod.getDmTarget() != null) {
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

        // --- Format all incoming whispers as [DM] ---
        if (!MessageCrypto.isAnyProtocol(raw)) {
            String text = extractMessageText(raw, sender);
            boolean isFriend = FriendManager.isFriend(sender);
            mc.player.sendSystemMessage(
                    Component.literal(isFriend ? "[DM ★] " : "[DM] ").withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal(sender + ": ").withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(text != null ? text : raw).withStyle(ChatFormatting.WHITE))
            );
            ci.cancel();
            return;
        }

        // --- Friend blocked response ---
        if (MessageCrypto.isFriendBlocked(raw)) {
            mc.player.sendSystemMessage(
                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                            .append(Component.literal(sender).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" blocked you.").withStyle(ChatFormatting.RED))
            );
            FriendManager.removeOutgoing(sender);
            ci.cancel();
            return;
        }

        // --- Unblocked ---
        if (MessageCrypto.isUnblock(raw)) {
            mc.player.sendSystemMessage(
                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                            .append(Component.literal(sender).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" unblocked you.").withStyle(ChatFormatting.GREEN))
            );
            ci.cancel();
            return;
        }

        // --- Friend request ---
        if (MessageCrypto.isFriendRequest(raw)) {
            if (FriendManager.isMutedAll() || FriendManager.isMuted(sender)) {
                ci.cancel();
                return;
            }
            if (FriendManager.isBlocked(sender)) {
                // Silently notify sender they are blocked — delay to get off the mixin thread
                final String blockedSender = sender;
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        mc.execute(() -> {
                            if (mc.getConnection() != null) {
                                mc.getConnection().sendUnattendedCommand("w " + blockedSender + " " + MessageCrypto.FRIEND_BLOCKED_PREFIX, null);
                            }
                        });
                    }
                }, 500);
                ci.cancel();
                return;
            }
            String token = MessageCrypto.extractToken(raw, MessageCrypto.FRIEND_REQ_PREFIX);
            String requester = token != null ? token.substring(MessageCrypto.FRIEND_REQ_PREFIX.length()) : sender;
            FriendManager.addIncoming(requester);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.0f));
            mc.player.sendSystemMessage(
                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                            .append(Component.literal(requester).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" wants to be your friend! ").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal("[Accept]").withStyle(Style.EMPTY
                                    .withColor(ChatFormatting.GREEN)
                                    .withBold(true)
                                    .withClickEvent(new ClickEvent.RunCommand("/wm accept " + requester))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to accept")))))
                            .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("[Decline]").withStyle(Style.EMPTY
                                    .withColor(ChatFormatting.RED)
                                    .withBold(true)
                                    .withClickEvent(new ClickEvent.RunCommand("/wm decline " + requester))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to decline")))))
            );
            ci.cancel();
            return;
        }

        // --- Friend accepted ---
        if (MessageCrypto.isFriendAccept(raw)) {
            String token = MessageCrypto.extractToken(raw, MessageCrypto.FRIEND_ACCEPT_PREFIX);
            String accepter = token != null ? token.substring(MessageCrypto.FRIEND_ACCEPT_PREFIX.length()) : sender;
            FriendManager.removeOutgoing(accepter);
            FriendManager.addFriend(accepter);
            mc.player.sendSystemMessage(
                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                            .append(Component.literal("You and ").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(accepter).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" are now friends!").withStyle(ChatFormatting.GREEN))
            );
            ci.cancel();
            return;
        }

        // --- Friend declined ---
        if (MessageCrypto.isFriendDecline(raw)) {
            String token = MessageCrypto.extractToken(raw, MessageCrypto.FRIEND_DECLINE_PREFIX);
            String decliner = token != null ? token.substring(MessageCrypto.FRIEND_DECLINE_PREFIX.length()) : sender;
            FriendManager.removeOutgoing(decliner);
            mc.player.sendSystemMessage(
                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                            .append(Component.literal(decliner).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" declined your friend request.").withStyle(ChatFormatting.RED))
            );
            ci.cancel();
            return;
        }

        // --- Unfriended ---
        if (MessageCrypto.isUnfriend(raw)) {
            String token = MessageCrypto.extractToken(raw, MessageCrypto.UNFRIEND_PREFIX);
            String who = token != null ? token.substring(MessageCrypto.UNFRIEND_PREFIX.length()) : sender;
            FriendManager.removeFriend(who);
            if (who.equalsIgnoreCase(WhisperMod.getEmTarget())) {
                WhisperMod.exitAllSilent();
                mc.player.sendSystemMessage(
                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                .append(Component.literal(who).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" unfriended you — encrypted session ended.").withStyle(ChatFormatting.RED))
                );
            } else {
                mc.player.sendSystemMessage(
                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                .append(Component.literal(who).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" unfriended you.").withStyle(ChatFormatting.RED))
                );
            }
            ci.cancel();
            return;
        }

        // --- EM session request ---
        if (MessageCrypto.isRequest(raw)) {
            if (!FriendManager.isFriend(sender)) { ci.cancel(); return; }
            if (FriendManager.isMutedAll() || FriendManager.isMuted(sender)) { ci.cancel(); return; }
            SessionManager.addIncoming(sender);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f));
            mc.player.sendSystemMessage(
                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                            .append(Component.literal(sender).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" wants to start an encrypted session. ").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal("[Accept]").withStyle(Style.EMPTY
                                    .withColor(ChatFormatting.GREEN)
                                    .withBold(true)
                                    .withClickEvent(new ClickEvent.RunCommand("/wm accept " + sender))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to accept")))))
                            .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("[Decline]").withStyle(Style.EMPTY
                                    .withColor(ChatFormatting.RED)
                                    .withBold(true)
                                    .withClickEvent(new ClickEvent.RunCommand("/wm decline " + sender))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to decline")))))
            );
            ci.cancel();
            return;
        }

        // --- Session ended by other player ---
        if (MessageCrypto.isEnd(raw)) {
            WhisperMod.exitAllSilent();
            SessionManager.remove(sender);
            mc.player.sendSystemMessage(
                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                            .append(Component.literal(sender).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" ended the encrypted session.").withStyle(ChatFormatting.RED))
            );
            ci.cancel();
            return;
        }

        // --- Decline ---
        if (MessageCrypto.isDecline(raw)) {
            SessionManager.removeOutgoing(sender);
            SessionManager.remove(sender);
            mc.player.sendSystemMessage(
                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
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
                    mc.execute(() -> mc.getConnection().sendUnattendedCommand("w " + sender + " " + kxReply, mc.screen));
                }

                WhisperMod.setEmTarget(sender);

                mc.player.sendSystemMessage(
                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                .append(Component.literal("Secure session established with ").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(sender).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" — you are now in encrypted chat.").withStyle(ChatFormatting.GREEN))
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
