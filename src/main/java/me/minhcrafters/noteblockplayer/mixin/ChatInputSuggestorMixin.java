package me.minhcrafters.noteblockplayer.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public class ChatInputSuggestorMixin {
    @Shadow
    final TextFieldWidget textField;
    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    public ChatInputSuggestorMixin() {
        textField = null;
    }

    @Shadow
    private static int getStartOfCurrentWord(String input) {
        return 0;
    }

    @Shadow
    public void show(boolean narrateFirstSuggestion) {
    }

    @Inject(at = @At("TAIL"), method = "refresh()V")
    public void onRefresh(CallbackInfo ci) {
        String textStr = this.textField.getText();
        int cursorPos = this.textField.getCursor();
        String preStr = textStr.substring(0, cursorPos);
        if (!preStr.startsWith(CommandManager.getCommandPrefix())) {
            return;
        }

        int wordStart = getStartOfCurrentWord(preStr);

        CompletableFuture<Suggestions> suggestions;
        try {
            suggestions = CommandManager.handleSuggestions(preStr, new SuggestionsBuilder(preStr, wordStart));
        } catch (Throwable e) {
            suggestions = null;
        }

        if (suggestions != null) {
            this.pendingSuggestions = suggestions;
            this.show(true);
        }
    }
}
