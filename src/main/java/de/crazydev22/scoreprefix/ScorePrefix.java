package de.crazydev22.scoreprefix;

import de.crazydev22.scoreprefix.scoreboard.ScoreboardManager;
import de.crazydev22.scoreprefix.tablist.TabListManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.ParsingException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public final class ScorePrefix extends JavaPlugin {
    public final File file = new File(getDataFolder(), "config.yml");
    private PrefixManager prefixManager;
    private ScoreboardManager scoreboardManager;
    private TabListManager tabListManager;
    private JoinQuitManager joinQuitManager;

    @Override
    public void onEnable() {
        if (!file.exists()) {
            if (!file.getParentFile().mkdirs())
                getLogger().info("Failed to create folder for config.yml");
            saveResource("config.yml", false);
        }
        var config = getConfig();
        if (config.getBoolean("prefix.enabled", false)) prefixManager = new PrefixManager(this, config.getBoolean("prefix.useExistingScoreboard", false));
        if (config.getBoolean("scoreboard", false)) scoreboardManager = new ScoreboardManager(this);
        if (config.getBoolean("tablist", false)) tabListManager = new TabListManager(this);
        if (config.getBoolean("join-quit.enabled", false)) joinQuitManager = new JoinQuitManager(this);
        var cmdInst = new ScorePrefixCMD(scoreboardManager, tabListManager, prefixManager, joinQuitManager);
        var cmd = Bukkit.getPluginCommand("sb");
        if (cmd != null) {
            cmd.setExecutor(cmdInst);
            cmd.setTabCompleter(cmdInst);
        }
    }

    public static String process(final Player player, String text, boolean... args) {
        if (text == null)
            return null;
        text = ChatColor.translateAlternateColorCodes('&', text);
        return args.length == 0 ?
                process(player, PlaceholderAPI.setPlaceholders(player, text), true) :
                PlaceholderAPI.setPlaceholders(player, text);
    }

    public static Component processComponent(final Player player, final String text) {
        try {
            return miniMessage().deserialize(process(player, text));
        } catch (ParsingException e) {
            return miniMessage().deserialize(miniMessage().serialize(legacySection().deserialize(process(player, text))));
        }
    }

    public static int checkConditions(final Player player, List<String> conditions) {
        try {
            int result = 2;
            for (final String condition : conditions) {
                result = check(player, condition);
                if (result == 0) {
                    break;
                }
            }
            return result;
        }
        catch (final Exception e) {
            return 0;
        }
    }

    private static int check(final Player player, final String condition){
        boolean reverse = false;
        final String[] split = condition.split(":", 2);
        if (split.length < 2) {
            return 0;
        }
        String type = split[0].toLowerCase();
        if (type.startsWith("!")) {
            reverse = true;
            type = type.substring(1);
        }
        var result = switch (type) {
            case "world" -> Objects.equals(player.getWorld().getName(), split[1]);
            case "gamemode" -> player.getGameMode().name().equalsIgnoreCase(split[1]);
            case "permission" -> player.hasPermission(split[1]);
            default -> true;
        };
        if (reverse) {
            result = !result;
        }
        return result ? 1 : 0;
    }

    public static class DisabledException extends Exception { }
}
