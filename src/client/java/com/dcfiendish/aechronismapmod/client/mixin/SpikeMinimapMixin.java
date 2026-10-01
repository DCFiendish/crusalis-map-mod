package com.dcfiendish.aechronismapmod.client.mixin;

import com.dcfiendish.aechronismapmod.spike.HookSpike;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.graphics.CustomRenderTypes;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.lib.client.graphics.XaeroBufferProvider;

/**
 * Spike: draw into the minimap's 512x512 scaling FBO right before the first endBatch() in
 * renderChunksToFBO — after map chunks (world-map tiles or minimap chunks) and the chunk
 * grid are queued, before Xaero copies the FBO through rotation and the circle/square mask.
 * Covers both the world-map-backed and the minimap-only (cave) paths with one hook.
 */
@Mixin(MinimapFBORenderer.class)
public class SpikeMinimapMixin {

    @Inject(
            method = "renderChunksToFBO",
            at = @At(value = "INVOKE", target = "Lxaero/lib/client/graphics/XaeroBufferProvider;endBatch()V", ordinal = 0),
            require = 0
    )
    private void crusalis$spikeQuad(CallbackInfo ci,
                                    @Local(argsOnly = true) PoseStack matrixStack,
                                    @Local(name = "renderTypeBuffers") XaeroBufferProvider renderTypeBuffers,
                                    @Local(name = "xFloored") int xFloored,
                                    @Local(name = "zFloored") int zFloored) {
        if (!HookSpike.ENABLED) return;
        HookSpike.drawTestChunks(matrixStack.last().pose(),
                renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_CHUNK_OVERLAY), xFloored, zFloored);
    }
}
