package de.crazydev22.scoreprefix;

import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class PrefixManager implements Listener {
    private final HashMap<UUID, String> teams = new HashMap<>();
    private final ScorePrefix plugin;
    private Scoreboard prefixBoard;

    public PrefixManager(final ScorePrefix plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.update(player);
            }
        }, 0L, 10L);
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.update(event.getPlayer()), 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(final AsyncPlayerChatEvent event) {
        event.setFormat("%s §7» %s");
    }

    private void update(final Player player) {
        if (this.prefixBoard == null)
            this.prefixBoard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
        if (!player.getScoreboard().equals(this.prefixBoard))
            player.setScoreboard(this.prefixBoard);
        final String id = this.getTeam(player.getUniqueId(), this.getWeight(player));
        final String prefix = this.getPrefix(player);
        final String suffix = this.getSuffix(player);
        Team team = this.prefixBoard.getTeam(id);
        if (team == null) team = this.prefixBoard.registerNewTeam(id);
        String name = player.getName();
        if (prefix != null) {
            if (!team.getPrefix().equals(prefix))
                team.setPrefix(prefix);
            name = prefix + " " + name;
        }
        if (suffix != null) {
            if (!team.getSuffix().equals(" " + suffix))
                team.setSuffix(" " + suffix);
            name = name + " " + suffix;
        }
        if (!Objects.equals(player.getDisplayName(), name))
            player.setDisplayName(name);
        if (!Objects.equals(player.getCustomName(), name))
            player.setCustomName(name);
        if (!team.getEntries().contains(player.getName()))
            team.addEntry(player.getName());
    }

    private int getWeight(final Player player) {
        return LuckPermsAPI.getGroup(player).getWeight().orElse(0);
    }

    private String getPrefix(final Player player) {
        String prefix = LuckPermsAPI.getUser(player).getCachedData().getMetaData().getPrefix();
        final Group group = LuckPermsAPI.getGroup(player);
        if (prefix == null && group != null) {
            prefix = group.getCachedData().getMetaData().getPrefix();
        }
        return ScorePrefix.process(player, prefix);
    }

    private String getSuffix(final Player player) {
        String prefix = LuckPermsAPI.getUser(player).getCachedData().getMetaData().getSuffix();
        final Group group = LuckPermsAPI.getGroup(player);
        if (prefix == null && group != null) {
            prefix = group.getCachedData().getMetaData().getSuffix();
        }
        return ScorePrefix.process(player, prefix);
    }

    private String getTeam(final UUID uuid, int weight) {
        weight = Integer.MAX_VALUE - weight;
        this.teams.remove(uuid);
        int i = 1;
        String id = String.format("%010d", weight) + "team-0";
        while (this.teams.containsValue(id)) {
            id = String.format("%010d", weight) + "team-" + i;
            ++i;
        }
        this.teams.put(uuid, id);
        return id;
    }
}
