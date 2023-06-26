package de.crazydev22.scoreprefix.scoreboard;

import de.crazydev22.scoreprefix.ScorePrefix;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Level;

public class ScoreboardManager implements Listener {
    private final ScorePrefix plugin;
    private final List<Scoreboard> scoreboards = Collections.synchronizedList(new ArrayList<>());
    private final Map<UUID, Scoreboard> players = Collections.synchronizedMap(new HashMap<>());

    public ScoreboardManager(final ScorePrefix plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(Bukkit.getPluginCommand("sb")).setExecutor(this::command);
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
        final File dir = this.plugin.getDataFolder();
        if (!dir.exists()) {
            dir.getParentFile().mkdirs();
        }
        if (dir.isFile()) {
            throw new IOException("The Scoreboards directory does not exist");
        }
        final File defaultFile = new File(dir, "scoreboard.yml");
        if (!defaultFile.exists()) {
            defaultFile.getParentFile().mkdirs();
            this.plugin.saveResource("scoreboard.yml", false);
        }
        for (final File file : Objects.requireNonNull(dir.listFiles())) {
            if (!file.isDirectory()) {
                try {
                    this.scoreboards.add(new Scoreboard(this.plugin, file));
                }
                catch (final Scoreboard.DisabledException ignored) {}
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

    public boolean command(@NotNull final CommandSender sender, @NotNull final Command cmd, @NotNull final String label, final String[] args) {
        if (!sender.hasPermission("scoreprefix.command")) {
            return false;
        }
        if (args.length == 1) {
            if (args[0].equals("reload")) {
                this.scoreboards.forEach(Scoreboard::destroy);
                this.scoreboards.clear();
                this.players.clear();
                try {
                    this.load();
                    sender.sendMessage("Done!");
                } catch (final Exception e) {
                    final StringWriter writer = new StringWriter();
                    e.printStackTrace(new PrintWriter(writer));
                    sender.sendMessage(writer.toString());
                }
            }
        }
        return true;
    }
}
