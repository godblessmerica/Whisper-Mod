package com.whispermod.mixin;

import com.whispermod.WhisperMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow protected EditBox input;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        String dm = WhisperMod.getDmTarget();
        String em = WhisperMod.getEmTarget();
        if (dm == null && em == null) return;

        Minecraft mc = Minecraft.getInstance();
        ChatScreen self = (ChatScreen)(Object)this;

        String labelText = em != null ? "[EM to " + em + "]" : "[DM to " + dm + "]";
        int labelX = 4;
        int labelY = self.height - 24;
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
