package me.minebuilders.clearlag;

import me.minebuilders.clearlag.exceptions.WrongCommandArgumentException;
import me.minebuilders.clearlag.language.LanguageValue;
import me.minebuilders.clearlag.language.messages.Message;
import me.minebuilders.clearlag.language.messages.MessageTree;
import me.minebuilders.clearlag.modules.CommandModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandListener implements CommandExecutor, TabCompleter {

    @LanguageValue(key = "command.lagg.")
    private MessageTree lang;

    private final List<CommandModule> cmds = new ArrayList<CommandModule>();

    public CommandListener() {
        Clearlag.getInstance().getCommand("lagg").setExecutor(this);
        Clearlag.getInstance().getCommand("lagg").setTabCompleter(this);
    }


    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        final CommandModule enteredSubCommand = (args.length > 0 ? getCmd(args[0]) : null);

        if (enteredSubCommand == null) {

            final List<CommandModule> cmds = getUserCmds(sender);

            if (cmds.size() == 0) {

                lang.sendMessage("nopermission", sender);

                return false;
            }

            final Message helpLineMessage = lang.getMessage("helpline");

            lang.sendMessage("header", sender);

            for (CommandModule cmd : cmds)
                helpLineMessage.sendMessage(sender, cmd.getDisplayName(), cmd.getDescription());

            lang.sendMessage("footer", sender);

        } else {

            try {
                enteredSubCommand.processCmd(sender, args);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(e.getMessage());
            } catch (WrongCommandArgumentException e) {
                e.getError().sendMessage(sender, e.getReplacables());
            }
        }

        return true;
    }


    public void addCmd(CommandModule cmd) {
        cmds.add(cmd);
    }

    private CommandModule getCmd(String s) {
        s = s.toLowerCase();

        for (CommandModule cmd : cmds) {

            if (cmd != null && cmd.getDisplayName().equals(s)) {
                return cmd;
            }
        }

        return null;
    }

    private List<CommandModule> getUserCmds(CommandSender p) {

        List<CommandModule> mod = new ArrayList<CommandModule>();

        for (CommandModule cmd : cmds) {

            if (p.hasPermission("lagg." + cmd.getName())) {
                mod.add(cmd);
            }
        }

        return mod;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                       @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Complete subcommand names
            List<String> subCommands = getUserCmds(sender).stream()
                    .map(CommandModule::getDisplayName)
                    .collect(Collectors.toList());
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length >= 2) {
            // Subcommand-specific completions
            String subCmd = args[0].toLowerCase();
            
            switch (subCmd) {
                case "killmobs":
                case "area":
                    // Suggest entity types
                    if (args.length == 2) {
                        List<String> entityTypes = new ArrayList<>();
                        for (EntityType type : EntityType.values()) {
                            if (type.isAlive() && type.isSpawnable()) {
                                entityTypes.add(type.name().toLowerCase());
                            }
                        }
                        StringUtil.copyPartialMatches(args[1], entityTypes, completions);
                    }
                    break;
                case "tpchunk":
                case "chunk":
                case "checkchunk":
                    // Suggest world names for chunk commands
                    if (args.length == 2) {
                        List<String> worlds = org.bukkit.Bukkit.getWorlds().stream()
                                .map(org.bukkit.World::getName)
                                .collect(Collectors.toList());
                        StringUtil.copyPartialMatches(args[1], worlds, completions);
                    }
                    break;
                case "clear":
                    // Suggest clear options
                    if (args.length == 2) {
                        StringUtil.copyPartialMatches(args[1], 
                                List.of("items", "mobs", "all"), completions);
                    }
                    break;
            }
        }
        
        Collections.sort(completions);
        return completions;
    }
}