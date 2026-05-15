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

    }
}
