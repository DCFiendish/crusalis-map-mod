package com.example;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.AutoConfig;

@Config(name = "aechronismapmod")
public class AechronisConfig implements ConfigData {

    // ── Map Overlay ──────────────────────────────────────────
    public boolean showEverything = true;
    public int nationFillOpacity = 39;
    public int nodeBorderOpacity = 100;
    // Occupied-not-annexed territory diagonal: single line across the whole node in
    // the occupier's color. Distinguished from the nation fill by OPACITY, not hue.
    public int occupiedDiagonalOpacity = 100;
    public float occupiedDiagonalWidth = 0.14f;
    public boolean showNationFills = true;
    public boolean showNodeBorders = true;
    public boolean showNodeLabels = true;
    public boolean showTownLabels = true;
    public boolean showNationLabels = true;
    public boolean showPorts = true;
    public boolean showPortConnections = true;
    public boolean whiteBorders = false;

    // ── Getters used by renderer ──────────────────────────────
    public int getNationFillAlpha() { return (int)(nationFillOpacity / 100f * 255); }
    public int getNodeBorderAlpha() { return (int)(nodeBorderOpacity / 100f * 255); }
    public int getOccupiedDiagonalAlpha() { return (int)(occupiedDiagonalOpacity / 100f * 255); }

    public static AechronisConfig get() {
        return AutoConfig.getConfigHolder(AechronisConfig.class).getConfig();
    }
}