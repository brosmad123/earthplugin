package com.example.customborder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Stops players from leaving the custom polygon border.
 * If a move would put them outside the shape, we cancel the X/Z component
 * of that movement (kept simple: snap them back to their last valid spot).
 */
public class BorderListener implements Listener {

    private final CustomBorderPlugin plugin;
    private final BorderManager borders;

    // Cooldown so we don't spam the action bar message every tick
    private final Map<String, Long> lastWarned = new HashMap<>();
    private static final long WARN_COOLDOWN_MS = 1500;

    public BorderListener(CustomBorderPlugin plugin) {
        this.plugin = plugin;
        this.borders = plugin.getBorderManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;

        String world = to.getWorld().getName();
        if (!borders.isEnabled(world) || !borders.hasValidPolygon(world)) return;

        // Only bother checking when they actually changed block-ish position
        Location from = event.getFrom();
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;

        if (!borders.isInside(world, to.getX(), to.getZ())) {
            // Cancel and snap back to where they came from (keep their look direction from "to")
            Location safe = from.clone();
            safe.setPitch(to.getPitch());
            safe.setYaw(to.getYaw());
            event.setTo(safe);
            warn(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;

        String world = to.getWorld().getName();
        if (!borders.isEnabled(world) || !borders.hasValidPolygon(world)) return;

        // Allow plugin/admin teleports to bypass if you want; here we just block
        // teleports that would land outside the shape (e.g. /tp, ender pearls).
        if (!borders.isInside(world, to.getX(), to.getZ())) {
            event.setCancelled(true);
            warn(event.getPlayer());
        }
    }

    private void warn(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastWarned.get(player.getUniqueId().toString());
        if (last != null && now - last < WARN_COOLDOWN_MS) return;
        lastWarned.put(player.getUniqueId().toString(), now);

        player.sendActionBar(Component.text("You have reached the border of this area!")
                .color(NamedTextColor.RED));
    }
}
