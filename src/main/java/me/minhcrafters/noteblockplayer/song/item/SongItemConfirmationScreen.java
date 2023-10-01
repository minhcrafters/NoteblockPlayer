package me.minhcrafters.noteblockplayer.song.item;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.song.SongManager;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SongItemConfirmationScreen extends Screen {
    private static final Text CONFIRM = Text.literal("Play");
    private static final Text CANCEL = Text.literal("Cancel");
    private ItemStack stack;
    private SongItemLoaderThread loaderThread;
    private MultilineText unloadedText;
    private MultilineText loadedText;
    private boolean loaded = false;

    public SongItemConfirmationScreen(ItemStack stack) throws IOException, IllegalArgumentException {
        super(Text.literal("Are you sure you want to play this music sheet?"));
        this.stack = stack;
        this.loaderThread = new SongItemLoaderThread(stack);
        this.loaderThread.start();
    }

    @Override
    protected void init() {
        super.init();
        String unloadedMessage = "§7Loading notes...";
        this.unloadedText = MultilineText.create(this.textRenderer, Text.literal(unloadedMessage));
    }

    private void addButtons(int y) {
        int centerX = this.width / 2;

        this.addDrawableChild(ButtonWidget.builder(CONFIRM, button -> {
            SongManager.getInstance().loadSong(loaderThread);
            this.client.setScreen(null);
        }).dimensions(centerX - 105, y, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(CANCEL, button -> {
            this.client.setScreen(null);
        }).dimensions(centerX + 5, y, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 40, 0xFFFFFF);

        if (!loaderThread.isAlive()) {
            if (loaderThread.exception != null) {
                NoteblockPlayer.addChatMessage("§cError: §4" + loaderThread.exception.getMessage());
                this.client.setScreen(null);
                return;
            } else if (loadedText == null) {
                String[] loadedMessages = {
                        "§3" + loaderThread.song.name,
                        String.format("§7Max notes per second: %s%d", getNumberColor(loaderThread.maxNotesPerSecond), loaderThread.maxNotesPerSecond),
                        String.format("§7Average notes per second: %s%.2f", getNumberColor(loaderThread.avgNotesPerSecond), loaderThread.avgNotesPerSecond),
                };
                List<Text> messageList = Arrays.stream(loadedMessages).map(Text::literal).collect(Collectors.toList());
                ;
                this.loadedText = MultilineText.createFromTexts(this.textRenderer, messageList);

                int loadedTextHeight = this.loadedText.count() * this.textRenderer.fontHeight;
                addButtons(60 + loadedTextHeight + 12);

                loaded = true;
            }
        }

        if (loaded) {
            loadedText.drawCenterWithShadow(context, this.width / 2, 60);
        } else {
            unloadedText.drawCenterWithShadow(context, this.width / 2, 60);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    public String getNumberColor(double number) {
        if (number < 50) {
            return "§a";
        } else if (number < 100) {
            return "§e";
        } else if (number < 300) {
            return "§6";
        } else if (number < 600) {
            return "§c";
        } else {
            return "§4";
        }

    }
}
