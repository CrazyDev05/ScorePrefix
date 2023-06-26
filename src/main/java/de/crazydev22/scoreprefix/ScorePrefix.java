package de.crazydev22.scoreprefix;

import de.crazydev22.scoreprefix.scoreboard.ScoreboardManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ScorePrefix extends JavaPlugin {

    @Override
    public void onEnable() {
        new PrefixManager(this);
        new ScoreboardManager(this);
    }

    public static String process(final Player player, String text, boolean... args) {
        if (text == null) {
            return null;
        }
        text = ChatColor.translateAlternateColorCodes('&', text);
        return args.length == 0 ?
                process(player, PlaceholderAPI.setPlaceholders(player, text), true) :
                PlaceholderAPI.setPlaceholders(player, text);
    }

    public static Component processComponent(final Player player, final String text) {
        return Component.text(process(player, text));
    }
}
