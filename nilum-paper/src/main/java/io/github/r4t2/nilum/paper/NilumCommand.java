package io.github.r4t2.nilum.paper;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        sender.sendMessage(Component.text("/nilum reload <models|tcp|config> - Reload part of Nilum."));
        sender.sendMessage(Component.text("/nilum placemodel <modelId> - Place a model at your location."));
        sender.sendMessage(Component.text("/nilum giveitem <modelId> [material] - Give yourself a custom item."));
    }

    private boolean reload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /nilum reload <models|tcp|config>"));
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "models" -> reloadAndReport(sender, plugin.reloadModels(), "Models", "reload the models folder");
            case "tcp" -> reloadAndReport(sender, plugin.reloadTcp(), "TCP side-channel", "reload the TCP side-channel");
            case "config" -> reloadAndReport(sender, plugin.reloadSettings(), "Config", "reload the config");
            default -> {
                sender.sendMessage(Component.text(
                        "Unknown reload target '" + args[1] + "'. Usage: /nilum reload <models|tcp|config>"));
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
            sender.sendMessage(Component.text("Usage: /nilum placemodel <modelId>"));
            return true;
        }

        String modelId = args[1];
        Location location = player.getLocation();
        UUID entityId = plugin.modelDisplays().place(location, modelId).orElse(null);

        if (entityId == null) {
            sender.sendMessage(Component.text("No loaded model named '" + modelId
                    + "' (drop a matching .bbmodel file into the plugin's models folder)."));
        } else {
            sender.sendMessage(Component.text("Placed '" + modelId + "' (entity " + entityId + ")."));
        }
        return true;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(List.of("ver", "help", "reload", "placemodel", "giveitem"), args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "reload" -> filterByPrefix(List.of("models", "tcp", "config"), args[1]);
                case "placemodel", "giveitem" -> filterByPrefix(plugin.models().modelIds(), args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("giveitem")) {
            return filterByPrefix(Arrays.stream(Material.values()).map(Material::name).toList(), args[2]);
        }

        return List.of();
    }

    private static List<String> filterByPrefix(Collection<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .toList();
    }
}
