package de.crazydev22.scoreprefix.scoreboard;

import de.crazydev22.scoreprefix.ScorePrefix;
import me.catcoder.sidebar.ProtocolSidebar;
import me.catcoder.sidebar.Sidebar;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class Scoreboard {
    private final List<String> conditions;
    private final Sidebar<Component> scoreboard;

    public Scoreboard(final ScorePrefix plugin, final File file) throws IOException, InvalidConfigurationException, DisabledException {
        if (!file.exists()) {
            throw new IOException("File " + file + " does not exist");
        }
        final YamlConfiguration config = new YamlConfiguration();
        config.load(file);
        if (!config.getBoolean("enabled", false)) {
            throw new DisabledException();
        }
        final String title = config.getString("title");
        final List<String> content = config.getStringList("content");
        this.conditions = config.getStringList("conditions");
        final int speed = config.getInt("speed", -1);
        if (title == null) {
            throw new IllegalArgumentException("The Title cannot be null");
        }
        this.scoreboard = ProtocolSidebar.newAdventureSidebar(ScorePrefix.processComponent(null, title), plugin);
        for (final String line : content) {
            this.scoreboard.addUpdatableLine(player -> ScorePrefix.processComponent(player, line));
        }
        this.scoreboard.updateLinesPeriodically(0L, speed);
    }

    public void add(final Player player) {
        if (this.checkConditions(player) == 0) {
            return;
        }
        this.scoreboard.addViewer(player);
    }

    public void remove(final Player player) {
        this.scoreboard.removeViewer(player);
    }

    public void destroy() {
        this.scoreboard.destroy();
    }

    public int checkConditions(final Player player) {
        try {
            int result = 2;
            for (final String condition : this.conditions) {
                result = this.check(player, condition);
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

    private int check(final Player player, final String condition){
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


    public static class DisabledException extends RuntimeException {}
}
