# Nilum Roadmap

Status tracker, checked against the full design doc (`Nilum_DesignDocument.md`).

## Foundation

- [x] Handshake + capability negotiation
- [x] TCP side-channel with plugin-channel fallback
- [x] Hash-based asset cache
- [x] Paper plugin (handshake, rejection)
- [x] Fabric client mod
- [x] NeoForge client mod (channels only, no rendering parity with Fabric)
- [x] Common library

## Models & Items

- [x] `.bbmodel` parser
- [x] In-world model rendering (display entities)
- [x] Collision group parser + VoxelShape derivation
- [x] Proxy material auto-classification
- [x] Custom item NBT rendering
- [x] Icon atlas (icon-only items)
- [x] Fake creative tabs
- [ ] Anti-cheat exemptions (Grim/Matrix/Spartan) — deferred on purpose

## HUD System

- [x] `.atlas` format + parser
- [x] Server-driven frame updates
- [x] Atlas patch (delta) updates
- [x] Override/release for auto elements
- [x] Expression language (lexer/parser/evaluator)
- [x] All built-in functions + value sources
- [x] Server client-variable push
- [x] Custom font streaming (`.ttf` -> `nilum/font/` on the client) - not wired into rendering yet
- [x] Dynamic text HUD elements (`type: render_text`), driven by the existing expression language - vanilla's default font only, numeric connectors and `name("client")` both work
- [ ] Actually use a streamed custom font when rendering text (registering it with Minecraft's font system)
- [x] `server_connector` + PlaceholderAPI integration for server-resolved text values - polled per player on an interval (`hud-text.update-interval-ticks` in `config/main.yml`), soft-depends on PlaceholderAPI, only pushes on change
- [x] Inline player heads in `render_text` via `<head:uuid-or-username[:hat]>` - real vanilla `Component.object(PlayerSprite)`, same mechanism as Paper's MiniMessage `<head>` tag

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
- [x] Iris shaderpack streaming + server-triggered activate/deactivate, confirmed working in-game (real Iris API: `IrisConfig` + `Iris.reload()`) — switching packs forces Iris to rebuild its pipeline, so this isn't seamless
- [ ] Server-pushed live uniform values into an already-active pack — not possible, Iris has no API for it (checked the real shipped mod, not just the doc)
- [ ] Seamless region-triggered shader swap with no reload — would need mixins into Iris's own internals (`WorldRenderingPipeline`), scoped but not attempted
- [ ] Block shader effects (Iris-only)

## Keybinds

- [x] 4 client keybinds, server-side event

## Integrations

- [ ] Skript addon
- [ ] Denizen extension
- [ ] Public API + permission gating
- [ ] Connect/disconnect events for other plugins

## Animation

- [x] Parse `animations` block (named tracks, per-bone keyframes, loop mode)
- [x] Keyframe interpolation (linear/step/catmullrom, verified against real files)
- [ ] Bone transform hierarchy (apply parsed tracks to a model's element tree over time)
- [ ] Playback on placed/in-world models (blocks, display-entity models)
- [ ] Playback on held/dropped items
- [ ] Custom player skeleton rendering - real `AvatarRenderer` mixin injection point found and confirmed firing in-game, renderer itself not built yet
- [ ] Server-driven animation triggering protocol (play/stop/blend, matching the packet patterns already used for HUD/blocks)

## Custom Biomes

- [ ] Custom biome definitions (custom generation settings)
- [ ] Custom block support in custom biome generation
- [ ] Datapack-like per-biome shader configuration

## Modded Server Hosting

- [ ] NeoForge-hosted server: asset registries (models/blocks/items/icons/HUD) — handshake only right now
- [ ] Fabric-hosted server — doesn't exist, Paper-only today

## Polish

- [x] Missing-collision-group console warning
- [x] `collision_intent` flag
- [ ] Trust/consent prompt on connect
- [ ] TCP warning suppress command
- [ ] Progressive HUD load indicators
- [ ] VRAM budget eviction (we invalidate on reload, not budget-based)
- [ ] Passthrough/partial-transparency collision
