package me.minhcrafters.noteblockplayer.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.Objects;

public class ProgressDisplay {
    private static ProgressDisplay instance = null;
    public MutableText displayText = Text.empty();
    public MutableText timeText = Text.empty();
    public int fade = 0;
    private int progress = 0;
    private int maxProgress = 100;

    private ProgressDisplay() {
    }

    public static ProgressDisplay getInstance() {
        if (instance == null) {
            instance = new ProgressDisplay();
        }
        return instance;
    }

    public void setText(MutableText text) {
        displayText = text;
        fade = 100;
    }

    public void setTimeText(MutableText text) {
        timeText = text;
        fade = 100;
    }

    public void onRenderHUD(DrawContext context, int scaledWidth, int scaledHeight, int heldItemTooltipFade) {
        if (fade <= 0) {
            return;
        }

        int textWidth = NoteblockPlayer.mc.textRenderer.getWidth(displayText);
        int timeTextWidth = NoteblockPlayer.mc.textRenderer.getWidth(timeText);
        int timeTextHeight = NoteblockPlayer.mc.textRenderer.getWrappedLinesHeight(timeText, timeTextWidth);

        // Draw the box
        int boxWidth = 20 + textWidth;
        int boxHeight = 40 + timeTextHeight;
        int boxX = (scaledWidth - boxWidth) / 2;
        int boxY = 10; // Adjust this value to change the vertical position of the box

        // Draw the progress bar
        int barWidth = 100;
        int barHeight = 10;
        int barX = (scaledWidth - barWidth) / 2;
        int barY = boxY + 20; // Adjust this value to change the vertical position of the progress bar within the box

        // Draw the progress bar fill
        int progressWidth = (int) (((float) progress / maxProgress) * barWidth);

        // Draw the progress bar border
        int borderColor = 0xFFCCCCCC;

//        if (!NoteblockPlayer.mc.interactionManager.hasStatusBars()) {
//            barY += 14;
//        }
//
//        if (heldItemTooltipFade > 0) {
//            barY -= 12;
//        }

        if (!NoteblockPlayer.mc.inGameHud.getBossBarHud().bossBars.isEmpty()) {
            barY -= 20;
        }

        // Draw the text
        int textX = (scaledWidth - textWidth) / 2;
        int timeTextX = (scaledWidth - timeTextWidth) / 2;
        int textY = barY - 17;
        int timeTextY = barY + 15;

        int opacity = (int) ((float) this.fade * 256.0F / 10.0F);

        if (opacity > 255) {
            opacity = 255;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Objects.requireNonNull(NoteblockPlayer.mc.textRenderer);

        context.fill(barX - 1, barY - 1, barX + barWidth + 1, barY, borderColor + (opacity << 24));
        context.fill(barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, borderColor + (opacity << 24));
        context.fill(barX - 1, barY, barX, barY + barHeight, borderColor + (opacity << 24));
        context.fill(barX + barWidth, barY, barX + barWidth + 1, barY + barHeight, borderColor + (opacity << 24));

        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x80000000 + (opacity << 24));
        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0x80000000 + (opacity << 24));

        context.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFF00BFFF + (opacity << 24));

        context.drawCenteredTextWithShadow(NoteblockPlayer.mc.textRenderer, displayText, scaledWidth / 2, textY, 16777215 + (opacity << 24));
        context.drawCenteredTextWithShadow(NoteblockPlayer.mc.textRenderer, timeText, scaledWidth / 2, timeTextY, 16777215 + (opacity << 24));

        RenderSystem.disableBlend();
    }

    public void onTick() {
        if (fade > 0) {
            fade--;
        }
    }

    public void setProgress(int progress) {
        this.progress = progress;
        fade = 100;
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
    }
}
