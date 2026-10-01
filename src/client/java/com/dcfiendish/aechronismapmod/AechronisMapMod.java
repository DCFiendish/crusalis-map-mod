package com.dcfiendish.aechronismapmod;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class AechronisMapMod implements ClientModInitializer {

	public static AechronisMapData mapData;
	private static AechronisDataFetcher fetcher;

	@Override
	public void onInitializeClient() {
		System.out.println("[Crusalis] Initializing...");

		// Register config
		AutoConfig.register(AechronisConfig.class, GsonConfigSerializer::new);

		// Create data objects
		mapData = new AechronisMapData();
		fetcher = new AechronisDataFetcher();
		fetcher.mapData = mapData;
		AechronisRenderer.init(mapData);

		// Register chat listener
		new AechronisChatListener(mapData).register();

		// Purges and cache rebuilds run here, independent of toggles, dimension or open screens.
		ClientTickEvents.END_CLIENT_TICK.register(client -> AechronisRenderer.tick());

		// Re-run on EVERY join, including proxy transfers (e.g. lobby -> main server).
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			// Only activate on the actual Crusalis server — skip singleplayer and any other server entirely.
			// Every early-return path must deactivate the overlay, or leaving Crusalis (e.g. quitting to
			// singleplayer, or to any other server) keeps drawing the previous session's data.
			var serverData = client.getCurrentServer();
			String serverAddress = serverData != null ? serverData.ip : null;
			boolean onCrusalis = serverAddress != null && serverAddress.toLowerCase().contains("crusalis.net");
			// Dev/testing only: draw live Crusalis data in any world (e.g. the dev client's singleplayer).
			if (!onCrusalis && Boolean.getBoolean("crusalis.devForceActive")) onCrusalis = true;
			if (!onCrusalis) {
				System.out.println("[Crusalis] Not connected to Crusalis (address=" + serverAddress + "), mod inactive.");
				AechronisRenderer.setActive(false);
				fetcher.onLeaveCrusalis();
				return;
			}

			AechronisRenderer.setActive(true);
			System.out.println("[Crusalis] Overlay enabled.");
			fetcher.onJoinCrusalis();
		});

		// Disconnecting (quit to title, kicked, connection lost) does not fire another JOIN.
		// The overlay holds no GPU resources of its own any more (Xaero's buffers carry the
		// vertices), so it can be switched off right away.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			AechronisRenderer.setActive(false);
			fetcher.onLeaveCrusalis();
		});

		System.out.println("[Crusalis] Initialized!");
	}
}
