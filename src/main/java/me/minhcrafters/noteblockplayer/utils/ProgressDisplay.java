package me.minhcrafters.noteblockplayer.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Objects;

public class ProgressDisplay {
    private static ProgressDisplay instance = null;

    public static ProgressDisplay getInstance() {
        if (instance == null) {
            instance = new ProgressDisplay();
        }
        return instance;
    }

    private ProgressDisplay() {
    }

    // Allow the header text to be configurable instead of always "Now playing"
    private MutableText headerText = Text.empty();
    public MutableText topText = Text.empty();
    public MutableText bottomText = Text.empty();
    public MutableText durationText = Text.empty();
    public int fade = 0;
    private long currentTick = 0; // Progress tracking
    private long totalTicks = 0; // Total duration (measured in ticks)

    public void setHeaderText(MutableText header) {
        this.headerText = header;
    }

    /**
     * Set the song name (topText) and additional text (bottomText) for further details.
     * Note: currentTick is reset, and fade is restarted.
     */
    public void setText(MutableText headerText, MutableText songName, MutableText songAuthor, MutableText durationText, int fade) {
        if (headerText != null) this.headerText = headerText;
        this.topText = songName;
        this.bottomText = songAuthor;
        this.durationText = durationText;
        this.fade = fade;
    }

    public void onRenderHUD_2(DrawContext context, int scaledWidth, int scaledHeight, int heldItemTooltipFade) {
        if (fade <= 0) {
            return;
        }

        int textWidth = NoteblockPlayer.mc.textRenderer.getWidth(topText);
        int timeTextWidth = NoteblockPlayer.mc.textRenderer.getWidth(durationText);
        int timeTextHeight = NoteblockPlayer.mc.textRenderer.getWrappedLinesHeight(durationText, timeTextWidth);

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
        int progressWidth = (int) (((float) currentTick / totalTicks) * barWidth);

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

        context.drawCenteredTextWithShadow(NoteblockPlayer.mc.textRenderer, topText, scaledWidth / 2, textY, 16777215 + (opacity << 24));
        context.drawCenteredTextWithShadow(NoteblockPlayer.mc.textRenderer, durationText, scaledWidth / 2, timeTextY, 16777215 + (opacity << 24));

        RenderSystem.disableBlend();
    }

    public void onRenderHUD(DrawContext context) {
        if (fade <= 0 || totalTicks <= 0) { // Don't render if faded or no duration
            return;
        }

        int screenWidth = NoteblockPlayer.mc.getWindow().getScaledWidth();
        // --- Calculate Opacity ---
        int opacity = (int) ((float) this.fade * 256.0F / 10.0F);
        opacity = MathHelper.clamp(opacity, 0, 255); // Clamp opacity between 0 and 255
        if (opacity <= 4) return; // Don't render if almost fully transparent

        int alpha = opacity << 24; // Prepare alpha for color values

        // --- Setup texts ---
        // Use headerText so that the display can be modular
        MutableText headerToDisplay = headerText;
        MutableText songNameText = topText;
        MutableText authorNameText = bottomText;

        // --- UI Element Dimensions ---
        Objects.requireNonNull(NoteblockPlayer.mc.textRenderer);
        int fontHeight = NoteblockPlayer.mc.textRenderer.fontHeight;
        int headerWidth = NoteblockPlayer.mc.textRenderer.getWidth(headerToDisplay);
        int songNameWidth = NoteblockPlayer.mc.textRenderer.getWidth(songNameText);
        int songAuthorWidth = NoteblockPlayer.mc.textRenderer.getWidth(authorNameText);
        int durationWidth = NoteblockPlayer.mc.textRenderer.getWidth(durationText);
        int textWidth = Math.max(Math.max(headerWidth, songNameWidth + 10), durationWidth);

        int padding = 4;
        int progressBarHeight = 4;
        int spacing = 2;

        int boxWidth = textWidth + padding * 2;
        boxWidth = Math.max(boxWidth, 100);

        // Total height: header, song name, author, progress bar, duration, plus paddings and spaces
        int boxHeight = padding
                + fontHeight        // header line
                + spacing;

        boxHeight += (!Objects.equals(topText.toString(), "")) ? (fontHeight + spacing) : 0; // song name
        boxHeight += (!Objects.equals(bottomText.toString(), "")) ? (fontHeight + spacing) : 0; // author name

        boxHeight += progressBarHeight + 1 // Progress bar
                + spacing
                + fontHeight        // duration
                + padding;

        // --- Positions ---
        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = NoteblockPlayer.mc.inGameHud.getBossBarHud().bossBars.isEmpty() ?
                10 : NoteblockPlayer.mc.inGameHud.getBossBarHud().bossBars.size() * 20;

        // Position for each text line
        int headerX = boxX + (boxWidth - headerWidth) / 2;
        int headerY = boxY + padding;

        int songNameX = boxX + (boxWidth - songNameWidth) / 2;
        int songNameY = headerY + fontHeight + spacing;

        int authorNameX = boxX + (boxWidth - songAuthorWidth) / 2;
        int authorNameY = songNameY + fontHeight + spacing;

        int progressBarX = boxX + padding;
        int progressBarY = authorNameY + fontHeight + spacing;
        int progressBarMaxWidth = boxWidth - padding * 2;

        int durationX = boxX + (boxWidth - durationWidth) / 2;
        int durationY = progressBarY + progressBarHeight + 1 + spacing;

        // --- Draw Background Box ---
        int backgroundColor = 0x80000000; // Semi-transparent black (ARGB)
        // Apply fade alpha to background color
        int fadedBackgroundColor = (backgroundColor & 0x00FFFFFF) | (((backgroundColor >> 24) * opacity / 255) << 24);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, fadedBackgroundColor);

