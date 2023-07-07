package de.crazydev22.scoreprefix.tablist;

import de.crazydev22.scoreprefix.ScorePrefix;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TabList {
    private final List<UUID> players = Collections.synchronizedList(new ArrayList<>());
    private final List<String> conditions;
    private final List<String> header;
    private final List<String> footer;
    private final int task;

    public TabList(final ScorePrefix plugin, final File file) throws IOException, ScorePrefix.DisabledException {
        if (!file.exists()) {
            throw new IOException("File " + file + " does not exist");
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.getBoolean("enabled", false)) {
            throw new ScorePrefix.DisabledException();
        }
        this.header = config.getStringList("header");
        this.footer = config.getStringList("footer");
        this.conditions = config.getStringList("conditions");
        int speed = config.getInt("speed", -1);

        task = speed < 0 ? -1 : Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 0L, speed);
    }

    public void add(final Player player) {
        if (checkConditions(player) == 0) {
            return;
        }
        players.add(player.getUniqueId());
        send(player);
    }

    public int checkConditions(Player player) {
        return ScorePrefix.checkConditions(player, conditions);
    }

    public void remove(final Player player) {
        players.remove(player.getUniqueId());
    }

    public void destroy() {
        this.footer.clear();
        this.header.clear();
        this.conditions.clear();
        this.players.clear();
        if (task != -1)
            Bukkit.getScheduler().cancelTask(task);

    }

    private void tick() {
        List<UUID> players = new ArrayList<>(this.players);
        players.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(this::send);
    }

    private void send(final Player player) {
        player.setPlayerListHeaderFooter(
                process(player, this.header),
                process(player, this.footer));
    }

    private static String process(final Player player, final List<String> lines) {
        return lines.stream()
                .map(line -> ScorePrefix.process(player, line))
                .collect(Collectors.joining("\n"));
    }
}
