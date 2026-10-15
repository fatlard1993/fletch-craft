# Fletch Craft

A Fabric mod that turns the fletching table into a crafting bench of its own, for arrows, bows, crossbows, flint and obsidian tools, throwing knives and wood processing.

![The fletching table as a bench: three columns of flint, stick and feather make twenty-four arrows](screenshots/arrow-bench.png)

## The Bench

Right-click a fletching table to open a 3x3 grid and a result slot, laid out like a crafting table. Every recipe also works flipped left to right, and a fletching recipe picked in the recipe book is laid out in the grid from your inventory: one set with a click, as many sets as you can afford (up to a full stack in each slot) with a shift-click.

Fletching recipes cannot be made at a crafting table; asking for one there says "That is made at a fletching table."

## Recipes

Layouts are rows from top to bottom, `_` an empty cell. Anything that fits in a smaller box can go anywhere in the grid.

| Makes | Layout |
|---|---|
| 8 Arrows | flint / stick / feather, in one column |
| 24 Arrows | three of that column side by side |
| 3 Spectral Arrows | glowstone dust x3 / arrow x3 |
| Bow | stick string / stick string / stick string |
| Crossbow | stick, iron nugget, stick / string, stick, string |
| Target | redstone / hay bale |
| 3 Flint | 4 gravel in a 2x2 square |
| 9 String | 1 wool, any colour |
| Stripped log | 1 log or stem of any wood, or a bamboo block |
| 16 Sticks | 1 stripped log or stem |
| 3 Sticks | 1 plank, any wood |
| 2 Sticks | 1 bamboo |
| Flint / Obsidian Axe | head head / head stick / _ stick |
| Flint / Obsidian Shovel | head / stick / stick |
| Flint / Obsidian Hoe | head head / _ stick / _ stick |
| 2 Flint / Obsidian Throwing Knives | head / stick |

"head" is flint for the flint version and obsidian for the obsidian one.

All of these are datapack recipes under `data/fletch_craft/recipe/`, so a server can add, remove or change them. A fletching recipe is a `width`, a `height` and its `ingredients` read row by row, with `""` for an empty cell.

## Flint And Obsidian Tools

The tools and knives come only from the fletching table.

| | Mines like | Durability | Speed | Enchantability | Repaired with |
|---|---|---|---|---|---|
| Flint | stone | 96 | 5 | 10 | flint |
| Obsidian | iron | 200 | 7 | 22 (as gold) | obsidian |

For comparison, stone tools have 131 durability and speed 4, iron 250 and 6.

Right-click a throwing knife to throw it. A flint knife hits for 4 damage, obsidian for 6. A knife drops where it lands, but one in five that hits a mob shatters.

## Village Quests

With [village-quests](https://github.com/fatlard1993/village-quests) installed, a fletcher teaches the table in a run of lessons: twelve flint from gravel, nine string from wool, thirty-two arrows made on the table, and a crossbow made on the table.

## Pandorical

Fletch Craft goes on the server only, and **Pandorical is required there and on every player's client**: the flint and obsidian tools and knives are new items, and a client without Pandorical has no entry for them. A player without Pandorical who right-clicks a fletching table is told "Fletching requires Pandorical." and no bench opens.

## Development

Installing is in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
