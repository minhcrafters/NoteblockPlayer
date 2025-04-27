package me.minhcrafters.noteblockplayer.stage;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
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
    public BlockPos position;
    public HashMap<Integer, BlockPos> noteblockPositions = new HashMap<>();

    private float playerYaw = 0;

    // Not used in survival-only mode
    public LinkedList<BlockPos> requiredBreaks = new LinkedList<>();
    public TreeSet<Integer> missingNotes = new TreeSet<>();
    public int totalMissingNotes = 0;

    // Only used in survival-only mode
    public LinkedList<BlockPos> requiredClicks = new LinkedList<>();

    public Stage() {
        position = mc.player.getBlockPos();

        playerYaw = mc.player.getYaw();

        // Information tracked for checking cleanup conditions
        worldName = Utils.getWorldName();
        serverIdentifier = Utils.getServerIdentifier();
        System.out.println("Server identifier: " + serverIdentifier);
    }

    public void movePlayerToStagePosition() {
        mc.player.refreshPositionAndAngles(position.getX() + 0.5, position.getY() + 0.0, position.getZ() + 0.5, mc.player.getYaw(), mc.player.getPitch());
        mc.player.setVelocity(Vec3d.ZERO);
        sendMovementPacketToStagePosition();
    }

    public void sendMovementPacketToStagePosition() {
        if (NoteblockPlayer.fakePlayer != null) {
            NoteblockPlayer.mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(position.getX() + 0.5, position.getY(), position.getZ() + 0.5, NoteblockPlayer.fakePlayer.getYaw(), NoteblockPlayer.fakePlayer.getPitch(), true, false));
        } else {
            NoteblockPlayer.mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(position.getX() + 0.5, position.getY(), position.getZ() + 0.5, NoteblockPlayer.mc.player.getYaw(), NoteblockPlayer.mc.player.getPitch(), true, false));
        }
    }

    public void checkBuildStatus(Song song) {
        this.currentSong = song;

        noteblockPositions.clear();
        missingNotes.clear();

        // Add all required notes to missingNotes
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

        // Sorting noteblock and break locations
        if (NoteblockPlayer.getConfig().stageType != StageType.STEREO) {
            noteblockLocations.sort((a, b) -> {
                // First sort by y
                int a_dy = a.getY() - position.getY();
                int b_dy = b.getY() - position.getY();
                if (a_dy == -1) a_dy = 0; // same layer
                if (b_dy == -1) b_dy = 0; // same layer
                if (Math.abs(a_dy) < Math.abs(b_dy)) {
                    return -1;
                } else if (Math.abs(a_dy) > Math.abs(b_dy)) {
                    return 1;
                }
                // Then sort by horizontal distance
                int a_dx = a.getX() - position.getX();
                int a_dz = a.getZ() - position.getZ();
                int b_dx = b.getX() - position.getX();
                int b_dz = b.getZ() - position.getZ();
                int a_dist = a_dx * a_dx + a_dz * a_dz;
                int b_dist = b_dx * b_dx + b_dz * b_dz;
                if (a_dist < b_dist) {
                    return -1;
                } else if (a_dist > b_dist) {
                    return 1;
                }
                // Finally sort by angle
                double a_angle = Math.atan2(a_dz, a_dx);
                double b_angle = Math.atan2(b_dz, b_dx);
                return Double.compare(a_angle, b_angle);
            });
        }

        // Remove already-existing notes from missingNotes, adding their positions to noteblockPositions, and create a list of unused noteblock locations
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

        // Cull noteblocks that won't fit in stage
        if (missingNotes.size() > unusedNoteblockLocations.size()) {
            while (missingNotes.size() > unusedNoteblockLocations.size()) {
                missingNotes.pollLast();
            }
        }

        // Populate missing noteblocks into the unused noteblock locations
        int idx = 0;
        for (int noteId : missingNotes) {
            BlockPos bp = unusedNoteblockLocations.get(idx++);
            noteblockPositions.put(noteId, bp);
        }

        for (BlockPos bp : noteblockPositions.values()) { // Optional break locations
            breakLocations.add(bp.up());
        }

        requiredBreaks = breakLocations.stream().filter((bp) -> {
            BlockState bs = NoteblockPlayer.mc.world.getBlockState(bp);
            return !bs.isAir() && !bs.isLiquid();
        }).sorted((a, b) -> {
            // First sort by y
            if (a.getY() < b.getY()) {
                return -1;
            } else if (a.getY() > b.getY()) {
                return 1;
            }
            // Then sort by horizontal distance
            int a_dx = a.getX() - position.getX();
            int a_dz = a.getZ() - position.getZ();
            int b_dx = b.getX() - position.getX();
            int b_dz = b.getZ() - position.getZ();
            int a_dist = a_dx * a_dx + a_dz * a_dz;
            int b_dist = b_dx * b_dx + b_dz * b_dz;
            if (a_dist < b_dist) {
                return -1;
            } else if (a_dist > b_dist) {
                return 1;
            }
            // Finally sort by angle
            double a_angle = Math.atan2(a_dz, a_dx);
            double b_angle = Math.atan2(b_dz, b_dx);
            return Double.compare(a_angle, b_angle);
        }).collect(Collectors.toCollection(LinkedList::new));

        if (requiredBreaks.stream().noneMatch(bp -> withinBreakingDist(bp.getX() - position.getX(), bp.getY() - position.getY(), bp.getZ() - position.getZ()))) {
            requiredBreaks.clear();
        }

        // Set total missing notes
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
                    Map.Entry<BlockPos, Integer> closest = instrumentMap[instrumentId].entrySet().stream().min((a, b) -> {
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
                    NoteblockPlayer.addChatMessage(String.format("    §3%s (%s): §%s%d/%d", instrument.name(), instrument.material, foundInstruments[instrumentId] < requiredInstruments[instrumentId] ? "c" : "a", foundInstruments[instrumentId], requiredInstruments[instrumentId]));
                }
            }
            NoteblockPlayer.addChatMessage("§c------------------------------");
        }
    }

    void loadDefaultBlocks(Collection<BlockPos> noteblockLocations, Collection<BlockPos> breakLocations) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (Math.abs(dx) == 4 && Math.abs(dz) == 4) {
                    noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
                    noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 2, position.getZ() + dz));
                    breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 1, position.getZ() + dz));
                } else {
                    noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 1, position.getZ() + dz));
                    noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 2, position.getZ() + dz));
                    breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
                    breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 1, position.getZ() + dz));
                }
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (withinBreakingDist(dx, -3, dz)) {
                    noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 3, position.getZ() + dz));
                }
                if (withinBreakingDist(dx, 4, dz)) {
                    noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 4, position.getZ() + dz));
                }
            }
        }
    }

    void loadWideBlocks(Collection<BlockPos> noteblockLocations, Collection<BlockPos> breakLocations) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (withinBreakingDist(dx, 2, dz)) {
                    noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 2, position.getZ() + dz));
                    if (withinBreakingDist(dx, -1, dz)) {
                        noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 1, position.getZ() + dz));
                        breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
                        breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 1, position.getZ() + dz));
                    } else if (withinBreakingDist(dx, 0, dz)) {
                        noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
                        breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 1, position.getZ() + dz));
                    }
                }
                if (withinBreakingDist(dx, -3, dz)) {
                    noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 3, position.getZ() + dz));
                }
                if (withinBreakingDist(dx, 4, dz)) {
                    noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 4, position.getZ() + dz));
                }
            }
        }
    }

    void loadStereoBlocks(Collection<BlockPos> noteblockLocations, Collection<BlockPos> breakLocations) {
        int maxRadius = NoteblockPlayer.getConfig().maxRadius;
        double maxAngleRad = Math.toRadians(180);
        double layerHeight = 1.0;

        // Determine which quadrant the player is facing
        float normalizedYaw = (playerYaw % 360 + 360) % 360; // Normalize to 0-360 range

        // Determine quadrant: 0=south, 90=west, 180=north, 270=east
        int facingQuadrant;

        if (normalizedYaw >= 315 || normalizedYaw < 45) {
            facingQuadrant = 0; // Looking south
        } else if (normalizedYaw >= 45 && normalizedYaw < 135) {
            facingQuadrant = 90; // Looking west
        } else if (normalizedYaw >= 135 && normalizedYaw < 225) {
            facingQuadrant = 180; // Looking north
        } else { // Between 225 and 315
            facingQuadrant = 270; // Looking east
        }

        // Convert quadrant to mathematical radians
        // South (0) → π/2, West (90) → π, North (180) → 3π/2, East (270) → 0
        double playerRotationRad = Math.toRadians(facingQuadrant);

        // System.out.println("Player yaw: " + normalizedYaw + ", Facing quadrant: " + facingQuadrant + ", Rotation: " + Math.toDegrees(playerRotationRad));

        // Get the song layers
        ArrayList<Layer> songLayers = currentSong.getLayers();
        int actualLayerCount = songLayers.size();
        if (actualLayerCount == 0) return;

        // Map to track which actual song layer each note belongs to
        Map<Integer, Integer> noteToLayerIndex = new HashMap<>();

        // Assign each note to its layer index
        for (int i = 0; i < songLayers.size(); i++) {
            Layer layer = songLayers.get(i);
            for (int noteId = 0; noteId < 400; noteId++) {
                if (layer.requiredNotes[noteId]) {
                    noteToLayerIndex.put(noteId, i);
                }
            }
        }

        // Track used horizontal positions to prevent any stacking
        Set<String> usedXZ = new HashSet<>();

        // Process all song layers and notes
        for (int noteId = 0; noteId < 400; noteId++) {
            boolean isRequired = false;

            // Check if note is required in any layer
            for (Layer layer : songLayers) {
                if (layer.requiredNotes[noteId]) {
                    isRequired = true;
                    break;
                }
            }

            if (!isRequired) continue;

            // Find the layer that uses this note (for velocity/panning information)
            Layer noteLayer = null;
            for (Layer layer : songLayers) {
                if (layer.requiredNotes[noteId]) {
                    noteLayer = layer;
                    break;
                }
            }

            // Skip if no layer found (should never happen since we checked isRequired)
            if (noteLayer == null) continue;

            // Get velocity and panning from the layer that contains this note
            int velocity = noteLayer.velocity;
            int pan = noteLayer.panning;

            // Base radius (loud notes closer)
            double baseRadius = ((100 - velocity) / 99.0) * (maxRadius - 1) + 1;
            // Base angle around player, relative to player's rotation
            // Pan 0 = full left, 100 = center, 200 = full right
            double baseAngle = (pan / 200.0) * maxAngleRad + playerRotationRad;

            // System.out.println("Note " + noteId + " panning: " + pan + " baseRadius: " + baseRadius + " baseAngle: " + Math.toDegrees(baseAngle));

            // Determine vertical position based on the actual layer in the song
            int layerIndex = noteToLayerIndex.getOrDefault(noteId, 0);
            // Distribute layers vertically, centering them around the player
            int dy = (int) Math.floor(layerIndex * layerHeight) - (int) Math.floor((actualLayerCount - 1) * layerHeight / 2.0);

            BlockPos chosen = null;
            boolean overlapping = false;

            // First search attempt in the normal direction
            double deltaAngle = Math.toRadians(5);
            outer:
            for (int rStep = 0; rStep <= 8; rStep++) {  // Increased max step to 8 for more range
                double radius = baseRadius + rStep;
                // Increased angle range for more positions
                for (int aStep = -6; aStep <= 6; aStep++) {
                    double angle = baseAngle + aStep * deltaAngle;
                    int dx = MathHelper.floor(radius * Math.cos(angle));
                    int dz = MathHelper.floor(radius * Math.sin(angle));
                    // Unique key for x/z footprint
                    String key = (position.getX() + dx) + "," + (position.getZ() + dz);
                    if (!usedXZ.contains(key)) {
                        chosen = position.add(dx, dy, dz);
                        usedXZ.add(key);
                        break outer;
                    }
                }
                // If we've gone through a few radius steps and still haven't found a spot,
                // mark as overlapping so we can try the opposite direction
                if (rStep >= 6) {
                    overlapping = true;
                }
            }

            // If we're having trouble finding a position, try the opposite direction (180 degrees away)
            if (chosen == null && overlapping) {
                // Flip the angle 180 degrees
                double oppositeAngle = baseAngle + Math.PI;

                // Try positions in the opposite direction with more variations
                outer:
                for (int rStep = 0; rStep <= 8; rStep++) {  // Increased max step to 8 for more range
                    double radius = baseRadius + rStep;
                    // Increased angle range for more positions
                    for (int aStep = -6; aStep <= 6; aStep++) {
                        double angle = oppositeAngle + aStep * deltaAngle;
                        int dx = MathHelper.floor(radius * Math.cos(angle));
                        int dz = MathHelper.floor(radius * Math.sin(angle));
                        // Unique key for x/z footprint
                        String key = (position.getX() + dx) + "," + (position.getZ() + dz);
                        if (!usedXZ.contains(key)) {
                            chosen = position.add(dx, dy, dz);
                            usedXZ.add(key);
                            break outer;
                        }
                    }
                }

                // If still no position found, try intermediate angles
                if (chosen == null) {
                    // Try positions at 90 degree angles from the original
                    double[] perpendicularAngles = {baseAngle + Math.PI / 2, baseAngle - Math.PI / 2};
                    outer:
                    for (double perpAngle : perpendicularAngles) {
                        for (int rStep = 0; rStep <= 8; rStep++) {
                            double radius = baseRadius + rStep;
                            for (int aStep = -4; aStep <= 4; aStep++) {
                                double angle = perpAngle + aStep * deltaAngle;
                                int dx = MathHelper.floor(radius * Math.cos(angle));
                                int dz = MathHelper.floor(radius * Math.sin(angle));
                                String key = (position.getX() + dx) + "," + (position.getZ() + dz);
                                if (!usedXZ.contains(key)) {
                                    chosen = position.add(dx, dy, dz);
                                    usedXZ.add(key);
                                    break outer;
                                }
                            }
                        }
                    }
                }
            }

            // Fallback if still none found - try a more extensive search
            if (chosen == null) {
                // Last resort: try a wider search in all directions with smaller angle increments
                for (int r = 1; r <= maxRadius * 2 && chosen == null; r++) {
                    // Use a smaller angle increment for more thorough coverage
                    for (int angle = 0; angle < 360; angle += 5) {
                        // Convert to radians and make relative to player rotation
                        double rad = Math.toRadians(angle) + playerRotationRad;
                        int dx = MathHelper.floor(r * Math.cos(rad));
                        int dz = MathHelper.floor(r * Math.sin(rad));
                        String key = (position.getX() + dx) + "," + (position.getZ() + dz);
                        if (!usedXZ.contains(key)) {
                            chosen = position.add(dx, dy, dz);
                            usedXZ.add(key);
                            break;
                        }
                    }
                }

                // If we still couldn't find a position, try different Y levels
                if (chosen == null) {
                    // Try alternative vertical positions if horizontal space is congested
                    for (int yOffset = 1; yOffset <= 5 && chosen == null; yOffset++) {
                        // Alternate between above and below the original y level
                        int altDy = dy + (yOffset % 2 == 0 ? yOffset / 2 : -yOffset / 2);

                        for (int r = 1; r <= maxRadius && chosen == null; r++) {
                            for (int angle = 0; angle < 360; angle += 10) {
                                // Ensure consistent angle calculation with the player's rotation
                                double rad = Math.toRadians(angle) + playerRotationRad;
                                int dx = MathHelper.floor(r * Math.cos(rad));
                                int dz = MathHelper.floor(r * Math.sin(rad));
                                String key = (position.getX() + dx) + "," + (position.getZ() + dz);
                                if (!usedXZ.contains(key)) {
                                    chosen = position.add(dx, altDy, dz);
                                    usedXZ.add(key);
                                    break;
                                }
                            }
                        }
                    }
                }

                // Absolute last resort - force placement at a unique position
                if (chosen == null) {
                    // Find any available position by scanning outward in a spiral
                    for (int spiral = 1; spiral <= maxRadius * 3 && chosen == null; spiral++) {
                        for (int dx = -spiral; dx <= spiral && chosen == null; dx++) {
                            for (int dz = -spiral; dz <= spiral && chosen == null; dz++) {
                                // Only check the perimeter of the current spiral square
                                if (Math.abs(dx) != spiral && Math.abs(dz) != spiral) continue;

                                String key = (position.getX() + dx) + "," + (position.getZ() + dz);
                                if (!usedXZ.contains(key)) {
                                    chosen = position.add(dx, dy, dz);
                                    usedXZ.add(key);
                                }
                            }
                        }
                    }
                }
            }

            noteblockLocations.add(chosen);
            // No break required for velocity-spatial mode
        }
    }

    // This code was taken from Sk8kman fork of NoteblockPlayer
    // Thanks Sk8kman and Lizard16 for this spherical stage design!
    void loadSphericalBlocks(Collection<BlockPos> noteblockLocations, Collection<BlockPos> breakLocations) {
        final int maxRange = 5;
        int[] yLayers = {-4, -2, -1, 0, 1, 2, 3, 4, 5, 6};

        for (int dx = -maxRange; dx <= maxRange; dx++) {
            for (int dz = -maxRange; dz <= maxRange; dz++) {
                for (int dy : yLayers) {
                    int adx = Math.abs(dx);
                    int adz = Math.abs(dz);
                    switch (dy) {
                        case -4: {
                            if (adx < 3 && adz < 3) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            if ((adx == 3 ^ adz == 3) && (adx == 0 ^ adz == 0)) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case -2: { // also takes care of -3
                            if (adz == 0 && adx == 0) { // prevents placing in the center
                                break;
                            }
                            if (adz * adx > 9) { // prevents building out too far
                                break;
                            }
                            if (adz + adx == 5 && adx != 0 && adz != 0) {
                                // add noteblocks above and below here
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 1, position.getZ() + dz));
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy - 1, position.getZ() + dz));
                                break;
                            }
                            if (adz * adx == 3) {
                                // add noteblocks above and below here
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 1, position.getZ() + dz));
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy - 1, position.getZ() + dz));
                                break;
                            }
                            if (adx < 3 && adz < 3 && adx + adz > 0) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 2, position.getZ() + dz));
                                break;
                            }
                            if (adz == 0 ^ adx == 0) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 2, position.getZ() + dz));
                                break;
                            }
                            if (adz * adx == 2 * maxRange) { // expecting one to be 2, and one to be maxRange (e.g. 5)
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 2, position.getZ() + dz));
                                break;
                            }
                            if (adz + adx == 6) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                if ((adx == maxRange) ^ (adz == maxRange)) {
                                    breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 2, position.getZ() + dz));
                                }
                                break;
                            }
                            break;
                        }
                        case -1: {
                            if (adx + adz == 7 || adx + adz == 0) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 0: {
                            int check = adx + adz;
                            if ((check == 8 || check == 6) && adx * adz > 5) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 1: {
                            int addl1 = adx + adz;
                            if (addl1 == 7 || addl1 == 3 || addl1 == 2) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            if ((adx == maxRange) ^ (adz == maxRange) && addl1 < 7) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            if (addl1 == 4 && adx * adz != 0) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            if (adx + adz < 7) {
                                breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
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
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            if ((addl2 == 4) && (adx == 0 ^ adz == 0)) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            if (addl2 == 0) {
                                breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 3: {
                            if (adx * adz == 12 || adx + adz == 0) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            if ((adx == maxRange) ^ (adz == maxRange) && ((adx < 2) ^ (adz < 2))) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            if (adx > 3 || adz > 3) { // don't allow any more checks past 3 blocks out
                                break;
                            }
                            if (adx + adz > 1 && adx + adz < 5) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 4: {
                            if (adx == maxRange || adz == maxRange) {
                                break;
                            }
                            if (adx + adz == 4 && adx * adz == 0) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            int addl4 = adx + adz;
                            if (addl4 == 1 || addl4 == 5 || addl4 == 6) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
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
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            break;
                        }
                        case 6: {
                            if (adx + adz < 2) {
                                noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
                                break;
                            }
                            break;
                        }
                    }
                    // all breaks lead here
                }
            }
        }
    }

    // Find available noteblocks in range for the player to use in survival only mode
    Map<BlockPos, Integer>[] loadSurvivalBlocks() {
        @SuppressWarnings("unchecked") Map<BlockPos, Integer>[] instrumentMap = new Map[16];
        for (int i = 0; i < 16; i++) {
            instrumentMap[i] = new TreeMap<>();
        }
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy : new int[]{-1, 0, 1, 2, -2, 3, -3, 4, -4, 5, 6}) {
                    BlockPos bp = position.add(dx, dy, dz);
                    BlockState bs = NoteblockPlayer.mc.world.getBlockState(bp);
                    BlockState aboveBs = NoteblockPlayer.mc.world.getBlockState(bp.up());
                    int blockId = Block.getRawIdFromState(bs);
                    if (blockId >= NoteblockPlayer.NOTEBLOCK_BASE_ID && blockId < NoteblockPlayer.NOTEBLOCK_BASE_ID + 800 && aboveBs.isAir()) {
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

    // This doesn't check for whether the block above the noteblock position is also reachable
    // Usually there is sky above you though so hopefully this doesn't cause a problem most of the time
    boolean withinBreakingDist(int dx, int dy, int dz) {
        double dy1 = dy + 0.5 - 1.62; // Standing eye height
        double dy2 = dy + 0.5 - 1.27; // Crouching eye height
        return dx * dx + dy1 * dy1 + dz * dz < 5.99999 * 5.99999 && dx * dx + dy2 * dy2 + dz * dz < 5.99999 * 5.99999;
    }

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
            if (!aboveBs.isAir() && !aboveBs.isLiquid()) {
                return true;
            }
        }
        return false;
    }

    public Vec3d getOriginBottomCenter() {
        return Vec3d.ofBottomCenter(position);
    }
}