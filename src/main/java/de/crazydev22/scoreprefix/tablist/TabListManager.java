package de.crazydev22.scoreprefix.tablist;

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

public class TabListManager implements Listener {
    private final ScorePrefix plugin;
    private final List<TabList> tabLists = Collections.synchronizedList(new ArrayList<>());
    private final Map<UUID, TabList> players = Collections.synchronizedMap(new HashMap<>());

    public TabListManager(final ScorePrefix plugin) {
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
        final File dir = new File(this.plugin.getDataFolder(), "tablists");
        if (!dir.exists()) {
            dir.getParentFile().mkdirs();
        }
        if (dir.isFile()) {
            throw new IOException("The TabLists directory does not exist");
        }
        final File defaultFile = new File(dir, "tablist.yml");
        if (!defaultFile.exists()) {
            defaultFile.getParentFile().mkdirs();
            this.plugin.saveResource("tablists/tablist.yml", false);
        }
        for (final File file : Objects.requireNonNull(dir.listFiles())) {
            if (!file.isDirectory()) {
                try {
                    this.tabLists.add(new TabList(this.plugin, file));
                }
                catch (final ScorePrefix.DisabledException ignored) {}
                catch (final Exception e) {
                    this.plugin.getLogger().log(Level.SEVERE, "Failed to initialize TabList", e);
                }
            }
        }
    }

    private Optional<TabList> getTabList(final Player player) {
        final Optional<TabList> opt = this.tabLists.stream().filter(score -> score.checkConditions(player) == 1).findFirst();
        if (opt.isEmpty()) {
            return this.tabLists.stream().filter(score -> score.checkConditions(player) == 2).findFirst();
        }
        return opt;
    }

    public void update() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            TabList tabList = this.players.get(player.getUniqueId());
            if (tabList == null) {
                this.getTabList(player).ifPresent(tab -> {
                    tab.add(player);
                    this.players.put(player.getUniqueId(), tab);
                });
            }
            else if (tabList.checkConditions(player) == 0) {
                tabList.remove(player);
                this.getTabList(player).ifPresent(tab -> {
                    tab.add(player);
                    this.players.put(player.getUniqueId(), tab);
                });
            }
            else {
                if (tabList.checkConditions(player) != 2) {
                    continue;
                }
                final Optional<TabList> opt = this.tabLists.stream().filter(score -> score.checkConditions(player) == 1).findFirst();
                opt.ifPresent(tab -> {
                    tabList.remove(player);
                    tab.add(player);
                    this.players.put(player.getUniqueId(), tab);
                });
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.getTabList(player).ifPresent(tabList -> {
            tabList.add(player);
            this.players.put(player.getUniqueId(), tabList);
        });
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.tabLists.forEach(tabList -> tabList.remove(event.getPlayer()));
        this.players.remove(event.getPlayer().getUniqueId());
    }

    public void reload() throws IOException {
        this.tabLists.forEach(TabList::destroy);
        this.tabLists.clear();
        this.players.clear();
        this.load();
    }
}
