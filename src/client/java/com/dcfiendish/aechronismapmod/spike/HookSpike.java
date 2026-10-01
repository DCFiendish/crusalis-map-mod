package com.dcfiendish.aechronismapmod.spike;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

/**
 * Proof-of-concept for drawing straight into Xaero's map framebuffers without XaeroPlus.
 * See docs/xaero-hooks.md. Off unless the JVM is started with -Dcrusalis.hookSpike=true.
 *
 * Both hooks hand us a pose whose units are world blocks relative to an integer origin
 * (the floored camera on the world map, the floored player position on the minimap),
 * with +X east and +Y south, so one helper covers both.
 */
public final class HookSpike {
    public static final boolean ENABLED = Boolean.getBoolean("crusalis.hookSpike");

    private HookSpike() {}

    /** Player's chunk in red, chunk 0,0 in blue, both solid. */
    public static void drawTestChunks(Matrix4f pose, VertexConsumer buffer, int originX, int originZ) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            chunk(pose, buffer, originX, originZ, player.chunkPosition().x, player.chunkPosition().z, 1f, 0f, 0f);
        }
        chunk(pose, buffer, originX, originZ, 0, 0, 0f, 0.3f, 1f);
    }

    private static void chunk(Matrix4f pose, VertexConsumer buffer, int originX, int originZ,
                              int chunkX, int chunkZ, float r, float g, float b) {
        float x1 = (chunkX << 4) - originX, z1 = (chunkZ << 4) - originZ;
        float x2 = x1 + 16, z2 = z1 + 16;
        // Same winding as Xaero's own RenderBufferUtil.addColoredRect: the minimap pipeline culls
        // back faces, the world map one (flipped Y) does not.
        buffer.addVertex(pose, x1, z2, 0).setColor(r, g, b, 1f);
        buffer.addVertex(pose, x2, z2, 0).setColor(r, g, b, 1f);
        buffer.addVertex(pose, x2, z1, 0).setColor(r, g, b, 1f);
        buffer.addVertex(pose, x1, z1, 0).setColor(r, g, b, 1f);
    }
}
