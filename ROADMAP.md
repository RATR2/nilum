# Nilum Roadmap

Status tracker

## Foundation

- [x] Handshake + capability negotiation
- [x] TCP side-channel with plugin-channel fallback
- [x] Hash-based asset cache
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

Intended support matrix: Paper is the one server type both client loaders must fully support
(including custom blocks), since it's the only tier where the client loader doesn't have to
match the server loader. A Fabric-hosted server only ever needs to support a Fabric client; a
NeoForge-hosted server only ever needs to support a NeoForge client.

- [x] Handshake + capability negotiation on both loaders, hosted server side
- [x] Asset streaming (models, HUD atlases, shader packs, fonts) on both loaders, hosted server side
- [x] Icon streaming on both loaders, hosted server side
- [x] Placing standalone models on both loaders, hosted server side
- [x] Custom blocks on both loaders, hosted server side (real registered `NilumBlock`/`NilumBlockEntity` per loader, not a proxy; `explosion_resistance` is parsed but not yet enforced per-block; now renders on NeoForge too, see below)
- [x] Collision on both loaders, hosted server side (real per-position `VoxelShape` from the placed model's `collision` group; PARTIAL boxes dampen movement via `entityInside`, matching the existing Paper behavior)
- [ ] Custom items, animation triggering, PlaceholderAPI-equivalent HUD text on both loaders, hosted server side
- [x] NeoForge client rendering parity with Fabric: real `NilumBlock` models/entity display/held-dropped-inventory items/icon-atlas rendering plus glints (`EntityRenderersEvent.RegisterRenderers`, `ModelEvent.ModifyBakingResult` wrapping every baked `ItemModel`), HUD atlas rendering (`RegisterGuiLayersEvent`, `GuiLayer.render` has the exact same signature as Fabric's `HudElement.render` so `HudAtlasRenderer` needed no logic changes), custom font streaming, fake creative tabs (`DeferredRegister<CreativeModeTab>` + `BuildCreativeModeTabContentsEvent`), and real Iris integration (`iris-neoforge-1.10.7+mc1.21.11.jar` vendored the same way `iris-fabric` is; same `net.irisshaders.iris` API on both loaders, ports verbatim; a one-time log recommends Iris if Oculus is installed without it). Plus the Paper-compatibility path both client loaders need regardless of hosted-server work: `ClientBlockRegistry`/`NilumBlockRenderer`/`NilumBlockStateModel`, ported using NeoForge's own `BlockStateModelExtension` position-aware `collectParts` in place of Fabric's FRAPI, and `RenderLevelStageEvent.AfterEntities` with immediate-mode `MultiBufferSource` rendering in place of Fabric's `SubmitNodeCollector`-based one (NeoForge's render-stage event doesn't hand one out). `NilumChunkBlocksPayload` came back after being deleted during the custom-blocks rework; that deletion was correct for NeoForge-hosted servers (real blocks, vanilla sync) but wrongly also removed a NeoForge client's ability to render Paper's wire-block overlay blocks. Almost the rest of the port was near-verbatim since nilum-fabric's `render/`/`hud/`/`font/`/`creativetab/` packages turned out to be vanilla-API only; only files touching a static `NilumFabricMod.LOGGER`/`NilumFabricClient.FONT_STORE` field needed real changes (NeoForge's mod class uses instance fields, so those became constructor parameters instead). The held-item hand-IK mixin is still unported, but that's not a parity gap since it's still unfinished on Fabric itself (see Animation)

## Distribution

- [ ] Client-only jar for playtesting, so it can't be decompiled into a working Paper plugin or a hosted Fabric/NeoForge server (`nilum-common-client` split off and wired into both loaders; the server-only half is blocked on `NilumAssetHost`'s dependency chain moving as one unit; still needs an actual client-only build variant and splitting `NilumNeoForgeMod` into client/dedicated-server classes)
- [x] Move `NilumAPI` out of nilum-paper into its own module (`nilum-api`), so other plugins can compile against it without depending on the whole plugin jar

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
