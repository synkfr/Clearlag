package me.minebuilders.clearlag;

import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class Util {

    private static final Logger log = Logger.getLogger("Minecraft");

    public static void log(String m) {
        log.info("[ClearLag] " + m);
    }

    public static void warning(String m) {
        log.warning("[ClearLag] " + m);
    }

    public static void msg(String m, CommandSender s) {
        s.sendMessage(color("&6[&aClearLag&6] &a" + m));
    }

    public static void scm(String m, CommandSender s) {
        s.sendMessage(color(m));
    }

    public static ChatColor getChatColorByNumberLength(int variable, int yellowSize, int redSize) {
        return (variable >= redSize ? ChatColor.RED : variable >= yellowSize ? ChatColor.YELLOW : ChatColor.GREEN);
    }

    public static void shiftRight(Object[] list, int dropIndex) {

        if (list.length < 2) return;

        System.arraycopy(list, dropIndex, list, dropIndex + 1, list.length - 1 - dropIndex);

    }

    public static void postToMainThread(Runnable runnable) {
        Bukkit.getScheduler().runTask(Clearlag.getInstance(), runnable);
    }

    public static boolean isInteger(String s) {
        return isInteger(s, 10);
    }

    public static boolean isInteger(String s, int radix) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), radix) < 0) return false;
        }
        return true;
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Translates color codes in a string, supporting both legacy (&c, &a, etc.)
     * and hex colors (&#RRGGBB format).
     * 
     * @param s The string to colorize
     * @return The colorized string
     */
    public static String color(String s) {
        // First, translate hex colors (&#RRGGBB)
        Matcher matcher = HEX_PATTERN.matcher(s);
        StringBuilder buffer = new StringBuilder();
        
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            ChatColor color = ChatColor.of("#" + hexColor);
            matcher.appendReplacement(buffer, color.toString());
        }
        matcher.appendTail(buffer);
        
        // Then translate legacy color codes (&c, &a, etc.)
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Strips all color codes (both legacy and hex) from a string.
     * 
     * @param s The string to strip
     * @return The string without color codes
     */
    public static String stripColor(String s) {
        return ChatColor.stripColor(color(s));
    }

    public static String[] cloneAndReplaceStringArr(String[] stringArr, String key, String replaced) {

        final String[] clone = new String[stringArr.length];

        for (int i = 0; i < stringArr.length; ++i) {
            clone[i] = stringArr[i].replace(key, replaced);
        }

        return clone;
    }

    public static EntityType getEntityTypeFromString(String s) {
        @SuppressWarnings("deprecation")
        EntityType et = EntityType.fromName(s);

        if (et != null) {
            return et;
        }

        s = s.replace("_", "").replace(" ", "");
        for (EntityType e : EntityType.values()) {
            if (e != null) {
                String name = e.name().replace("_", "");
                if (name.equalsIgnoreCase(s)) {
                    return e;
                }
            }
        }
        return null;
    }

    /**
     * Gets the raw NMS version string (e.g., "v1_20_R3").
     * For 1.17+ servers using Mojang mappings, returns null.
     * 
     * @return The NMS version string or null if not available
     */
    @Nullable
    public static String getRawBukkitVersion() {
        try {
            String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
            if (parts.length > 3 && parts[3].startsWith("v")) {
                return parts[3];
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Gets the Minecraft version (e.g., "1.20.4").
     * Uses Bukkit.getBukkitVersion() for modern servers.
     * 
     * @return The Minecraft version string
     */
    @NotNull
    public static String getBukkitVersion() {
        // Modern approach: parse from Bukkit.getBukkitVersion() (e.g., "1.20.4-R0.1-SNAPSHOT")
        String bukkitVersion = Bukkit.getBukkitVersion();
        if (bukkitVersion.contains("-")) {
            return bukkitVersion.split("-")[0];
        }
        return bukkitVersion;
    }

    /**
     * Gets the major.minor version as an integer (e.g., 1.20 -> 120, 1.8 -> 18).
     * Useful for version comparisons.
     * 
     * @return The version as an integer
     */
    public static int getVersionInt() {
        String version = getBukkitVersion();
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /**
     * Checks if the server is running Minecraft 1.17 or newer (Mojang mappings).
     * 
     * @return true if 1.17+
     */
    public static boolean isModernServer() {
        return getVersionInt() >= 117;
    }

    public static Date parseTime(String time) {

        try {

            String[] frag = time.split("-");

            if (frag.length < 2)
                return new Date();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            return dateFormat.parse(frag[0] + "-" + frag[1] + "-" + frag[2]);

        } catch (Exception e) {
            return new Date();
        }
    }

    public static String getTime(long time) {

        long seconds = Math.abs(time) / 1000L;

        final StringBuilder message = new StringBuilder();

        if (seconds >= 86400) {
            int days = (int) (seconds / 86400);
            seconds %= 86400;

            message.append(days).append(days > 1 ? " days" : " day");
        }

        if (seconds >= 3600) {
            int hours = (int) (seconds / 3600);
            seconds %= 3600;

            if (message.length() > 0) message.append(", ");

            message.append(hours).append(hours > 1 ? " hours" : " hour");
        }

        if (seconds >= 60) {
            int min = (int) (seconds / 60);
            seconds %= 60;

            if (message.length() > 0) message.append(", ");

            message.append(min).append(min > 1 ? " minutes" : " minute");
        }

        if (seconds >= 0) {
            if (message.length() > 0) message.append(", ");

            message.append(seconds).append(seconds > 1 ? " seconds" : " second");
        }

        return message.toString();
    }

}
