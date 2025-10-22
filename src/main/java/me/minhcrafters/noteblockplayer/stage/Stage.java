package me.minhcrafters.noteblockplayer.stage;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.song.Note;
import me.minhcrafters.noteblockplayer.utils.Utils;
import me.minhcrafters.noteblockplayer.song.Instrument;
import me.minhcrafters.noteblockplayer.song.Layer;
import me.minhcrafters.noteblockplayer.song.Song;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.stream.Collectors;

public class Stage {
    private final MinecraftClient mc = NoteblockPlayer.mc;

    public enum StageType {
        DEFAULT, WIDE, SPHERICAL, STEREO
    }

    private Song currentSong;
    public String worldName;
    public String serverIdentifier;
    public BlockPos playerPosition;
    public HashMap<Integer, BlockPos> noteblockPositions = new HashMap<>();

    private float playerYaw = 0;

    public LinkedList<BlockPos> requiredBreaks = new LinkedList<>();
    public TreeSet<Integer> missingNotes = new TreeSet<>();
    public int totalMissingNotes = 0;

    public LinkedList<BlockPos> requiredClicks = new LinkedList<>();

    public Stage() {
        playerPosition = mc.player.getBlockPos();

        playerYaw = mc.player.getYaw();

        worldName = Utils.getWorldName();
        serverIdentifier = Utils.getServerIdentifier();
        System.out.println("Server identifier: " + serverIdentifier);
    }

    public void movePlayerToStagePosition() {
        mc.player.refreshPositionAndAngles(playerPosition.getX() + 0.5, playerPosition.getY() + 0.0,
                playerPosition.getZ() + 0.5, mc.player.getYaw(), mc.player.getPitch());
        mc.player.setVelocity(Vec3d.ZERO);
        sendMovementPacketToStagePosition();
    }

    public void sendMovementPacketToStagePosition() {
        if (NoteblockPlayer.fakePlayer != null) {
            NoteblockPlayer.mc.getNetworkHandler()
                    .sendPacket(new PlayerMoveC2SPacket.Full(playerPosition.getX() + 0.5, playerPosition.getY(),
                            playerPosition.getZ() + 0.5, NoteblockPlayer.fakePlayer.getYaw(),
                            NoteblockPlayer.fakePlayer.getPitch(), true, false));
        } else {
            NoteblockPlayer.mc.getNetworkHandler()
                    .sendPacket(new PlayerMoveC2SPacket.Full(playerPosition.getX() + 0.5, playerPosition.getY(),
                            playerPosition.getZ() + 0.5, NoteblockPlayer.mc.player.getYaw(),
                            NoteblockPlayer.mc.player.getPitch(), true, false));
        }
    }

