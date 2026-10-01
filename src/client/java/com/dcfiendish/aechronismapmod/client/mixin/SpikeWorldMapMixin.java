package com.dcfiendish.aechronismapmod.client.mixin;

import com.dcfiendish.aechronismapmod.spike.HookSpike;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.gui.GuiMap;

/**
 * Spike: draw into the world map's primary FBO right before Xaero flushes the tile batch
 * (the first endBatch() after the region/tile loop), i.e. after map tiles, before map
 * elements (waypoints, player arrow, radar). Same call XaeroPlus wraps for its overlays.
 */
@Mixin(GuiMap.class)
public class SpikeWorldMapMixin {

    @Inject(
            method = "render",
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lxaero/map/gui/GuiMap;prevLoadingLeaves:Z", opcode = Opcodes.PUTFIELD),
                    to = @At(value = "INVOKE", target = "Lxaero/map/graphics/ImprovedFramebuffer;bindDefaultFramebuffer(Lnet/minecraft/client/Minecraft;)V")
            ),
            at = @At(value = "INVOKE", target = "Lxaero/lib/client/graphics/XaeroBufferProvider;endBatch()V", ordinal = 0),
            require = 0
    )
    private void crusalis$spikeQuad(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci,
                                    @Local(name = "matrixStack") PoseStack matrixStack,
                                    @Local(name = "renderTypeBuffers") XaeroBufferProvider renderTypeBuffers,
                                    @Local(name = "flooredCameraX") int flooredCameraX,
                                    @Local(name = "flooredCameraZ") int flooredCameraZ) {
        if (!HookSpike.ENABLED) return;
        HookSpike.drawTestChunks(matrixStack.last().pose(),
                renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_COLOR_OVERLAY), flooredCameraX, flooredCameraZ);
    }
}
