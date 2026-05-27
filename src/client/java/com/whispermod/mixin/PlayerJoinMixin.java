package com.whispermod.mixin;

import com.whispermod.friends.FriendManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(ClientPacketListener.class)
public class PlayerJoinMixin {

    private static final Map<String, Long> recentLeave = new HashMap<>();
    private static final long DEDUPE_MS = 1000;

    // Timestamp of when we joined the server — used to suppress the initial bulk player list sync
    private static long serverJoinTime = 0;
    private static final long JOIN_GRACE_MS = 3000;

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        serverJoinTime = System.currentTimeMillis();
    }

    @Inject(method = "handlePlayerInfoUpdate", at = @At("TAIL"))
    private void onPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        if (!packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        // Suppress notifications during the initial bulk sync right after joining
        if (System.currentTimeMillis() - serverJoinTime < JOIN_GRACE_MS) return;

        String localName = mc.player.getName().getString();

        for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
            String joining = entry.profile().name();
            if (joining.equalsIgnoreCase(localName)) continue;

            if (FriendManager.isFriend(joining)) {
                mc.execute(() -> mc.player.sendSystemMessage(
                        Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                .append(Component.literal(joining).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" joined the server.").withStyle(ChatFormatting.GREEN))
                ));
            }
        }
    }

    @Inject(method = "handlePlayerInfoRemove", at = @At("HEAD"))
    private void onPlayerInfoRemove(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        String localName = mc.player.getName().getString();

        for (UUID uuid : packet.profileIds()) {
            mc.getConnection().getOnlinePlayers().stream()
                    .filter(info -> info.getProfile().id().equals(uuid))
                    .findFirst()
                    .ifPresent(info -> {
                        String leaving = info.getProfile().name();
                        if (leaving.equalsIgnoreCase(localName)) return;
                        if (FriendManager.isFriend(leaving)) {
                            long now = System.currentTimeMillis();
                            Long last = recentLeave.get(leaving.toLowerCase());
                            if (last != null && now - last < DEDUPE_MS) return;
                            recentLeave.put(leaving.toLowerCase(), now);
                            mc.execute(() -> mc.player.sendSystemMessage(
                                    Component.literal("[WM] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                                            .append(Component.literal(leaving).withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal(" left the server.").withStyle(ChatFormatting.RED))
                            ));
                        }
                    });
        }
    }
}
