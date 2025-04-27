package me.minhcrafters.noteblockplayer;

import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.config.Config;
import me.minhcrafters.noteblockplayer.config.ConfigImpl;
import me.minhcrafters.noteblockplayer.utils.FileUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Files;
import java.nio.file.Path;

public class NoteblockPlayer implements ModInitializer {
    public static final String MOD_ID = "noteblockplayer";
    public static final Logger LOGGER = LogManager.getLogger("NoteblockPlayer");

    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final int NOTEBLOCK_BASE_ID = Block.getRawIdFromState(Blocks.NOTE_BLOCK.getDefaultState()) - 1;

    public static final Path MAIN_DIR = Path.of("NoteblockPlayer");
    public static final Path SONGS_DIR = MAIN_DIR.resolve("songs");
    public static final Path PLAYLISTS_DIR = MAIN_DIR.resolve("playlists");
    public static FakePlayerEntity fakePlayer;

    private static ConfigImpl config;

    @Override
    public void onInitialize() {
        if (!Files.exists(SONGS_DIR)) {
            FileUtils.createDirectoriesSilently(SONGS_DIR);
        }
        if (!Files.exists(MAIN_DIR)) {
            FileUtils.createDirectoriesSilently(MAIN_DIR);
        }
        if (!Files.exists(PLAYLISTS_DIR)) {
            FileUtils.createDirectoriesSilently(PLAYLISTS_DIR);
        }

        Config.init(MOD_ID, ConfigImpl.class);

        CommandManager.initCommands();

        LOGGER.info("NoteblockPlayer initialized");
    }

    public static ConfigImpl getConfig() {
        if (config != null) return config;
        return null;
    }

    public static void addChatMessage(String message) {
        mc.player.sendMessage(Text.of(message), false);
    }

    public static void addChatMessage(Text text) {
        mc.player.sendMessage(text, false);
    }

    public static void removeFakePlayer() {
        if (fakePlayer != null) {
            fakePlayer.remove(Entity.RemovalReason.DISCARDED);
            fakePlayer = null;
        }
    }
}
