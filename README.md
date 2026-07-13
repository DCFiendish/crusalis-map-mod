# Crusalis Map Mod

A Fabric client mod that adds a live nation/territory overlay to Xaero's
Minimap and World Map, built for the [Nodes](https://nodes.soy/) town/nation
plugin. It polls the server's public map-data endpoints and Minecraft chat to
keep the overlay in sync in near real time — nation-colored territory fills,
resource node borders/labels, town and nation name labels, port markers, and
live war visuals during a siege.

## Features

- **Nation territory fills** — every claimed territory tinted by its owning
  nation's color, with the home/core chunk of each territory marked distinctly.
- **Resource node borders & labels** — territory outlines and resource-type
  labels (diamonds, gold, iron, etc.) for nodes with resources.
- **Town & nation labels** — town names at their spawn point; nation names at
  each nation's capital.
- **Ports** — port markers.
- **War visuals**, driven live off in-game `[War]` chat messages:
  - *Occupied territories* — a diagonal marker across a captured-but-not-yet-annexed
    territory, in the occupier's color, while the base fill still shows the
    original owner.
  - *Per-chunk war stripes* — a temporary highlight + X-mark on chunks captured
    within the last 90 seconds.
  - *Under-attack stripes* — a diagonal on chunks with a flag currently planted.
- All of the above are individually toggleable and opacity/width-adjustable via
  [Mod Menu](https://modrinth.com/mod/modmenu) / [Cloth Config](https://modrinth.com/mod/cloth-config).

## Requirements

- Fabric Loader, Fabric API
- [XaeroPlus](https://modrinth.com/mod/xaeroplus)
- Xaero's Minimap and Xaero's World Map
- Cloth Config, Mod Menu

## Scope

This mod is built against a specific server's Nodes plugin deployment and map
endpoints, and its more invasive rendering behavior (see below) was approved
by that server's admin specifically for that server — it will not activate on
any other server.

One piece of it is worth calling out explicitly: XaeroPlus gates its own
advanced draw-tool rendering behind a "fairplay" check on certain servers.
With the target server admin's explicit approval, this mod bypasses that check
*only while connected to that specific server* (`AechronisDrawManagerMixin`) so
XaeroPlus's own tools render there too, alongside this mod's overlay. That
bypass is scoped in code to the approved server and never applies anywhere
else — entity radar and cave-mode fairplay enforcement (separate mechanisms)
are untouched everywhere, per the admin's condition. If you're adapting this
mod for a different server, get your own admin's sign-off before enabling
anything equivalent, and keep the scoping intact.

## Building

See `libs/README.md` for one manual step (a couple of third-party jars aren't
redistributed in this repo). Otherwise, standard Fabric/Loom build:

```
./gradlew build
```

For IDE setup, see the [Fabric documentation](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up).

## License

CC0-1.0 — see `LICENSE`.
