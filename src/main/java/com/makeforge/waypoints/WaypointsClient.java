package com.makeforge.waypoints;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class WaypointsClient implements ClientModInitializer {
    public static final String MOD_ID = "makeforgewaypoints";

    private static final String CATEGORY = "category.makeforgewaypoints";

    @Override
    public void onInitializeClient() {
        KeyMapping addKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.makeforgewaypoints.add", GLFW.GLFW_KEY_B, CATEGORY));
        KeyMapping toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.makeforgewaypoints.toggle", GLFW.GLFW_KEY_V, CATEGORY));
        KeyMapping menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.makeforgewaypoints.menu", GLFW.GLFW_KEY_M, CATEGORY));

        // Render the waypoint overlay on top of the vanilla HUD.
        HudElementRegistry.addLast(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "waypoints"),
                WaypointHud::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null) {
                return;
            }

            while (addKey.consumeClick()) {
                Waypoint wp = WaypointManager.get().addAtPlayer(player, null);
                player.displayClientMessage(Component.literal(
                        "\u00a7b[Waypoints] \u00a7fAdded \u00a7a" + wp.name
                                + " \u00a77(" + wp.x + ", " + wp.y + ", " + wp.z + ")"), false);
            }

            while (toggleKey.consumeClick()) {
                WaypointHud.enabled = !WaypointHud.enabled;
                player.displayClientMessage(Component.literal(
                        "\u00a7b[Waypoints] \u00a7fHUD "
                                + (WaypointHud.enabled ? "\u00a7aON" : "\u00a7cOFF")), true);
            }

            while (menuKey.consumeClick()) {
                client.setScreen(new WaypointScreen(null));
            }
        });
    }
}
