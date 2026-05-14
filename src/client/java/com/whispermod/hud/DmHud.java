package com.whispermod.hud;

import com.whispermod.WhisperMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class DmHud {

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        String target = WhisperMod.getDmTarget();
        if (target == null) return;

        Minecraft mc = Minecraft.getInstance();

        Component label = Component.literal("[DM] ")
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(target).withStyle(ChatFormatting.AQUA));

        int x = 4;
        int y = graphics.guiHeight() - 36;
        int w = mc.font.width(label.getString());

        // Dark background behind text
        graphics.fill(x - 2, y - 2, x + w + 2, y + 11, 0xFF000000);
        graphics.text(mc.font, label, x, y, -1, true);
    }
}
