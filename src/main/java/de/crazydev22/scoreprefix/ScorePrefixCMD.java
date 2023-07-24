package de.crazydev22.scoreprefix;

import de.crazydev22.scoreprefix.scoreboard.ScoreboardManager;
import de.crazydev22.scoreprefix.tablist.TabListManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class ScorePrefixCMD implements CommandExecutor, TabCompleter {
    private final ScoreboardManager scoreboardManager;
    private final TabListManager tabListManager;
    private final PrefixManager prefixManager;
    private final JoinQuitManager joinQuitManager;


    ScorePrefixCMD(ScoreboardManager scoreboardManager, TabListManager tabListManager, PrefixManager prefixManager, JoinQuitManager joinQuitManager) {
        this.scoreboardManager = scoreboardManager;
        this.tabListManager = tabListManager;
        this.prefixManager = prefixManager;
        this.joinQuitManager = joinQuitManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("scoreprefix.command"))
            return false;
        if (args.length == 2) {
            if (args[0].equals("reload")) {
                String args1 = args[1].toLowerCase();
                try {
                    switch (args1) {
                        case "scoreboard" -> { if (scoreboardManager != null) scoreboardManager.reload(); }
                        case "tablist" -> { if (tabListManager != null) tabListManager.reload(); }
                        case "prefix" -> { if (prefixManager != null) prefixManager.reload(); }
                        case "join-quit" -> { if (joinQuitManager != null) joinQuitManager.reload(); }
                        case "all" -> {
                            if (scoreboardManager != null) scoreboardManager.reload();
                            if (tabListManager != null) tabListManager.reload();
                            if (prefixManager != null) prefixManager.reload();
                            if (joinQuitManager != null) joinQuitManager.reload();
                        }
                    }
                    return true;
                } catch (Exception e) {
                    final StringWriter writer = new StringWriter();
                    e.printStackTrace(new PrintWriter(writer));
                    sender.sendMessage(writer.toString());
                    return false;
                }
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("scoreprefix.command"))
            return null;
        if (args.length == 1)
            return List.of("reload");
        else if (args.length == 2) {
            if (args[0].equals("reload")) {
                List<String> list = new ArrayList<>();

                if (scoreboardManager != null)
                    list.add("scoreboard");
                if (tabListManager != null)
                    list.add("tablist");
                if (prefixManager != null)
                    list.add("prefix");
                if (joinQuitManager != null)
                    list.add("join-quit");
                if (list.size() > 1)
                    list.add("all");
            }
        }
        return null;
    }
}
