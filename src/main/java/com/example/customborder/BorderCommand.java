package com.example.customborder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BorderCommand implements CommandExecutor {

    private final CustomBorderPlugin plugin;
    private final BorderManager borders;

    public BorderCommand(CustomBorderPlugin plugin) {
        this.plugin = plugin;
        this.borders = plugin.getBorderManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "clear" -> handleClear(sender);
            case "enable" -> handleEnable(sender, true);
            case "disable" -> handleEnable(sender, false);
            case "particles" -> handleParticles(sender, args);
            case "info" -> handleInfo(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) && args.length < 4) {
            sender.sendMessage(Component.text("Console must specify: /cborder add <world> <x> <z>").color(NamedTextColor.RED));
            return;
        }

        String world;
        double x, z;

        try {
            if (args.length >= 4) {
                // /cborder add <world> <x> <z>
                world = args[1];
                x = Double.parseDouble(args[2]);
                z = Double.parseDouble(args[3]);
            } else if (sender instanceof Player player) {
                // /cborder add  -> uses player's current location
                world = player.getWorld().getName();
                x = player.getLocation().getX();
                z = player.getLocation().getZ();
            } else {
                sendHelp(sender);
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("X and Z must be numbers.").color(NamedTextColor.RED));
            return;
        }

        borders.addPoint(world, x, z);
        borders.save();
        int count = borders.getPoints(world).size();
        sender.sendMessage(Component.text(String.format(
                "Added point (%.1f, %.1f) to '%s'. Total points: %d", x, z, world, count))
                .color(NamedTextColor.GREEN));

        if (count == 3) {
            sender.sendMessage(Component.text("You now have 3 points - the border is active as a triangle. Add a 4th for a quadrilateral.")
                    .color(NamedTextColor.YELLOW));
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cborder remove <index> [world]").color(NamedTextColor.RED));
            return;
        }
        String world = args.length >= 3 ? args[2] : worldOf(sender);
        if (world == null) {
            sender.sendMessage(Component.text("Specify a world when running from console.").color(NamedTextColor.RED));
            return;
        }
        try {
            int index = Integer.parseInt(args[1]) - 1; // 1-indexed for the user
            if (borders.removePoint(world, index)) {
                borders.save();
                sender.sendMessage(Component.text("Removed point #" + (index + 1) + " from '" + world + "'.").color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("No point at that index.").color(NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Index must be a number.").color(NamedTextColor.RED));
        }
    }

    private void handleList(CommandSender sender) {
        String world = worldOf(sender);
        if (world == null) {
            sender.sendMessage(Component.text("Specify a world: run this in-game, or see borders.yml.").color(NamedTextColor.RED));
            return;
        }
        List<double[]> points = borders.getPoints(world);
        if (points.isEmpty()) {
            sender.sendMessage(Component.text("No points set for '" + world + "'.").color(NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("Points for '" + world + "':").color(NamedTextColor.AQUA));
        for (int i = 0; i < points.size(); i++) {
            double[] p = points.get(i);
            sender.sendMessage(Component.text(String.format("  %d. (%.1f, %.1f)", i + 1, p[0], p[1])).color(NamedTextColor.WHITE));
        }
    }

    private void handleClear(CommandSender sender) {
        String world = worldOf(sender);
        if (world == null) return;
        borders.clearPoints(world);
        borders.save();
        sender.sendMessage(Component.text("Cleared all points for '" + world + "'.").color(NamedTextColor.GREEN));
    }

    private void handleEnable(CommandSender sender, boolean value) {
        String world = worldOf(sender);
        if (world == null) return;
        if (value && !borders.hasValidPolygon(world)) {
            sender.sendMessage(Component.text("You need at least 3 points before enabling the border.").color(NamedTextColor.RED));
            return;
        }
        borders.setEnabled(world, value);
        borders.save();
        sender.sendMessage(Component.text("Border " + (value ? "enabled" : "disabled") + " for '" + world + "'.")
                .color(NamedTextColor.GREEN));
    }

    private void handleParticles(CommandSender sender, String[] args) {
        String world = worldOf(sender);
        if (world == null) return;
        boolean current = borders.isParticlesOn(world);
        boolean newValue = !current;
        if (args.length >= 2) {
            newValue = args[1].equalsIgnoreCase("on");
        }
        borders.setParticlesOn(world, newValue);
        borders.save();
        sender.sendMessage(Component.text("Border particles " + (newValue ? "ON" : "OFF") + " for '" + world + "'.")
                .color(NamedTextColor.GREEN));
    }

    private void handleInfo(CommandSender sender) {
        String world = worldOf(sender);
        if (world == null) return;
        sender.sendMessage(Component.text("=== Border info for '" + world + "' ===").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Points: " + borders.getPoints(world).size()).color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Enabled: " + borders.isEnabled(world)).color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Particles: " + borders.isParticlesOn(world)).color(NamedTextColor.WHITE));
    }

    private String worldOf(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getWorld().getName();
        }
        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== CustomBorder ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/cborder add [world] [x] [z]  - add a point (uses your location if no coords given)").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/cborder remove <index> [world]  - remove point # (see /cborder list)").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/cborder list  - list points in your current world").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/cborder clear  - remove all points in your current world").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/cborder enable | disable  - turn the border on/off").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/cborder particles [on|off]  - toggle the particle wall").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/cborder info  - show current settings").color(NamedTextColor.WHITE));
    }
}
