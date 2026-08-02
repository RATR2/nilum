package io.github.r4t2.nilum.paper;

import io.github.r4t2.nilum.paper.block.CustomBlockBroadcaster;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class NilumCommand implements CommandExecutor, TabCompleter {

    private final NilumPlugin plugin;

    public NilumCommand(NilumPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendVersion(sender);
            sender.sendMessage(Component.text("If you're looking for help with Nilum, run /nilum help"));
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "ver", "version" -> {
                sendVersion(sender);
                yield true;
            }
            case "help" -> {
                sendHelp(sender);
                yield true;
            }
            case "reload" -> reload(sender, args);
            case "placemodel" -> placeModel(sender, args);
            case "giveitem" -> giveItem(sender, args);
            case "giveicon" -> giveIcon(sender, args);
            case "hudframe" -> hudFrame(sender, args);
            case "placeblock" -> placeBlock(sender, args);
            case "removeblock" -> removeBlock(sender, args);
            case "giveitemdef" -> giveItemDef(sender, args);
            case "shaderpack" -> shaderPack(sender, args);
            default -> {
                sender.sendMessage(Component.text(
                        "Unknown subcommand '" + args[0] + "'. Run /nilum help for a list of commands."));
                yield true;
            }
        };
    }

    private void sendVersion(CommandSender sender) {
        String version = plugin.getPluginMeta().getVersion();
        String commit = plugin.buildCommit();
        boolean showCommit = version.endsWith("-SNAPSHOT") && !commit.equals("unknown");
        sender.sendMessage(Component.text("Nilum v" + version + (showCommit ? " (commit " + commit + ")" : "")));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Nilum commands:"));
        sender.sendMessage(Component.text("/nilum ver - Show the plugin version."));
        sender.sendMessage(Component.text("/nilum help - Show this message."));
        sender.sendMessage(Component.text(
                "/nilum reload <models|icons|hud|items|blocks|shaderpacks|fonts|tcp|config> - Reload part of Nilum."));
        sender.sendMessage(Component.text(
                "/nilum placemodel <modelId> [x] [y] [z] [yaw] [pitch] - Place a model. "
                        + "Coordinates and rotation default to ~ (your position/facing)."));
        sender.sendMessage(Component.text("/nilum giveitem <modelId> [material] - Give yourself a custom item."));
        sender.sendMessage(Component.text("/nilum giveicon <iconId> [material] - Give yourself an icon-only custom item."));
        sender.sendMessage(Component.text("/nilum hudframe <atlasId>:<elementId> <frame> - Set a HUD frame for yourself."));
        sender.sendMessage(Component.text(
                "/nilum placeblock <blockId> [x] [y] [z] - Place a real, rendered custom block."));
        sender.sendMessage(Component.text("/nilum removeblock [x] [y] [z] - Remove a Nilum block."));
        sender.sendMessage(Component.text("/nilum giveitemdef <id> - Give yourself a defined item (name/lore/enchants included)."));
        sender.sendMessage(Component.text(
                "/nilum shaderpack <id>|off - Switch yourself to a Nilum shaderpack, or back to normal. Needs Iris."));
    }

    private boolean reload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /nilum reload <models|icons|hud|items|blocks|shaderpacks|fonts|tcp|config>"));
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "models" -> reloadAndReport(sender, plugin.reloadModels(), "Models", "reload the models folder");
            case "icons" -> reloadAndReport(sender, plugin.reloadIcons(), "Icons", "reload the icons folder");
            case "hud" -> reloadAndReport(sender, plugin.reloadHudAtlases(), "HUD atlases", "reload the hud folder");
            case "items" -> reloadAndReport(sender, plugin.reloadItems(), "Item definitions", "reload the items folder");
            case "blocks" -> reloadAndReport(sender, plugin.reloadBlocks(), "Block definitions", "reload the blocks folder");
            case "shaderpacks" -> reloadAndReport(sender, plugin.reloadShaderPacks(), "Shader packs", "reload the shaderpacks folder");
            case "fonts" -> reloadAndReport(sender, plugin.reloadFonts(), "Fonts", "reload the fonts folder");
            case "tcp" -> reloadAndReport(sender, plugin.reloadTcp(), "TCP side-channel", "reload the TCP side-channel");
            case "config" -> reloadAndReport(sender, plugin.reloadSettings(), "Config", "reload the config");
            default -> {
                sender.sendMessage(Component.text(
                        "Unknown reload target '" + args[1] + "'. Usage: /nilum reload <models|icons|hud|items|blocks|shaderpacks|fonts|tcp|config>"));
                yield true;
            }
        };
    }

    private boolean reloadAndReport(CommandSender sender, boolean success, String subjectCapitalized, String failureAction) {
        sender.sendMessage(Component.text(success
                ? subjectCapitalized + " reloaded."
                : "Failed to " + failureAction + ", check the console/log for details."));
        return true;
    }

    private boolean placeModel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can place a model (needs a location)."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /nilum placemodel <modelId> [x] [y] [z] [yaw] [pitch]"));
            return true;
        }

        String modelId = args[1];
        Location base = player.getLocation();
        Location location;
        try {
            double x = parseCoordinate(args.length > 2 ? args[2] : "~", base.getX());
            double y = parseCoordinate(args.length > 3 ? args[3] : "~", base.getY());
            double z = parseCoordinate(args.length > 4 ? args[4] : "~", base.getZ());
            float yaw = (float) parseCoordinate(args.length > 5 ? args[5] : "~", base.getYaw());
            float pitch = (float) parseCoordinate(args.length > 6 ? args[6] : "~", base.getPitch());
            location = new Location(base.getWorld(), x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid coordinate - use a number or ~ (optionally ~offset)."));
            return true;
        }

        UUID entityId = plugin.modelDisplays().place(location, modelId).orElse(null);

        if (entityId == null) {
            sender.sendMessage(Component.text("No loaded model named '" + modelId
                    + "' (drop a matching .bbmodel file into the plugin's models folder)."));
        } else {
            sender.sendMessage(Component.text("Placed '" + modelId + "' (entity " + entityId + ")."));
        }
        return true;
    }

    private boolean placeBlock(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can place a block (needs a location)."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /nilum placeblock <blockId> [x] [y] [z]"));
            return true;
        }

        String blockTypeId = args[1];
        Location base = player.getLocation();
        Location location;
        try {
            double x = parseCoordinate(args.length > 2 ? args[2] : "~", base.getX());
            double y = parseCoordinate(args.length > 3 ? args[3] : "~", base.getY());
            double z = parseCoordinate(args.length > 4 ? args[4] : "~", base.getZ());
            location = new Location(base.getWorld(), x, y, z);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid coordinate - use a number or ~ (optionally ~offset)."));
            return true;
        }

        var definition = plugin.customBlocks().place(location, blockTypeId);
        if (definition.isEmpty()) {
            sender.sendMessage(Component.text("No loaded block type named '" + blockTypeId
                    + "' (drop a matching blocks/" + blockTypeId + ".yml into the plugin's blocks folder)."));
            return true;
        }

        CustomBlockBroadcaster.broadcastPlacement(plugin, location, definition.get().modelId());
        sender.sendMessage(Component.text("Placed '" + blockTypeId + "' block at "
                + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "."));
        return true;
    }

    private boolean removeBlock(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can remove a block (needs a location)."));
            return true;
        }

        Location base = player.getLocation();
        Location location;
        try {
            double x = parseCoordinate(args.length > 1 ? args[1] : "~", base.getX());
            double y = parseCoordinate(args.length > 2 ? args[2] : "~", base.getY());
            double z = parseCoordinate(args.length > 3 ? args[3] : "~", base.getZ());
            location = new Location(base.getWorld(), x, y, z);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid coordinate - use a number or ~ (optionally ~offset)."));
            return true;
        }

        Optional<String> removed = plugin.customBlocks().remove(location);
        if (removed.isEmpty()) {
            sender.sendMessage(Component.text("No Nilum block at that position."));
            return true;
        }

        CustomBlockBroadcaster.broadcastRemoval(plugin, location);
        sender.sendMessage(Component.text("Removed '" + removed.get() + "' block."));
        return true;
    }

    private boolean giveItemDef(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can be given an item."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /nilum giveitemdef <id>"));
            return true;
        }

        String id = args[1];
        Optional<ItemStack> item = plugin.items().resolve(id);
        if (item.isEmpty()) {
            sender.sendMessage(Component.text("No item definition named '" + id
                    + "' (or its underlying model/icon isn't loaded)."));
            return true;
        }

        player.getInventory().addItem(item.get());
        sender.sendMessage(Component.text("Gave you '" + id + "'."));
        return true;
    }

    /** Standard Minecraft tilde notation: "~" or "~offset" is relative to base, anything else is absolute. */
    private static double parseCoordinate(String arg, double base) {
        if (arg.startsWith("~")) {
            String offset = arg.substring(1);
            return offset.isEmpty() ? base : base + Double.parseDouble(offset);
        }
        return Double.parseDouble(arg);
    }

    private boolean giveItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can be given an item."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /nilum giveitem <modelId> [material]"));
            return true;
        }

        String modelId = args[1];
        Material baseMaterial = Material.PAPER;
        if (args.length >= 3) {
            Material requested = Material.matchMaterial(args[2]);
            if (requested == null) {
                sender.sendMessage(Component.text("Unknown material '" + args[2] + "'."));
                return true;
            }
            baseMaterial = requested;
        }

        Optional<ItemStack> item = plugin.customItems().createItem(modelId, baseMaterial);
        if (item.isEmpty()) {
            sender.sendMessage(Component.text("No loaded model named '" + modelId
                    + "' (drop a matching .bbmodel file into the plugin's models folder)."));
            return true;
        }

        player.getInventory().addItem(item.get());
        sender.sendMessage(Component.text("Gave you a '" + modelId + "' item (" + baseMaterial + ")."));
        return true;
    }

    private boolean giveIcon(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can be given an item."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /nilum giveicon <iconId> [material]"));
            return true;
        }

        String iconId = args[1];
        Material baseMaterial = Material.PAPER;
        if (args.length >= 3) {
            Material requested = Material.matchMaterial(args[2]);
            if (requested == null) {
                sender.sendMessage(Component.text("Unknown material '" + args[2] + "'."));
                return true;
            }
            baseMaterial = requested;
        }

        Optional<ItemStack> item = plugin.iconItems().createItem(iconId, baseMaterial);
        if (item.isEmpty()) {
            sender.sendMessage(Component.text("No loaded icon named '" + iconId
                    + "' (drop a matching .png file into the plugin's icons folder)."));
            return true;
        }

        player.getInventory().addItem(item.get());
        sender.sendMessage(Component.text("Gave you a '" + iconId + "' icon item (" + baseMaterial + ")."));
        return true;
    }

    private boolean hudFrame(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can be sent a HUD frame update."));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /nilum hudframe <atlasId>:<elementId> <frame>"));
            return true;
        }

        String[] parts = args[1].split(":", 2);
        if (parts.length != 2) {
            sender.sendMessage(Component.text("Expected <atlasId>:<elementId>, e.g. hud_demo:health_bar"));
            return true;
        }
        String atlasId = parts[0];
        String elementId = parts[1];

        int frame;
        try {
            frame = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Frame must be an integer."));
            return true;
        }

        plugin.hud().setHudFrame(player, atlasId, elementId, frame);
        sender.sendMessage(Component.text("Sent frame " + frame + " for '" + atlasId + ":" + elementId + "'."));
        return true;
    }

    private boolean shaderPack(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can be switched to a shaderpack."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /nilum shaderpack <id>|off"));
            return true;
        }

        String id = args[1];
        if (id.equalsIgnoreCase("off")) {
            plugin.shaderPackService().deactivate(player);
            sender.sendMessage(Component.text("Told your client to restore its previous shaderpack state."));
            return true;
        }

        if (!plugin.shaderPacks().packIds().contains(id)) {
            sender.sendMessage(Component.text("No shader pack '" + id + "' loaded."));
            return true;
        }

        plugin.shaderPackService().activate(player, id);
        sender.sendMessage(Component.text("Told your client to switch to shader pack '" + id + "'."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(List.of("ver", "help", "reload", "placemodel", "giveitem", "giveicon",
                    "hudframe", "placeblock", "removeblock", "giveitemdef", "shaderpack"), args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "reload" -> filterByPrefix(
                        List.of("models", "icons", "hud", "items", "blocks", "shaderpacks", "fonts", "tcp", "config"), args[1]);
                case "placemodel", "giveitem" -> filterByPrefix(plugin.models().modelIds(), args[1]);
                case "placeblock" -> filterByPrefix(plugin.blockDefinitions().blockIds(), args[1]);
                case "giveicon" -> filterByPrefix(plugin.icons().iconIds(), args[1]);
                case "giveitemdef" -> filterByPrefix(plugin.items().itemIds(), args[1]);
                case "hudframe" -> filterByPrefix(plugin.hudAtlases().atlasIds().stream()
                        .map(id -> id + ":").toList(), args[1]);
                case "shaderpack" -> {
                    List<String> options = new ArrayList<>(plugin.shaderPacks().packIds());
                    options.add("off");
                    yield filterByPrefix(options, args[1]);
                }
                default -> List.of();
            };
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("giveitem") || args[0].equalsIgnoreCase("giveicon"))) {
            return filterByPrefix(Arrays.stream(Material.values()).map(Material::name).toList(), args[2]);
        }

        if (args.length <= 7 && args[0].equalsIgnoreCase("placemodel")) {
            List<String> options = new ArrayList<>(List.of("~"));
            if (sender instanceof Player player) {
                Location location = player.getLocation();
                Double rounded = switch (args.length - 1) {
                    case 2 -> (double) Math.round(location.getX());
                    case 3 -> (double) Math.round(location.getY());
                    case 4 -> (double) Math.round(location.getZ());
                    case 5 -> (double) Math.round(location.getYaw());
                    case 6 -> (double) Math.round(location.getPitch());
                    default -> null;
                };
                if (rounded != null) {
                    options.add(String.valueOf(rounded.longValue()));
                }
            }
            return filterByPrefix(options, args[args.length - 1]);
        }

        if (args.length <= 5 && args[0].equalsIgnoreCase("placeblock")) {
            return filterByPrefix(coordinateOptions(sender, args.length - 1, 2), args[args.length - 1]);
        }

        if (args.length <= 4 && args[0].equalsIgnoreCase("removeblock")) {
            return filterByPrefix(coordinateOptions(sender, args.length - 1, 1), args[args.length - 1]);
        }

        return List.of();
    }

    /** "~" plus the player's own rounded coordinate for whichever of x/y/z this arg index is, given where x starts. */
    private static List<String> coordinateOptions(CommandSender sender, int argIndex, int xIndex) {
        List<String> options = new ArrayList<>(List.of("~"));
        if (sender instanceof Player player) {
            Location location = player.getLocation();
            Double rounded = switch (argIndex - xIndex) {
                case 0 -> (double) Math.round(location.getX());
                case 1 -> (double) Math.round(location.getY());
                case 2 -> (double) Math.round(location.getZ());
                default -> null;
            };
            if (rounded != null) {
                options.add(String.valueOf(rounded.longValue()));
            }
        }
        return options;
    }

    private static List<String> filterByPrefix(Collection<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .toList();
    }
}
