package me.minhcrafters.noteblockplayer.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;

public class ModMenuImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> Config.getScreen(parent, NoteblockPlayer.MOD_ID);
    }
}
