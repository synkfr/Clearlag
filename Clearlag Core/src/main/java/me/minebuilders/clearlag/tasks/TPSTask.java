package me.minebuilders.clearlag.tasks;

import me.minebuilders.clearlag.Clearlag;
import me.minebuilders.clearlag.Util;
import me.minebuilders.clearlag.annotations.AutoWire;
import me.minebuilders.clearlag.annotations.ConfigPath;
import me.minebuilders.clearlag.config.ConfigHandler;
import me.minebuilders.clearlag.events.TPSUpdateEvent;
import me.minebuilders.clearlag.modules.TaskModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

@ConfigPath(path = "settings")
public class TPSTask extends TaskModule {

    private final double[] tpsHistory = new double[10];

    private int index = 0;

    // Volatile since it can be reassigned from inner class fallback logic
    private volatile TPSCalculator tpsCalculator;

    @AutoWire
    private ConfigHandler configHandler;

    // Used by inner classes - all run on main thread so no synchronization needed
    private int elapsedTicks = 0;

    @Override
    public void setEnabled() {
        super.setEnabled();

        Arrays.fill(tpsHistory, 20.0);
    }

    @Override
    protected int startTask() {

        elapsedTicks = 0;

        if (configHandler.getConfig().getBoolean("settings.use-internal-tps")) {
            // Try Paper/Spigot API first (cleanest, no reflection needed)
            try {
                tpsCalculator = new PaperTPSCalculator();
                Util.log("Using Paper/Spigot TPS API");
            } catch (Exception e1) {
                // Fall back to NMS reflection
                try {
                    tpsCalculator = new InternalTPSYoinker();
                    Util.log("Using NMS reflection for TPS");
                } catch (Exception e2) {
                    Util.warning("Clearlag failed to use internal TPS tracker. Reverted to estimation... (" + e2.getMessage() + ")");
                    tpsCalculator = new EstimatedTPSCalculator();
                }
            }
        } else {
            tpsCalculator = new EstimatedTPSCalculator();
        }


        return Bukkit.getScheduler().scheduleSyncRepeatingTask(Clearlag.getInstance(), this, 120L, getInterval());
    }

    public double getTPS() {
        double tpsSum = 0.0;

        for (double d : tpsHistory) {
            tpsSum += d;
        }

        return Math.round((tpsSum / 10.0) * 100.0) / 100.0;
    }

    public String getStringTPS() {
        return (getColor() + String.valueOf(getTPS()));
    }

    public ChatColor getColor() {
        double tps = getTPS();

        if (tps > 17) return ChatColor.GREEN;
        if (tps > 13) return ChatColor.GOLD;

        return ChatColor.RED;
    }

    @Override
    public void run() {

        tpsCalculator.tick();

        if (elapsedTicks % 20 == 0) {

            double tps = tpsCalculator.calculateCurrentAverageTPS();

            if (tps > 0 && tps <= 21.0) {

                tpsHistory[index++] = tps;

                if (index >= tpsHistory.length) {

                    index = 0;

                    Bukkit.getPluginManager().callEvent(new TPSUpdateEvent(getTPS()));
                }
            }
        }
    }

    @Override
    public int getInterval() {
        return 1;
    }

    private interface TPSCalculator {

        double calculateCurrentAverageTPS();

        void tick();

    }

    private class EstimatedTPSCalculator implements TPSCalculator {

        private long lasTimestamp = -1;

        private final int[] tickLengths = new int[20];

        private double tps = 20.0;

        public EstimatedTPSCalculator() {
            Arrays.fill(tickLengths, 50);
        }

        @Override
        public void tick() {

            final long currentTime = System.currentTimeMillis();

            if (lasTimestamp != -1) {

                int elaspedTime = (int) (currentTime - lasTimestamp);

                if (elaspedTime == 49 || elaspedTime == 51)
                    elaspedTime = 50;

                tickLengths[elapsedTicks % 20] = elaspedTime;
            }

            lasTimestamp = currentTime;

            if (++elapsedTicks % 20 == 0) {

                double tickSum = 0.0;

                for (int tickLength : tickLengths)
                    tickSum += tickLength;

                final double tickLength = (tickSum / 20.0);

                tps = (50.0 / tickLength) * 20.0;
            }
        }

