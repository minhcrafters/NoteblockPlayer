package me.minhcrafters.noteblockplayer.utils;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;

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
                NoteblockPlayer.addChatMessage(Text.literal("§cThe list is empty!"));
            } else {
                NoteblockPlayer.addChatMessage(Text.literal("§6-----------------------------------").append("§r\n").append(pageTitle));

                MutableText counter = Text.literal("§6------------- ")
                        .append("§3 [ " + pageNumber + "/" + totalPagesCount + " ] ")
                        .append(" §6-------------");

                NoteblockPlayer.addChatMessage(counter);

                for (int i = (pageNumber - 1) * contentLinesPerPage; i < pageNumber * contentLinesPerPage && i < entries.size(); i++) {
                    NoteblockPlayer.addChatMessage(Text.literal("§6> ").append(entries.get(i)));
                }

                NoteblockPlayer.addChatMessage(Text.literal("§6-----------------------------------"));

                // Add navigation buttons
                if (pageNumber > 1 || pageNumber < totalPagesCount) {
                    MutableText navigation = Text.literal("");
                    if (pageNumber > 1) {
                        Text prev = Text.literal("§3[Previous]").setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandToRun + " " + (pageNumber - 1)))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Go to page " + (pageNumber - 1)))));
                        navigation.append(prev);
                    }
                    if (pageNumber > 1 && pageNumber < totalPagesCount) {
                        navigation.append(Text.literal(" §6| "));
                    }
                    if (pageNumber < totalPagesCount) {
                        Text next = Text.literal("§3[Next]").setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandToRun + " " + (pageNumber + 1)))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Go to page " + (pageNumber + 1)))));
                        navigation.append(next);
                    }
                    NoteblockPlayer.addChatMessage(navigation);
                }
            }
        } else {
            NoteblockPlayer.addChatMessage(Text.literal("§cThere are only §3" + totalPagesCount + "§c pages!"));
        }
    }
}
