package com.dcfiendish.aechronismapmod;

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
 * Dev-only self-test (-Dcrusalis.autotest=true, with -Dcrusalis.devForceActive=true for live
 * data; `gradlew runClient -Pautotest`). Once the live Crusalis data has loaded it parks the
 * player (spectator) in a nation-held chunk, adds fake war / under-attack / occupied markers
 * next to it, screenshots the minimap in every rotation/shape combination and the world map at
 * several zooms, repeats one minimap + world map shot in the Nether (must be empty), then quits.
 * Screenshots land in run/screenshots/autotest_*.png.
 */
public class DevAutoTest implements ClientModInitializer {
    private record Step(int waitTicks, Runnable action) {}

    private final Queue<Step> steps = new ArrayDeque<>();
    private int wait;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("crusalis.autotest")) return;
        Minecraft mc = Minecraft.getInstance();

        step(100, () -> {
            mc.options.pauseOnLostFocus = false; // the dev window rarely has focus
            command("gamemode spectator @a");
            command("time set day");
        });
        // Live Crusalis data loads over ~5 s (devForceActive); then park the player on the
        // first nation's label, with fake war/attack/occupation markers next to it.
        step(400, () -> {
            AechronisMapData d = AechronisMapMod.mapData;
            // A nation-held chunk, so fills, borders and labels are all around.
            long chunk = d.buildAlphaCache(255).keySet().iterator().nextLong();
            int x = (net.minecraft.world.level.ChunkPos.getX(chunk) << 4) + 8;
            int z = (net.minecraft.world.level.ChunkPos.getZ(chunk) << 4) + 8;
            System.out.printf("[AutoTest] data: nations=%d nodes=%d towns=%d ports=%d borders=%d target=%d,%d%n",
                    d.nationLabelInfos.size(), d.nodeLabelInfos.size(), d.townLabelInfos.size(), d.ports.size(),
                    d.nodeBorderLines.size(), x, z);
            int cx = x >> 4, cz = z >> 4;
            long now = System.currentTimeMillis();
            d.warChunks.put(net.minecraft.world.level.ChunkPos.asLong(cx + 2, cz), new AechronisMapData.WarChunk("Test", 0xFF0000, now));
            d.underAttackChunks.put(net.minecraft.world.level.ChunkPos.asLong(cx + 3, cz), new AechronisMapData.UnderAttackChunk("Test", 0xFFFF00, now));
            d.territoryDiagonals.keySet().stream().findFirst().ifPresent(tid -> {
                d.capturedTerritoryIds.add(tid);
                d.territoryDiagonalColors.put(tid, 0x00FFFF);
            });
            command("tp @a " + (x + 0.5) + " 200 " + (z + 0.5) + " 30 60");
            target[0] = x;
            target[1] = z;
        });
        step(300, () -> {});
        for (boolean north : new boolean[]{true, false}) {
            for (int shape : new int[]{0, 1}) {
                step(10, () -> {
                    cfg(MinimapProfiledConfigOptions.NORTH_LOCKED, north);
                    cfg(MinimapProfiledConfigOptions.SHAPE, shape);
                });
                step(30, () -> shot("mm_" + (north ? "north" : "rotating") + "_" + (shape == 0 ? "square" : "circle")));
            }
        }
        for (double zoom : new double[]{0.25, 1, 4, 16}) {
            worldMap(zoom, "wm_zoom_" + zoom);
        }
        // Nether: both maps must be empty of Crusalis data.
        step(10, () -> command("execute in minecraft:the_nether run tp @a " + target[0] + " 100 " + target[1]));
        step(200, () -> shot("nether_mm"));
        worldMap(1, "nether_wm");
        // -Pautotest=stay: go back to the Overworld test spot and leave the client open.
        if (Boolean.getBoolean("crusalis.autotest.stay")) {
            step(20, () -> command("execute in minecraft:overworld run tp @a " + target[0] + " 200 " + target[1]));
        } else {
            step(20, mc::stop);
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getSingleplayerServer() == null || steps.isEmpty()) return;
            if (wait-- > 0) return;
            Step step = steps.poll();
            step.action().run();
            wait = steps.isEmpty() ? 0 : steps.peek().waitTicks();
        });
        wait = steps.peek().waitTicks();
    }

    private final int[] target = new int[2];

    private void worldMap(double zoom, String name) {
        Minecraft mc = Minecraft.getInstance();
        step(10, () -> {
            setStatic(GuiMap.class, "destScale", zoom);
            mc.setScreen(new GuiMap(null, null, WorldMapSession.getCurrentSession().getMapProcessor(), mc.getCameraEntity()));
        });
        step(100, () -> shot(name));
        step(5, () -> mc.setScreen(null));
    }

    private static void command(String cmd) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd));
    }

    private void step(int waitTicks, Runnable action) {
        steps.add(new Step(waitTicks, action));
    }

    private static <T> void cfg(xaero.lib.common.config.option.ConfigOption<T> option, T value) {
        HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getCurrentProfile().set(option, value);
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "autotest_" + name + ".png", mc.getMainRenderTarget(), 1,
                msg -> System.out.println("[AutoTest] " + msg.getString()));
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
