package me.minebuilders.clearlag.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.minebuilders.clearlag.Clearlag;
import me.minebuilders.clearlag.tasks.TPSTask;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for Clearlag.
 * Provides placeholders for TPS, entity counts, and more.
 */
public class ClearlagPlaceholders extends PlaceholderExpansion {

    private final Clearlag plugin;

    public ClearlagPlaceholders(Clearlag plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "clearlag";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    @Nullable
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // TPS placeholders
        if (params.equalsIgnoreCase("tps")) {
            TPSTask tpsTask = Clearlag.getInstance().getModule(TPSTask.class);
            return tpsTask != null ? String.valueOf(tpsTask.getTPS()) : "N/A";
        }

        if (params.equalsIgnoreCase("tps_colored")) {
            TPSTask tpsTask = Clearlag.getInstance().getModule(TPSTask.class);
            return tpsTask != null ? tpsTask.getStringTPS() : "&cN/A";
        }

        // Entity count placeholders
        if (params.equalsIgnoreCase("entities_total")) {
            int count = 0;
            for (World world : Bukkit.getWorlds()) {
                count += world.getEntities().size();
            }
            return String.valueOf(count);
        }

        if (params.equalsIgnoreCase("entities_living")) {
            int count = 0;
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                        count++;
                    }
                }
            }
            return String.valueOf(count);
        }

        if (params.equalsIgnoreCase("entities_items")) {
            int count = 0;
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Item) {
                        count++;
                    }
                }
            }
            return String.valueOf(count);
        }

        // Per-world entity counts: clearlag_entities_<world>
        if (params.startsWith("entities_world_")) {
            String worldName = params.substring("entities_world_".length());
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                return String.valueOf(world.getEntities().size());
            }
            return "0";
        }

        // Memory placeholders
        if (params.equalsIgnoreCase("memory_used")) {
            Runtime runtime = Runtime.getRuntime();
            long used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            return String.valueOf(used);
        }

        if (params.equalsIgnoreCase("memory_max")) {
            return String.valueOf(Runtime.getRuntime().maxMemory() / 1024 / 1024);
        }

        if (params.equalsIgnoreCase("memory_percent")) {
            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory();
            long max = runtime.maxMemory();
            return String.valueOf((int) ((used * 100) / max));
        }

        return null;
    }
}
