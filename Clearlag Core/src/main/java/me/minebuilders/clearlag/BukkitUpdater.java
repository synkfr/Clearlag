package me.minebuilders.clearlag;

import java.io.File;

/**
 * @deprecated The CurseForge API used by this updater has been deprecated.
 * Auto-update functionality is disabled. Consider using a modern update checker
 * like UpdateChecker or integrating with Hangar/Modrinth APIs.
 */
@Deprecated
public class BukkitUpdater implements Runnable {

    public BukkitUpdater(File file) {
        Util.log("Auto-updater is disabled. Check for updates manually at https://www.spigotmc.org/resources/clearlag.68271/");
    }

    @Override
    public void run() {
        // Updater disabled - CurseForge API no longer functional
    }
}
