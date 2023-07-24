package de.crazydev22.scoreprefix;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JoinQuitManager implements Listener {
	private final ScorePrefix plugin;

	public JoinQuitManager(ScorePrefix plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public void reload() {
		try {
			plugin.getConfig().load(plugin.file);
			plugin.getConfig();
		} catch (IOException | InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	@EventHandler
	public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
		var msg = plugin.getConfig().getString(event.getPlayer().hasPlayedBefore() ? "join-message" : "first-join", "null");
		event.setJoinMessage(msg.equalsIgnoreCase("null") ? null : msg);
	}

	@EventHandler
	public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
		var msg = plugin.getConfig().getString("quit-message", "null");
		event.setQuitMessage(msg.equalsIgnoreCase("null") ? null : msg);
	}
}