        @Override
        public double calculateCurrentAverageTPS() {
            return tps;
        }
    }

    /**
     * TPS calculator that uses Paper/Spigot's Bukkit.getTPS() API (1.15+)
     */
    private class PaperTPSCalculator implements TPSCalculator {
        
        private final Method getTpsMethod;
        private double tps = 20.0;
        
        public PaperTPSCalculator() throws Exception {
            // Paper/Spigot 1.15+ exposes TPS via Bukkit.getTPS()
            getTpsMethod = Bukkit.class.getMethod("getTPS");
        }
        
        @Override
        public void tick() {
            if (++elapsedTicks % 60 == 0) {
                try {
                    double[] tpsArray = (double[]) getTpsMethod.invoke(null);
                    tps = Math.min(tpsArray[0], 20.0); // Cap at 20
                } catch (Exception e) {
                    Util.warning("Clearlag failed to get TPS via Paper API: " + e.getMessage());
                    tpsCalculator = new EstimatedTPSCalculator();
                }
            }
        }
        
        @Override
        public double calculateCurrentAverageTPS() {
            return tps;
        }
    }

    /**
     * TPS calculator that uses NMS reflection (legacy fallback)
     */
    private class InternalTPSYoinker implements TPSCalculator {

        private final Field recentTpsField;
        private final Object minecraftServerInstance;
        private double tps = 20.0;

        public InternalTPSYoinker() throws Exception {
            Class<?> minecraftServerClazz = findMinecraftServerClass();
            
            minecraftServerInstance = minecraftServerClazz.getDeclaredMethod("getServer").invoke(null);
            
            // Try different field names used across versions
            Field field = null;
            for (String fieldName : new String[]{"recentTps", "f", "recentTickTimes"}) {
                try {
                    field = minecraftServerClazz.getDeclaredField(fieldName);
                    if (field.getType() == double[].class) {
                        break;
                    }
                    field = null;
                } catch (NoSuchFieldException ignored) {}
            }
            
            if (field == null) {
                throw new NoSuchFieldException("Could not find TPS field in MinecraftServer");
            }
            
            recentTpsField = field;
            recentTpsField.setAccessible(true);
        }
        
        private Class<?> findMinecraftServerClass() throws ClassNotFoundException {
            // Try Paper's relocated class first (1.20.5+)
            try {
                return Class.forName("net.minecraft.server.MinecraftServer");
            } catch (ClassNotFoundException ignored) {}
            
            // Try legacy NMS path
            String rawVersion = Util.getRawBukkitVersion();
            if (rawVersion != null) {
                try {
                    return Class.forName("net.minecraft.server." + rawVersion + ".MinecraftServer");
                } catch (ClassNotFoundException ignored) {}
            }
            
            // Try CraftServer approach
            try {
                Object craftServer = Bukkit.getServer();
                Method getServerMethod = craftServer.getClass().getMethod("getServer");
                return getServerMethod.getReturnType();
            } catch (Exception ignored) {}
            
            throw new ClassNotFoundException("Could not find MinecraftServer class");
        }

        @Override
        public void tick() {
            if (++elapsedTicks % 60 == 0) {
                try {
                    tps = Math.min(((double[]) recentTpsField.get(minecraftServerInstance))[0], 20.0);
                } catch (Exception e) {
                    Util.warning("Clearlag failed to use the internal TPS tracker during runtime. Reverted to estimation... (" + e.getMessage() + ")");
                    tpsCalculator = new EstimatedTPSCalculator();
                }
            }
        }

        @Override
        public double calculateCurrentAverageTPS() {
            return tps;
        }
    }
}