        // --- Draw Progress Bar ---
        float progress = (float) currentTick / totalTicks;
        int progressBarCurrentWidth = (int) (progressBarMaxWidth * MathHelper.clamp(progress, 0.0f, 1.0f));
        int progressBarColor = 0xFFFFFFFF; // White (ARGB)
        int progressBarBgColor = 0x60000000; // Semi-transparent black (ARGB)
        int borderColor = 0xFFCCCCCC;
        int fadedProgressBarColor = (progressBarColor & 0x00FFFFFF) | alpha;
        int fadedProgressBarBgColor = (progressBarBgColor & 0x00FFFFFF) | alpha;

        context.fill(
                progressBarX - 1,
                progressBarY - 1,
                progressBarX + progressBarMaxWidth + 1,
                progressBarY + progressBarHeight + 1,
                borderColor + (opacity << 24)
        );

        context.fill(progressBarX, progressBarY, progressBarX + progressBarMaxWidth, progressBarY + progressBarHeight, fadedProgressBarBgColor);
        context.fill(progressBarX, progressBarY, progressBarX + progressBarCurrentWidth, progressBarY + progressBarHeight, fadedProgressBarColor);

        // --- Draw Text Lines ---
        int textColor = 0xFFFFFFFF; // White (ARGB)
        int fadedTextColor = (textColor & 0x00FFFFFF) | alpha;
        context.drawTextWithShadow(NoteblockPlayer.mc.textRenderer, headerToDisplay, headerX, headerY, fadedTextColor);
        context.drawTextWithShadow(NoteblockPlayer.mc.textRenderer, songNameText, songNameX, songNameY, fadedTextColor);
        context.drawTextWithShadow(NoteblockPlayer.mc.textRenderer, authorNameText, authorNameX, authorNameY, fadedTextColor);
        context.drawTextWithShadow(NoteblockPlayer.mc.textRenderer, durationText, durationX, durationY, fadedTextColor);

        RenderSystem.disableBlend();
    }

    /**
     * Call this every game tick.
     */
    public void tick() {
        if (fade > 0) {
            fade--;
        }
        if (currentTick < totalTicks) {
            currentTick++;
        }
    }

    public long getCurrentTick() {
        return this.currentTick;
    }

    public long getTotalTicks() {
        return this.totalTicks;
    }

    public void setCurrentTick(long currentTick) {
        this.currentTick = MathHelper.clamp(currentTick, 0, totalTicks);
    }

    public void setTotalTicks(long totalTicks) {
        this.totalTicks = MathHelper.clamp(totalTicks, 0, Long.MAX_VALUE);
    }
}