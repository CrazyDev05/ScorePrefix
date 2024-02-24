package de.crazydev22.scoreprefix.scoreboard;

import de.crazydev22.scoreprefix.ScorePrefix;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ScoreboardManager implements Listener {
    private final ScorePrefix plugin;
    private final List<Scoreboard> scoreboards = Collections.synchronizedList(new ArrayList<>());
    private final Map<UUID, Scoreboard> players = Collections.synchronizedMap(new HashMap<>());

    public ScoreboardManager(final ScorePrefix plugin) {
        this.plugin = plugin;
        try {
            this.load();
            Bukkit.getPluginManager().registerEvents(this, plugin);
            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::update, 0L, 10L);
        }
        catch (final Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize Scoreboard", e);
        }
    }

    private void load() throws IOException {
        final File dir = new File(plugin.getDataFolder(), "scoreboards");
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Failed to create Scoreboards directory");
        if (dir.isFile())
            throw new IOException("The Scoreboards directory does not exist");
        final File defaultFile = new File(dir, "scoreboard.yml");
        if (!defaultFile.exists()) {
            if (!defaultFile.getParentFile().exists() && !defaultFile.getParentFile().mkdirs())
                throw new IOException("Failed to create parent directory");
            this.plugin.saveResource("scoreboards/scoreboard.yml", false);
        }
        for (final File file : Objects.requireNonNull(dir.listFiles((parent, name) -> name.endsWith(".yml")))) {
            if (!file.isDirectory()) {
                try {
                    this.scoreboards.add(new Scoreboard(this.plugin, file));
                }
                catch (final ScorePrefix.DisabledException ignored) {}
                catch (final Exception e) {
                    this.plugin.getLogger().log(Level.SEVERE, "Failed to initialize Scoreboard", e);
                }
            }
        }
    }

    private Optional<Scoreboard> getScoreboard(final Player player) {
        final Optional<Scoreboard> opt = this.scoreboards.stream().filter(score -> score.checkConditions(player) == 1).findFirst();
        if (opt.isEmpty()) {
            return this.scoreboards.stream().filter(score -> score.checkConditions(player) == 2).findFirst();
        }
        return opt;
    }

    public void update() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final Scoreboard scoreboard = this.players.get(player.getUniqueId());
            if (scoreboard == null) {
                this.getScoreboard(player).ifPresent(score -> {
                    score.add(player);
                    this.players.put(player.getUniqueId(), score);
                });
            }
            else if (scoreboard.checkConditions(player) == 0) {
                scoreboard.remove(player);
                this.getScoreboard(player).ifPresent(score -> {
                    score.add(player);
                    this.players.put(player.getUniqueId(), score);
                });
            }
            else {
                if (scoreboard.checkConditions(player) != 2) {
                    continue;
                }
                final Optional<Scoreboard> opt = this.scoreboards.stream().filter(score -> score.checkConditions(player) == 1).findFirst();
                opt.ifPresent(score -> {
                    scoreboard.remove(player);
                    score.add(player);
                    this.players.put(player.getUniqueId(), score);
                });
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.getScoreboard(player).ifPresent(scoreboard -> {
            scoreboard.add(player);
            this.players.put(player.getUniqueId(), scoreboard);
        });
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.scoreboards.forEach(scoreboard -> scoreboard.remove(event.getPlayer()));
        this.players.remove(event.getPlayer().getUniqueId());
    }

    public void reload() throws IOException {
        this.scoreboards.forEach(Scoreboard::destroy);
        this.scoreboards.clear();
        this.players.clear();
        this.load();
    }
}
