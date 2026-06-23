package com.example.customborder;

import org.bukkit.plugin.java.JavaPlugin;

public class CustomBorderPlugin extends JavaPlugin {

    private BorderManager borderManager;

    @Override
    public void onEnable() {
        this.borderManager = new BorderManager(this);

        getServer().getPluginManager().registerEvents(new BorderListener(this), this);
        getCommand("cborder").setExecutor(new BorderCommand(this));

        // Render particles every 10 ticks (twice a second) - adjust to taste
        new BorderParticleTask(this).runTaskTimer(this, 20L, 10L);

        getLogger().info("CustomBorder enabled. Use /cborder for commands.");
    }

    @Override
    public void onDisable() {
        if (borderManager != null) {
            borderManager.save();
        }
        getLogger().info("CustomBorder disabled.");
    }

    public BorderManager getBorderManager() {
        return borderManager;
    }
}
