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
import java.util.Optional;
import java.util.UUID;

public class PrefixManager implements Listener {
    private final HashMap<UUID, TeamEntry> teams = new HashMap<>();
    private final ScorePrefix plugin;

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
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.update(event.getPlayer()), 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(final AsyncPlayerChatEvent event) {
        event.setFormat("%s §7» %s");
    }

    private void update(final Player player) {
        var scoreboard = player.getScoreboard();
        var entry = this.getTeam(player.getUniqueId(), this.getWeight(player));
        entry.set(this.getPrefix(player), this.getSuffix(player));
        Team team = scoreboard.getTeam(entry.getId());
        if (team == null) team = scoreboard.registerNewTeam(entry.getId());

        String name = player.getName();
        if (!team.getPrefix().equals(entry.getPrefix())) team.setPrefix(entry.getPrefix());
        name = entry.getPrefix() + name;
        if (!team.getSuffix().equals(entry.getSuffix())) team.setSuffix(entry.getSuffix());
        name = name + entry.getSuffix();
        if (!Objects.equals(player.getDisplayName(), name)) player.setDisplayName(name);
        if (!Objects.equals(player.getCustomName(), name)) player.setCustomName(name);
        if (!team.getEntries().contains(player.getName())) team.addEntry(player.getName());

        teams.forEach(((uuid, teamEntry) -> {
            var target = Bukkit.getPlayer(uuid);
            if (target == null || teamEntry == entry) return;
            Team sT = scoreboard.getTeam(teamEntry.getId());
            if (sT == null) sT = scoreboard.registerNewTeam(teamEntry.getId());

            if (!Objects.equals(sT.getPrefix(), teamEntry.getPrefix()))
                sT.setPrefix(teamEntry.getPrefix());
            if (!Objects.equals(sT.getSuffix(), teamEntry.getSuffix()))
                sT.setSuffix(teamEntry.getSuffix());
            if (!sT.getEntries().contains(target.getName()))
                sT.addEntry(player.getName());
        }));
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

    private TeamEntry getTeam(final UUID uuid, int weight) {
        weight = Integer.MAX_VALUE - weight;
        Optional.ofNullable(this.teams.remove(uuid)).ifPresent(TeamEntry::destroy);
        var teams = this.teams.values().stream().map(TeamEntry::getId).toList();
        int i = 1;
        String id = String.format("%010d", weight) + "team-0";
        while (teams.contains(id)) {
            id = String.format("%010d", weight) + "team-" + i;
            ++i;
        }
        TeamEntry entry = new TeamEntry(id);
        this.teams.put(uuid, entry);
        return entry;
    }

    private static class TeamEntry {
        private final String id;
        private String prefix = "";
        private String suffix = "";

        TeamEntry(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void set(String prefix, String suffix) {
            this.prefix = prefix != null ? prefix : "";
            this.suffix = suffix != null ? suffix : "";
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public void destroy() {
            prefix = null;
            suffix = null;
            System.gc();
        }
    }
}
