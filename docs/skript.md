# Nilum's Skript addon

Registered from `NilumSkriptAddon` (`nilum-paper/src/main/java/io/github/r4t2/nilum/paper/skript/`),
only once Skript is confirmed installed. Every effect delegates to `NilumAPI`, they don't touch
Nilum's internals directly.

## Effects

Give Nilum Item
- Pattern: `give %player% [the] nilum item %string%`
- Gives a player an item defined in Nilum's `items/<id>.yml`.
- Example: `give player the nilum item "ray_gun"`

Place Nilum Model
- Pattern: `place nilum model %string% at %location%`
- Places a loaded Nilum model as a standalone in-world display.
- Example: `place nilum model "scanner" at player's location`

Place/Remove Nilum Block
- Patterns: `place nilum block %string% at %location%`, `remove nilum block at %location%`
- Places or removes a real, rendered Nilum custom block.
- Examples: `place nilum block "crate" at target block's location`, `remove nilum block at target block's location`

Play/Stop Nilum Entity Animation
- Patterns: `play nilum animation %string% on %entity%`, `stop nilum animation on %entity%`
- Plays or stops a named animation on a player skeleton or placed model entity.
- Examples: `play nilum animation "reload" on player`, `stop nilum animation on player`

Play/Stop Nilum Block Animation
- Patterns: `play nilum animation %string% on [the] block at %location%`, `stop nilum animation on [the] block at %location%`
- Plays or stops a named animation on a Nilum block.
- Examples: `play nilum animation "open" on block at target block's location`, `stop nilum animation on block at target block's location`

Switch/Reset Nilum Shaderpack
- Patterns: `switch nilum shaderpack of %player% to %string%`, `reset nilum shaderpack of %player%`
- Switches a player's client to a Nilum shaderpack, or back to their previous state. Needs Iris on their client.
- Examples: `switch nilum shaderpack of player to "cave"`, `reset nilum shaderpack of player`

Open Nilum UI
- Pattern: `open [nilum] ui %string% for %player%`
- Opens a Nilum custom UI for a player.
- Example: `open nilum ui "main_menu" for player`

Set Nilum HUD Visibility
- Pattern: `set [nilum] hud %string% for %player% to %boolean%`
- Shows or hides an entire HUD atlas (`"hud_demo"`), or one element within it
  (`"hud_demo:health_bar"`), for a player. New per-player visibility toggle, before this every
  loaded atlas/element rendered unconditionally for every connected client.
- Examples: `set nilum hud "hud_demo" for player to false`, `set nilum hud "hud_demo:health_bar" for player to true`

Set Nilum HUD Frame
- Pattern: `set [nilum] hud frame %string% for %player% to %integer%`
- Sets a HUD atlas element's frame (`"hud_demo:health_bar"`) for a player. Wraps the existing
  `NilumAPI.setHudFrame`.
- Example: `set nilum hud frame "hud_demo:health_bar" for player to 3`

## Expressions

Nilum UI Of Player
- Patterns: `[nilum] ui of %players%`, `%players%'[s] [nilum] ui`
- The id of the Nilum custom UI a player currently has open, or nothing if they don't have one open.
- Example: `broadcast "%nilum ui of player%"`

Closed Nilum UI
- Pattern: `[the] closed [nilum] ui`
- The id of the Nilum custom UI that was just closed. Only meaningful inside `on ui close:`.
- Example: `on ui close:` / `	broadcast "%player% closed %closed nilum ui%"`

## Conditions

None registered.

## Events

Nilum UI Close
- Pattern: `[nilum] ui close`
- Fires when a player closes a Nilum custom UI, backed by a real Bukkit event
  (`NilumUiCloseEvent`, `nilum-paper/.../event/NilumUiCloseEvent.java`) that's actually fired
  through Bukkit's event bus, unlike `NilumUiActionEvent` below. `player` resolves to who closed
  it (registered as a real event value); `the closed nilum ui` resolves to which UI.
- Example: `on ui close:` / `	broadcast "%player% closed %closed nilum ui%"`

`NilumUiActionEvent` (`nilum-paper/.../skript/NilumUiActionEvent.java`) is not a Skript event and
never will be a `on ...:` trigger, despite the name. It's an internal, synthetic Bukkit event
Nilum constructs itself to run a Custom UI's `action:` line through Skript's real effect parser
(`Effect.parse` + `TriggerItem.walk`), never fired through Bukkit's own event bus. Its only
visible effect from a script author's side is that Skript's own `player` expression, and
`%player%` interpolation inside variable names (e.g. `{button1_clicked::%player%}`), resolve
correctly when used inside a Custom UI action string.

## Skript variables (not addon syntax)

`NilumSkriptVariables` (`nilum-paper/.../skript/NilumSkriptVariables.java`) reads and writes
Skript's real global variables (`ch.njol.skript.variables.Variables`) from Nilum's own Java code.
It isn't exposed as new Skript syntax; it's how `skriptvar(name)` in Nilum's HUD/Custom-UI
expression language reads a Skript variable's value, and how Custom UI actions running through
`NilumSkriptEffectRunner` end up modifying real `{...}` variables via ordinary Skript effects
like `set`/`delete`.
