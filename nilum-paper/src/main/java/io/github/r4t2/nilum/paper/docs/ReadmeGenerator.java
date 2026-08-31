package io.github.r4t2.nilum.paper.docs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes a plain folder-layout explainer into the plugin's data folder; see DocsConfig. */
public final class ReadmeGenerator {

    private ReadmeGenerator() {
    }

    public static void generate(Path dataFolder) throws IOException {
        Files.createDirectories(dataFolder);
        Files.writeString(dataFolder.resolve("README.md"), CONTENT, StandardCharsets.UTF_8);
    }

    private static final String CONTENT = """
            # Nilum

            This file is regenerated automatically (see `docs.generate-readme` in `config/main.yml` \
            to turn that off). It's a quick tour of what each folder here is for.

            ## blocks/

            One `.yml` per custom block, e.g. `blocks/ritual_table.yml`. Two ways to give it a look:

            - `model: <modelId>`: a full custom shape from `models/<modelId>.bbmodel`.
            - `textures:`: no model needed, just a plain cube reskinned per face. Keys: `top`,
              `bottom`, `north`, `south`, `east`, `west` (or `side` for all four sides at once, or
              `all` for every face), each naming a file in `textures/`.

            Every block also needs a `proxy:` section, the real vanilla block backing it:

            - `mode: vanilla` + `vanilla_block: <material>`: a real vanilla block, full vanilla
              mechanics for free (e.g. reskinning oak_log as a new wood type).
            - `mode: custom` + `wire_block: <material>`: an arbitrary custom block. `wire_block`
              defaults to glass if omitted (avoids visual gaps for models that don't fill a full
              cube). Also takes `break_time_seconds` and `explosion_resistant`.

            Optional `drops:` list for what it drops when broken.

            ## textures/

            Plain PNGs, shared by blocks and icons, referenced by filename from `blocks/*.yml` and
            `icons/*.yml`. Each texture gets its own auto-generated `<name>.yml` here recording
            which systems actually use it (`where: [icons, blocks]`); informational, not something
            you need to write yourself.

            ## icons/

            One `.yml` per icon-only custom item (a flat 2D icon, not a 3D model), e.g.
            `icons/ruby.yml`. Needs a `texture:` field naming a file in `textures/`, plus optional
            per-context (gui, thirdperson, etc.) positioning: either `generated` (vanilla's flat
            item default), `blockbench` (pulled from a referenced model's own Display panel data),
            or a literal `{x, y, z}` override.

            ## items/

            One `.yml` per full item definition (name, lore, enchantments), e.g.
            `items/fire_sword.yml`. `base:` is either a plain vanilla material, `nilum:model:<id>`
            for a full 3D model item, or `nilum:icon:<id>` for a flat icon item. Optional `glint:`
            section for a custom recolored enchant glint (color/intensity/speed, texture optional,
            defaults to vanilla's own glint texture, just recolored).

            ## models/

            Full `.bbmodel` files exported from Blockbench: real 3D geometry, streamed to clients
            and used for in-world models, full 3D custom blocks/items, and (eventually) animation.

            ## hud/

            `.atlas` descriptor + matching `.png` pairs for animated/expression-driven HUD elements
            (health bars, custom indicators, etc.). Elements with `"type": "render_text"` draw text
            instead of a sprite frame: `client_connector` for anything evaluable on the client
            (numbers, `name("client")`), `server_connector` for PlaceholderAPI placeholders like
            `placeholderapi("vault_eco_balance")`, evaluated server-side on an interval (see
            `hud-text.*` in `config/main.yml`) since PlaceholderAPI isn't installed on the client.

            ## shaderpacks/

            Real Iris-format shaderpack `.zip` files. Streamed to clients with Iris installed and
            switched on with `/nilum shaderpack <id>` (or back to normal with `/nilum shaderpack off`).

            ## fonts/

            Custom `.ttf` files, streamed to clients and saved to their own `nilum/font/` folder,
            for HUD text elements that want something other than the default font. Just the font
            files themselves for now; referencing one from a HUD element is still in progress.

            ## config/

            `main.yml`: the plugin's own settings (TCP side-channel, handshake, moderation,
            logging, this file).

            ## logs/

            Nilum's own log files, separate from the server's main log.

            ---

            Everything above has a matching `/nilum reload <type>` command, and most things also
            have a `/nilum give...`/`/nilum place...` command for testing; run `/nilum help` in
            game for the full list.
            """;
}
