package me.minhcrafters.noteblockplayer.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Base64;

public class SongItemUtils {
    public static final String SONG_ITEM_KEY = "MusicSheetData";
    public static final String SONG_DATA_KEY = "MusicData";
    public static final String FILE_NAME_KEY = "FileName";
    public static final String DISPLAY_NAME_KEY = "DisplayName";

    /**
     * Creates a music sheet with the specified song data, filename, and display name.
     *
     * @param stack       the ItemStack to create the music sheet on
     * @param songData    the byte array containing the song data
     * @param filename    the name of the song file
     * @param displayName the display name of the music sheet
     * @return the modified ItemStack with the music sheet
     */
    public static ItemStack createSongItem(ItemStack stack, byte[] songData, String filename, String displayName) {
        NbtCompound noteblockPlayerNbt = new NbtCompound();
        stack.setSubNbt(SONG_ITEM_KEY, noteblockPlayerNbt);
        noteblockPlayerNbt.putString(SONG_DATA_KEY, Base64.getEncoder().encodeToString(songData));
        noteblockPlayerNbt.putString(FILE_NAME_KEY, filename);
        noteblockPlayerNbt.putString(DISPLAY_NAME_KEY, displayName);
        addSongItemDisplay(stack);
        return stack;
    }

    /**
     * Retrieves the NbtCompound tag associated with a music sheet from the given ItemStack.
     *
     * @param stack the ItemStack to retrieve the tag from
     * @return the NbtCompound tag associated with the music sheet, or null if not found
     */
    public static NbtCompound getSongItemTag(ItemStack stack) {
        return stack.getSubNbt(SONG_ITEM_KEY);
    }

    public static boolean isSongItem(ItemStack stack) {
        return getSongItemTag(stack) != null;
    }

    public static byte[] getSongData(ItemStack stack) throws IllegalArgumentException {
        NbtCompound noteblockPlayerNbt = getSongItemTag(stack);
        if (noteblockPlayerNbt == null || !noteblockPlayerNbt.contains(SONG_DATA_KEY, NbtElement.STRING_TYPE)) {
            return null;
        } else {
            return Base64.getDecoder().decode(noteblockPlayerNbt.getString(SONG_DATA_KEY));
        }
    }

    public static void addSongItemDisplay(ItemStack stack) {
        if (!isSongItem(stack)) return;
        NbtCompound noteblockPlayerNbt = getSongItemTag(stack);
        String name = noteblockPlayerNbt.getString(DISPLAY_NAME_KEY);
        if (name == null || name.isEmpty()) name = noteblockPlayerNbt.getString(FILE_NAME_KEY);
        if (name == null || name.isEmpty()) name = "unnamed";
        Text nameText = getStyledText(name, Style.EMPTY.withColor(Formatting.GRAY).withItalic(false));
        setItemName(stack, getStyledText("Music Sheet", Style.EMPTY.withColor(Formatting.DARK_AQUA).withItalic(false)));
        setItemLore(stack,
                nameText,
                getStyledText("Requires NoteblockPlayer 1.6+", Style.EMPTY.withColor(Formatting.GOLD).withItalic(true))
        );
    }

    public static MutableText getStyledText(String str, Style style) {
        MutableText text = MutableText.of(new LiteralTextContent(str));
        text.setStyle(style);
        return text;
    }

    public static void setItemName(ItemStack stack, Text text) {
        stack.getOrCreateSubNbt(ItemStack.DISPLAY_KEY).putString(ItemStack.NAME_KEY, Text.Serializer.toJson(text));
    }

    public static void setItemLore(ItemStack stack, Text... loreLines) {
        NbtList lore = new NbtList();
        for (Text line : loreLines) {
            lore.add(NbtString.of(Text.Serializer.toJson(line)));
        }
        stack.getOrCreateSubNbt(ItemStack.DISPLAY_KEY).put(ItemStack.LORE_KEY, lore);
    }
}
