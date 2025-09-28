package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.utils.Paginator;
import me.minhcrafters.noteblockplayer.utils.Utils;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.SongHandler;
import me.minhcrafters.noteblockplayer.song.Playlist;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PlaylistCommand extends Command {
    public String getName() {
        return "playlist";
    }

    public String[] getSyntax() {
        return new String[] {
                "play <playlist>",
                "create <playlist>",
                "list [<playlist>] [<page>]",
                "delete <playlist> <song>",
                "addSong <playlist> <song>",
                "removeSong <playlist> <song>",
                "renameSong <playlist> <index> <new name>",
                "loop",
                "shuffle",
        };
    }

    public String getDescription() {
        return "Configures playlists";
    }

    public boolean processCommand(String args) {
        String[] split = args.split(" ");

        if (split.length < 1)
            return false;

        try {
            Path playlistDir = null;
            if (split.length >= 2) {
                playlistDir = NoteblockPlayer.PLAYLISTS_DIR.resolve(split[1]);
            }
            switch (split[0].toLowerCase(Locale.ROOT)) {
                case "play": {
                    if (split.length != 2)
                        return false;
                    if (!Files.exists(playlistDir)) {
                        NoteblockPlayer.addChatMessage("§cPlaylist does not exist");
                        return true;
                    }
                    SongHandler.getInstance().setPlaylist(playlistDir);
                    return true;
                }
                case "create": {
                    if (split.length > 2) {
                        NoteblockPlayer.addChatMessage("§cCannot have spaces in playlist name");
                        return true;
                    }
                    if (split.length != 2)
                        return false;
                    Playlist.createPlaylist(split[1]);
                    NoteblockPlayer.addChatMessage(String.format("§6Created playlist §3%s", split[1]));
                    return true;
                }
                case "delete": {
                    if (split.length != 2)
                        return false;
                    Playlist.deletePlaylist(playlistDir);
                    NoteblockPlayer.addChatMessage(String.format("§6Deleted playlist §3%s", split[1]));
                    return true;
                }
                case "list": {
                    if (split.length == 1) {
                        if (!Files.exists(NoteblockPlayer.PLAYLISTS_DIR))
                            return true;
                        List<String> playlists = Files.list(NoteblockPlayer.PLAYLISTS_DIR)
                                .filter(Files::isDirectory)
                                .map(Path::getFileName)
                                .map(Path::toString)
                                .collect(Collectors.toList());
                        if (playlists.size() == 0) {
                            NoteblockPlayer.addChatMessage("§6No playlists found");
                        } else {
                            NoteblockPlayer.addChatMessage("§6Playlists: §3" + String.join(", ", playlists));
                        }
                        return true;
                    }
                    int page = 1;
                    if (split.length == 3) {
                        try {
                            page = Integer.parseInt(split[2]);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    } else if (split.length > 3) {
                        return false;
                    }
                    List<String> playlistIndex = Playlist.listSongs(playlistDir);
                    if (playlistIndex.isEmpty()) {
                        NoteblockPlayer.addChatMessage("§6Playlist is empty");
                    } else {
                        ArrayList<Text> entries = new ArrayList<>();
                        int index = 1;
                        for (String songName : playlistIndex) {
                            entries.add(Text.literal(String.format("§6%d. §3%s", index, songName)));
                            index++;
                        }
                        Paginator paginator = new Paginator(entries, "§6Playlist: " + split[1],
                                "/" + CommandManager.COMMAND_ROOT + " playlist list " + split[1], page, 10);
                        paginator.display();
                    }
                    return true;
                }
                case "addsong": {
                    if (split.length < 3)
                        return false;
                    String location = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
                    Playlist.addSong(playlistDir, NoteblockPlayer.SONGS_DIR.resolve(location));
                    NoteblockPlayer.addChatMessage(String.format("§6Added §3%s §6to §3%s", location, split[1]));
                    return true;
                }
                case "removesong": {
                    if (split.length < 3)
                        return false;
                    String location = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
                    Playlist.removeSong(playlistDir, location);
                    NoteblockPlayer.addChatMessage(String.format("§6Removed §3%s §6from §3%s", location, split[1]));
                    return true;
                }
                case "renamesong": {
                    if (split.length < 4)
                        return false;
                    String location = String.join(" ", Arrays.copyOfRange(split, 3, split.length));
                    int index = 0;
                    try {
                        index = Integer.parseInt(split[2]);
                    } catch (Exception e) {
                        NoteblockPlayer.addChatMessage(String.format("§cIndex must be an integer"));
                        return true;
                    }
                    String oldName = Playlist.renameSong(playlistDir, index - 1, location);
                    NoteblockPlayer.addChatMessage(String.format("§6Renamed §3%s §6to §3%s", oldName, location));
                    return true;
                }
                case "loop": {
                    if (split.length != 1)
                        return false;
                    NoteblockPlayer.getConfig().loopPlaylists = !NoteblockPlayer.getConfig().loopPlaylists;
                    SongHandler.getInstance().setPlaylistLoop(NoteblockPlayer.getConfig().loopPlaylists);
                    if (NoteblockPlayer.getConfig().loopPlaylists) {
                        NoteblockPlayer.addChatMessage("§6Enabled playlist looping");
                    } else {
                        NoteblockPlayer.addChatMessage("§6Disabled playlist looping");
                    }

                    return true;
                }
                case "shuffle": {
                    if (split.length != 1)
                        return false;
                    NoteblockPlayer.getConfig().shufflePlaylists = !NoteblockPlayer.getConfig().shufflePlaylists;
                    SongHandler.getInstance().setPlaylistShuffle(NoteblockPlayer.getConfig().shufflePlaylists);
                    if (NoteblockPlayer.getConfig().shufflePlaylists) {
                        NoteblockPlayer.addChatMessage("§6Enabled playlist shuffling");
                    } else {
                        NoteblockPlayer.addChatMessage("§6Disabled playlist shuffling");
                    }

                    return true;
                }
                default: {
                    return false;
                }
            }
        } catch (IOException e) {
            NoteblockPlayer.addChatMessage("§c" + e.getMessage());
            return true;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        String[] split = args.split(" ", -1);
        if (split.length <= 1) {
            return CommandSource.suggestMatching(new String[] {
                    "play",
                    "create",
                    "delete",
                    "list",
                    "addSong",
                    "removeSong",
                    "renameSong",
                    "loop",
                    "shuffle",
            }, suggestionsBuilder);
        }
        switch (split[0].toLowerCase(Locale.ROOT)) {
            case "create":
            case "loop":
            case "shuffle":
            default: {
                return null;
            }
            case "play":
            case "list":
            case "delete": {
                if (split.length == 2) {
                    return Utils.givePlaylistSuggestions(suggestionsBuilder);
                }
                return null;
            }
            case "addsong": {
                if (split.length == 2) {
                    return Utils.givePlaylistSuggestions(suggestionsBuilder);
                } else if (split.length >= 3) {
                    String location = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
                    return Utils.giveSongSuggestions(location, suggestionsBuilder);
                }
                return null;
            }
            case "removesong": {
                if (split.length == 2) {
                    return Utils.givePlaylistSuggestions(suggestionsBuilder);
                } else if (split.length == 3) {
                    Path playlistDir = NoteblockPlayer.PLAYLISTS_DIR.resolve(split[1]);
                    Stream<Path> playlistFiles = Playlist.getSongFiles(playlistDir);
                    if (playlistFiles == null) {
                        return null;
                    }
                    return CommandSource.suggestMatching(
                            playlistFiles.map(Path::getFileName)
                                    .map(Path::toString),
                            suggestionsBuilder);
                }
                return null;
            }
            case "renamesong": {
                if (split.length == 2) {
                    return Utils.givePlaylistSuggestions(suggestionsBuilder);
                } else if (split.length == 3) {
                    Path playlistDir = NoteblockPlayer.PLAYLISTS_DIR.resolve(split[1]);
                    Stream<Path> playlistFiles = Playlist.getSongFiles(playlistDir);
                    if (playlistFiles == null) {
                        return null;
                    }
                    int max = playlistFiles.toList().size();
                    Stream<String> suggestions = IntStream.range(1, max + 1).mapToObj(Integer::toString);
                    return CommandSource.suggestMatching(suggestions, suggestionsBuilder);
                }
                return null;
            }
        }
    }
}
