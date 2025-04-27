package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.config.ConfigImpl;
import net.minecraft.command.CommandSource;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class Announcement extends Command {
    public String getName() {
        return "announcement";
    }

    public String[] getSyntax() {
        return new String[]{
                "enable",
                "disable",
                "getMessage",
                "setMessage <message>",
        };
    }

    public String getDescription() {
        return "Set an announcement message that is sent when you start playing a song. With setMessage, write [name] where the song name should go.";
    }

    public boolean processCommand(String args) {
        String[] split = args.split(" ", 2);
        switch (split[0].toLowerCase(Locale.ROOT)) {
            case "enable":
                if (split.length != 1) return false;
                ConfigImpl.doAnnouncement = true;
                NoteblockPlayer.addChatMessage("§6Enabled song announcements");
                return true;
            case "disable":
                if (split.length != 1) return false;
                ConfigImpl.doAnnouncement = false;
                NoteblockPlayer.addChatMessage("§6Disabled song announcements");
                return true;
            case "getmessage":
                if (split.length != 1) return false;
                NoteblockPlayer.addChatMessage("§6Current announcement message is §r" + ConfigImpl.announcementMessage);
                return true;
            case "setmessage":
                if (split.length != 2) return false;
                ConfigImpl.announcementMessage = split[1];
                NoteblockPlayer.addChatMessage("§6Set announcement message to §r" + split[1]);
                return true;
            default:
                return false;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        if (!args.contains(" ")) {
            return CommandSource.suggestMatching(new String[]{"enable", "disable", "getMessage", "setMessage"}, suggestionsBuilder);
        } else {
            return null;
        }
    }
}
