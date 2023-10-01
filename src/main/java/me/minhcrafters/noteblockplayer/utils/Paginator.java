package me.minhcrafters.noteblockplayer.utils;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;

import static me.minhcrafters.noteblockplayer.NoteblockPlayer.FORCE_COMMAND_PREFIX;

public class Paginator {
    public ArrayList<Text> entries;
    public String pageTitle;
    public int pageNumber;
    public int contentLinesPerPage;
    public String commandToRun;
    private int totalPagesCount = 1;

    public Paginator(ArrayList<Text> entries, String pageTitle, String commandToRun, int pageNumber, int contentLinesPerPage) {
        this.pageTitle = pageTitle;
        this.entries = entries;
        this.commandToRun = commandToRun;
        this.pageNumber = pageNumber;
        this.contentLinesPerPage = contentLinesPerPage;
    }

    public ArrayList<Text> getEntries() {
        return entries;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getTotalPagesCount() {
        return totalPagesCount;
    }

    public int getContentLinesPerPage() {
        return contentLinesPerPage;
    }

    public void display() {
        if ((entries.size() % contentLinesPerPage) == 0) {
            if (!entries.isEmpty()) {
                totalPagesCount = entries.size() / contentLinesPerPage;
            }
        } else {
            totalPagesCount = (entries.size() / contentLinesPerPage) + 1;
        }

        if (pageNumber <= totalPagesCount && pageNumber > 0) {
            if (entries.isEmpty()) {
                NoteblockPlayer.addChatMessage(Text.of("§cThe list is empty!"));
            } else {
                NoteblockPlayer.addChatMessage(Text.literal("§6-----------------------------------").append("§r\n").append(pageTitle));

                MutableText counter = Text.literal("§6------------- ")
                        .append("§3 [ " + pageNumber + "/" + totalPagesCount + " ] ")
                        .append(" §6-------------");

                NoteblockPlayer.addChatMessage(counter);

                for (int i = (pageNumber - 1) * contentLinesPerPage; i < pageNumber * contentLinesPerPage && i < entries.size(); i++) {
                    NoteblockPlayer.addChatMessage(Text.literal("§6> ").append(entries.get(i)));
                }

                NoteblockPlayer.addChatMessage(Text.of("§6-----------------------------------"));
            }
        } else {
            NoteblockPlayer.addChatMessage(Text.of("§cThere are only §3" + totalPagesCount + "§c pages!"));
        }
    }
}
