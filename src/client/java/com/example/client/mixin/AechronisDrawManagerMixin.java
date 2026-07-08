package com.example.client.mixin;

import com.example.AechronisRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.HudMod;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawFeature;
import xaeroplus.feature.render.DrawManager;

/**
 * Renders Aechronis's own draw features (nation fills, borders, labels) directly,
 * completely independent of XaeroPlus's shared DrawFeatureRegistry — which is
 * gated behind HudMod.INSTANCE.isFairPlay() (xaeroplus.feature.render.DrawManager,
 * methods drawMinimapFeatures / drawWorldMapFeatures).
 *
 * SCOPE OF THE FAIRPLAY WORKAROUND IN THIS FILE:
 *   1. HEAD inject: renders OUR overlay (AechronisRenderer.ourFeatures) unconditionally.
 *   2. @Redirect on isFairPlay() within drawMinimapFeatures / drawWorldMapFeatures:
 *      forces the check to return false so XaeroPlus's OWN draw features registered
 *      via Globals.drawManager.registry() (the user drawing tool, view-distance
 *      squares, etc.) also render on this server. This is a broader scope than the
 *      original overlay-only workaround, explicitly approved by the server admin.
 *
 * WHAT THIS DOES NOT TOUCH (per admin condition: "cave mode and entity radar must
 * stay disabled, everything else is ok"):
 *   - Core Xaero's entity radar fairplay enforcement (separate mechanism, lives in
 *     core Xaero classes like MixinGuiEntityRadarSettings — completely untouched).
 *   - Core Xaero's cave mode fairplay enforcement (separate mechanism, untouched).
 *   - The HudMod.isFairPlay() flag itself is NOT modified globally — the redirect
 *     only takes effect when isFairPlay() is called from within these two specific
 *     methods. Any other code that checks isFairPlay() still sees the real value.
 *
 * VERIFICATION TO RUN AFTER DEPLOY: confirm cave mode and entity radar are STILL
 * blocked on this server (try to enable them in XaeroPlus settings — they should
 * still be gated). If they're somehow not, revert this file immediately — the
 * admin's condition was specific to leaving those enforcements intact.
 */
@Mixin(DrawManager.class)
public class AechronisDrawManagerMixin {

    @Inject(method = "drawMinimapFeatures", at = @At("HEAD"))
    private void aechronis$alwaysDrawMinimap(int chunkX, int chunkZ, int tileX, int tileZ,
                                             int insideX, int insideZ,
                                             PoseStack matrixStack, XaeroBufferProvider renderTypeBuffers,
                                             CallbackInfo ci) {
        if (AechronisRenderer.ourFeatures.isEmpty()) return;
        DrawContext ctx = new DrawContext(matrixStack, renderTypeBuffers, 1.0, false);
        matrixStack.pushPose();
        matrixStack.translate(
                (float) (-(chunkX * 64) - tileX * 16 - insideX),
                (float) (-(chunkZ * 64) - tileZ * 16 - insideZ),
                0.0F
        );
        for (DrawFeature feature : AechronisRenderer.ourFeatures) {
            feature.render(ctx);
        }
        matrixStack.popPose();
    }

    @Inject(method = "drawWorldMapFeatures", at = @At("HEAD"))
    private void aechronis$alwaysDrawWorldMap(int flooredCameraX, int flooredCameraZ,
                                              PoseStack matrixStack, double fboScale,
                                              XaeroBufferProvider renderTypeBuffers,
                                              CallbackInfo ci) {
        if (AechronisRenderer.ourFeatures.isEmpty()) return;
        DrawContext ctx = new DrawContext(matrixStack, renderTypeBuffers, fboScale, true);
        matrixStack.pushPose();
        matrixStack.translate((float) (-flooredCameraX), (float) (-flooredCameraZ), 1.0F);
        for (DrawFeature feature : AechronisRenderer.ourFeatures) {
            feature.render(ctx);
        }
        matrixStack.popPose();
    }

    /**
     * Force HudMod.isFairPlay() to return false when called from within DrawManager's
     * draw methods, so XaeroPlus's OWN registered draw features render alongside ours.
     *
     * This redirect ONLY intercepts calls to isFairPlay() that originate inside the two
     * target methods — it does not change the global fairplay state, and any other code
     * (including core Xaero's entity radar / cave mode enforcement, which lives in
     * different classes entirely) still sees the real value.
     *
     * Targeting Lvalue: the boolean returned by HudMod.isFairPlay() — we return false to
     * make the gating `if (!HudMod.INSTANCE.isFairPlay())` evaluate true, letting the
     * registered draw features render.
     */
    @Redirect(
            method = {"drawMinimapFeatures", "drawWorldMapFeatures"},
            at = @At(value = "INVOKE", target = "Lxaero/common/HudMod;isFairPlay()Z")
    )
    private boolean aechronis$forceFairPlayFalse(HudMod instance) {
        return false;
    }
}