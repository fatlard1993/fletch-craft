# Fletch Craft

A Fabric mod that makes the fletching table functional as a specialized crafting station for arrows, bows, crossbows, and wood processing, with its own dedicated crafting interface and an in-menu recipe browser.

## Features

- **Functional Fletching Table**: Right-click a fletching table to open a dedicated crafting screen with its own 3x3 grid, result slot, and recipe browser panel
- **Recipe Browser**: Click any recipe in the panel to auto-fill the grid from your inventory (shift-click to fill as many sets as your materials allow)
- **Data-Driven Recipes**: All fletching recipes are JSON files under `data/fletch_craft/recipe/`, customizable via datapacks without touching code
- Included recipes:
  - **Arrows** (16): flint + sticks + feathers
  - **Spectral Arrows** (3): glowstone dust + arrows
  - **Bow**: sticks + string
  - **Crossbow**: sticks + iron nugget + string (simplified recipe)
  - **Stripping**: place any log or bamboo block to get its stripped variant
  - **Sticks**: from stripped logs, planks, or bamboo
  - **Flint** (3): gravel
  - **String** (9): any wool color
  - **Target Block**: redstone + hay bale

## Requirements

- Targets the Minecraft, Fabric Loader, and Fabric API versions declared in this mod's `gradle.properties`. Check there for the exact currently-supported version
- Java version as declared in `fabric.mod.json`'s `depends` block
- Pandorical (see below)

## Pandorical

Fletch Craft's entire fletching table interface (the crafting grid, result slot, and recipe browser) is a Pandorical container screen. The mod checks for the `screens` capability when a player opens a fletching table and, without it, tells the player fletching requires Pandorical instead of opening anything.

**The Pandorical mod must be installed client-side to use the fletching table at all.** Vanilla clients (or clients without Pandorical) cannot interact with the fletching table through this mod.

## Installation

Install alongside its declared dependencies (see `fabric.mod.json`), including Pandorical on both the server and every connecting client.

## Customization

All recipes are data-driven JSON files in `data/fletch_craft/recipe/`. Server admins can add, remove, or modify fletching recipes via datapacks without touching code.

## License

This mod is licensed under the MIT License. See [LICENSE](LICENSE) for details.
