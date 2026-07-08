package com.example;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class AechronisMapMod implements ClientModInitializer {

	public static AechronisMapData mapData;
	private static AechronisDataFetcher fetcher;
	private static AechronisRenderer renderer;
	private static boolean rendererRegistered = false;

	@Override
	public void onInitializeClient() {
		System.out.println("[Aechronis] Initializing...");

		// Register config
		AutoConfig.register(AechronisConfig.class, GsonConfigSerializer::new);

		// Create data objects
		mapData = new AechronisMapData();
		fetcher = new AechronisDataFetcher();
		fetcher.mapData = mapData;

		// Register chat listener
		new AechronisChatListener(mapData).register();

		// Re-run on EVERY join, including proxy transfers (e.g. lobby -> main server),
		// since XaeroPlus may treat a backend transfer as a new map-world and drop
		// previously-registered draw features. We re-enable (not just enable-once)
		// so the draw features get freshly re-registered every time.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {

			// Only activate on the actual Crusalis server — skip singleplayer and any other server entirely.
			// IMPORTANT: every early-return path below must explicitly disable an already-enabled
			// renderer, not just skip re-enabling it. Otherwise leaving Crusalis (e.g. quitting to
			// singleplayer, or to any other server) leaves the PREVIOUS session's renderer running —
			// its draw features stay registered in AechronisRenderer.ourFeatures and the mixin keeps
			// rendering them unconditionally every frame regardless of what world you're actually in.
			// (This was the actual cause of the nation overlay showing up in singleplayer.)
			var serverData = client.getCurrentServer();
			String serverAddress = serverData != null ? serverData.ip : null;
			if (serverAddress == null || !serverAddress.toLowerCase().contains("crusalis.net")) {
				System.out.println("[Aechronis] Not connected to Crusalis (address=" + serverAddress + "), mod inactive.");
				if (rendererRegistered) {
					renderer.disable();
					System.out.println("[Aechronis] Renderer disabled (left Crusalis).");
				}
				return;
			}

			String uuid = client.player != null
					? client.player.getGameProfile().id().toString().toLowerCase()
					: null;

			if (uuid == null) {
				System.out.println("[Aechronis] Could not get player UUID, mod inactive.");
				if (rendererRegistered) {
					renderer.disable();
					System.out.println("[Aechronis] Renderer disabled (no player UUID).");
				}
				return;
			}

			// Retry loop — whitelist fetch may still be in progress (cheap no-op if already loaded)
			boolean allowed = false;
			for (int i = 0; i < 10; i++) {
				if (!mapData.whitelistedUuids.isEmpty()) {
					allowed = mapData.whitelistedUuids.contains(uuid);
					break;
				}
				try { Thread.sleep(500); } catch (InterruptedException ignored) {}
			}

			if (!allowed) {
				System.out.println("[Aechronis] Not whitelisted, mod inactive.");
				if (rendererRegistered) {
					renderer.disable();
					System.out.println("[Aechronis] Renderer disabled (not whitelisted).");
				}
				return;
			}

			if (!rendererRegistered) {
				// First time ever this session: create and add the module once.
				renderer = new AechronisRenderer(mapData);
				xaeroplus.module.ModuleManager.addModule(renderer);
				renderer.enable();
				rendererRegistered = true;
				System.out.println("[Aechronis] Renderer created and enabled.");
			} else {
				// Subsequent joins (proxy transfers etc.) — force a fresh re-registration
				// of draw features by disabling then re-enabling the same module instance.
				renderer.disable();
				renderer.enable();
				System.out.println("[Aechronis] Renderer re-enabled (fresh registration).");
			}
		});

		// Start fetcher
		fetcher.start();

		System.out.println("[Aechronis] Initialized!");
	}
}