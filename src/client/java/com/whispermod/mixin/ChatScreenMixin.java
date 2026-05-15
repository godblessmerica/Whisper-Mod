package com.whispermod.mixin;

import com.whispermod.WhisperMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow protected EditBox input;

    @Unique
    private static String getPrefix() {
        String dm = WhisperMod.getDmTarget();
        String em = WhisperMod.getEmTarget();
        if (dm != null) return "[DM to " + dm + "] ";
        if (em != null) return "[EM to " + em + "] ";
        return null;
    }

    // Set prefix text in the input box when chat opens
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        String prefix = getPrefix();
        if (prefix != null && input != null) {
            input.setValue(prefix);
            input.moveCursorToEnd(false);
        }
    }

    // Monitor for prefix deletion — if prefix is gone, exit session
    @Inject(method = "keyPressed", at = @At("TAIL"))
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        String prefix = getPrefix();
        if (prefix != null && input != null && !input.getValue().startsWith(prefix)) {
            WhisperMod.exitAll();
            input.setValue("");
        }
    }

    // Draw mode label and back button
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ChatScreen self = (ChatScreen)(Object)this;
        int screenHeight = self.height;
        Minecraft mc = Minecraft.getInstance();

        String dm = WhisperMod.getDmTarget();
        String em = WhisperMod.getEmTarget();

        // Mode label
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
        int labelY = screenHeight - 46;
        int labelW = mc.font.width(modeLabel.getString());

        graphics.fill(labelX - 2, labelY - 2, labelX + labelW + 2, labelY + 10, 0x60000000);
        graphics.text(mc.font, modeLabel, labelX, labelY, 0xFFFFFF, false);

        // Back button — only when in a session
        if (dm != null || em != null) {
            int btnX = labelX + labelW + 6;
            int btnY = labelY - 2;
            int btnW = mc.font.width("← Back") + 6;
            int btnH = 12;

            boolean hovered = mouseX >= btnX && mouseX <= btnX + btnW
                    && mouseY >= btnY && mouseY <= btnY + btnH;

            graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, hovered ? 0xA0333333 : 0x60000000);
            graphics.text(mc.font, "← Back", btnX + 3, btnY + 2, 0xFFFFFF);
        }
    }

    // Handle back button click
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        String dm = WhisperMod.getDmTarget();
        String em = WhisperMod.getEmTarget();
        if (dm == null && em == null) return;

        ChatScreen self = (ChatScreen)(Object)this;
        int screenHeight = self.height;
        Minecraft mc = Minecraft.getInstance();

        String labelText = em != null ? "[EM to " + em + "]" : "[DM to " + dm + "]";
        int labelX = 4;
        int labelY = screenHeight - 46;
        int labelW = mc.font.width(labelText);

        int btnX = labelX + labelW + 6;
        int btnY = labelY - 2;
        int btnW = mc.font.width("← Back") + 6;
        int btnH = 12;

        double mouseX = event.x();
        double mouseY = event.y();

        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            WhisperMod.exitAll();
            if (input != null) input.setValue("");
            cir.setReturnValue(true);
        }
    }
}
