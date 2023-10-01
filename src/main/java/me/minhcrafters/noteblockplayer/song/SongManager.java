package me.minhcrafters.noteblockplayer.song;

import me.minhcrafters.noteblockplayer.FakePlayerEntity;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import me.minhcrafters.noteblockplayer.stage.Stage;
import me.minhcrafters.noteblockplayer.utils.ProgressDisplay;
import me.minhcrafters.noteblockplayer.utils.TimeUtils;
import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.io.IOException;
import java.util.LinkedList;

import static me.minhcrafters.noteblockplayer.NoteblockPlayer.MOD_ID;
import static me.minhcrafters.noteblockplayer.NoteblockPlayer.mc;

public class SongManager {
    private static SongManager instance = null;
    private final String[] instrumentNames = {"harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar", "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling"};
    public SongLoaderThread loaderThread = null;
    public LinkedList<Song> songQueue = new LinkedList<>();
    public Song currentSong = null;
    public Stage stage = null;
    public boolean building = false;
    public boolean wasFlying = false;
    public GameMode originalGamemode = GameMode.CREATIVE;
    // Runs every tick
    private int buildStartDelay = 0;
    private int buildEndDelay = 0;
    private int buildCooldown = 0;
    private int buildSlot = -1;
    private ItemStack prevHeldItem = null;
    private long lastCommandTime = System.currentTimeMillis();
    private String cachedCommand = null;

    private SongManager() {
    }

    public static SongManager getInstance() {
        if (instance == null) {
            instance = new SongManager();
        }
        return instance;
    }

    public void onUpdate(boolean tick) {
        if (currentSong == null && songQueue.size() > 0) {
            setSong(songQueue.poll());
        }

        if (loaderThread != null && !loaderThread.isAlive()) {
            if (loaderThread.exception != null) {
                NoteblockPlayer.addChatMessage(Text.of("§cFailed to load file: §4" + loaderThread.exception.getMessage()));
            } else {
                if (currentSong == null) {
                    setSong(loaderThread.song);
                } else {
                    queueSong(loaderThread.song);
                }
            }
            loaderThread = null;
        }

        if (currentSong == null) {
            if (stage != null || NoteblockPlayer.fakePlayer != null) {
                if (stage != null) {
                    stage.movePlayerToStagePosition();
                }
                restoreStateAndCleanUp();
            } else {
                originalGamemode = mc.interactionManager.getCurrentGameMode();
            }
            return;
        }

        if (stage == null) {
            stage = new Stage();
            stage.movePlayerToStagePosition();
        }

        if (NoteblockPlayer.showFakePlayer && NoteblockPlayer.fakePlayer == null) {
            NoteblockPlayer.fakePlayer = new FakePlayerEntity();
            NoteblockPlayer.fakePlayer.copyStagePosAndPlayerLook();
        } else if (!NoteblockPlayer.showFakePlayer && NoteblockPlayer.fakePlayer != null) {
            NoteblockPlayer.removeFakePlayer();
        }

        if (NoteblockPlayer.fakePlayer != null) {
            NoteblockPlayer.fakePlayer.getInventory().clone(NoteblockPlayer.mc.player.getInventory());
        }

        mc.player.getAbilities().allowFlying = true;
        wasFlying = mc.player.getAbilities().flying;

        checkCommandCache();

        if (building) {
            if (tick) {
                handleBuilding();
            }
        } else {
            // Check if stage was broken
            handlePlaying(tick);
        }
    }

    public void loadSong(String location) {
        if (loaderThread != null) {
            NoteblockPlayer.addChatMessage(Text.of("§cAlready loading another file."));
        } else {
            try {
                loaderThread = new SongLoaderThread(location);
                NoteblockPlayer.addChatMessage(Text.of("§6Loading §3" + location));
                loaderThread.start();
            } catch (IOException e) {
                NoteblockPlayer.addChatMessage(Text.of("§cFailed to load file: §4" + e.getMessage()));
            }
        }
    }

    public void loadSong(SongLoaderThread thread) {
        if (loaderThread != null) {
            NoteblockPlayer.addChatMessage("§cAlready loading a song, cannot load another.");
        } else {
            loaderThread = thread;
        }
    }

