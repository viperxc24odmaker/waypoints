package com.makeforge.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Holds all waypoints and persists them to the Fabric config dir. */
public class WaypointManager {
    private static WaypointManager INSTANCE;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<ArrayList<Waypoint>>() {}.getType();

    // Distinct, readable colours cycled as you add waypoints.
    private static final int[] PALETTE = {
            0xFF5555, 0x55FF55, 0x5599FF, 0xFFAA00,
            0xFF55FF, 0x55FFFF, 0xFFFF55, 0xFFFFFF
    };

    private final Path file;
    private final List<Waypoint> waypoints = new ArrayList<>();

    private WaypointManager() {
        this.file = FabricLoader.getInstance().getConfigDir().resolve("makeforge-waypoints.json");
        load();
    }

    public static WaypointManager get() {
        if (INSTANCE == null) {
            INSTANCE = new WaypointManager();
        }
        return INSTANCE;
    }

    public List<Waypoint> all() {
        return waypoints;
    }

    /** Adds a waypoint at the player's current block position. Blank name auto-generates one. */
    public Waypoint addAtPlayer(LocalPlayer player, String name) {
        BlockPos pos = player.blockPosition();
        String dim = player.level().dimension().location().toString();
        int color = PALETTE[waypoints.size() % PALETTE.length];
        String finalName = (name == null || name.trim().isEmpty())
                ? "WP" + (waypoints.size() + 1)
                : name.trim();
        Waypoint wp = new Waypoint(finalName, pos.getX(), pos.getY(), pos.getZ(), color, dim);
        waypoints.add(wp);
        save();
        return wp;
    }

    public void remove(Waypoint wp) {
        if (waypoints.remove(wp)) {
            save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(waypoints, LIST_TYPE));
        } catch (IOException e) {
            System.err.println("[MakeForge Waypoints] Failed to save waypoints: " + e.getMessage());
        }
    }

    private void load() {
        try {
            if (Files.exists(file)) {
                String json = Files.readString(file);
                List<Waypoint> loaded = GSON.fromJson(json, LIST_TYPE);
                if (loaded != null) {
                    waypoints.clear();
                    waypoints.addAll(loaded);
                }
            }
        } catch (Exception e) {
            System.err.println("[MakeForge Waypoints] Failed to load waypoints: " + e.getMessage());
        }
    }
}
