package com.dcfiendish.aechronismapmod.client.mixin;

import com.dcfiendish.aechronismapmod.AechronisRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.HudMod;
import xaeroplus.feature.render.DrawFeature;
import xaeroplus.feature.render.DrawFeatureRegistry;
import xaeroplus.feature.render.DrawManager;

import java.util.function.Consumer;

/**
 * Renders Aechronis's own draw features (nation fills, borders, labels) directly,
 * completely independent of XaeroPlus's shared DrawFeatureRegistry — which is
 * gated behind HudMod.INSTANCE.isFairPlay() (xaeroplus.feature.render.DrawManager,
 * methods drawMinimapFeatures / drawWorldMapFeatures).
 *
 * SCOPE OF THE FAIRPLAY WORKAROUND IN THIS FILE:
 *   1. @WrapOperation on the registry.forEach(...) call inside drawMinimapFeatures /
 *      drawWorldMapFeatures: renders OUR overlay (AechronisRenderer.ourFeatures) with
 *      the exact DrawContext and translated PoseStack XaeroPlus just built for its own
 *      features, then lets XaeroPlus's registry render as normal (a no-op for us when
 *      the list is empty — see gating note below). This only runs once the method is
 *      past its fairplay early-return, which the redirect in (2) handles on Crusalis.
 *   2. @Redirect on isFairPlay() within drawMinimapFeatures / drawWorldMapFeatures:
 *      forces the check to return false so XaeroPlus's OWN draw features registered
 *      via Globals.drawManager.registry() (the user drawing tool, view-distance
 *      squares, etc.) also render on this server. This is a broader scope than the
 *      original overlay-only workaround, explicitly approved by the server admin
 *      FOR CRUSALIS SPECIFICALLY — see gating note below.
 *
 * SERVER GATING: the @Redirect only forces isFairPlay() false while
 * AechronisRenderer.ourFeatures is non-empty, which is exactly the condition
 * AechronisMapMod's JOIN handler maintains: non-empty only while connected to
 * Crusalis (server address contains "crusalis.net") and the renderer is enabled;
 * cleared on disconnect, server switch, or any other early-return path in that
 * handler. Elsewhere (any other server, or before the renderer has enabled), the
 * redirect delegates to the REAL isFairPlay() value — the bypass never applies off
 * Crusalis. This mod may later ship separate per-server builds/approvals; each
 * such build should gate this the same way, scoped to whatever server it targets.
 *
 * WHAT THIS DOES NOT TOUCH (per admin condition: "cave mode and entity radar must
 * stay disabled, everything else is ok"):
 *   - Core Xaero's entity radar fairplay enforcement (separate mechanism, lives in
 *     core Xaero classes like MixinGuiEntityRadarSettings — completely untouched).
 *   - Core Xaero's cave mode fairplay enforcement (separate mechanism, untouched).
 *   - The HudMod.isFairPlay() flag itself is NOT modified globally — the redirect
 *     only takes effect when isFairPlay() is called from within these two specific
 *     methods, AND only while on Crusalis per the gating above. Any other code that
 *     checks isFairPlay() still sees the real value.
 *
 * VERIFICATION TO RUN AFTER DEPLOY: confirm cave mode and entity radar are STILL
 * blocked on Crusalis (try to enable them in XaeroPlus settings — they should still
 * be gated), AND confirm fairplay is untouched on a non-Crusalis server (join one,
 * confirm XaeroPlus's own draw features stay fairplay-gated there). If either check
 * fails, revert this file immediately.
 *
 * VERSION COMPATIBILITY: XaeroPlus changed drawMinimapFeatures' parameters (2.34.1
 * added a zoom arg) and DrawContext's fields (2.34.1 added the view matrix and camera
 * position) during the 1.21.11 line. This mixin deliberately depends on neither: it
 * never names the target methods' parameters or builds its own DrawContext, it only
 * hooks two call sites that are identical in every 1.21.11 release from 2.29.2
 * through 2.36.x — the HudMod.isFairPlay() check and DrawFeatureRegistry.forEach(
 * Consumer) — and hands our features the Consumer XaeroPlus already built.
 *
 * Every injector is still `require = 0` (soft-fail) rather than the mixins.json
 * default of 1: these are DrawManager internals, not XaeroPlus's stable addon API, so
 * a future release could change them. A mismatch then fails this one mixin quietly
 * (Mixin logs a WARN) and the overlay/fairplay-bypass no-ops instead of crashing.
 */
@Mixin(DrawManager.class)
public class AechronisDrawManagerMixin {

    @WrapOperation(
            method = {"drawMinimapFeatures", "drawWorldMapFeatures"},
            at = @At(value = "INVOKE", target = "Lxaeroplus/feature/render/DrawFeatureRegistry;forEach(Ljava/util/function/Consumer;)V"),
            require = 0
    )
    private void aechronis$drawOurFeatures(DrawFeatureRegistry registry, Consumer<DrawFeature> renderFeature,
                                           Operation<Void> original) {
        // renderFeature is XaeroPlus's own `feature -> feature.render(ctx)`, so ours draw
        // beneath XaeroPlus's features with the same context and transforms they get.
        for (DrawFeature feature : AechronisRenderer.ourFeatures) {
            renderFeature.accept(feature);
        }
        original.call(registry, renderFeature);
    }

    /**
     * Force HudMod.isFairPlay() to return false when called from within DrawManager's
     * draw methods, so XaeroPlus's OWN registered draw features render alongside ours —
     * but ONLY while connected to Crusalis (see the class-level "SERVER GATING" note).
     * AechronisRenderer.ourFeatures is non-empty exactly while AechronisMapMod's JOIN
     * handler has the renderer enabled, which only happens on Crusalis; everywhere else
     * this delegates to the real isFairPlay() value, so the bypass never leaks to other
     * servers this mod might be installed on.
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
            at = @At(value = "INVOKE", target = "Lxaero/common/HudMod;isFairPlay()Z"),
            require = 0
    )
    private boolean aechronis$forceFairPlayFalse(HudMod instance) {
        if (AechronisRenderer.ourFeatures.isEmpty()) return instance.isFairPlay();
        return false;
    }
}