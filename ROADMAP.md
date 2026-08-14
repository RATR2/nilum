# Nilum Roadmap

Status tracker

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

## HUD System

- [x] `.atlas` format + parser
- [x] Server-driven frame updates
- [x] Atlas patch (delta) updates
- [x] Override/release for auto elements
- [x] Expression language (lexer/parser/evaluator)
- [x] All built-in functions + value sources
- [x] Server client-variable push
- [x] Custom font streaming (`.ttf` -> `nilum/font/` on the client)
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

## Integrations

- [x] Skript addon
- [ ] Denizen extension
- [x] Public API + permission gating
- [ ] Connect/disconnect events for other plugins

## Animation

- [x] Parse `animations` block (named tracks, per-bone keyframes, loop mode)
- [x] Keyframe interpolation (linear/step/catmullrom)
- [x] Bone transform hierarchy
- [x] Playback on placed/in-world models
- [x] Playback on held/dropped items
- [x] Custom player skeleton rendering
- [x] Real player skin support
- [x] Server-driven animation triggering protocol (play/stop/blend)
- [ ] Player hand IK animation for held items

## Custom Biomes

- [ ] Custom biome definitions (custom generation settings)
- [ ] Custom block support in custom biome generation
- [ ] Datapack-like per-biome shader configuration

## Modded Server Hosting

- [x] Handshake + capability negotiation on both loaders, hosted server side
- [x] Asset streaming (models, HUD atlases, shader packs, fonts) on both loaders, hosted server side
- [x] Icon streaming on both loaders, hosted server side
- [x] Placing standalone models on both loaders, hosted server side
- [ ] Custom blocks on both loaders, hosted server side
- [ ] Collision on both loaders, hosted server side
- [ ] Custom items, animation triggering, PlaceholderAPI-equivalent HUD text on both loaders, hosted server side
- [ ] NeoForge client rendering parity with Fabric

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
