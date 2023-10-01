package me.minhcrafters.noteblockplayer.config;

import me.minhcrafters.noteblockplayer.stage.Stage;

public class ConfigImpl extends Config {

    @Entry(name = "Command Prefix")
    public static String commandPrefix = "$";

    @Entry(name = "Stage Design")
    public static Stage.StageType stageType = Stage.StageType.DEFAULT;

    @Entry(min = 0, name = "Build Start Delay")
    public static int buildStartDelay = 20;

    @Entry(min = 0, name = "Build End Delay")
    public static int buildEndDelay = 20;

    @Entry(min = 0, name = "Build Cooldown")
    public static int buildCooldown = 0;

    @Entry(name = "Print block state changes in chat")
    public static boolean blockStateDebug = true;

    @Entry(name = "Enable Arm Swinging")
    public static boolean armSwinging = false;

    @Entry(name = "Enable Fake Player's Body Rotation")
    public static boolean bodyRotate = false;
}
