package com.dcfiendish.aechronismapmod.spike;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import xaero.common.HudMod;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Dev-only self-test for the hook spike (-Dcrusalis.hookSpike.autotest=true, plus
 * -Dcrusalis.hookSpike=true for the quads). Once in a singleplayer world it parks the
 * player (spectator, so it stays put) at block 34,18 (chunk 2,1, next to chunk 0,0), screenshots the minimap in every
 * rotation/shape combination and the world map at several zooms, logs the world map
 * camera so the screenshots can be checked pixel-exact, then quits.
 */
public class HookSpikeAutoTest implements ClientModInitializer {
    private record Step(int waitTicks, Runnable action) {}

    private final Queue<Step> steps = new ArrayDeque<>();
    private int wait;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("crusalis.hookSpike.autotest")) return;
        Minecraft mc = Minecraft.getInstance();

        step(100, () -> {
            mc.options.pauseOnLostFocus = false; // the dev window rarely has focus
            var server = mc.getSingleplayerServer();
            String name = mc.player.getGameProfile().name();
            server.execute(() -> {
                var src = server.createCommandSourceStack();
                server.getCommands().performPrefixedCommand(src, "gamemode spectator " + name);
                server.getCommands().performPrefixedCommand(src, "time set day");
                server.getCommands().performPrefixedCommand(src, "tp " + name + " 34.5 200 18.5 30 60");
            });
        });
        step(300, () -> cfg(MinimapProfiledConfigOptions.CHUNK_GRID, 0));
        for (boolean north : new boolean[]{true, false}) {
            for (int shape : new int[]{0, 1}) {
                step(10, () -> {
                    cfg(MinimapProfiledConfigOptions.NORTH_LOCKED, north);
                    cfg(MinimapProfiledConfigOptions.SHAPE, shape);
                });
                step(30, () -> shot("mm_" + (north ? "north" : "rotating") + "_" + (shape == 0 ? "square" : "circle")));
            }
        }
        for (double zoom : new double[]{0.5, 1, 3, 8, 16}) {
            step(10, () -> {
                setStatic(GuiMap.class, "destScale", zoom);
                var session = WorldMapSession.getCurrentSession();
                mc.setScreen(new GuiMap(null, null, session.getMapProcessor(), mc.getCameraEntity()));
            });
            step(100, () -> {
                Object gui = mc.screen;
                System.out.printf("[HookSpike] wm zoom=%s cameraX=%s cameraZ=%s scale=%s window=%dx%d%n", zoom,
                        get(gui, "cameraX"), get(gui, "cameraZ"), get(gui, "scale"),
                        mc.getWindow().getWidth(), mc.getWindow().getHeight());
                shot("wm_zoom_" + zoom);
            });
            step(5, () -> mc.setScreen(null));
        }
        step(20, mc::stop);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getSingleplayerServer() == null || steps.isEmpty()) return;
            if (wait-- > 0) return;
            Step step = steps.poll();
            step.action().run();
            wait = steps.isEmpty() ? 0 : steps.peek().waitTicks();
        });
        wait = steps.peek().waitTicks();
    }

    private void step(int waitTicks, Runnable action) {
        steps.add(new Step(waitTicks, action));
    }

    private static <T> void cfg(xaero.lib.common.config.option.ConfigOption<T> option, T value) {
        HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getCurrentProfile().set(option, value);
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "hookspike_" + name + ".png", mc.getMainRenderTarget(), 1,
                msg -> System.out.println("[HookSpike] " + msg.getString()));
    }

    private static Object get(Object target, String field) {
        try {
            Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.get(target);
        } catch (ReflectiveOperationException e) {
            return e.toString();
        }
    }

    private static void setStatic(Class<?> owner, String field, Object value) {
        try {
            Field f = owner.getDeclaredField(field);
            f.setAccessible(true);
            f.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
