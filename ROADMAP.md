# Nilum Roadmap

Status tracker

## Foundation

- [x] Handshake + capability negotiation
- [x] TCP side-channel with plugin-channel fallback
- [x] Hash-based asset cache
- [x] Per-server cache scoping, manifest-pruned
- [x] Paper plugin (handshake, rejection)
- [x] Fabric client mod
- [x] NeoForge client mod, full rendering parity with Fabric (see Modded Server Hosting)
- [x] Common library

## Models & Items

- [x] `.bbmodel` parser
- [x] In-world model rendering (display entities)
- [x] Collision group parser + VoxelShape derivation
- [x] Proxy material auto-classification
- [x] Custom item NBT rendering
- [x] Icon atlas (icon-only items)
- [x] Fake creative tabs
- [x] `/nilum giveitem`/`givemodel` split (item-definition lookup vs raw model id)
- [x] Server reports item-defined models/icons (`ItemDefinedAssetsPacket`)

## HUD System

- [x] `.atlas` format + parser
- [x] Server-driven frame updates
- [x] Atlas patch (delta) updates
- [x] Override/release for auto elements
- [x] Expression language (lexer/parser/evaluator)
- [x] All built-in functions + value sources
- [x] Server client-variable push
- [x] Custom font streaming (`.ttf` -> `nilum/font/` on the client)
- [ ] Font provider icons (image -> Private Use Area codepoint, inline in text)
- [x] Dynamic text HUD elements (`type: render_text`)
- [x] Actually use a streamed custom font when rendering text
- [x] `server_connector` + PlaceholderAPI integration for server-resolved text values
- [x] Inline player heads in `render_text` via `<head:uuid-or-username[:hat]>`
- [x] `text(...)` string concatenation, bare (unquoted) connector identifiers, `true`/`false` literals
- [x] `java(key)` server_connector value source
- [x] More vanilla value sources: `saturation`, `max_air`, `swimming`
- [x] `type: duplicate` HUD elements
- [x] `render_text` elements combining `client_connector` and `server_connector` via `format`
- [x] `type: image` HUD elements
- [x] Per-player HUD atlas/element visibility toggle, plus Skript effects for it and for setting a frame directly

## Custom Blocks

- [x] Block definitions (vanilla proxy / custom proxy)
- [x] Default-retexture cube model (no `.bbmodel` needed)
- [x] Break/place/explosion handling, approximate break timing
- [x] Item definitions + drop tables
- [x] Separate render pass with face/frustum/distance culling
- [ ] Exact break timing (currently approximate, on purpose)

## Shaders & Glints

- [x] Iris/Sodium/vanilla detection
- [x] Custom glint (suppress-and-replace, masked to real item geometry)
- [x] Iris shaderpack streaming + server-triggered activate/deactivate
- [ ] Server-pushed live uniform values into an already-active pack
- [x] Region-triggered shader swap
- [x] Block shader effects (Iris-only)
- [x] Per-player shaderpack activate/deactivate targeting

## Keybinds

- [x] 4 client keybinds, server-side event
- [x] Skript event (`on nilum keybind <1-4> <press|release|both>:`)

## Integrations

- [x] Skript addon
- [x] `skriptvar(name)` reads a real Skript global variable from the HUD expression language (read-only for now, write support planned for the Custom UI action language)
- [ ] Denizen extension
- [x] Public API + permission gating
- [ ] Connect/disconnect events for other plugins

## Animation

- [x] Parse `animations` block (named tracks, per-bone keyframes, loop mode)
- [x] Keyframe interpolation (linear/step/catmullrom)
- [x] Bone transform hierarchy
- [x] Playback on placed/in-world models
- [x] Playback on held/dropped items (real play/stop trigger state)
- [x] Custom player skeleton rendering
- [x] Real player skin support
- [x] Server-driven animation triggering protocol (play/stop/blend), placed models and held items
- [ ] Per-trigger loop-mode override for held items
- [x] Player hand IK animation for held items (Fabric only; live-tunable via `NilumHandTuneScreen`)
- [x] Empty-hand marker borrowing (driven by the other hand's item/animation)
- [x] Blockbench authoring: standalone "Nilum Animation" format (`nilum-blockbench-plugin`)

## Custom Biomes

- [ ] Custom biome definitions (custom generation settings)
- [ ] Custom block support in custom biome generation
- [ ] Datapack-like per-biome shader configuration

## Modded Server Hosting

- [x] Handshake + capability negotiation on both loaders, hosted server side
- [x] Asset streaming (models, HUD atlases, shader packs, fonts) on both loaders, hosted server side
- [x] Icon streaming on both loaders, hosted server side
- [x] Placing standalone models on both loaders, hosted server side
- [x] Custom blocks on both loaders, hosted server side (real block/blockentity, not a proxy)
- [x] Collision on both loaders, hosted server side (real per-position VoxelShape)
- [ ] Custom items, animation triggering, PlaceholderAPI-equivalent HUD text on both loaders, hosted server side
- [x] NeoForge client rendering parity with Fabric (hand-IK mixin still unported, see Animation)

## Dimensions

- [ ] Custom world generation (define new chunk/noise generation settings)
- [ ] Custom dimensions that use a defined custom world gen

## Custom UIs

- [x] `type: custom` layered UI (image/button/text/head layers, `anchor: center`)
- [x] Button layers (default/pressed images, click action, drag-off-cancel)
- [x] `type: text` (literal or one-time expression, inline head tags)
- [x] `type: head` (standalone player head glyph)
- [ ] Per-player variable state (skript-var-flavored get/set)
- [ ] Per-layer `requirement` (conditional visibility)
- [ ] Live-updating text (server-pushed, like HUD's `server_connector`)
- [ ] `type: chest` (custom GUI texture on a real chest inventory)
- [ ] Custom buttons inside chest UI (feasibility unconfirmed)

## Distribution

- [x] Client-only jar for playtesting (strips dedicated-server code)
- [x] `nilum-api` split into its own module
- [x] nilum-builds output reorganized by role
- [x] Bundled starter content on first enable (`DefaultAssetInstaller`)

## Web Tools

- [ ] HUD layout builder
- [ ] More tools TBD

## Polish

- [x] Missing-collision-group console warning
- [x] `collision_intent` flag
- [ ] Trust/consent prompt on connect
- [ ] TCP warning suppress command
- [ ] Progressive HUD load indicators
- [x] VRAM budget eviction
- [x] Passthrough/partial-transparency collision
