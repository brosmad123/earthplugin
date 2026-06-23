package com.example.customborder;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds one polygon border per world and persists it to borders.yml.
 * A border is just a list of (x, z) points - Y is ignored, same as vanilla world border.
 */
public class BorderManager {

    private final CustomBorderPlugin plugin;
    private final File file;
    private FileConfiguration config;

    // worldName -> ordered list of points [x, z]
    private final Map<String, List<double[]>> borders = new HashMap<>();
    // worldName -> enabled state
    private final Map<String, Boolean> enabled = new HashMap<>();
    // worldName -> show particles state
    private final Map<String, Boolean> particlesOn = new HashMap<>();

    public BorderManager(CustomBorderPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "borders.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create borders.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        borders.clear();
        enabled.clear();
        particlesOn.clear();

        if (config.isConfigurationSection("worlds")) {
            for (String worldName : config.getConfigurationSection("worlds").getKeys(false)) {
                String base = "worlds." + worldName;
                List<Double> xs = config.getDoubleList(base + ".x");
                List<Double> zs = config.getDoubleList(base + ".z");
                List<double[]> points = new ArrayList<>();
                for (int i = 0; i < Math.min(xs.size(), zs.size()); i++) {
                    points.add(new double[]{xs.get(i), zs.get(i)});
                }
                borders.put(worldName, points);
                enabled.put(worldName, config.getBoolean(base + ".enabled", false));
                particlesOn.put(worldName, config.getBoolean(base + ".particles", true));
            }
        }
    }

    public void save() {
        config.set("worlds", null); // clear and rewrite
        for (String worldName : borders.keySet()) {
            String base = "worlds." + worldName;
            List<double[]> points = borders.get(worldName);
            List<Double> xs = new ArrayList<>();
            List<Double> zs = new ArrayList<>();
            for (double[] p : points) {
                xs.add(p[0]);
                zs.add(p[1]);
            }
            config.set(base + ".x", xs);
            config.set(base + ".z", zs);
            config.set(base + ".enabled", enabled.getOrDefault(worldName, false));
            config.set(base + ".particles", particlesOn.getOrDefault(worldName, true));
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save borders.yml: " + e.getMessage());
        }
    }

    public List<double[]> getPoints(String world) {
        return borders.computeIfAbsent(world, k -> new ArrayList<>());
    }

    public void addPoint(String world, double x, double z) {
        getPoints(world).add(new double[]{x, z});
    }

    public boolean removePoint(String world, int index) {
        List<double[]> points = getPoints(world);
        if (index < 0 || index >= points.size()) return false;
        points.remove(index);
        return true;
    }

    public void clearPoints(String world) {
        getPoints(world).clear();
    }

    public boolean isEnabled(String world) {
        return enabled.getOrDefault(world, false);
    }

    public void setEnabled(String world, boolean value) {
        enabled.put(world, value);
    }

    public boolean isParticlesOn(String world) {
        return particlesOn.getOrDefault(world, true);
    }

    public void setParticlesOn(String world, boolean value) {
        particlesOn.put(world, value);
    }

    /**
     * Ray-casting point-in-polygon test. Works for convex AND concave shapes,
     * not just rectangles - so any 4+ point shape works correctly.
     */
    public boolean isInside(String world, double x, double z) {
        List<double[]> points = getPoints(world);
        if (points.size() < 3) return true; // no real polygon defined yet, don't restrict

        boolean inside = false;
        int n = points.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = points.get(i)[0], zi = points.get(i)[1];
            double xj = points.get(j)[0], zj = points.get(j)[1];
            boolean intersects = ((zi > z) != (zj > z))
                    && (x < (xj - xi) * (z - zi) / (zj - zi) + xi);
            if (intersects) inside = !inside;
        }
        return inside;
    }

    public boolean isInside(Location loc) {
        return isInside(loc.getWorld().getName(), loc.getX(), loc.getZ());
    }

    public boolean hasValidPolygon(String world) {
        return getPoints(world).size() >= 3;
    }
}