    @SuppressWarnings("static-access")
    public void checkBuildStatus(Song song) {
        this.currentSong = song;

        noteblockPositions.clear();
        missingNotes.clear();

        for (int i = 0; i < 400; i++) {
            int j = i;
            song.getLayers().forEach((layer) -> {
                if (layer.requiredNotes[j]) {
                    missingNotes.add(j);
                }
            });
        }

        ArrayList<BlockPos> noteblockLocations = new ArrayList<>();
        HashSet<BlockPos> breakLocations = new HashSet<>();
        switch (NoteblockPlayer.getConfig().stageType) {
            case DEFAULT -> loadDefaultBlocks(noteblockLocations, breakLocations);
            case WIDE -> loadWideBlocks(noteblockLocations, breakLocations);
            case SPHERICAL -> loadSphericalBlocks(noteblockLocations, breakLocations);
            case STEREO -> loadStereoBlocks(noteblockLocations, breakLocations);
        }

        if (NoteblockPlayer.getConfig().stageType != StageType.STEREO) {
            noteblockLocations.sort((a, b) -> {
                int a_dy = a.getY() - playerPosition.getY();
                int b_dy = b.getY() - playerPosition.getY();
                if (a_dy == -1)
                    a_dy = 0;
                if (b_dy == -1)
                    b_dy = 0;
                if (Math.abs(a_dy) < Math.abs(b_dy)) {
                    return -1;
                } else if (Math.abs(a_dy) > Math.abs(b_dy)) {
                    return 1;
                }
                int a_dx = a.getX() - playerPosition.getX();
                int a_dz = a.getZ() - playerPosition.getZ();
                int b_dx = b.getX() - playerPosition.getX();
                int b_dz = b.getZ() - playerPosition.getZ();
                int a_dist = a_dx * a_dx + a_dz * a_dz;
                int b_dist = b_dx * b_dx + b_dz * b_dz;
                if (a_dist < b_dist) {
                    return -1;
                } else if (a_dist > b_dist) {
                    return 1;
                }
                double a_angle = Math.atan2(a_dz, a_dx);
                double b_angle = Math.atan2(b_dz, b_dx);
                return Double.compare(a_angle, b_angle);
            });
        }

        ArrayList<BlockPos> unusedNoteblockLocations = new ArrayList<>();
        for (BlockPos nbPos : noteblockLocations) {
            BlockState bs = NoteblockPlayer.mc.world.getBlockState(nbPos);
            int blockId = Block.getRawIdFromState(bs);
            if (blockId >= NoteblockPlayer.NOTEBLOCK_BASE_ID && blockId < NoteblockPlayer.NOTEBLOCK_BASE_ID + 800) {
                int noteId = (blockId - NoteblockPlayer.NOTEBLOCK_BASE_ID) / 2;
                if (missingNotes.contains(noteId)) {
                    missingNotes.remove(noteId);
                    noteblockPositions.put(noteId, nbPos);
                } else {
                    unusedNoteblockLocations.add(nbPos);
                }
            } else {
                unusedNoteblockLocations.add(nbPos);
            }
        }

        if (missingNotes.size() > unusedNoteblockLocations.size()) {
            while (missingNotes.size() > unusedNoteblockLocations.size()) {
                missingNotes.pollLast();
            }
        }

        int idx = 0;
        for (int noteId : missingNotes) {
            BlockPos bp = unusedNoteblockLocations.get(idx++);
            noteblockPositions.put(noteId, bp);
        }

        for (BlockPos bp : noteblockPositions.values()) {
            breakLocations.add(bp.up());
        }

        requiredBreaks = breakLocations.stream().filter((bp) -> {
            BlockState bs = NoteblockPlayer.mc.world.getBlockState(bp);
            return !bs.isAir() && bs.getFluidState().isEmpty();
        }).sorted((a, b) -> {
            if (a.getY() < b.getY()) {
                return -1;
            } else if (a.getY() > b.getY()) {
                return 1;
            }
            int a_dx = a.getX() - playerPosition.getX();
            int a_dz = a.getZ() - playerPosition.getZ();
            int b_dx = b.getX() - playerPosition.getX();
            int b_dz = b.getZ() - playerPosition.getZ();
            int a_dist = a_dx * a_dx + a_dz * a_dz;
            int b_dist = b_dx * b_dx + b_dz * b_dz;
            if (a_dist < b_dist) {
                return -1;
            } else if (a_dist > b_dist) {
                return 1;
            }
            double a_angle = Math.atan2(a_dz, a_dx);
            double b_angle = Math.atan2(b_dz, b_dx);
            return Double.compare(a_angle, b_angle);
        }).collect(Collectors.toCollection(LinkedList::new));

        if (requiredBreaks.stream().noneMatch(bp -> withinBreakingDist(bp.getX() - playerPosition.getX(),
                bp.getY() - playerPosition.getY(), bp.getZ() - playerPosition.getZ()))) {
            requiredBreaks.clear();
        }

        totalMissingNotes = missingNotes.size();
    }

