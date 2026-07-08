package com.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AechronisDataFetcher {

    private static final String TOWNS_URL     = "https://map.crusalis.net/nodes/towns.json";
    private static final String WORLD_URL     = "https://map.crusalis.net/nodes/world.json";
    private static final String PORTS_URL     = "https://map.crusalis.net/nodes/ports.json";
    private static final String MAP_REFERER   = "https://map.crusalis.net/";
    private static final String GIST_URL      = "https://gist.githubusercontent.com/DCFiendish/a0989e75d3d6dadb9a2af6254232a350/raw/nation_colors.json";
    private static final String WHITELIST_URL = "https://gist.githubusercontent.com/DCFiendish/9853abe237d1fee0818ec540ef92e0d0/raw/whitelist.json";

    public AechronisMapData mapData;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Aechronis-Fetcher");
        t.setDaemon(true);
        return t;
    });

    public void start() {
        scheduler.schedule(this::fetchWhitelist, 0, TimeUnit.SECONDS);
        scheduler.schedule(this::fetchGistColors, 0, TimeUnit.SECONDS);
        scheduler.schedule(this::fetchWorldAndTerritories, 2, TimeUnit.SECONDS);
        scheduler.schedule(this::fetchPorts, 3, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::fetchTownsJson, 5, 60, TimeUnit.SECONDS);
    }

    private void fetchWhitelist() {
        try {
            System.out.println("[Aechronis] Fetching whitelist...");
            String json = fetch(WHITELIST_URL);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray list = obj.getAsJsonArray("whitelist");
            Set<String> uuids = new HashSet<>();
            for (JsonElement e : list) uuids.add(e.getAsString().toLowerCase());
            mapData.whitelistedUuids = uuids;
            System.out.println("[Aechronis] Whitelist loaded: " + uuids.size() + " entries");
        } catch (Exception e) {
            System.out.println("[Aechronis] Whitelist fetch error: " + e.getMessage());
        }
    }

    private void fetchGistColors() {
        try {
            System.out.println("[Aechronis] Fetching nation color overrides from Gist...");
            String json = fetch(GIST_URL);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            mapData.loadGistColors(obj);
            System.out.println("[Aechronis] Gist colors loaded.");
        } catch (Exception e) {
            System.out.println("[Aechronis] Gist fetch error: " + e.getMessage());
        }
    }

    private void fetchWorldAndTerritories() {
        try {
            System.out.println("[Aechronis] Fetching world.json and towns.json for territory data...");
            String worldStr = fetch(WORLD_URL);
            String townsStr = fetch(TOWNS_URL);
            JsonObject worldJson = JsonParser.parseString(worldStr).getAsJsonObject();
            JsonObject townsJson = JsonParser.parseString(townsStr).getAsJsonObject();
            mapData.loadWorldData(worldJson);
            mapData.loadTownsData(townsJson, townsStr);
            System.out.println("[Aechronis] World and territory data loaded.");
        } catch (Exception e) {
            System.out.println("[Aechronis] World fetch error: " + e.getMessage());
        }
    }

    private void fetchPorts() {
        try {
            System.out.println("[Aechronis] Fetching ports.json...");
            String json = fetch(PORTS_URL);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            mapData.loadPortData(obj);
            System.out.println("[Aechronis] Ports loaded.");
        } catch (Exception e) {
            System.out.println("[Aechronis] Ports fetch error: " + e.getMessage());
        }
    }

    private void fetchTownsJson() {
        try {
            String json = fetch(TOWNS_URL);
            JsonObject townsJson = JsonParser.parseString(json).getAsJsonObject();
            mapData.loadTownsData(townsJson, json);
        } catch (Exception e) {
            System.out.println("[Aechronis] Towns fetch error: " + e.getMessage());
        }
    }

    private String fetch(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        var conn = url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "AechronisMapMod/1.0");
        if (urlStr.startsWith("https://map.crusalis.net/")) {
            conn.setRequestProperty("Referer", MAP_REFERER);
        }
        try (InputStream is = conn.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}