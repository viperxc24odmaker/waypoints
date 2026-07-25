package com.makeforge.waypoints;

/** Simple serializable waypoint. Public fields so Gson can (de)serialize directly. */
public class Waypoint {
    public String name;
    public int x;
    public int y;
    public int z;
    public int color;        // packed RGB, no alpha
    public String dimension; // e.g. "minecraft:overworld"
    public Boolean visible;  // null is treated as visible (keeps old saves working)

    public Waypoint() {
    }

    public Waypoint(String name, int x, int y, int z, int color, String dimension) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.dimension = dimension;
        this.visible = Boolean.TRUE;
    }

    public boolean isVisible() {
        return visible == null || visible;
    }
}