    public void checkSurvivalBuildStatus(Song song) throws NotEnoughInstrumentsException {
        noteblockPositions.clear();

        Map<BlockPos, Integer>[] instrumentMap = loadSurvivalBlocks();

        int[] requiredInstruments = new int[16];
        boolean hasMissing = false;
        for (int instrumentId = 0; instrumentId < 16; instrumentId++) {
            for (int pitch = 0; pitch < 25; pitch++) {
                int noteId = instrumentId * 25 + pitch;
                int finalInstrumentId = instrumentId;
                song.getLayers().forEach((layer) -> {
                    if (layer.requiredNotes[noteId]) {
                        requiredInstruments[finalInstrumentId]++;
                    }
                });
            }
            if (requiredInstruments[instrumentId] > instrumentMap[instrumentId].size()) {
                hasMissing = true;
            }
        }

        if (hasMissing) {
            int[] foundInstruments = new int[16];
            for (int i = 0; i < 16; i++) {
                foundInstruments[i] = instrumentMap[i].size();
            }
            throw new NotEnoughInstrumentsException(requiredInstruments, foundInstruments);
        }

        for (int noteid = 0; noteid < 400; noteid++) {
            int finalNoteid = noteid;
            song.getLayers().forEach((layer) -> {
                if (layer.requiredNotes[finalNoteid]) {
                    int instrumentId = finalNoteid / 25;
                    int targetPitch = finalNoteid % 25;
                    Map.Entry<BlockPos, Integer> closest = instrumentMap[instrumentId].entrySet().stream()
                            .min((a, b) -> {
                                int adist = (targetPitch - a.getValue() + 25) % 25;
                                int bdist = (targetPitch - b.getValue() + 25) % 25;
                                return Integer.compare(adist, bdist);
                            }).get();
                    BlockPos bp = closest.getKey();
                    int closestPitch = closest.getValue();
                    instrumentMap[instrumentId].remove(bp);
                    noteblockPositions.put(finalNoteid, bp);
                    int repetitions = (targetPitch - closestPitch + 25) % 25;
                    for (int i = 0; i < repetitions; i++) {
                        requiredClicks.add(bp);
                    }
                }
            });
        }
    }

    public static class NotEnoughInstrumentsException extends Exception {
        public int[] requiredInstruments;
        public int[] foundInstruments;

        public NotEnoughInstrumentsException(int[] requiredInstruments, int[] foundInstruments) {
            this.requiredInstruments = requiredInstruments;
            this.foundInstruments = foundInstruments;
        }

        public void giveInstrumentSummary() {
            NoteblockPlayer.addChatMessage("§c------------------------------");
            NoteblockPlayer.addChatMessage("§cMissing instruments required to play song:");
            for (int instrumentId = 0; instrumentId < 16; instrumentId++) {
                if (requiredInstruments[instrumentId] > 0) {
                    Instrument instrument = Instrument.getInstrumentFromId(instrumentId);
                    NoteblockPlayer.addChatMessage(
                            String.format("    §3%s (%s): §%s%d/%d", instrument.name(), instrument.material,
                                    foundInstruments[instrumentId] < requiredInstruments[instrumentId] ? "c" : "a",
                                    foundInstruments[instrumentId], requiredInstruments[instrumentId]));
                }
            }
            NoteblockPlayer.addChatMessage("§c------------------------------");
        }
    }

