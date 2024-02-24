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

public class Scoreboard {
    private final List<String> conditions;
    private final Sidebar<Component> scoreboard;

    Scoreboard(final ScorePrefix plugin, final File file) throws IOException, InvalidConfigurationException, ScorePrefix.DisabledException {
        if (!file.exists()) {
            throw new IOException("File " + file + " does not exist");
        }
        final YamlConfiguration config = new YamlConfiguration();
        config.load(file);
        if (!config.getBoolean("enabled", false)) {
            throw new ScorePrefix.DisabledException();
        }
        final String title = config.getString("title");
        final List<String> content = config.getStringList("content");
        this.conditions = config.getStringList("conditions");
        final int speed = config.getInt("speed", -1);
        if (title == null) {
            throw new IllegalArgumentException("The Title cannot be null");
        }
        this.scoreboard = ProtocolSidebar.newAdventureSidebar(ScorePrefix.processComponent(null, title), plugin);
        for (final String line : content)
            this.scoreboard.addUpdatableLine(player -> ScorePrefix.processComponent(player, line));
        this.scoreboard.updateLinesPeriodically(0L, speed);
    }

    public void add(final Player player) {
        if (checkConditions(player) == 0) {
            return;
        }
        this.scoreboard.addViewer(player);
    }

    public int checkConditions(Player player) {
        return ScorePrefix.checkConditions(player, conditions);
    }

    public void remove(final Player player) {
        this.scoreboard.removeViewer(player);
    }

    public void destroy() {
        this.scoreboard.destroy();
    }
}