    public void setSong(Song song) {
        currentSong = song;
        building = true;
        setCreativeIfNeeded();

        if (stage == null) {
            stage = new Stage();
            stage.movePlayerToStagePosition();
        } else {
            stage.sendMovementPacketToStagePosition();
        }

        getAndSaveBuildSlot();
        NoteblockPlayer.addChatMessage(Text.translatable(MOD_ID + ".building_noteblocks").formatted(Formatting.GOLD));
    }

    private void queueSong(Song song) {
        songQueue.add(song);
        NoteblockPlayer.addChatMessage(Text.of("§6Added file to queue: §3" + song.name));
    }

    private void handleBuilding() {
        setBuildProgressDisplay();

        if (buildStartDelay > 0) {
            buildStartDelay--;
            return;
        }

        if (buildCooldown > 0) {
            buildCooldown--;
            return;
        }

        ClientWorld world = mc.world;

        if (mc.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
            return;
        }

        if (stage.nothingToBuild()) {
            if (buildEndDelay > 0) {
                buildEndDelay--;
                return;
            } else {
                stage.checkBuildStatus(currentSong);
            }
        }

        if (!stage.requiredBreaks.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                if (stage.requiredBreaks.isEmpty()) break;
                BlockPos bp = stage.requiredBreaks.poll();
                attackBlock(bp);
            }
            buildEndDelay = NoteblockPlayer.getConfig().buildEndDelay;
        } else if (!stage.missingNotes.isEmpty()) {
            int desiredNoteId = stage.missingNotes.pollFirst();
            BlockPos bp = stage.noteblockPositions.get(desiredNoteId);

            if (bp == null) {
                return;
            }

            int blockId = Block.getRawIdFromState(world.getBlockState(bp));
            int currentNoteId = (blockId - NoteblockPlayer.NOTEBLOCK_BASE_ID) / 2;

            if (currentNoteId != desiredNoteId) {
                holdNoteblock(desiredNoteId, buildSlot);
                if (blockId != 0) {
                    attackBlock(bp);
                }
                placeBlock(bp);
            }

            buildCooldown = NoteblockPlayer.getConfig().buildCooldown;
            buildEndDelay = NoteblockPlayer.getConfig().buildEndDelay;
        } else {
            restoreBuildSlot();
            building = false;
            setSurvivalIfNeeded();
            stage.sendMovementPacketToStagePosition();
            NoteblockPlayer.addChatMessage(Text.empty().append(Text.translatable(MOD_ID + ".now_playing").formatted(Formatting.GOLD)).append(Text.of(": §6" + currentSong.name)));
        }
    }

    private void setBuildProgressDisplay() {
        MutableText text = Text.empty().append(Text.translatable(MOD_ID + ".building_noteblocks").formatted(Formatting.GOLD).formatted(Formatting.BOLD));
        MutableText timeText = Text.empty().append(Text.literal((stage.totalMissingNotes - stage.missingNotes.size()) + " / " + stage.totalMissingNotes).formatted(Formatting.DARK_AQUA));
        ProgressDisplay.getInstance().setText(text);
        ProgressDisplay.getInstance().setTimeText(timeText);
        ProgressDisplay.getInstance().setProgress(stage.totalMissingNotes - stage.missingNotes.size());
        ProgressDisplay.getInstance().setMaxProgress(stage.totalMissingNotes);
    }

    // Runs every frame
    private void handlePlaying(boolean tick) {
        if (tick) {
            setPlayProgressDisplay();
        }

        if (mc.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
            currentSong.pause();
            return;
        }

        if (tick) {
            if (stage.hasBreakingModification()) {
                stage.checkBuildStatus(currentSong);
            }
            if (!stage.nothingToBuild()) {
                building = true;
                setCreativeIfNeeded();
                stage.movePlayerToStagePosition();
                currentSong.pause();
                buildStartDelay = NoteblockPlayer.getConfig().buildStartDelay;

                NoteblockPlayer.LOGGER.info("Total missing notes: " + stage.missingNotes.size());

                for (int note : stage.missingNotes) {
                    int pitch = note % 25;
                    int instrumentId = note / 25;
                    NoteblockPlayer.LOGGER.info("Missing note: " + Instrument.getInstrumentFromId(instrumentId).name() + ":" + pitch);
                }

                getAndSaveBuildSlot();
                NoteblockPlayer.addChatMessage(Text.of("§6Stage modification detected. Rebuilding..."));
                return;
            }
        }

        currentSong.play();

        boolean somethingPlayed = false;
        currentSong.advanceTime();
        while (currentSong.reachedNextNote()) {
            Note note = currentSong.getNextNote();
            BlockPos bp = stage.noteblockPositions.get(note.noteId);
            if (bp != null) {
                attackBlock(bp);
                somethingPlayed = true;
            }
        }

        if (somethingPlayed) {
            stopAttack();
        }

        if (currentSong.finished()) {
            // NoteblockPlayer.addChatMessage("§6Done playing §3" + currentSong.name);
            MutableText text = Text.empty().append(Text.translatable(MOD_ID + ".finished_playing").formatted(Formatting.GOLD).formatted(Formatting.BOLD)).append(Text.literal(": ").formatted(Formatting.GOLD).formatted(Formatting.BOLD)).append(Text.literal(currentSong.name).formatted(Formatting.BLUE).formatted(Formatting.ITALIC));

            ProgressDisplay.getInstance().setText(text);

            if (mc.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL) {
                // mc.interactionManager.setGameMode(GameMode.CREATIVE);
                sendCachedCommand("gamemode creative");
            }

            currentSong = null;
        }
    }

    public void setPlayProgressDisplay() {
        long currentTime = Math.min(currentSong.time, currentSong.length);
        long totalTime = currentSong.length;
        MutableText text = Text.empty().append(Text.translatable(MOD_ID + ".now_playing").formatted(Formatting.GOLD).formatted(Formatting.BOLD)).append(Text.literal(": ").formatted(Formatting.GOLD).formatted(Formatting.BOLD)).append(Text.literal(currentSong.name).formatted(Formatting.BLUE).formatted(Formatting.ITALIC));

        if (currentSong.looping) {
            if (currentSong.loopCount > 0) {
                text.append(Text.literal(String.format(" (Loop [%d/%d])", currentSong.currentLoop, currentSong.loopCount)).formatted(Formatting.GOLD));
            } else {
                text.append(Text.literal(" (Looping enabled)").formatted(Formatting.GOLD));
            }
        }

        MutableText timeText = Text.empty().append(Text.literal(String.format("%s / %s", TimeUtils.formatTime(currentTime), TimeUtils.formatTime(totalTime))).formatted(Formatting.DARK_AQUA));

        if (currentSong.paused) {
            text.append(Text.literal(" (Paused)").formatted(Formatting.RED));
        }

        ProgressDisplay.getInstance().setText(text);
        ProgressDisplay.getInstance().setTimeText(timeText);
        ProgressDisplay.getInstance().setProgress((int) currentTime);
        ProgressDisplay.getInstance().setMaxProgress((int) totalTime);
    }

    public void cleanup() {
        currentSong = null;
        songQueue.clear();
        stage = null;
        buildSlot = -1;
        NoteblockPlayer.removeFakePlayer();
    }

    public void restoreStateAndCleanUp() {
        if (stage != null) {
            stage.movePlayerToStagePosition();
        }
        if (originalGamemode != NoteblockPlayer.mc.interactionManager.getCurrentGameMode()) {
            if (originalGamemode == GameMode.CREATIVE) {
                sendCachedCommand("gamemode creative");
            } else if (originalGamemode == GameMode.SURVIVAL) {
                sendCachedCommand("gamemode survival");
            }
        }
        restoreBuildSlot();
        cleanup();
    }

    public void onNotIngame() {
        currentSong = null;
        songQueue.clear();
    }

    private void sendCachedCommand(String command) {
        cachedCommand = command;
    }

    private void checkCommandCache() {
        if (cachedCommand != null && System.currentTimeMillis() >= lastCommandTime + 1500) {
            mc.getNetworkHandler().sendCommand(cachedCommand);
            cachedCommand = null;
            lastCommandTime = System.currentTimeMillis();
        }
    }

    private void setCreativeIfNeeded() {
        cachedCommand = null;
        if (mc.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
            // mc.interactionManager.setGameMode(GameMode.CREATIVE);
            sendCachedCommand("gamemode creative");
        }
    }

    private void setSurvivalIfNeeded() {
        cachedCommand = null;
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
            // mc.interactionManager.setGameMode(GameMode.SURVIVAL);
            sendCachedCommand("gamemode survival");
        }
    }

    private void holdNoteblock(int id, int slot) {
        PlayerInventory inventory = mc.player.getInventory();
        inventory.selectedSlot = slot;
        ((ClientPlayerInteractionManagerAccessor) NoteblockPlayer.mc.interactionManager).invokeSyncSelectedSlot();
        int instrument = id / 25;
        int note = id % 25;
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", "minecraft:note_block");
        nbt.putByte("Count", (byte) 1);
        NbtCompound tag = new NbtCompound();
        NbtCompound bsTag = new NbtCompound();
        bsTag.putString("instrument", instrumentNames[instrument]);
        bsTag.putString("note", Integer.toString(note));
        tag.put("BlockStateTag", bsTag);
        nbt.put("tag", tag);
        ItemStack noteblockStack = ItemStack.fromNbt(nbt);
        inventory.main.set(slot, noteblockStack);
        NoteblockPlayer.mc.interactionManager.clickCreativeStack(noteblockStack, 36 + slot);
    }

    private void placeBlock(BlockPos bp) {
        double fx = Math.max(0.0, Math.min(1.0, (stage.position.getX() + 0.5 - bp.getX())));
        double fy = Math.max(0.0, Math.min(1.0, (stage.position.getY() + 0.0 - bp.getY())));
        double fz = Math.max(0.0, Math.min(1.0, (stage.position.getZ() + 0.5 - bp.getZ())));
        fx += bp.getX();
        fy += bp.getY();
        fz += bp.getZ();
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(fx, fy, fz), Direction.UP, bp, false));
        doMovements(fx, fy, fz);
    }

    private void attackBlock(BlockPos bp) {
        mc.interactionManager.attackBlock(bp, Direction.UP);
        doMovements(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5);
    }

    private void stopAttack() {
        mc.interactionManager.cancelBlockBreaking();
    }

    private void doMovements(double lookX, double lookY, double lookZ) {
        if (NoteblockPlayer.getConfig().armSwinging) {
            mc.player.swingHand(Hand.MAIN_HAND);
            if (NoteblockPlayer.fakePlayer != null) {
                NoteblockPlayer.fakePlayer.swingHand(Hand.MAIN_HAND);
            }
        }

        if (NoteblockPlayer.getConfig().bodyRotate) {
            double d = lookX - (stage.position.getX() + 0.5);
            double e = lookY - (stage.position.getY() + mc.player.getStandingEyeHeight());
            double f = lookZ - (stage.position.getZ() + 0.5);
            double g = Math.sqrt(d * d + f * f);
            float pitch = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(e, g) * 57.2957763671875)));
            float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(f, d) * 57.2957763671875) - 90.0f);
            if (NoteblockPlayer.fakePlayer != null) {
                NoteblockPlayer.fakePlayer.setPitch(pitch);
                NoteblockPlayer.fakePlayer.setYaw(yaw);
                NoteblockPlayer.fakePlayer.setHeadYaw(yaw);
            }
            NoteblockPlayer.mc.player.networkHandler.getConnection().send(new PlayerMoveC2SPacket.Full(stage.position.getX() + 0.5, stage.position.getY(), stage.position.getZ() + 0.5, yaw, pitch, true));
        }
    }

    private void getAndSaveBuildSlot() {
        buildSlot = NoteblockPlayer.mc.player.getInventory().getSwappableHotbarSlot();
        prevHeldItem = NoteblockPlayer.mc.player.getInventory().getStack(buildSlot);
        NoteblockPlayer.LOGGER.info(buildSlot);
        NoteblockPlayer.LOGGER.info(prevHeldItem.toString());
    }

    private void restoreBuildSlot() {
        if (buildSlot != -1) {
            NoteblockPlayer.mc.player.getInventory().setStack(buildSlot, prevHeldItem);
            NoteblockPlayer.mc.interactionManager.clickCreativeStack(prevHeldItem, 36 + buildSlot);
            buildSlot = -1;
        }
    }
}