    void loadDefaultBlocks(Collection<BlockPos> noteblockLocations, Collection<BlockPos> breakLocations) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (Math.abs(dx) == 4 && Math.abs(dz) == 4) {
                    noteblockLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 0,
                            playerPosition.getZ() + dz));
                    noteblockLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 2,
                            playerPosition.getZ() + dz));
                    breakLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 1,
                            playerPosition.getZ() + dz));
                } else {
                    noteblockLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() - 1,
                            playerPosition.getZ() + dz));
                    noteblockLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 2,
                            playerPosition.getZ() + dz));
                    breakLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 0,
                            playerPosition.getZ() + dz));
                    breakLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 1,
                            playerPosition.getZ() + dz));
                }
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (withinBreakingDist(dx, -3, dz)) {
                    noteblockLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() - 3,
                            playerPosition.getZ() + dz));
                }
                if (withinBreakingDist(dx, 4, dz)) {
                    noteblockLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 4,
                            playerPosition.getZ() + dz));
                }
            }
        }
    }

    void loadWideBlocks(Collection<BlockPos> noteblockLocations, Collection<BlockPos> breakLocations) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (withinBreakingDist(dx, 2, dz)) {
                    noteblockLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 2,
                            playerPosition.getZ() + dz));
                    if (withinBreakingDist(dx, -1, dz)) {
                        noteblockLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() - 1,
                                playerPosition.getZ() + dz));
                        breakLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 0,
                                playerPosition.getZ() + dz));
                        breakLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 1,
                                playerPosition.getZ() + dz));
                    } else if (withinBreakingDist(dx, 0, dz)) {
                        noteblockLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 0,
                                playerPosition.getZ() + dz));
                        breakLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 1,
                                playerPosition.getZ() + dz));
                    }
                }
                if (withinBreakingDist(dx, -3, dz)) {
                    noteblockLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() - 3,
                            playerPosition.getZ() + dz));
                }
                if (withinBreakingDist(dx, 4, dz)) {
                    noteblockLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + 4,
                            playerPosition.getZ() + dz));
                }
            }
        }
    }

    void loadStereoBlocks(Collection<BlockPos> noteblockLocations, Collection<BlockPos> breakLocations) {
        @SuppressWarnings("static-access")
        int maxRadius = NoteblockPlayer.getConfig().maxRadius;
        double maxAngleRad = Math.toRadians(180);
        double layerHeight = 1.0;

        float normalizedYaw = (playerYaw % 360 + 360) % 360;

        int facingQuadrant;

        if (normalizedYaw >= 315 || normalizedYaw < 45) {
            facingQuadrant = 0;
        } else if (normalizedYaw >= 45 && normalizedYaw < 135) {
            facingQuadrant = 90;
        } else if (normalizedYaw >= 135 && normalizedYaw < 225) {
            facingQuadrant = 180;
        } else {
            facingQuadrant = 270;
        }

        double playerRotationRad = Math.toRadians(facingQuadrant);

        ArrayList<Layer> songLayers = currentSong.getLayers();
        int actualLayerCount = songLayers.size();
        if (actualLayerCount == 0)
            return;

        Map<Integer, Integer> noteToLayerIndex = new HashMap<>();

        for (int i = 0; i < songLayers.size(); i++) {
            Layer layer = songLayers.get(i);
            for (int noteId = 0; noteId < 400; noteId++) {
                if (layer.requiredNotes[noteId]) {
                    noteToLayerIndex.put(noteId, i);
                }
            }
        }

        Set<String> usedXZ = new HashSet<>();

        Map<Integer, Note> earliestNote = new HashMap<>();
        for (Note note : currentSong.getTotalNotes()) {
            if (!earliestNote.containsKey(note.noteId) || note.time < earliestNote.get(note.noteId).time) {
                earliestNote.put(note.noteId, note);
            }
        }

        List<Integer> requiredNoteIds = new ArrayList<>();
        for (int noteId = 0; noteId < 400; noteId++) {
            for (Layer layer : songLayers) {
                if (layer.requiredNotes[noteId]) {
                    requiredNoteIds.add(noteId);
                    break;
                }
            }
        }

        requiredNoteIds.sort(Comparator.comparingInt((Integer n) -> -earliestNote.get(n).velocity)
                .thenComparingLong(n -> earliestNote.get(n).time)
                .thenComparingInt(n -> noteToLayerIndex.get(n))
                .thenComparingInt(n -> n % 25));

        for (int noteId : requiredNoteIds) {
            Note currentNote = earliestNote.get(noteId);
            int velocity = currentNote.velocity;
            int pan = currentSong.getLayers().get(noteToLayerIndex.get(noteId)).panning;

            double hearingRange = 48.0;
            double effectiveMaxRadius = Math.min(maxRadius, hearingRange);
            double baseRadius = 1 + ((100 - velocity) / 100.0) * (effectiveMaxRadius - 1);
            double baseAngle = ((pan + 100.0) / 200.0) * maxAngleRad + playerRotationRad;

            int layerIndex = noteToLayerIndex.get(noteId);
            int dy = (int) Math.floor(layerIndex * layerHeight)
                    - (int) Math.floor((actualLayerCount - 1) * layerHeight / 2.0);

            BlockPos chosen = null;
            boolean overlapping = false;

            double deltaAngle = Math.toRadians(5);

            outer: for (int rStep = 1; rStep <= 16; rStep++) {
                double radius = baseRadius + rStep;
                for (int aStep = -6; aStep <= 6; aStep++) {
                    double angle = baseAngle + aStep * deltaAngle;
                    int dx = MathHelper.floor(radius * Math.cos(angle));
                    int dz = MathHelper.floor(radius * Math.sin(angle));
                    String key = (playerPosition.getX() + dx) + "," + (playerPosition.getZ() + dz);
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (!usedXZ.contains(key) && distance <= hearingRange) {
                        chosen = playerPosition.add(dx, dy, dz);
                        usedXZ.add(key);
                        break outer;
                    }
                }
                if (rStep >= 8) {
                    overlapping = true;
                }
            }

            if (chosen == null && overlapping) {
                double oppositeAngle = baseAngle + Math.PI;

                outer: for (int rStep = 0; rStep <= 8; rStep++) {
                    double radius = baseRadius + rStep;
                    for (int aStep = -6; aStep <= 6; aStep++) {
                        double angle = oppositeAngle + aStep * deltaAngle;
                        int dx = MathHelper.floor(radius * Math.cos(angle));
                        int dz = MathHelper.floor(radius * Math.sin(angle));
                        String key = (playerPosition.getX() + dx) + "," + (playerPosition.getZ() + dz);
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (!usedXZ.contains(key) && distance <= hearingRange) {
                            chosen = playerPosition.add(dx, dy, dz);
                            usedXZ.add(key);
                            break outer;
                        }
                    }
                }

            }

            if (chosen == null) {

                for (int r = 1; r <= effectiveMaxRadius * 2 && chosen == null; r++) {
                    for (int angle = 0; angle < 360; angle += 5) {
                        double rad = Math.toRadians(angle) + playerRotationRad;
                        int dx = MathHelper.floor(r * Math.cos(rad));
                        int dz = MathHelper.floor(r * Math.sin(rad));
                        String key = (playerPosition.getX() + dx) + "," + (playerPosition.getZ() + dz);
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (!usedXZ.contains(key) && distance <= hearingRange) {
                            chosen = playerPosition.add(dx, dy, dz);
                            usedXZ.add(key);
                            break;
                        }
                    }
                }
            }

            if (chosen == null) {

                for (int yOffset = 1; yOffset <= 5 && chosen == null; yOffset++) {
                    int altDy = dy + (yOffset % 2 == 0 ? yOffset / 2 : -yOffset / 2);

                    for (int r = 1; r <= effectiveMaxRadius && chosen == null; r++) {
                        for (int angle = 0; angle < 360; angle += 10) {
                            double rad = Math.toRadians(angle) + playerRotationRad;
                            int dx = MathHelper.floor(r * Math.cos(rad));
                            int dz = MathHelper.floor(r * Math.sin(rad));
                            String key = (playerPosition.getX() + dx) + "," + (playerPosition.getZ() + dz);
                            double distance = Math.sqrt(dx * dx + altDy * altDy + dz * dz);
                            if (!usedXZ.contains(key) && distance <= hearingRange) {
                                chosen = playerPosition.add(dx, altDy, dz);
                                usedXZ.add(key);
                                break;
                            }
                        }
                    }
                }
            }

            if (chosen == null) {

                for (int spiral = 1; spiral <= effectiveMaxRadius * 3 && chosen == null; spiral++) {
                    for (int dx = -spiral; dx <= spiral && chosen == null; dx++) {
                        for (int dz = -spiral; dz <= spiral && chosen == null; dz++) {
                            if (Math.abs(dx) != spiral && Math.abs(dz) != spiral)
                                continue;

                            String key = (playerPosition.getX() + dx) + "," + (playerPosition.getZ() + dz);
                            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (!usedXZ.contains(key) && distance <= hearingRange) {
                                chosen = playerPosition.add(dx, dy, dz);
                                usedXZ.add(key);
                            }
                        }
                    }
                }
            }

            noteblockLocations.add(chosen);
        }
    }

    void loadSphericalBlocks(Collection<BlockPos> noteblockLocations, Collection<BlockPos> breakLocations) {
        final int maxRange = 5;
        int[] yLayers = { -4, -2, -1, 0, 1, 2, 3, 4, 5, 6 };

        for (int dx = -maxRange; dx <= maxRange; dx++) {
            for (int dz = -maxRange; dz <= maxRange; dz++) {
                for (int dy : yLayers) {
                    int adx = Math.abs(dx);
                    int adz = Math.abs(dz);
                    switch (dy) {
                        case -4: {
                            if (adx < 3 && adz < 3) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            if ((adx == 3 ^ adz == 3) && (adx == 0 ^ adz == 0)) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case -2: {
                            if (adz == 0 && adx == 0) {
                                break;
                            }
                            if (adz * adx > 9) {
                                break;
                            }
                            if (adz + adx == 5 && adx != 0 && adz != 0) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy + 1, playerPosition.getZ() + dz));
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy - 1, playerPosition.getZ() + dz));
                                break;
                            }
                            if (adz * adx == 3) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy + 1, playerPosition.getZ() + dz));
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy - 1, playerPosition.getZ() + dz));
                                break;
                            }
                            if (adx < 3 && adz < 3 && adx + adz > 0) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                breakLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy + 2, playerPosition.getZ() + dz));
                                break;
                            }
                            if (adz == 0 ^ adx == 0) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                breakLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy + 2, playerPosition.getZ() + dz));
                                break;
                            }
                            if (adz * adx == 2 * maxRange) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                breakLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy + 2, playerPosition.getZ() + dz));
                                break;
                            }
                            if (adz + adx == 6) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                if ((adx == maxRange) ^ (adz == maxRange)) {
                                    breakLocations.add(new BlockPos(playerPosition.getX() + dx,
                                            playerPosition.getY() + dy + 2, playerPosition.getZ() + dz));
                                }
                                break;
                            }
                            break;
                        }
                        case -1: {
                            if (adx + adz == 7 || adx + adz == 0) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 0: {
                            int check = adx + adz;
                            if ((check == 8 || check == 6) && adx * adz > 5) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 1: {
                            int addl1 = adx + adz;
                            if (addl1 == 7 || addl1 == 3 || addl1 == 2) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            if ((adx == maxRange) ^ (adz == maxRange) && addl1 < 7) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            if (addl1 == 4 && adx * adz != 0) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            if (adx + adz < 7) {
                                breakLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + dy,
                                        playerPosition.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 2: {
                            int addl2 = adx + adz;
                            if (adx == maxRange || adz == maxRange) {
                                break;
                            }
                            if (addl2 == 8 || addl2 == 6 || addl2 == 5 || addl2 == 1) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            if ((addl2 == 4) && (adx == 0 ^ adz == 0)) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            if (addl2 == 0) {
                                breakLocations.add(new BlockPos(playerPosition.getX() + dx, playerPosition.getY() + dy,
                                        playerPosition.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 3: {
                            if (adx * adz == 12 || adx + adz == 0) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            if ((adx == maxRange) ^ (adz == maxRange) && ((adx < 2) ^ (adz < 2))) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            if (adx > 3 || adz > 3) {
                                break;
                            }
                            if (adx + adz > 1 && adx + adz < 5) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 4: {
                            if (adx == maxRange || adz == maxRange) {
                                break;
                            }
                            if (adx + adz == 4 && adx * adz == 0) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            int addl4 = adx + adz;
                            if (addl4 == 1 || addl4 == 5 || addl4 == 6) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 5: {
                            if (adx > 3 || adz > 3) {
                                break;
                            }
                            int addl5 = adx + adz;
                            if (addl5 > 1 && addl5 < 5) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 6: {
                            if (adx + adz < 2) {
                                noteblockLocations.add(new BlockPos(playerPosition.getX() + dx,
                                        playerPosition.getY() + dy, playerPosition.getZ() + dz));
                                break;
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    Map<BlockPos, Integer>[] loadSurvivalBlocks() {
        @SuppressWarnings("unchecked")
        Map<BlockPos, Integer>[] instrumentMap = new Map[16];
        for (int i = 0; i < 16; i++) {
            instrumentMap[i] = new TreeMap<>();
        }
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy : new int[] { -1, 0, 1, 2, -2, 3, -3, 4, -4, 5, 6 }) {
                    BlockPos bp = playerPosition.add(dx, dy, dz);
                    BlockState bs = NoteblockPlayer.mc.world.getBlockState(bp);
                    BlockState aboveBs = NoteblockPlayer.mc.world.getBlockState(bp.up());
                    int blockId = Block.getRawIdFromState(bs);
                    if (blockId >= NoteblockPlayer.NOTEBLOCK_BASE_ID
                            && blockId < NoteblockPlayer.NOTEBLOCK_BASE_ID + 800 && aboveBs.isAir()) {
                        int noteId = (blockId - NoteblockPlayer.NOTEBLOCK_BASE_ID) / 2;
                        int instrument = noteId / 25;
                        int pitch = noteId % 25;
                        instrumentMap[instrument].put(bp, pitch);
                    }
                }
            }
        }
        return instrumentMap;
    }

    boolean withinBreakingDist(int dx, int dy, int dz) {
        double dy1 = dy + 0.5 - 1.62;
        double dy2 = dy + 0.5 - 1.27;
        return dx * dx + dy1 * dy1 + dz * dz < 5.99999 * 5.99999 && dx * dx + dy2 * dy2 + dz * dz < 5.99999 * 5.99999;
    }

    @SuppressWarnings("static-access")
    public boolean nothingToBuild() {
        if (!NoteblockPlayer.getConfig().survivalOnly) {
            return requiredBreaks.isEmpty() && missingNotes.isEmpty();
        } else {
            return requiredClicks.isEmpty();
        }
    }

    public boolean hasBreakingModification() {
        for (Map.Entry<Integer, BlockPos> entry : noteblockPositions.entrySet()) {
            BlockState bs = NoteblockPlayer.mc.world.getBlockState(entry.getValue());
            int blockId = Block.getRawIdFromState(bs);
            int actualNoteId = (blockId - NoteblockPlayer.NOTEBLOCK_BASE_ID) / 2;
            if (actualNoteId < 0 || actualNoteId >= 400) {
                return true;
            }
            int actualInstrument = actualNoteId / 25;
            int actualPitch = actualNoteId % 25;
            int targetInstrument = entry.getKey() / 25;
            int targetPitch = entry.getKey() % 25;
            if (targetPitch != actualPitch) {
                return true;
            }
            if (targetInstrument != actualInstrument) {
                return true;
            }

            BlockState aboveBs = NoteblockPlayer.mc.world.getBlockState(entry.getValue().up());
            if (!aboveBs.isAir() && aboveBs.getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public Vec3d getOriginBottomCenter() {
        return Vec3d.ofBottomCenter(playerPosition);
    }
}