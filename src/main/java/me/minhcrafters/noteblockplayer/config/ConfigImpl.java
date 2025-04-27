package me.minhcrafters.noteblockplayer.config;

import me.minhcrafters.noteblockplayer.stage.Stage;

public class ConfigImpl extends Config {
    @Entry(name = "Survival-only Mode", category = "general")
    public static boolean survivalOnly = false;

    @Entry(name = "Enable Noclip", category = "general")
    public static boolean flightNoclip = false;

    // Commands
    @Entry(name = "Creative Command", category = "commands")
    public static String creativeCommand = "gmc";

    @Entry(name = "Survival Command", category = "commands")
    public static String survivalCommand = "gms";

    // Playback settings
    @Entry(name = "Loop Playlists", category = "playback")
    public static boolean loopPlaylists = false;

    @Entry(name = "Shuffle Playlists", category = "playback")
    public static boolean shufflePlaylists = false;

    @Entry(name = "Max Note Spacing", category = "playback")
    public static int maxNoteSpacing = 5000; // Maximum time distance (ms) between consecutive notes

    @Entry(name = "Velocity Threshold", category = "playback")
    public static int velocityThreshold = 0;

    // Visual settings
    @Entry(name = "Show Fake Player", category = "visual")
    public static boolean showFakePlayer = false;

    @Entry(name = "Stage Design", category = "visual")
    public static Stage.StageType stageType = Stage.StageType.DEFAULT;

    @Entry(name = "Max Radius", category = "visual")
    public static int maxRadius = 30;

    @Entry(name = "Swing", category = "visual")
    public static boolean swing = false;

    @Entry(name = "Rotate", category = "visual")
    public static boolean rotate = false;

    // Performance settings
    @Entry(name = "Break Speed", category = "performance")
    public static double breakSpeed = 40.0;

    @Entry(name = "Place Speed", category = "performance")
    public static double placeSpeed = 20.0;

    @Entry(name = "Auto Cleanup", category = "performance")
    public static boolean autoCleanup = false;

    // Announcements
    @Entry(name = "Do Announcement", category = "visual")
    public static boolean doAnnouncement = false;

    @Entry(name = "Announcement Message", category = "visual")
    public static String announcementMessage = "&6Now playing: &3[name]";
}
