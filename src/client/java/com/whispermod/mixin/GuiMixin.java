package com.whispermod.mixin;

import com.whispermod.WhisperMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        if (!(mc.screen instanceof ChatScreen)) return;

        String dm = WhisperMod.getDmTarget();
        String em = WhisperMod.getEmTarget();

        // --- Mode label and back button ---
        MutableComponent modeLabel;
        if (em != null) {
            modeLabel = Component.literal("[EM to ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(em).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("]").withStyle(ChatFormatting.GREEN));
        } else if (dm != null) {
            modeLabel = Component.literal("[DM to ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(dm).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("]").withStyle(ChatFormatting.YELLOW));
        } else {
            modeLabel = Component.literal("Public Chat").withStyle(ChatFormatting.GRAY);
        }

        int labelX = 4;
        int labelY = graphics.guiHeight() - 24;
        int labelW = mc.font.width(modeLabel.getString());

        graphics.fill(labelX - 2, labelY - 2, labelX + labelW + 2, labelY + 10, 0x60000000);
        graphics.text(mc.font, modeLabel, labelX, labelY, -1, false);

        // Back button — only when in a session
        if (dm != null || em != null) {
            int btnX = labelX + labelW + 6;
            int btnY = labelY - 2;
            int btnW = mc.font.width("← Back") + 6;
            int btnH = 12;

            graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0x60000000);
            graphics.text(mc.font, "← Back", btnX + 3, btnY + 2, 0xFFFFFF);
        }

        // --- Colored prefix overlay in the chat input box ---
        String prefix = WhisperMod.getChatPrefix();
        if (prefix != null) {
            MutableComponent coloredPrefix;
            if (em != null) {
                // [EM to player] — green + aqua
                coloredPrefix = Component.literal("[EM to ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(em).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal("] ").withStyle(ChatFormatting.GREEN));
            } else {
                // [DM to player] — yellow + aqua
                coloredPrefix = Component.literal("[DM to ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(dm).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal("] ").withStyle(ChatFormatting.YELLOW));
            }

            int prefixX = 8;
            int prefixY = graphics.guiHeight() - 10;
            int prefixW = mc.font.width(prefix);

            // Cover plain text with solid black, then draw colored text on top
            graphics.fill(prefixX - 1, prefixY - 1, prefixX + prefixW + 1, prefixY + 9, 0xFF000000);
            graphics.text(mc.font, coloredPrefix, prefixX, prefixY, -1, false);
        }
    }
}
