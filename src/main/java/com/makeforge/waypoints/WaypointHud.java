package com.makeforge.waypoints;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Draws waypoint markers as a 2D HUD overlay.
 *
 * VulkanMod-friendly by design: everything goes through GuiGraphics (the
 * sanctioned 2D path) and world positions are projected to the screen with
 * plain camera math, so there are no raw OpenGL calls and no custom world
 * RenderLayers, which is what usually breaks under the Vulkan backend.
 */
public final class WaypointHud {
    public static boolean enabled = true;

    private WaypointHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        if (!enabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        if (mc.options.hideGui) {
            return;
        }

        String dim = player.level().dimension().location().toString();
        Vec3 eye = player.getEyePosition();

        double yaw = Math.toRadians(player.getYRot());
        double pitch = Math.toRadians(player.getXRot());

        // Camera forward (Minecraft look vector).
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);

        // Right = normalize(cross(forward, worldUp)).
        double rl = Math.sqrt(fx * fx + fz * fz);
        if (rl < 1.0e-6) {
            rl = 1.0e-6;
        }
        double rx = -fz / rl;
        double ry = 0.0;
        double rz = fx / rl;

        // Up = cross(right, forward). Already unit length.
        double ux = ry * fz - rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy - ry * fx;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        double fov = mc.options.fov().get();
        double focal = (h / 2.0) / Math.tan(Math.toRadians(fov) / 2.0);

        Font font = mc.font;
        int margin = 10;

        for (Waypoint wp : WaypointManager.get().all()) {
            if (wp.dimension == null || !wp.dimension.equals(dim)) {
                continue;
            }
            if (!wp.isVisible()) {
                continue;
            }

            double dx = (wp.x + 0.5) - eye.x;
            double dy = (wp.y + 0.5) - eye.y;
            double dz = (wp.z + 0.5) - eye.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Into camera space.
            double cz = dx * fx + dy * fy + dz * fz; // depth (front = positive)
            double cx = dx * rx + dy * ry + dz * rz; // right
            double cy = dx * ux + dy * uy + dz * uz; // up

            double sx;
            double sy;
            if (cz > 0.05) {
                sx = w / 2.0 + (cx / cz) * focal;
                sy = h / 2.0 - (cy / cz) * focal;
            } else {
                // Behind the camera: pin to the bottom edge as an indicator.
                sx = (cx > 0) ? (w - margin) : margin;
                sy = h - 24;
            }

            sx = Math.max(margin, Math.min(w - margin, sx));
            sy = Math.max(margin, Math.min(h - margin, sy));
            int ix = (int) Math.round(sx);
            int iy = (int) Math.round(sy);

            int color = 0xFF000000 | (wp.color & 0xFFFFFF);

            // Marker: outlined diamond-ish dot.
            graphics.fill(ix - 4, iy - 4, ix + 4, iy + 4, 0xFF000000);
            graphics.fill(ix - 3, iy - 3, ix + 3, iy + 3, color);
            graphics.fill(ix - 1, iy - 1, ix + 1, iy + 1, 0xFFFFFFFF);

            // Two-line label: name + distance, then the coordinates.
            String line1 = wp.name + "  " + (int) dist + "m";
            String line2 = wp.x + ", " + wp.y + ", " + wp.z;
            int tw = Math.max(font.width(line1), font.width(line2));
            int tx = ix - tw / 2;
            int ty = iy + 7;
            int lh = font.lineHeight;

            graphics.fill(tx - 2, ty - 1, tx + tw + 2, ty + lh * 2, 0x99000000);
            graphics.drawString(font, line1, ix - font.width(line1) / 2, ty, color);
            graphics.drawString(font, line2, ix - font.width(line2) / 2, ty + lh, 0xFFCFCFCF);
        }
    }
}
