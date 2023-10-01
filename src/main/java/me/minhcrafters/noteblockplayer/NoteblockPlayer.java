package me.minhcrafters.noteblockplayer;

import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.config.Config;
import me.minhcrafters.noteblockplayer.config.ConfigImpl;
import me.minhcrafters.noteblockplayer.utils.FileUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class NoteblockPlayer implements ModInitializer {
    public static final String MOD_ID = "noteblockplayer";
    public static final Logger LOGGER = LogManager.getLogger("NoteblockPlayer");
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final int NOTEBLOCK_BASE_ID = Block.getRawIdFromState(Blocks.NOTE_BLOCK.getDefaultState()) - 1;
    public static final Path MAIN_DIR = Path.of("NoteblockPlayer");
    public static final Path SONGS_DIR = MAIN_DIR.resolve("songs");
    private static final String modName = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .get()
            .getMetadata()
            .getName();
    private static final String modVersion = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .get()
            .getMetadata()
            .getVersion().getFriendlyString();
    private static final ArrayList<String> modAuthors = new ArrayList<>();
    private static final Collection<Person> modAuthors1 = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .get()
            .getMetadata()
            .getAuthors();
    private static final String modDescription = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .get()
            .getMetadata()
            .getDescription();
    public static String FORCE_PREFIX = String.format("<<%s>>", UUID.randomUUID());
    public static boolean showFakePlayer = false;
    public static FakePlayerEntity fakePlayer;
    private static ConfigImpl config;

    public static void addChatMessage(Text message) {
        mc.player.sendMessage(message, false);
    }

    public static void addChatMessage(String message) {
        mc.player.sendMessage(Text.of(message), false);
    }

    public static void removeFakePlayer() {
        if (fakePlayer != null) {
            fakePlayer.remove(Entity.RemovalReason.DISCARDED);
            fakePlayer = null;
        }
    }

    public static String getModVersion() {
        return modVersion;
    }

    public static String getModName() {
        return modName;
    }

    public static ArrayList<String> getModAuthors() {
        return modAuthors;
    }

    public static String getModDescription() {
        return modDescription;
    }

    public static ConfigImpl getConfig() {
        if (config != null) return config;
        return null;
    }

    @Override
    public void onInitialize() {
        if (!Files.exists(MAIN_DIR)) {
            FileUtils.createDirectoriesSilently(MAIN_DIR);
        }

        if (!Files.exists(SONGS_DIR)) {
            FileUtils.createDirectoriesSilently(SONGS_DIR);
        }

        modAuthors1.forEach(person -> modAuthors.add(person.getName()));

        Config.init(MOD_ID, ConfigImpl.class);

        CommandManager.initCommands();
    }
}
