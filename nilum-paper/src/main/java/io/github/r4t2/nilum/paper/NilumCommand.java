package io.github.r4t2.nilum.paper;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class NilumCommand implements CommandExecutor {

    private final NilumPlugin plugin;

    public NilumCommand(NilumPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /nilum <reload|placemodel>"));
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload" -> reload(sender);
            case "placemodel" -> placeModel(sender, args);
            default -> {
                sender.sendMessage(Component.text("Usage: /nilum <reload|placemodel>"));
                yield true;
            }
        };
    }

    private boolean reload(CommandSender sender) {
        boolean success = plugin.reloadNilumConfig();
        sender.sendMessage(Component.text(success
                ? "Nilum config reloaded."
                : "Failed to reload Nilum config, check the console/log for details."));
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
}
