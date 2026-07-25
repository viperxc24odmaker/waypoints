package com.makeforge.waypoints;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Clean, vanilla-styled manager for everyday use.
 *
 * Uses only stock widgets (Button, EditBox) and GuiGraphics text, so it matches
 * the vanilla look and stays VulkanMod-friendly. The list scrolls by rebuilding
 * the visible rows, which keeps the code on stable APIs (no custom list widget).
 */
public class WaypointScreen extends Screen {

    private static final int ROW_H = 24;
    private static final int CONTENT_W = 328;

    private final Screen parent;
    private int scrollOffset = 0;
    private int editingIndex = -1;
    private String pendingText = "";
    private EditBox nameBox;

    public WaypointScreen(Screen parent) {
        super(Component.literal("MakeForge Waypoints"));
        this.parent = parent;
    }

    private int panelLeft() {
        return (this.width - CONTENT_W) / 2;
    }

    private int panelRight() {
        return panelLeft() + CONTENT_W;
    }

    private int listTop() {
        return 40;
    }

    private int listBottom() {
        return this.height - 62;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_H);
    }

    @Override
    protected void init() {
        List<Waypoint> list = WaypointManager.get().all();

        // Keep scroll in range (list may have shrunk).
        int maxOffset = Math.max(0, list.size() - visibleRows());
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;
        if (scrollOffset < 0) scrollOffset = 0;

        // Preserve typed text across rebuilds.
        if (nameBox != null) {
            pendingText = nameBox.getValue();
        }

        int left = panelLeft();
        int right = panelRight();

        // --- Waypoint rows ---
        int shown = Math.min(visibleRows(), list.size() - scrollOffset);
        for (int r = 0; r < shown; r++) {
            final int idx = scrollOffset + r;
            final Waypoint wp = list.get(idx);
            int rowY = listTop() + r * ROW_H;

            int delX = right - 28;
            int editX = delX - 2 - 32;
            int tpX = editX - 2 - 28;
            int visX = tpX - 2 - 32;

            Button visBtn = Button.builder(
                    Component.literal(wp.isVisible() ? "On" : "Off"),
                    b -> {
                        wp.visible = !wp.isVisible();
                        WaypointManager.get().save();
                        rebuildWidgets();
                    }).bounds(visX, rowY + 2, 32, 20).build();

            Button tpBtn = Button.builder(
                    Component.literal("TP"),
                    b -> teleportTo(wp)).bounds(tpX, rowY + 2, 28, 20).build();

            Button editBtn = Button.builder(
                    Component.literal("Edit"),
                    b -> {
                        editingIndex = idx;
                        rebuildWidgets();
                    }).bounds(editX, rowY + 2, 32, 20).build();

            Button delBtn = Button.builder(
                    Component.literal("Del"),
                    b -> {
                        WaypointManager.get().remove(wp);
                        if (editingIndex == idx) editingIndex = -1;
                        rebuildWidgets();
                    }).bounds(delX, rowY + 2, 28, 20).build();

            addRenderableWidget(visBtn);
            addRenderableWidget(tpBtn);
            addRenderableWidget(editBtn);
            addRenderableWidget(delBtn);
        }

        // --- Bottom bar ---
        int by1 = this.height - 54;
        int by2 = this.height - 28;
        boolean editing = editingIndex >= 0 && editingIndex < list.size();

        nameBox = new EditBox(this.font, left, by1, 150, 20, Component.literal("Name"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal("Waypoint name"));
        if (editing) {
            nameBox.setValue(list.get(editingIndex).name);
        } else {
            nameBox.setValue(pendingText);
        }
        addRenderableWidget(nameBox);

        Button actionBtn = Button.builder(
                Component.literal(editing ? "Save" : "Add Here"),
                b -> {
                    LocalPlayer p = this.minecraft != null ? this.minecraft.player : null;
                    if (editing) {
                        String v = nameBox.getValue().trim();
                        if (!v.isEmpty()) {
                            list.get(editingIndex).name = v;
                            WaypointManager.get().save();
                        }
                        editingIndex = -1;
                        pendingText = "";
                    } else if (p != null) {
                        WaypointManager.get().addAtPlayer(p, nameBox.getValue());
                        pendingText = "";
                    }
                    rebuildWidgets();
                }).bounds(left + 154, by1, 70, 20).build();
        addRenderableWidget(actionBtn);

        if (editing) {
            Button cancelBtn = Button.builder(
                    Component.literal("Cancel"),
                    b -> {
                        editingIndex = -1;
                        pendingText = "";
                        rebuildWidgets();
                    }).bounds(left + 226, by1, 54, 20).build();
            addRenderableWidget(cancelBtn);
        }

        Button hudBtn = Button.builder(
                Component.literal("HUD: " + (WaypointHud.enabled ? "On" : "Off")),
                b -> {
                    WaypointHud.enabled = !WaypointHud.enabled;
                    rebuildWidgets();
                }).bounds(left, by2, 110, 20).build();
        addRenderableWidget(hudBtn);

        Button doneBtn = Button.builder(
                Component.literal("Done"),
                b -> onClose()).bounds(right - 90, by2, 90, 20).build();
        addRenderableWidget(doneBtn);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<Waypoint> list = WaypointManager.get().all();
        int maxOffset = Math.max(0, list.size() - visibleRows());
        if (maxOffset > 0 && scrollY != 0) {
            scrollOffset = (int) Math.max(0, Math.min(maxOffset, scrollOffset - Math.signum(scrollY)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        int left = panelLeft();
        int right = panelRight();

        // List panel backdrop.
        g.fill(left - 6, listTop() - 6, right + 6, listBottom() + 2, 0x66000000);

        super.render(g, mouseX, mouseY, partialTick);

        // Title.
        g.drawCenteredString(this.font, "MakeForge Waypoints", this.width / 2, 16, 0xFFFFFF);

        List<Waypoint> list = WaypointManager.get().all();
        LocalPlayer player = this.minecraft != null ? this.minecraft.player : null;
        String playerDim = player != null ? player.level().dimension().location().toString() : null;
        Vec3 eye = player != null ? player.getEyePosition() : null;

        if (list.isEmpty()) {
            g.drawCenteredString(this.font, "No waypoints yet - press \"Add Here\"",
                    this.width / 2, listTop() + 20, 0xAAAAAA);
        }

        int shown = Math.min(visibleRows(), list.size() - scrollOffset);
        for (int r = 0; r < shown; r++) {
            int idx = scrollOffset + r;
            Waypoint wp = list.get(idx);
            int rowY = listTop() + r * ROW_H;

            // Colour swatch.
            int c = 0xFF000000 | (wp.color & 0xFFFFFF);
            g.fill(left, rowY + 4, left + 8, rowY + 16, 0xFF000000);
            g.fill(left + 1, rowY + 5, left + 7, rowY + 15, c);

            int nameColor = wp.isVisible() ? 0xFFFFFF : 0x808080;
            g.drawString(this.font, wp.name, left + 14, rowY + 2, nameColor);

            String coords = wp.x + ", " + wp.y + ", " + wp.z;
            String tail;
            if (wp.dimension != null && wp.dimension.equals(playerDim) && eye != null) {
                double dx = (wp.x + 0.5) - eye.x;
                double dy = (wp.y + 0.5) - eye.y;
                double dz = (wp.z + 0.5) - eye.z;
                tail = "  -  " + (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + "m";
            } else {
                tail = "  -  " + shortDim(wp.dimension);
            }
            g.drawString(this.font, coords + tail, left + 14, rowY + 12, 0x9AA0A6);
        }

        // Scroll indicator.
        int maxOffset = Math.max(0, list.size() - visibleRows());
        if (maxOffset > 0) {
            int first = scrollOffset + 1;
            int last = Math.min(list.size(), scrollOffset + visibleRows());
            g.drawString(this.font, first + "-" + last + " / " + list.size(),
                    right - 60, listTop() - 16, 0x9AA0A6);
            g.drawString(this.font, "scroll", left, listTop() - 16, 0x9AA0A6);
        }
    }

    private static String shortDim(String dim) {
        if (dim == null) return "?";
        int colon = dim.indexOf(':');
        String s = colon >= 0 ? dim.substring(colon + 1) : dim;
        int slash = s.lastIndexOf('/');
        return slash >= 0 ? s.substring(slash + 1) : s;
    }

    /**
     * Runs the vanilla teleport command. This only succeeds where the player has
     * command permission (singleplayer with cheats on, or an op/allowed server).
     * On a normal survival server it is refused by the server, exactly as if the
     * player typed /tp themselves - we don't try to bypass server authority.
     */
    private void teleportTo(Waypoint wp) {
        LocalPlayer p = this.minecraft != null ? this.minecraft.player : null;
        if (p == null || p.connection == null) {
            return;
        }
        String cmd;
        if (wp.dimension != null && !wp.dimension.isEmpty()) {
            cmd = "execute in " + wp.dimension + " run tp @s " + wp.x + " " + wp.y + " " + wp.z;
        } else {
            cmd = "tp @s " + wp.x + " " + wp.y + " " + wp.z;
        }
        p.connection.sendCommand(cmd);
        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
