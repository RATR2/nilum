<div align="center">

# <img width="86" height="86" alt="nilum-blender" src="https://github.com/user-attachments/assets/3da93e76-0dc4-426b-aefe-fb3b3b06e11f" /> Nilum

[![Release](https://img.shields.io/github/v/release/RATR2/nilum?display_name=release&style=for-the-badge&logo=github)](https://github.com/RATR2/nilum/releases/latest)
[![Issues](https://img.shields.io/github/issues/RATR2/nilum?style=for-the-badge&logo=github)](https://github.com/RATR2/nilum/issues)
[![Loaders](https://img.shields.io/badge/loaders-paper%20%7C%20fabric%20%7C%20neoforge-blue?style=for-the-badge)](#project-structure)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.11-blue?style=for-the-badge)](#project-structure)
[![Stars](https://img.shields.io/github/stars/RATR2/nilum?style=for-the-badge&logo=github)](https://github.com/RATR2/nilum/stargazers)
[![Status](https://img.shields.io/badge/status-in--development-yellow?style=for-the-badge)](#roadmap)

> Most Minecraft resource pack tools ship you a ZIP and hope the client applies it.
> Nilum flips that around: the server owns the truth and pushes it to the client, live, per player, no reload.

If you find any issues, please report them [here](https://github.com/RATR2/nilum/issues)

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
| `nilum-common` | Shared, loader-agnostic logic: wire protocol, a custom config system and logger, the TCP side-channel plus hash-based asset transfer, and the `.bbmodel` parser (geometry, textures, collision groups) |
| `nilum-neoforge` | NeoForge mod: handshake (server and client roles), TCP side-channel and asset sync. In-world model rendering isn't built yet |
| `nilum-fabric` | Fabric mod: handshake (server and client roles), TCP side-channel and asset sync, and a client-side renderer for in-world Nilum models |
| `nilum-paper` | Paper/Bukkit server plugin: handshake, TCP asset server, model registry and in-world placement, collision group resolution. Skript and Denizen integrations will live here as addon packages, not separate modules |

Group ID: `io.github.r4t2.nilum`. Targets Minecraft 1.21.11. Classic Forge is legacy from 1.20.2 onward and isn't binary-compatible with mods from this era, so NeoForge is the tier-3 loader here instead.

## Roadmap

Foundation is done: the handshake protocol, the TCP side-channel, and hash-based asset transfer all work end to end. The Blockbench model pipeline is in progress: the `.bbmodel` parser, in-world placement, and collision group parsing all work, and Fabric has a working client-side renderer for placed models (not yet checked against a live client). Still to come: NeoForge's renderer, proxy-block material classification, anti-cheat exemption, the HUD system, shaders, and the Skript/Denizen integrations. Watch the [releases](https://github.com/RATR2/nilum/releases) page for progress.

## License

See [LICENSE.md](LICENSE.md). Source-available: you may read, build, and modify for personal use. Resale of the Software, Modifications, or Forks is reserved exclusively to the Owner.

---

<div align="center">

Thanks, R4T Out.

</div>
