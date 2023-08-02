package de.crazydev22.scoreprefix;

import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PrefixManager implements Listener {
    private final ScorePrefix plugin;
    private final boolean useExisting;
    private Scoreboard scoreboard;

    public PrefixManager(ScorePrefix plugin, boolean useExisting) {
        this.plugin = plugin;
        this.useExisting = useExisting;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        var interval = plugin.getConfig().getInt("prefix.update", -1);
        if (interval > 0) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () ->
                    Bukkit.getOnlinePlayers().forEach(this::updateTablistRanks),
                    0, interval);
        }

        LuckPermsAPI.getAPI().getEventBus().subscribe(UserDataRecalculateEvent.class, event ->
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
                    if(player != null) {
                        TeamEntry entry = TeamEntry.get(player);
                        if(entry != null) {
                            updateTablistRanks(player);
                        }
                    }
                }, 10));
    }

    public void reload() {
        try {
            plugin.getConfig().load(plugin.file);
            plugin.getConfig();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        var player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TeamEntry.register(player);
            setTablistRanks(player);
        }, 2);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(final AsyncPlayerChatEvent event) {
        var player = event.getPlayer();
        var entry = TeamEntry.get(player);
        var format = plugin.getConfig().getString("prefix.chat-format", "%prefix% %display_name% §7» %message%");
        format = ScorePrefix.process(player, format)
                .replaceAll("%prefix%", entry != null ? entry.getPrefix() : "")
                .replaceAll("%suffix%", entry != null ? entry.getSuffix() : "")
                .replaceAll("%display_name%", player.getDisplayName())
                .replaceAll("%name%", player.getName())
                .replaceAll("%message%", event.getMessage());
        event.setFormat(format);
    }

    public void updateTablistRanks(Player player) {
        TeamEntry.register(player);
        try {
            TeamEntry teams = TeamEntry.get(player);
            if(teams != null)
                send(player, teams);
        } catch (Exception e) {
            plugin.getLogger().warning("There was an error whilst updating "+player.getName()+"'s rank!");
        }
    }

    public void setTablistRanks(@NotNull Player player) {
        for(Player all : Bukkit.getOnlinePlayers()) {
            if(all != player) {
                TeamEntry entry = TeamEntry.get(all);
                if(entry != null) {
                    updateScoreboard(player);
                    var name = entry.getTeamName();
                    Team team = player.getScoreboard().getTeam(name);
                    if(team == null)
                        team = player.getScoreboard().registerNewTeam(name);
                    setPrefixSuffix(player, team, entry.getPrefix(), entry.getSuffix());

                    team.addEntry(all.getName());
                } else
                    plugin.getLogger().warning("Did not set "+all.getName()+"'s rank for player "+player.getName());
            }
        }

        TeamEntry entry = TeamEntry.get(player);
        if(entry != null) {
            send(player, entry);
        } else
            plugin.getLogger().warning("Did not set "+plugin.getName()+"'s rank for the already online players");
    }

    private void send(@NotNull Player player, TeamEntry entry) {
        for(Player target : Bukkit.getOnlinePlayers()) {
            updateScoreboard(target);
            var name = entry.getTeamName();
            Team team = target.getScoreboard().getTeam(name);
            if(team == null)
                team = target.getScoreboard().registerNewTeam(name);

            setPrefixSuffix(player, team, entry.getPrefix(), entry.getSuffix());
            team.addEntry(player.getName());
        }
    }

    private void updateScoreboard(Player player) {
        if (!useExisting) {
            if (scoreboard == null)
                scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
            if (player.getScoreboard() != scoreboard)
                player.setScoreboard(scoreboard);
        }
    }

    public void setPrefixSuffix(@NotNull Player player, @NotNull Team team, @NotNull String prefix, @NotNull String suffix) {
        if (prefix.length() != 0)
            team.setPrefix(prefix + (plugin.getConfig().getBoolean("prefix.add-prefix-space") ? " " : ""));
        if (suffix.length() != 0)
            team.setSuffix(suffix + (plugin.getConfig().getBoolean("prefix.add-suffix-space") ? " " : ""));
        player.setPlayerListName(null);
    }

    public static class TeamEntry {
        private static final Map<UUID, TeamEntry> teams = new HashMap<>();
        private String prefix = "";
        private String suffix = "";
        private int weight = 0;

        public static void register(@NotNull Player player) {
            var uuid = player.getUniqueId();
            if (!teams.containsKey(uuid))
                teams.put(uuid, new TeamEntry());
            teams.get(uuid).set(
                    LuckPermsAPI.getPrefix(player),
                    LuckPermsAPI.getSuffix(player),
                    LuckPermsAPI.getWeight(player)
            );
        }

        public static TeamEntry get(@NotNull Player player) {
            return teams.get(player.getUniqueId());
        }

        public void set(@NotNull String prefix, @NotNull String suffix, int weight) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.weight = weight;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public String getTeamName() {
            return String.format("%010d", weight) + "team-" + teams.size();
        }
    }
}
