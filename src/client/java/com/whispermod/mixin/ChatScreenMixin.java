package com.whispermod.mixin;

import com.whispermod.WhisperMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow protected EditBox input;

    // Set prefix text in the input box when chat opens
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        String prefix = WhisperMod.getChatPrefix();
        if (prefix != null && input != null) {
            input.setValue(prefix);
            input.moveCursorToEnd(false);
        }
    }

    // Check every render frame — catches backspace, delete, select-all-replace, anything
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        String prefix = WhisperMod.getChatPrefix();
        if (prefix != null && input != null && !input.getValue().startsWith(prefix)) {
            WhisperMod.exitAll();
            input.setValue("");
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
        int labelY = screenHeight - 36;
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
