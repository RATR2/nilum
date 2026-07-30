<div align="center">

# Nilum

[![Release](https://img.shields.io/github/v/release/RATR2/nilum?display_name=release&style=for-the-badge&logo=github)](https://github.com/RATR2/nilum/releases/latest)
[![Issues](https://img.shields.io/github/issues/RATR2/nilum?style=for-the-badge&logo=github)](https://github.com/RATR2/nilum/issues)
[![Loaders](https://img.shields.io/badge/loaders-paper%20%7C%20fabric%20%7C%20forge-blue?style=for-the-badge)](#project-structure)
[![Stars](https://img.shields.io/github/stars/RATR2/nilum?style=for-the-badge&logo=github)](https://github.com/RATR2/nilum/stargazers)
[![Status](https://img.shields.io/badge/status-pre--implementation-yellow?style=for-the-badge)](#roadmap)

> Most Minecraft resource pack tools ship you a ZIP and hope the client applies it.
> Nilum flips that around: the server owns the truth and pushes it to the client, live, per player, no reload.

If you find any issues, please report them [here](https://github.com/RATR2/nilum/issues)

Thanks, R4T Out.

</div>

---

## What is this

Nilum is a Minecraft client mod paired with a server-side plugin. Join a Nilum server and it hands your client a manifest of custom models, textures, HUD elements, and shaders. Your client checks what it already has cached, pulls only what is missing or changed, and hot-loads the rest. No resource pack download screen, no reload stutter, no zip file.

It does not stop at join time either. The server can keep talking to the client for the rest of the session: swap a model mid-fight, patch one frame of a HUD texture, fire off a shader event for a boss phase, all without a reconnect.

## Why not just use a resource pack

Every existing tool in this space (ItemsAdder, Oraxen, Nexo, CraftEngine, and friends) does the same trick under the hood: bake a resource pack ZIP, host it somewhere, force the client to download it on join. That works, and it works well, but it has a hard ceiling because the client is never anything more than a file recipient:

- **Every player sees the same pack.** Nilum can show player A something different from player B, on the same server, at the same moment.
- **Custom collision is always a barrier underneath.** Nilum derives the real collision shape from the model itself, so the sword-you-can-stand-on problem does not exist here.
- **A texture update means a full reload.** Nilum patches the live GPU texture directly, no stutter, no reload.
- **HUDs are stuck with scoreboards and action bars.** Nilum drives pixel-perfect animated HUD elements, some of which the client animates entirely on its own with zero server packets.

None of that is "we do the same thing but nicer." It is a different category of feature that only exists because the client is an active participant, not a static renderer waiting on a zip file.

## Project Structure

| Module | Purpose |
|---|---|
| `nilum-common` | Shared, loader-agnostic logic: protocol definitions, asset cache manager, `.bbmodel` parser, HUD atlas renderer, expression evaluator, shader pipeline abstraction, trust model |
| `nilum-forge` | Forge client mod: channel registration, rendering hooks, creative tab injection |
| `nilum-fabric` | Fabric client mod: channel registration, rendering hooks, creative tab injection |
| `nilum-paper` | Paper/Bukkit server plugin: plugin channel messaging, TCP asset server, custom block registry, anti-cheat integration, asset registry, Open API |
| `nilum-skript` | Skript addon registering Nilum effects, conditions, and expressions |
| `nilum-denizen` | Denizen extension with commands and tags |

Group ID: `io.github.r4t2.nilum`

## Roadmap

Still pre-implementation. Foundation (protocol, TCP side-channel, asset cache) comes first, then model rendering and collision, then the HUD system, then shaders, then the Skript/Denizen integrations. Watch the [releases](https://github.com/RATR2/nilum/releases) page for progress.

## License

See [LICENSE.md](LICENSE.md). Source-available: you may read, build, and modify for personal use. Resale of the Software, Modifications, or Forks is reserved exclusively to the Owner.
