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

        String dmTarget = WhisperMod.getDmTarget();
        String emTarget = WhisperMod.getEmTarget();

        MutableComponent label;

        if (emTarget != null) {
            // encrypted session — green for secure
            label = Component.literal("[EM] ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(emTarget).withStyle(ChatFormatting.AQUA));
        } else if (dmTarget != null) {
            // unencrypted session — yellow for casual
            label = Component.literal("[DM] ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(dmTarget).withStyle(ChatFormatting.AQUA));
        } else {
            return;
        }

        int x = 4;
        int y = graphics.guiHeight() - 36;
        int w = mc.font.width(label.getString());

        graphics.fill(x - 2, y - 2, x + w + 2, y + 11, 0x40000000);
        graphics.text(mc.font, label, x, y, -1, true);
    }
}
