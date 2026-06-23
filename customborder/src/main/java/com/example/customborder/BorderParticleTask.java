package com.example.customborder;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Periodically draws a vertical line of particles along every edge of every
 * enabled polygon, from well below to well above the nearest player, so the
 * shape of the border is visible in the world (similar to vanilla's border wall).
 */
public class BorderParticleTask extends BukkitRunnable {

    private final CustomBorderPlugin plugin;
    private final BorderManager borders;

    // Spacing between particle points along each edge, in blocks
    private static final double STEP = 1.0;
    // How far above/below the player's Y level to draw the wall
    private static final int VERTICAL_RANGE = 6;
    // Only draw the border near players within this distance, for performance
    private static final double RENDER_DISTANCE = 48.0;

    public BorderParticleTask(CustomBorderPlugin plugin) {
        this.plugin = plugin;
        this.borders = plugin.getBorderManager();
    }

    @Override
    public void run() {
        for (World world : plugin.getServer().getWorlds()) {
            String worldName = world.getName();
            if (!borders.isEnabled(worldName) || !borders.isParticlesOn(worldName)) continue;
            List<double[]> points = borders.getPoints(worldName);
            if (points.size() < 3) continue;

            List<Player> players = world.getPlayers();
            if (players.isEmpty()) continue;

            int n = points.size();
            for (int i = 0; i < n; i++) {
                double[] p1 = points.get(i);
                double[] p2 = points.get((i + 1) % n);
                drawEdge(world, players, p1[0], p1[1], p2[0], p2[1]);
            }
        }
    }

    private void drawEdge(World world, List<Player> players, double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001) return;

        int steps = (int) Math.ceil(length / STEP);
        double stepX = dx / steps;
        double stepZ = dz / steps;

        for (int i = 0; i <= steps; i++) {
            double x = x1 + stepX * i;
            double z = z1 + stepZ * i;

            // Only render this point if some player is reasonably close to it,
            // to avoid spawning particles across the whole map every tick
            Player nearby = findNearbyPlayer(players, world, x, z);
            if (nearby == null) continue;

            int baseY = nearby.getLocation().getBlockY();
            for (int y = baseY - VERTICAL_RANGE; y <= baseY + VERTICAL_RANGE; y++) {
                world.spawnParticle(Particle.FLAME, x + 0.5, y, z + 0.5, 1, 0, 0, 0, 0);
            }
        }
    }

    private Player findNearbyPlayer(List<Player> players, World world, double x, double z) {
        for (Player p : players) {
            Location loc = p.getLocation();
            double dx = loc.getX() - x;
            double dz = loc.getZ() - z;
            if (dx * dx + dz * dz <= RENDER_DISTANCE * RENDER_DISTANCE) {
                return p;
            }
        }
        return null;
    }
}
